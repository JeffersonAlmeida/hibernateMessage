//$Id: DefaultLoadEventListener.java,v 1.6 2005/02/22 03:09:34 oneovthafew Exp $
package org.hibernate.event.def;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.ObjectDeletedException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.PersistentObjectException;
import org.hibernate.cache.CacheConcurrencyStrategy;
import org.hibernate.cache.CacheKey;
import org.hibernate.cache.entry.CacheEntry;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.Status;
import org.hibernate.engine.TwoPhaseLoad;
import org.hibernate.engine.Versioning;
import org.hibernate.event.LoadEvent;
import org.hibernate.event.LoadEventListener;
import org.hibernate.event.PostLoadEvent;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;

/**
 * Defines the default load event listeners used by hibernate for loading entities
 * in response to generated load events.
 *
 * @author Steve Ebersole
 */
public class DefaultLoadEventListener extends AbstractLockUpgradeEventListener implements LoadEventListener {

	private static final Log log = LogFactory.getLog(DefaultLoadEventListener.class);

	public static final LockMode DEFAULT_LOCK_MODE = LockMode.NONE;

	/** 
	 * Handle the given load event.
	 *
	 * @param event The load event to be handled.
	 * @return The result (i.e., the loaded entity).
	 * @throws HibernateException
	 */
	public Object onLoad(LoadEvent event, LoadEventListener.LoadType loadType) throws HibernateException {

		final SessionImplementor source = event.getSession();

		EntityPersister persister;
		if ( event.getInstanceToLoad() != null ) {
			persister = source.getEntityPersister( null, event.getInstanceToLoad() ); //the load() which takes an entity does not pass an entityName
			event.setEntityClassName( event.getInstanceToLoad().getClass().getName() );
		}
		else {
			persister = source.getFactory().getEntityPersister( event.getEntityClassName() );
		}

		if ( persister == null ) {
			throw new HibernateException( "Unable to locate persister: " + event.getEntityClassName() );
		}

		//log.debug("Got persister [" + persister + "]");

		EntityKey keyToLoad = new EntityKey( event.getEntityId(), persister, source.getEntityMode()  );

		Options options = new Options(); 
		//TODO: the options should be properties of the event,
		//      get rid of this ugly switch statement
		if ( event.getInstanceToLoad() != null ) {
			options.setAllowNulls(false);
			options.setAllowProxyCreation(false);
			options.setCheckDeleted(true);
			options.setImmediateLoad(false);
		}
		else if ( loadType == GET ) {
			options.setAllowNulls(true);
			options.setAllowProxyCreation(false);
			options.setCheckDeleted(true);
			options.setImmediateLoad(false);
		}
		else if ( loadType == LOAD ) {
			options.setAllowNulls(false);
			options.setAllowProxyCreation(true);
			options.setCheckDeleted(true);
			options.setImmediateLoad(false);
		}
		else if ( loadType == IMMEDIATE_LOAD ) {
			options.setAllowNulls(false);
			options.setAllowProxyCreation(false);
			options.setCheckDeleted(true);
			options.setImmediateLoad(true);
		}
		else if ( loadType == INTERNAL_LOAD ) {
			options.setAllowNulls(false);
			options.setAllowProxyCreation(true);
			options.setCheckDeleted(false);
			options.setImmediateLoad(false);
		}
		else if ( loadType == INTERNAL_LOAD_ONE_TO_ONE ) {
			options.setAllowNulls(false);
			options.setAllowProxyCreation(false);
			options.setCheckDeleted(false);
			options.setImmediateLoad(false);
		}

		try {
			if ( options.isImmediateLoad() ) {
				//do not return a proxy!
				//(this option inidicates we are initializing a proxy)
				return load(event, persister, keyToLoad, options);
			}
			else {
				//return a proxy if appropriate
				return event.getLockMode() == LockMode.NONE ?
					proxyOrLoad(event, persister, keyToLoad, options) :
					lockAndLoad(event, persister, keyToLoad, options, source);
			}
		}
		catch(HibernateException e) {
			log.info("Error performing load command", e);
			throw e;
		}
	}

