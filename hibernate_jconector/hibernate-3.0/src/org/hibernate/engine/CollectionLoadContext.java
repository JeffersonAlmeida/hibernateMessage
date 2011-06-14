//$Id: CollectionLoadContext.java,v 1.10 2005/02/21 08:15:32 oneovthafew Exp $
package org.hibernate.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CacheMode;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.cache.CacheKey;
import org.hibernate.cache.entry.CollectionCacheEntry;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;

/**
 * Represents the state of collections currently being loaded. Eventually, I
 * would like to have multiple instances of this per session - one per JDBC
 * result set, instead of the resultSetId being passed.
 * @author Gavin King
 */
public class CollectionLoadContext {
	
	private static final Log log = LogFactory.getLog(CollectionLoadContext.class);

	// The collections we are currently loading
	private final Map loadingCollections = new HashMap(8);
	private final PersistenceContext context;
	
	public CollectionLoadContext(PersistenceContext context) {
		this.context = context;
	}
	
	private static final class LoadingCollectionEntry {

		final PersistentCollection collection;
		final Serializable key;
		final Object resultSetId;

		LoadingCollectionEntry(PersistentCollection collection, Serializable key, Object resultSetId) {
			this.collection = collection;
			this.key = key;
			this.resultSetId = resultSetId;
		}
	}

	/**
	 * Retrieve a collection that is in the process of being loaded, instantiating
	 * a new collection if there is nothing for the given id, or returning null
	 * if the collection with the given id is already fully loaded in the session
	 */
	public PersistentCollection getLoadingCollection(
			CollectionPersister persister, 
			Serializable key, 
			Object resultSetId, 
			EntityMode em)
	throws HibernateException {
		CollectionKey ckey = new CollectionKey(persister, key, em);
		LoadingCollectionEntry lce = getLoadingCollectionEntry(ckey);
		if ( lce == null ) {
			//look for existing collection
			PersistentCollection pc = context.getCollection(ckey);
			if ( pc != null ) {
				CollectionEntry ce = context.getCollectionEntry(pc);
				if ( ce.isInitialized() ) {
					log.trace( "collection already initialized: ignoring" );
					return null; //ignore this row of results! Note the early exit
				}
				else {
					//initialize this collection
					log.trace( "uninitialized collection: initializing" );
				}
			}
			else {
				Object entity = context.getCollectionOwner(key, persister);
				final boolean newlySavedEntity = entity != null && 
						context.getEntry(entity).getStatus() != Status.LOADING && 
						em!=EntityMode.DOM4J;
				if ( newlySavedEntity ) {
					//important, to account for newly saved entities in query
					//TODO: some kind of check for new status...
					log.trace( "owning entity already loaded: ignoring" );
					return null;
				}
				else {
					//create one
					log.trace( "new collection: instantiating" );
					pc = persister.getCollectionType().instantiate( context.getSession(), persister, key );
				}
			}
			pc.beforeInitialize(persister);
			pc.beginRead();
			addLoadingCollectionEntry(ckey, pc, resultSetId);
			return pc;
		}
		else {
			if ( lce.resultSetId == resultSetId ) {
				log.trace( "reading row" );
				return lce.collection;
			}
			else {
				// ignore this row, the collection is in process of
				// being loaded somewhere further "up" the stack
				log.trace( "collection is already being initialized: ignoring row" );
				return null;
			}
		}
	}

	/**
	 * Retrieve a collection that is in the process of being loaded, returning null
	 * if there is no loading collection with the given id
	 */
	public PersistentCollection getLoadingCollection(CollectionPersister persister, Serializable id, EntityMode em) {
		LoadingCollectionEntry lce = getLoadingCollectionEntry( new CollectionKey(persister, id, em) );
		if ( lce != null ) {
			if ( log.isTraceEnabled() ) {
				log.trace( 
						"returning loading collection:" + 
						MessageHelper.collectionInfoString(persister, id, context.getSession().getFactory()) 
				);
			}		
			return lce.collection;
		}
		else {
			if ( log.isTraceEnabled() ) {
				log.trace( 
						"creating collection wrapper:" + 
						MessageHelper.collectionInfoString(persister, id, context.getSession().getFactory()) 
				);
			}
			return null;
		}
	}

	/**
	 * Create a new loading collection entry
	 */
	private void addLoadingCollectionEntry(CollectionKey collectionKey, PersistentCollection collection, Object resultSetId ) {
		loadingCollections.put(
				collectionKey,
				new LoadingCollectionEntry(
						collection,
						collectionKey.getKey(),
						resultSetId
				)
		);
	}

	/**
	 * get an existing new loading collection entry
	 */
	private LoadingCollectionEntry getLoadingCollectionEntry(CollectionKey collectionKey) {
		return ( LoadingCollectionEntry ) loadingCollections.get( collectionKey );
	}

