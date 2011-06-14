//$Id: OneToManyPersister.java,v 1.10 2005/03/24 18:33:23 oneovthafew Exp $
package org.hibernate.persister.collection;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cache.CacheConcurrencyStrategy;
import org.hibernate.cache.CacheException;
import org.hibernate.cfg.Configuration;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.loader.collection.BatchingCollectionInitializer;
import org.hibernate.loader.collection.CollectionInitializer;
import org.hibernate.mapping.Collection;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.sql.Update;
import org.hibernate.util.ArrayHelper;

/**
 * Collection persister for one-to-many associations.
 *
 * @author Gavin King
 */
public class OneToManyPersister extends AbstractCollectionPersister {

	private final boolean cascadeDeleteEnabled;
	private final boolean keyIsNullable;
	private final boolean keyIsUpdateable;

	protected boolean isRowDeleteEnabled() {
		return keyIsUpdateable && keyIsNullable;
	}

	protected boolean isRowInsertEnabled() {
		return keyIsUpdateable;
	}

	public boolean isCascadeDeleteEnabled() {
		return cascadeDeleteEnabled;
	}

	public OneToManyPersister(Collection collection,
							  CacheConcurrencyStrategy cache,
							  Configuration cfg,
							  SessionFactoryImplementor factory)
			throws MappingException, CacheException {
		super( collection, cache, cfg, factory );
		cascadeDeleteEnabled = collection.getKey().isCascadeDeleteEnabled() &&
				factory.getDialect().supportsCascadeDelete();
		keyIsNullable = collection.getKey().isNullable();
		keyIsUpdateable = collection.getKey().isUpdateable();
	}

	/**
	 * Generate the SQL UPDATE that updates all the foreign keys to null
	 */
	protected String generateDeleteString() {
		
		Update update = new Update()
				.setTableName( qualifiedTableName )
				.addColumns( keyColumnNames, "null" )
				.setPrimaryKeyColumnNames( keyColumnNames );
		
		if ( hasIndex && !indexIsFormula ) update.addColumns( indexColumnNames, "null" );
		
		if ( hasWhere ) update.setWhere( sqlWhereString );
		
		if ( getFactory().getSettings().isCommentsEnabled() ) {
			update.setComment( "delete one-to-many " + getRole() );
		}
		
		return update.toStatementString();
	}

	/**
	 * Generate the SQL UPDATE that updates a foreign key to a value
	 */
	protected String generateInsertRowString() {
		
		Update update = new Update()
				.setTableName( qualifiedTableName )
				.addColumns( keyColumnNames );
		
		if ( hasIndex && !indexIsFormula ) update.addColumns( indexColumnNames );
		
		//identifier collections not supported for 1-to-many
		if ( getFactory().getSettings().isCommentsEnabled() ) {
			update.setComment( "create one-to-many row " + getRole() );
		}
		
		return update.setPrimaryKeyColumnNames( elementColumnNames )
				.toStatementString();
	}

	/**
	 * Not needed for one-to-many association
	 */
	protected String generateUpdateRowString() {
		return null;
	}

	/**
	 * Generate the SQL UPDATE that updates a particular row's foreign
	 * key to null
	 */
	protected String generateDeleteRowString() {
		
		Update update = new Update()
				.setTableName( qualifiedTableName )
				.addColumns( keyColumnNames, "null" );
		
		if ( hasIndex && !indexIsFormula ) update.addColumns( indexColumnNames, "null" );
		
		if ( getFactory().getSettings().isCommentsEnabled() ) {
			update.setComment( "delete one-to-many row " + getRole() );
		}
		
		//use a combination of foreign key columns and pk columns, since
		//the ordering of removal and addition is not guaranteed when
		//a child moves from one parent to another
		String[] rowSelectColumnNames = ArrayHelper.join(keyColumnNames, elementColumnNames);
		return update.setPrimaryKeyColumnNames( rowSelectColumnNames )
				.toStatementString();
	}

	public boolean consumesAlias() {
		return true;
	}

	public boolean isOneToMany() {
		return true;
	}

	public boolean isManyToMany() {
		return false;
	}