	protected Object load(
		final LoadEvent event, 
		final EntityPersister persister, 
		final EntityKey keyToLoad, 
		final Options options)
	throws HibernateException {
		return load(event, persister, keyToLoad, options, null);
	}
	/**
	 * Perfoms the load of an entity.
	 *
	 * @return The loaded entity.
	 * @throws HibernateException
	 */
	protected Object load(
		final LoadEvent event, 
		final EntityPersister persister, 
		final EntityKey keyToLoad, 
		final Options options,
		final Object result) 
	throws HibernateException {
	
		if ( event.getInstanceToLoad() != null ) {
			if ( event.getSession().getPersistenceContext().getEntry( event.getInstanceToLoad() ) != null ) {
				throw new PersistentObjectException(
						"attempted to load into an instance that was already associated with the session: " +
						MessageHelper.infoString( persister, event.getEntityId(), event.getSession().getFactory() )
				);
			}
			persister.setIdentifier( event.getInstanceToLoad(), event.getEntityId(), event.getSession().getEntityMode() );
		}

		Object entity = doLoad(event, persister, keyToLoad, options, result);

		if ( event.getInstanceToLoad() != null ) {
			ObjectNotFoundException.throwIfNull( entity, event.getEntityId(), event.getEntityClassName() );
			if ( entity != event.getInstanceToLoad() ) {
				throw new NonUniqueObjectException( event.getEntityId(), event.getEntityClassName() );
			}
		}

		return entity;
	}

	/** 
	 * Based on configured options, will either return a pre-existing proxy,
	 * generate a new proxy, or perform an actual load.
	 *
	 * @return The result of the proxy/load operation.
	 * @throws HibernateException
	 */
	protected Object proxyOrLoad(
		final LoadEvent event, 
		final EntityPersister persister,
		final EntityKey keyToLoad, 
		final Options options) 
	throws HibernateException {
		
		if ( log.isTraceEnabled() ) {
			log.trace(
					"loading entity: " + 
					MessageHelper.infoString( persister, event.getEntityId(), event.getSession().getFactory() )
			);
		}

		if ( !persister.hasProxy() ) {
			// this class has no proxies (so do a shortcut)
			return load(event, persister, keyToLoad, options);
		}
		else {
			SessionImplementor source = event.getSession();
			final PersistenceContext persistenceContext = source.getPersistenceContext();
			Object existing = persistenceContext.getEntity(keyToLoad);
			if ( existing != null ) {
				// return existing object or initialized proxy (unless deleted)
				log.trace("entity found in session cache");
				Object proxy = persistenceContext.proxyFor(
						persister,
						keyToLoad,
						// we still need to call load(), to do exception checking
						// and lock upgrade, but pass the entity we already found 
						// to avoid double-lookup
						load(event, persister, keyToLoad, options, existing) 
				);
				//force the proxy to resolve itself
				Hibernate.initialize(proxy);
				return proxy;
			}
			else {
				// look for a proxy
				Object proxy = persistenceContext.getProxy(keyToLoad);
				if ( proxy != null ) {
					log.trace("entity proxy found in session cache");
					Object impl;
					if ( options.isAllowProxyCreation() ) {
						impl = null;
					}
					else {
						//force initialization of the proxy
						impl = ( (HibernateProxy) proxy ).getHibernateLazyInitializer().getImplementation();
					}
					// return existing or narrowed proxy
					Object narrowed = persistenceContext.narrowProxy( proxy, persister, keyToLoad, impl );
					return narrowed;
				}
				else if ( options.isAllowProxyCreation() ) {
					log.trace("creating new proxy for entity");
					// return new uninitialized proxy
					proxy = persister.createProxy( event.getEntityId(), event.getSession() );
					persistenceContext.getBatchFetchQueue().addBatchLoadableEntityKey(keyToLoad);
					persistenceContext.addProxy(keyToLoad, proxy);
					return proxy;
				}
				else {
					// return a newly loaded object
					return load(event, persister, keyToLoad, options);
				}
			}
		}
	}

	/** 
	 * If the class to be loaded has been configured with a cache, then lock
	 * given id in that cache and then perform the load.
	 *
	 * @return The loaded entity
	 * @throws HibernateException
	 */
	protected Object lockAndLoad(
		final LoadEvent event, 
		final EntityPersister persister,
		final EntityKey keyToLoad, 
		final Options options,
		final SessionImplementor source) 
	throws HibernateException {
		
		CacheConcurrencyStrategy.SoftLock lock = null;
		final CacheKey ck;
		if ( persister.hasCache() ) {
			ck = new CacheKey( 
					event.getEntityId(), 
					persister.getIdentifierType(), 
					persister.getRootEntityName(), 
					source.getEntityMode() 
			);
			lock = persister.getCache().lock(ck, null );
		}
		else {
			ck = null;
		}

		Object entity = null;
		try {
			entity = load(event, persister, keyToLoad, options);
		}
		finally {
			if ( persister.hasCache() ) {
				persister.getCache().release(ck, lock );
			}
		}

		if ( !options.isAllowNulls() ) {
			ObjectNotFoundException.throwIfNull( entity, event.getEntityId(), event.getEntityClassName() );
		}

		Object proxy = event.getSession().getPersistenceContext().proxyFor( persister, keyToLoad, entity );
		if ( !options.isAllowProxyCreation() ) {
			//force initialization of the proxy (just to connect it to the already-loaded entity)
			Hibernate.initialize(proxy);
		}
		return proxy;
	}


