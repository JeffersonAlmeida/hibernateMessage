//$Id: AbstractFlushingEventListener.java,v 1.5 2005/03/17 05:50:08 oneovthafew Exp $
package org.hibernate.event.def;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.action.CollectionRecreateAction;
import org.hibernate.action.CollectionRemoveAction;
import org.hibernate.action.CollectionUpdateAction;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.Cascades;
import org.hibernate.engine.CollectionEntry;
import org.hibernate.engine.CollectionKey;
import org.hibernate.engine.Collections;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.Status;
import org.hibernate.event.FlushEntityEvent;
import org.hibernate.event.FlushEvent;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.Printer;
import org.hibernate.util.IdentityMap;

/**
 * A convenience base class for listeners whose functionality results in flushing.
 *
 * @author Steve Eberole
 */
public abstract class AbstractFlushingEventListener extends AbstractEventListener {

	private static final Log log = LogFactory.getLog(AbstractFlushingEventListener.class);
	
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Pre-flushing section
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/** 
	 * Coordinates the processing necessary to get things ready for executions
	 * as db calls by preping the session caches and moving the appropriate
	 * entities and collections to their respective execution queues.
	 *
	 * @param event The flush event.
	 * @throws HibernateException Error flushing caches to execution queues.
	 */
	protected void flushEverythingToExecutions(FlushEvent event) throws HibernateException {

		log.trace("flushing session");
		
		SessionImplementor session = event.getSession();
		
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		session.getInterceptor().preFlush( persistenceContext.getEntitiesByKey().values().iterator() );

		prepareEntityFlushes(session);
		// we could move this inside if we wanted to
		// tolerate collection initializations during
		// collection dirty checking:
		prepareCollectionFlushes(session);
		// now, any collections that are initialized
		// inside this block do not get updated - they
		// are ignored until the next flush
				
		persistenceContext.setFlushing(true);
		try {
			flushEntities(event);
			flushCollections(session);
		}
		finally {
			persistenceContext.setFlushing(false);
		}

		//some statistics
		if ( log.isDebugEnabled() ) {
			log.debug( "Flushed: " +
					session.getActionQueue().numberOfInsertions() + " insertions, " +
					session.getActionQueue().numberOfUpdates() + " updates, " +
					session.getActionQueue().numberOfDeletions() + " deletions to " +
					persistenceContext.getEntityEntries().size() + " objects"
			);
			log.debug( "Flushed: " +
					session.getActionQueue().numberOfCollectionCreations() + " (re)creations, " +
					session.getActionQueue().numberOfCollectionUpdates() + " updates, " +
					session.getActionQueue().numberOfCollectionRemovals() + " removals to " +
					persistenceContext.getCollectionEntries().size() + " collections"
			);
			new Printer( session.getFactory() ).toString( persistenceContext.getEntitiesByKey().values().iterator(), session.getEntityMode() );
		}
	}

	/**
	 * process cascade save/update at the start of a flush to discover
	 * any newly referenced entity that must be passed to saveOrUpdate(),
	 * and also apply orphan delete
	 */
	private void prepareEntityFlushes(SessionImplementor session) throws HibernateException {
		
		log.debug("processing flush-time cascades");

		final Map.Entry[] list = IdentityMap.concurrentEntries( session.getPersistenceContext().getEntityEntries() );
		//safe from concurrent modification because of how entryList() is implemented on IdentityMap
		final int size = list.length;
		for ( int i=0; i<size; i++ ) {
			Map.Entry me = list[i];
			EntityEntry entry = (EntityEntry) me.getValue();
			Status status = entry.getStatus();
			if ( status == Status.MANAGED || status == Status.SAVING ) {
				cascadeOnFlush( session, entry.getPersister(), me.getKey() );
			}
		}
	}
	
	private void cascadeOnFlush(SessionImplementor session, EntityPersister persister, Object object) 
	throws HibernateException {
		session.getPersistenceContext().incrementCascadeLevel();
		try {
			Cascades.cascade(
					session,
					persister,
					object,
					getCascadingAction(),
					Cascades.CASCADE_BEFORE_FLUSH,
					getAnything()
			);
		}
		finally {
			session.getPersistenceContext().decrementCascadeLevel();
		}
	}
	
	protected Object getAnything() { return null; }
	
	protected Cascades.CascadingAction getCascadingAction() {
		return Cascades.ACTION_SAVE_UPDATE;
	}

	/**
	 * Initialize the flags of the CollectionEntry, including the
	 * dirty check.
	 */
	private void prepareCollectionFlushes(SessionImplementor session) throws HibernateException {

		// Initialize dirty flags for arrays + collections with composite elements
		// and reset reached, doupdate, etc.
		
		log.debug("dirty checking collections");

		final List list = IdentityMap.entries( session.getPersistenceContext().getCollectionEntries() );
		final int size = list.size();
		for ( int i = 0; i < size; i++ ) {
			Map.Entry e = ( Map.Entry ) list.get( i );
			( (CollectionEntry) e.getValue() ).preFlush( (PersistentCollection) e.getKey() );
		}
	}

