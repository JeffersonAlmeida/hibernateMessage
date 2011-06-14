//$Id: DefaultRefreshEventListener.java,v 1.5 2005/02/22 03:09:34 oneovthafew Exp $
package org.hibernate.event.def;

import org.hibernate.HibernateException;
import org.hibernate.PersistentObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.cache.CacheKey;
import org.hibernate.engine.Cascades;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.event.RefreshEvent;
import org.hibernate.event.RefreshEventListener;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Defines the default refresh event listener used by hibernate for refreshing entities
 * in response to generated refresh events.
 *
 * @author Steve Ebersole
 */
public class DefaultRefreshEventListener extends AbstractEventListener implements RefreshEventListener {

	private static final Log log = LogFactory.getLog(DefaultRefreshEventListener.class);

	/** 
	 * Handle the given refresh event.
	 *
	 * @param event The refresh event to be handled.
	 * @throws HibernateException
	 */
	public void onRefresh(RefreshEvent event) throws HibernateException {

		final SessionImplementor source = event.getSession();
		
		if ( source.getPersistenceContext().reassociateIfUninitializedProxy( event.getObject() ) ) return;

		final Object object = source.getPersistenceContext().unproxyAndReassociate( event.getObject() );
		EntityEntry e = source.getPersistenceContext().removeEntry(object);

		final EntityPersister persister;
		final Serializable id;
		if ( e == null ) {
			persister = source.getEntityPersister(null, object); //refresh() does not pass an entityName
			id = persister.getIdentifier( object, event.getSession().getEntityMode() );

			if ( log.isTraceEnabled() )
				log.trace(
						"refreshing transient " +
						MessageHelper.infoString( persister, id, source.getFactory() )
				);
			if ( source.getPersistenceContext().getEntry( new EntityKey( id, persister, source.getEntityMode() ) ) != null ) {
				throw new PersistentObjectException(
						"attempted to refresh transient instance when persistent instance was already associated with the Session: " +
						MessageHelper.infoString(persister, id, source.getFactory() )
				);
			}
		}
		else {
			if ( log.isTraceEnabled() )
				log.trace(
						"refreshing " +
						MessageHelper.infoString( e.getPersister(), e.getId(), source.getFactory()  )
				);
			if ( !e.isExistsInDatabase() ) throw new HibernateException( "this instance does not yet exist as a row in the database" );

			persister = e.getPersister();
			id = e.getId();
			EntityKey key = new EntityKey( id, persister, source.getEntityMode() );
			source.getPersistenceContext().removeEntity(key);
			if ( persister.hasCollections() ) new EvictVisitor( source ).process(object, persister);
		}

		if ( persister.hasCache() ) {
			final CacheKey ck = new CacheKey( 
					id, 
					persister.getIdentifierType(), 
					persister.getRootEntityName(), 
					source.getEntityMode() 
			);
			persister.getCache().remove(ck);
		}
		evictCachedCollections( persister, id, source.getFactory() );
		Object result = persister.load( id, object, event.getLockMode(), source );
		UnresolvableObjectException.throwIfNull( result, id, persister.getEntityName() );
		
		Cascades.cascade( source, persister, object, Cascades.ACTION_REFRESH, Cascades.CASCADE_AFTER_REFRESH );
	}

	/**
	 * Evict collections from the factory-level cache
	 */
	private void evictCachedCollections(EntityPersister persister, Serializable id, SessionFactoryImplementor factory)
	throws HibernateException {
		evictCachedCollections( persister.getPropertyTypes(), id, factory );
	}

	private void evictCachedCollections(Type[] types, Serializable id, SessionFactoryImplementor factory)
	throws HibernateException {
		for ( int i = 0; i < types.length; i++ ) {
			if ( types[i].isCollectionType() ) {
				factory.evictCollection( ( (CollectionType) types[i] ).getRole(), id );
			}
			else if ( types[i].isComponentType() ) {
				AbstractComponentType actype = (AbstractComponentType) types[i];
				evictCachedCollections( actype.getSubtypes(), id, factory );
			}
		}
	}

}