	/**
	 * After we have finished processing a result set, a particular loading collection that
	 * we are done.
	 */
	private void endLoadingCollection(LoadingCollectionEntry lce, CollectionPersister persister, EntityMode em) {

		boolean hasNoQueuedAdds = lce.collection.endRead(); //warning: can cause a recursive query! (proxy initialization)

		if ( persister.getCollectionType().hasHolder(em) ) {
			context.addCollectionHolder(lce.collection);
		}
		
		CollectionEntry ce = context.getCollectionEntry(lce.collection);
		if ( ce==null ) {
			ce = context.addInitializedCollection(lce.collection, persister, lce.key, em);
		}
		else {
			ce.postInitialize(lce.collection);
		}

		final SessionImplementor session = context.getSession();
		
		boolean addToCache = hasNoQueuedAdds && // there were no queued additions
			persister.hasCache() &&             // and the role has a cache
			session.getCacheMode().isPutEnabled() &&
			!ce.isDoremove();                   // and this is not a forced initialization during flush
		if (addToCache) addCollectionToCache(lce, persister);

		if ( log.isDebugEnabled() ) {
			log.debug(
					"collection fully initialized: " +
					MessageHelper.collectionInfoString(persister, lce.key, context.getSession().getFactory())
			);
		}

		if ( session.getFactory().getStatistics().isStatisticsEnabled() ) {
			session.getFactory().getStatisticsImplementor().loadCollection(
					persister.getRole()
			);
		}

	}
	/**
	 * Finish the process of loading collections for a particular result set
	 */
	public void endLoadingCollections(CollectionPersister persister, Object resultSetId, EntityMode em)
	throws HibernateException {

		// scan the loading collections for collections from this result set
		// put them in a new temp collection so that we are safe from concurrent
		// modification when the call to endRead() causes a proxy to be
		// initialized
		List resultSetCollections = null; //TODO: make this the resultSetId?
		Iterator iter = loadingCollections.values().iterator();
		while ( iter.hasNext() ) {
			LoadingCollectionEntry lce = (LoadingCollectionEntry) iter.next();
			if ( lce.resultSetId == resultSetId ) {
				if ( resultSetCollections == null ) resultSetCollections = new ArrayList();
				resultSetCollections.add(lce);
				iter.remove();
			}
		}

		endLoadingCollections(persister, resultSetCollections, em);
	}
	
	/**
	 * After we have finished processing a result set, notify the loading collections that
	 * we are done.
	 */
	private void endLoadingCollections(CollectionPersister persister, List resultSetCollections, EntityMode em)
	throws HibernateException {

		final int count = (resultSetCollections == null) ? 0 : resultSetCollections.size();

		if ( log.isDebugEnabled() ) log.debug( count + " collections were found in result set" );

		//now finish them
		for ( int i = 0; i < count; i++ ) {
			LoadingCollectionEntry lce = (LoadingCollectionEntry) resultSetCollections.get(i);
			endLoadingCollection(lce, persister, em);
		}

		if ( log.isDebugEnabled() ) log.debug( count + " collections initialized" );
	}

	/**
	 * Add a collection to the second-level cache
	 */
	private void addCollectionToCache(LoadingCollectionEntry lce, CollectionPersister persister) {

		if ( log.isDebugEnabled() ) {
			log.debug(
					"Caching collection: " +
					MessageHelper.collectionInfoString(persister, lce.key, context.getSession().getFactory())
			);
		}

		final SessionImplementor session = context.getSession();
		final SessionFactoryImplementor factory = session.getFactory();

		if ( !session.getEnabledFilters().isEmpty() && persister.isAffectedByEnabledFilters( session ) ) {
			// some filters affecting the collection are enabled on the session, so do not do the put into the cache.
			log.debug( "Refusing to add to cache due to enabled filters" );
			// todo : add the notion of enabled filters to the CacheKey to differentiate filtered collections from non-filtered;
			//      but CacheKey is currently used for both collections and entities; would ideally need to define two seperate ones;
			//      currently this works in conjuction with the check on
			//      DefaultInitializeCollectionEventHandler.initializeCollectionFromCache() (which makes sure to not read from
			//      cache with enabled filters).
			return; // EARLY EXIT!!!!!
		}

		final Comparator versionComparator;
		final Object version;
		if ( persister.isVersioned() ) {
			versionComparator = persister.getOwnerEntityPersister().getVersionType().getComparator();
			version = context.getEntry( context.getCollectionOwner(lce.key, persister) ).getVersion();
		}
		else {
			version = null;
			versionComparator = null;
		}
		
		CollectionCacheEntry entry = new CollectionCacheEntry(lce.collection, persister);

		boolean put = persister.getCache().put(
				new CacheKey( lce.key, persister.getKeyType(), persister.getRole(), session.getEntityMode() ),
				persister.getCacheEntryStructure().structure(entry),
				session.getTimestamp(),
				version,
				versionComparator,
				factory.getSettings().isMinimalPutsEnabled() && session.getCacheMode()!=CacheMode.REFRESH
		);

		if ( put && factory.getStatistics().isStatisticsEnabled() ) {
			factory.getStatisticsImplementor().secondLevelCachePut(
					persister.getCache().getRegionName()
			);
		}
	}


}