	/**
	 * 1. detect any dirty entities
	 * 2. schedule any entity updates
	 * 3. search out any reachable collections
	 */
	private void flushEntities(FlushEvent event) throws HibernateException {

		log.trace("Flushing entities and processing referenced collections");

		// Among other things, updateReachables() will recursively load all
		// collections that are moving roles. This might cause entities to
		// be loaded.

		// So this needs to be safe from concurrent modification problems.
		// It is safe because of how IdentityMap implements entrySet()

		final SessionImplementor source = event.getSession();
		
		final Map.Entry[] list = IdentityMap.concurrentEntries( source.getPersistenceContext().getEntityEntries() );
		final int size = list.length;
		for ( int i = 0; i < size; i++ ) {

			// Update the status of the object and if necessary, schedule an update

			Map.Entry me = list[i];
			EntityEntry entry = (EntityEntry) me.getValue();
			Status status = entry.getStatus();

			if ( status != Status.LOADING && status != Status.GONE ) {
				FlushEntityEvent entityEvent = new FlushEntityEvent( source, me.getKey(), entry );
				source.getListeners().getFlushEntityEventListener().onFlushEntity(entityEvent);
			}
		}

		source.getActionQueue().sortUpdateActions();
	}

	/**
	 * process any unreferenced collections and then inspect all known collections,
	 * scheduling creates/removes/updates
	 */
	private void flushCollections(SessionImplementor session) throws HibernateException {

		log.trace("Processing unreferenced collections");

		List list = IdentityMap.entries( session.getPersistenceContext().getCollectionEntries() );
		int size = list.size();
		for ( int i = 0; i < size; i++ ) {
			Map.Entry me = ( Map.Entry ) list.get( i );
			CollectionEntry ce = (CollectionEntry) me.getValue();
			if ( !ce.isReached() && !ce.isIgnore() ) {
				Collections.updateUnreachableCollection( (PersistentCollection) me.getKey(), session );
			}
		}

		// Schedule updates to collections:

		log.trace( "Scheduling collection removes/(re)creates/updates" );

		list = IdentityMap.entries( session.getPersistenceContext().getCollectionEntries() );
		size = list.size();
		for ( int i = 0; i < size; i++ ) {
			Map.Entry me = (Map.Entry) list.get(i);
			PersistentCollection coll = (PersistentCollection) me.getKey();
			CollectionEntry ce = (CollectionEntry) me.getValue();

			if ( ce.isDorecreate() ) {
				session.getActionQueue().addAction(
						new CollectionRecreateAction( coll, ce.getCurrentPersister(), ce.getCurrentKey(), session )
				);
			}
			if ( ce.isDoremove() ) {
				session.getActionQueue().addAction(
						new CollectionRemoveAction( ce.getLoadedPersister(), ce.getLoadedKey(), ce.snapshotIsEmpty(), session )
				);
			}
			if ( ce.isDoupdate() ) {
				session.getActionQueue().addAction(
						new CollectionUpdateAction( coll, ce.getLoadedPersister(), ce.getLoadedKey(), ce.snapshotIsEmpty(), session )
				);
			}

		}

		session.getActionQueue().sortCollectionActions();
		
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// flush/execution section
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Execute all SQL and second-level cache updates, in a
	 * special order so that foreign-key constraints cannot
	 * be violated:
	 * <ol>
	 * <li> Inserts, in the order they were performed
	 * <li> Updates
	 * <li> Deletion of collection elements
	 * <li> Insertion of collection elements
	 * <li> Deletes, in the order they were performed
	 * </ol>
	 */
	protected void performExecutions(SessionImplementor session) throws HibernateException {

		log.trace("executing flush");

		try {
			// we need to lock the collection caches before
			// executing entity inserts/updates in order to
			// account for bidi associations
			session.getActionQueue().prepareActions();
			session.getActionQueue().executeActions();
		}
		catch (HibernateException he) {
			log.error("Could not synchronize database state with session", he);
			throw he;
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Post-flushing section
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * 1. Recreate the collection key -> collection map
	 * 2. rebuild the collection entries
	 * 3. call Interceptor.postFlush()
	 */
	protected void postFlush(SessionImplementor session) throws HibernateException {

		log.trace( "post flush" );

		final PersistenceContext persistenceContext = session.getPersistenceContext();
		persistenceContext.getCollectionsByKey().clear();
		persistenceContext.getBatchFetchQueue().clearSubselect(); //the database has changed now, so the subselect results need to be invalidated

		Iterator iter = persistenceContext.getCollectionEntries().entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry me = (Map.Entry) iter.next();
			CollectionEntry ce = (CollectionEntry) me.getValue();
			PersistentCollection pc = (PersistentCollection) me.getKey();
			if ( ce.postFlush(pc) ) {
				//if the collection is dereferenced, remove from the session cache
				iter.remove();
			}
			else if ( ce.isReached() ) {
				//otherwise recreate the mapping between the collection and its key
				persistenceContext.getCollectionsByKey().put(
						new CollectionKey( ce.getCurrentPersister(), ce.getCurrentKey(), session.getEntityMode() ),
						pc
				);
			}
		}
		
		session.getInterceptor().postFlush( persistenceContext.getEntitiesByKey().values().iterator() );

	}

}
