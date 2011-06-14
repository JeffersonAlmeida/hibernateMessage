//$Id: BasicCollectionPersister.java,v 1.8 2005/03/18 02:32:23 oneovthafew Exp $
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
import org.hibernate.pretty.MessageHelper;
import org.hibernate.sql.Delete;
import org.hibernate.sql.Insert;
import org.hibernate.sql.Update;
import org.hibernate.util.ArrayHelper;

/**
 * Collection persister for collections of values and many-to-many associations.
 *
 * @author Gavin King
 */
public class BasicCollectionPersister extends AbstractCollectionPersister {

	public boolean isCascadeDeleteEnabled() {
		return false;
	}

	public BasicCollectionPersister(Collection collection,
									CacheConcurrencyStrategy cache,
									Configuration cfg,
									SessionFactoryImplementor factory)
			throws MappingException, CacheException {
		super( collection, cache, cfg, factory );
	}

	/**
	 * Generate the SQL DELETE that deletes all rows
	 */
	protected String generateDeleteString() {
		
		Delete delete = new Delete()
				.setTableName( qualifiedTableName )
				.setPrimaryKeyColumnNames( keyColumnNames );
		
		if ( hasWhere ) delete.setWhere( sqlWhereString );
		
		if ( getFactory().getSettings().isCommentsEnabled() ) {
			delete.setComment( "delete collection " + getRole() );
		}
		
		return delete.toStatementString();
	}

	/**
	 * Generate the SQL INSERT that creates a new row
	 */
	protected String generateInsertRowString() {
		
		Insert insert = new Insert( null )
				.setTableName( qualifiedTableName )
				.addColumns( keyColumnNames );
		
		if ( hasIdentifier) insert.addColumn( identifierColumnName );
		
		if ( hasIndex /*&& !indexIsFormula*/ ) {
			insert.addColumns( indexColumnNames, indexColumnIsSettable );
		}
		
		if ( getFactory().getSettings().isCommentsEnabled() ) {
			insert.setComment( "insert collection row " + getRole() );
		}
		
		//if ( !elementIsFormula ) {
			insert.addColumns( elementColumnNames, elementColumnIsSettable );
		//}
		
		return insert.toStatementString();
	}

	/**
	 * Generate the SQL UPDATE that updates a row
	 */
	protected String generateUpdateRowString() {
		
		Update update = new Update()
			.setTableName( qualifiedTableName );
		
		//if ( !elementIsFormula ) {
			update.addColumns( elementColumnNames, elementColumnIsSettable );
		//}
		
		if ( hasIdentifier ) {
			update.setPrimaryKeyColumnNames( new String[]{ identifierColumnName } );
		}
		else if ( hasIndex && !indexIsFormula ) {
			update.setPrimaryKeyColumnNames( ArrayHelper.join( keyColumnNames, indexColumnNames ) );
		}
		else {
			update.setPrimaryKeyColumnNames( ArrayHelper.join( keyColumnNames, elementColumnNames ) );
		}
		
		if ( getFactory().getSettings().isCommentsEnabled() ) {
			update.setComment( "update collection row " + getRole() );
		}
		
		return update.toStatementString();
	}

	/**
	 * Generate the SQL DELETE that deletes a particular row
	 */
	protected String generateDeleteRowString() {
		
		Delete delete = new Delete()
			.setTableName( qualifiedTableName );
		
		if ( hasIdentifier ) {
			delete.setPrimaryKeyColumnNames( new String[]{ identifierColumnName } );
		}
		else if ( hasIndex && !indexIsFormula ) {
			delete.setPrimaryKeyColumnNames( ArrayHelper.join( keyColumnNames, indexColumnNames ) );
		}
		else {
			delete.setPrimaryKeyColumnNames( ArrayHelper.join( keyColumnNames, elementColumnNames ) );
		}
		
		if ( getFactory().getSettings().isCommentsEnabled() ) {
			delete.setComment( "delete collection row " + getRole() );
		}
		
		return delete.toStatementString();
	}

	public boolean consumesAlias() {
		return false;
	}

	public boolean isOneToMany() {
		return false;
	}

	public boolean isManyToMany() {
		return elementType.isEntityType(); //instanceof AssociationType;
	}

	protected int doUpdateRows(Serializable id, PersistentCollection collection, SessionImplementor session)
			throws HibernateException {
		
		if ( ArrayHelper.isAllFalse(elementColumnIsSettable) ) return 0;

		try {
			PreparedStatement st = null;
			boolean callable = isUpdateCallable();
			Iterator entries = collection.entries(this);
			try {
				int i = 0;
				int count = 0;
				while ( entries.hasNext() ) {
					int offset = 1;
					Object entry = entries.next();
					if ( collection.needsUpdating( entry, i, elementType ) ) {
						if ( st == null ) {
							if ( callable ) {
								CallableStatement callstatement = session.getBatcher()
										.prepareBatchCallableStatement( getSQLUpdateRowString() );
								callstatement.registerOutParameter( offset++, Types.NUMERIC ); // TODO: should we require users to return number of update rows ? (we cant make it return this without changing collectionpersister interface)
								st = callstatement;
							}
							else {
								st = session.getBatcher().prepareBatchStatement( getSQLUpdateRowString() );
							}
						}
						
						int loc = writeElement(st, collection.getElement(entry), offset, session );
						if ( hasIdentifier ) {
							loc = writeIdentifier(st, collection.getIdentifier(entry, i), loc, session);
						}
						else {
							loc = writeKey( st, id, loc, session );
							if ( hasIndex && !indexIsFormula ) {
								loc = writeIndexToWhere( st, collection.getIndex(entry, i, this), loc, session );
							}
							else {
								loc = writeElementToWhere( st, collection.getSnapshotElement(entry, i), loc, session );
							}
						}
						session.getBatcher().addToBatch( 1 );
						count++;
					}
					i++;
				}
				return count;
			}
			catch ( SQLException sqle ) {
				session.getBatcher().abortBatch( sqle );
				throw sqle;
			}
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
					getSQLExceptionConverter(),
					sqle,
					"could not update collection rows: " + MessageHelper.collectionInfoString( this, id, getFactory() ),
					getSQLUpdateRowString()
			);
		}
	}

	public String selectFragment(String alias, String suffix, boolean includeCollectionColumns) {
		return includeCollectionColumns ? selectFragment( alias ) : "";
	}

	/**
	 * Create the <tt>CollectionLoader</tt>
	 *
	 * @see org.hibernate.loader.collection.CollectionLoader
	 */
	protected CollectionInitializer createCollectionInitializer(java.util.Map enabledFilters)
			throws MappingException {
		return BatchingCollectionInitializer.createBatchingCollectionInitializer( this, batchSize, getFactory(), enabledFilters );
	}

	public String fromJoinFragment(String alias, boolean innerJoin, boolean includeSubclasses) {
		return "";
	}

	public String whereJoinFragment(String alias, boolean innerJoin, boolean includeSubclasses) {
		return "";
	}

}