	protected int doUpdateRows(Serializable id, PersistentCollection collection, SessionImplementor session)
			throws HibernateException {

		// we finish all the "removes" first to take care of possible unique
		// constraints and so that we can take better advantage of batching
		
		try {
			int count = 0;
			if ( isRowDeleteEnabled() ) {
				// update removed rows fks to null
				try {
					PreparedStatement st = null;
					int i = 0;
	
					Iterator entries = collection.entries(this);
					int offset = 1;
					while ( entries.hasNext() ) {
	
						Object entry = entries.next();
						if ( collection.needsUpdating( entry, i, elementType ) ) {  // will still be issued when it used to be null
							if ( st == null ) {
								if ( isDeleteCallable() ) {
									CallableStatement callstatement = session.getBatcher()
										.prepareBatchCallableStatement( getSQLDeleteRowString() );
									callstatement.registerOutParameter( offset++, Types.NUMERIC ); // TODO: should we require users to return number of update rows ? (we cant make it return this without changing collectionpersister interface)
									st = callstatement;
								}
								else {
									st = session.getBatcher().prepareBatchStatement( getSQLDeleteRowString() );
								}
							}
							int loc = writeKey( st, id, offset, session );
							writeElementToWhere( st, collection.getSnapshotElement(entry, i), loc, session );
							session.getBatcher().addToBatch( -1 );
							count++;
						}
						i++;
					}
				}
				catch ( SQLException sqle ) {
					session.getBatcher().abortBatch( sqle );
					throw sqle;
				}
			}
			
			if ( isRowInsertEnabled() ) {
				// now update all changed or added rows fks
				try {
					PreparedStatement st = null;
					int i = 0;
					Iterator entries = collection.entries(this);
					while ( entries.hasNext() ) {
						Object entry = entries.next();
						int offset = 1;
						if ( collection.needsUpdating( entry, i, elementType ) ) {
							if ( st == null ) {
								st = session.getBatcher()
									.prepareBatchStatement( getSQLInsertRowString() );
							}
							int loc = writeKey( st, id, offset, session );
							if ( hasIndex && !indexIsFormula ) {
								loc = writeIndexToWhere(st, collection.getIndex(entry, i, this), loc, session );
							}
							writeElementToWhere( st, collection.getElement(entry), loc, session );
							session.getBatcher().addToBatch( 1 );
							count++;
						}
						i++;
					}
				}
				catch ( SQLException sqle ) {
					session.getBatcher().abortBatch( sqle );
					throw sqle;
				}
			}
			
			return count;
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
					getSQLExceptionConverter(),
					sqle,
					"could not update collection rows: " + 
					MessageHelper.collectionInfoString( this, id, getFactory() ),
					getSQLInsertRowString()
			);
		}
	}

	public String selectFragment(String alias, String suffix, boolean includeCollectionColumns) {
		StringBuffer buf = new StringBuffer();
		if ( includeCollectionColumns ) {
			buf.append( selectFragment( alias ) )//ignore suffix for collection columns!
					.append( ", " );
		}
		OuterJoinLoadable ojl = ( OuterJoinLoadable ) getElementPersister();
		return buf.append( ojl.selectFragment( alias, suffix ) )//use suffix for the entity columns
				.toString();
	}

	/**
	 * Create the <tt>OneToManyLoader</tt>
	 *
	 * @see org.hibernate.loader.collection.OneToManyLoader
	 */
	protected CollectionInitializer createCollectionInitializer(java.util.Map enabledFilters) throws MappingException {
		return BatchingCollectionInitializer.createBatchingOneToManyInitializer( this, batchSize, getFactory(), enabledFilters );
	}

	public String fromJoinFragment(String alias,
								   boolean innerJoin,
								   boolean includeSubclasses) {
		return ( ( Joinable ) getElementPersister() ).fromJoinFragment( alias, innerJoin, includeSubclasses );
	}

	public String whereJoinFragment(String alias,
									boolean innerJoin,
									boolean includeSubclasses) {
		return ( ( Joinable ) getElementPersister() ).whereJoinFragment( alias, innerJoin, includeSubclasses );
	}

	public String getTableName() {
		return ( ( Joinable ) getElementPersister() ).getTableName();
	}

	public String filterFragment(String alias) throws MappingException {
		String result = super.filterFragment( alias );
		if ( getElementPersister() instanceof Joinable ) {
			result += ( ( Joinable ) getElementPersister() ).oneToManyFilterFragment( alias );
		}
		return result;

	}

}