	/**
	 * Coordinates the efforts to load a given entity.  First, an attempt is
	 * made to load the entity from the session-level cache.  If not found there,
	 * an attempt is made to locate it in second-level cache.  Lastly, an
	 * attempt is made to load it directly from the datasource.
	 *
	 * @return The loaded entity.
	 * @throws HibernateException
	 */
	protected Object doLoad(
		final LoadEvent event, 
		final EntityPersister persister,
		final EntityKey keyToLoad, 
		final Options options,
		final Object result) 
	throws HibernateException {
		
		if ( log.isTraceEnabled() ) {
			log.trace(
					"attempting to resolve: " + 
					MessageHelper.infoString( persister, event.getEntityId(), event.getSession().getFactory() )
			);
		}

		Object entity = loadFromSessionCache(event, keyToLoad, options, result);
		if ( entity != null ) {
			if ( log.isTraceEnabled() ) {
				log.trace(
						"resolved object in session cache: " +
						MessageHelper.infoString( persister, event.getEntityId(), event.getSession().getFactory()  )
				);
			}
			return entity;
		}

		// Entity not found in session; before going any further, see if we
		// already determined that this entity does not exist
		if ( event.getSession().getPersistenceContext().isNonExistant(keyToLoad) ) {
			if ( log.isTraceEnabled() ) log.trace("entity does not exist");
			return null;
		}

		entity = loadFromSecondLevelCache(event, persister, options);
		if ( entity != null ) {
			if ( log.isTraceEnabled() ) {
				log.trace(
						"resolved object in second-level cache: " +
						MessageHelper.infoString( persister, event.getEntityId(), event.getSession().getFactory() )
				);
			}
			return entity;
		}

		if ( log.isTraceEnabled() ) {
			log.trace(
					"object not resolved in any cache: " +
					MessageHelper.infoString( persister, event.getEntityId(), event.getSession().getFactory() )
			);
		}

		return loadFromDatasource(event, persister, keyToLoad, options);
	}

	/**
	 * Performs the process of loading an entity from the configured
	 * underlying datasource.
	 *
	 * @return The object loaded from the datasource, or null if not found.
	 * @throws HibernateException
	 */
	protected Object loadFromDatasource(
		final LoadEvent event,
		final EntityPersister persister,
		final EntityKey keyToLoad,
		final Options options) 
	throws HibernateException {
		
		final SessionImplementor source = event.getSession();
		
		Object entity = persister.load(
				event.getEntityId(), 
				event.getInstanceToLoad(), 
				event.getLockMode(), 
				source
		);
		
		if ( entity == null ) {
			//remember it doesn't exist, in case of next time
			source.getPersistenceContext().addNonExistantEntityKey(keyToLoad);
		}
		
		if ( event.isAssociationFetch() && source.getFactory().getStatistics().isStatisticsEnabled() ) {
			source.getFactory().getStatisticsImplementor().fetchEntity( event.getEntityClassName() );
		}

		return entity;
	}

	/**
	 * Attempts to locate the entity in the session-level cache.  If
	 * checkDeleted was set to true, then if the entity is found in the
	 * session-level cache, it's current status within the session cache
	 * is checked to see if it has previously been scheduled for deletion.
	 *
	 * @return The entity from the session-level cache, or null.
	 * @throws HibernateException
	 */
	protected Object loadFromSessionCache(
		final LoadEvent event,
		final EntityKey keyToLoad,
		final Options options,
		final Object result) 
	throws HibernateException {
		
		final PersistenceContext persistenceContext = event.getSession().getPersistenceContext();
		Object old = result==null ?
				persistenceContext.getEntity(keyToLoad) : result;
		if ( old != null ) {
			// this object was already loaded
			EntityEntry oldEntry = persistenceContext.getEntry(old);
			Status status = oldEntry.getStatus();
			if ( options.isCheckDeleted() && (status == Status.DELETED || status == Status.GONE) ) {
				throw new ObjectDeletedException(
						"The object with that id was deleted",
						event.getEntityId(),
						event.getEntityClassName()
				);
			}
			upgradeLock( old, oldEntry, event.getLockMode(), event.getSession() );
		}
		return old;
	}

