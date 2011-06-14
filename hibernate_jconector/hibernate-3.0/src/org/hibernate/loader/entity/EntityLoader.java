//$Id: EntityLoader.java,v 1.4 2005/03/21 17:55:24 oneovthafew Exp $
package org.hibernate.loader.entity;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.loader.AbstractEntityLoader;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;
import org.hibernate.util.StringHelper;

/**
 * Load an entity using outerjoin fetching to fetch associated entities.
 * <br>
 * The <tt>EntityPersister</tt> must implement <tt>Loadable</tt>. For other entities,
 * create a customized subclass of <tt>Loader</tt>.
 *
 * @see SimpleEntityLoader
 * @author Gavin King
 */
public class EntityLoader extends AbstractEntityLoader 
	implements UniqueEntityLoader {
	
	private static final Log log = LogFactory.getLog(EntityLoader.class);

	private final Type uniqueKeyType;
	private final boolean batchLoader;
	private final String entityName;
	private final LockMode lockMode;

	public EntityLoader(
			OuterJoinLoadable persister, 
			LockMode lockMode,
			SessionFactoryImplementor factory, 
			Map enabledFilters) 
	throws MappingException {
		this(persister, 1, lockMode, factory, enabledFilters);
	}
	
	public EntityLoader(
			OuterJoinLoadable persister, 
			int batchSize, 
			LockMode lockMode,
			SessionFactoryImplementor factory, 
			Map enabledFilters) 
	throws MappingException {
		this( 
			persister, 
			persister.getIdentifierColumnNames(), 
			persister.getIdentifierType(), 
			batchSize,
			lockMode,
			false,
			factory, 
			enabledFilters 
		);
	}
	
	public EntityLoader(
			OuterJoinLoadable persister, 
			String[] uniqueKey, 
			Type uniqueKeyType, 
			int batchSize, 
			LockMode lockMode,
			SessionFactoryImplementor factory, 
			Map enabledFilters) 
	throws MappingException {
		this(
			persister,
			uniqueKey,
			uniqueKeyType,
			batchSize,
			lockMode,
			true, //we include extra filter conditions only when not loading by pk
			factory,
			enabledFilters
		);
	}

	public EntityLoader(
			OuterJoinLoadable persister, 
			String[] uniqueKey, 
			Type uniqueKeyType, 
			int batchSize, 
			LockMode lockMode,
			boolean includeFilter,
			SessionFactoryImplementor factory, 
			Map enabledFilters) 
	throws MappingException {
		super(persister, factory, enabledFilters);

		this.uniqueKeyType = uniqueKeyType;
		this.entityName = persister.getEntityName();
		this.lockMode = lockMode;
		
		StringBuffer whereCondition = whereString( getAlias(), uniqueKey, batchSize, null );
		if (includeFilter) {
			String filter = persister.filterFragment( getAlias(), enabledFilters );
			whereCondition.insert( 0, StringHelper.moveAndToBeginning( filter ) );
		}

		initAll( whereCondition.toString(), "", lockMode );

		postInstantiate();

		batchLoader = batchSize > 1;
		
		log.debug( "Static select for entity " + entityName + ": " + getSQLString() );

	}

	public Object load(Serializable id, Object optionalObject, SessionImplementor session) 
	throws HibernateException {
		return load(session, id, optionalObject, id);
	}

	public Object loadByUniqueKey(SessionImplementor session, Object key) 
	throws HibernateException {
		return load(session, key, null, null);
	}

	private Object load(
			SessionImplementor session, 
			Object id, 
			Object optionalObject, 
			Serializable optionalId) 
	throws HibernateException {
		
		List list = loadEntity(
				session, 
				id, 
				uniqueKeyType, 
				optionalObject, 
				entityName, 
				optionalId, 
				getPersister()
			);
		
		if ( list.size()==1 ) {
			return list.get(0);
		}
		else if ( list.size()==0 ) {
			return null;
		}
		else {
			if ( getCollectionOwner()>-1 ) {
				return list.get(0);
			}
			else {
				throw new HibernateException(
					"More than one row with the given identifier was found: " +
					id +
					", for class: " +
					getPersister().getEntityName()
				);
			}
		}
		
	}

	protected Object getResultColumnOrRow(
		Object[] row,
		ResultSet rs,
		SessionImplementor session)
	throws SQLException, HibernateException {
		return row[row.length-1];
	}

	protected boolean isSingleRowLoader() {
		return !batchLoader;
	}

	/**
	 * Disable outer join fetching if this loader obtains an
	 * upgrade lock mode
	 */
	protected boolean isJoinedFetchEnabled(AssociationType type, FetchMode config) {
		return lockMode.greaterThan(LockMode.READ) ?
			false :
			super.isJoinedFetchEnabled(type, config);
	}

	public String getComment() {
		return "load " + getPersister().getEntityName();
	}
	
}