	/**
	 * Attempts to load the entity from the second-level cache.
	 *
	 * @return The entity from the second-level cache, or null.
	 * @throws HibernateException
	 */
	protected Object loadFromSecondLevelCache(
		final LoadEvent event,
		final EntityPersister persister,
		final Options options) 
	throws HibernateException {
		
		final SessionImplementor source = event.getSession();
		
		final boolean useCache = persister.hasCache() && 
			source.getCacheMode().isGetEnabled() && 
			event.getLockMode().lessThan(LockMode.READ);
		
		if (useCache) {
			
			final SessionFactoryImplementor factory = source.getFactory();
			
			final CacheKey ck = new CacheKey( 
					event.getEntityId(), 
					persister.getIdentifierType(), 
					persister.getRootEntityName(),
					source.getEntityMode()
			);
			Object ce = persister.getCache()
				.get( ck, source.getTimestamp() );
			
			if ( factory.getStatistics().isStatisticsEnabled() ) {
				if (ce==null) {
					factory.getStatisticsImplementor().secondLevelCacheMiss( 
						persister.getCache().getRegionName() 
					);
				}
				else {
					factory.getStatisticsImplementor().secondLevelCacheHit( 
						persister.getCache().getRegionName() 
					);
				}
			}

			if ( ce != null ) {

				CacheEntry entry = (CacheEntry) persister.getCacheEntryStructure()
					.destructure(ce, factory);
			
				// Entity was found in second-level cache...
				return assembleCacheEntry(
						entry,
						event.getEntityId(),
						persister,
						event
				);
			}
		}
		
		return null;
	}

	private Object assembleCacheEntry(
		final CacheEntry entry,
		final Serializable id,
		final EntityPersister persister,
		final LoadEvent event)
	throws HibernateException {
		
		final Object optionalObject = event.getInstanceToLoad();
		final SessionImplementor session = event.getSession();
		final SessionFactoryImplementor factory = session.getFactory();
		
		if ( log.isTraceEnabled() ) log.trace(
			"resolved object in second-level cache: " +
			MessageHelper.infoString( persister, id, factory )
		);

		EntityPersister subclassPersister = factory.getEntityPersister( entry.getSubclass() );
		Object result = optionalObject == null ? session.instantiate( subclassPersister, id ) : optionalObject;

		// make it circular-reference safe
		TwoPhaseLoad.addUninitializedEntity( id, result, subclassPersister, LockMode.NONE, session );

		Type[] types = subclassPersister.getPropertyTypes();
		Object[] values = entry.assemble( result, id, subclassPersister, session.getInterceptor(), session ); // intializes result by side-effect
		TypeFactory.deepCopy( 
				values, 
				types, 
				subclassPersister.getPropertyUpdateability(), 
				values, 
				session
		);
		
		Object version = Versioning.getVersion( values, subclassPersister );
		if ( log.isTraceEnabled() ) log.trace( "Cached Version: " + version );
		
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		persistenceContext.addEntry( 
			result, 
			Status.MANAGED, 
			values, null, 
			id, 
			version, 
			LockMode.NONE, 
			true, 
			subclassPersister, 
			false 
		);
		subclassPersister.afterInitialize( result, entry.areLazyPropertiesUnfetched(), session );
		persistenceContext.initializeNonLazyCollections();
		// upgrade the lock if necessary:
		//lock(result, lockMode);

		//PostLoad is needed for EJB3
		//TODO: reuse the PostLoadEvent...
		PostLoadEvent postLoadEvent = new PostLoadEvent(session).setEntity(result)
				.setId(id).setPersister(persister);
		session.getListeners().getPostLoadEventListener().onPostLoad(postLoadEvent);
		
		return result;
	}


	/**
	 * Represents the various load options needed by the worker methods.
	 */
	private static final class Options {

		private boolean immediateLoad;
		private boolean allowNulls;
		private boolean checkDeleted;
		private boolean allowProxyCreation;

		public boolean isAllowNulls() {
			return allowNulls;
		}

		public void setAllowNulls(boolean allowNulls) {
			this.allowNulls = allowNulls;
		}

		public boolean isImmediateLoad() {
			return immediateLoad;
		}

		public void setImmediateLoad(boolean immediateLoad) {
			this.immediateLoad = immediateLoad;
		}

		public boolean isCheckDeleted() {
			return checkDeleted;
		}

		public void setCheckDeleted(boolean checkDeleted) {
			this.checkDeleted = checkDeleted;
		}

		public boolean isAllowProxyCreation() {
			return allowProxyCreation;
		}

		public void setAllowProxyCreation(boolean allowProxyCreation) {
			this.allowProxyCreation = allowProxyCreation;
		}
	}
}
