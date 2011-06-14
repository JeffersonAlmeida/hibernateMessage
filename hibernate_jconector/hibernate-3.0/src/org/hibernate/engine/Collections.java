//$Id: Collections.java,v 1.14 2005/02/21 13:15:24 oneovthafew Exp $
package org.hibernate.engine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

/**
 * Implements book-keeping for the collection persistence by reachability algorithm
 * @author Gavin King
 */
public final class Collections {
	
	private Collections() {}
	
	private static final Log log = LogFactory.getLog(Collections.class);
	
	/**
	 * record the fact that this collection was dereferenced
	 *
	 * @param coll The collection to be updated by unreachability.
	 * @throws HibernateException
	 */
	public static void updateUnreachableCollection(PersistentCollection coll, SessionImplementor session) 
	throws HibernateException {

		final PersistenceContext persistenceContext = session.getPersistenceContext();
		CollectionEntry entry = persistenceContext.getCollectionEntry(coll);

		if ( log.isDebugEnabled() && entry.getLoadedPersister() != null )
			log.debug(
					"Collection dereferenced: " +
					MessageHelper.collectionInfoString( entry.getLoadedPersister(), entry.getLoadedKey(), session.getFactory() )
			);

		// do a check
		if (
				entry.getLoadedPersister() != null &&
				entry.getLoadedPersister().hasOrphanDelete()
		) {
			EntityKey key = new EntityKey(
					entry.getLoadedKey(),
					entry.getLoadedPersister().getOwnerEntityPersister(),
					session.getEntityMode()
			);
			Object owner = persistenceContext.getEntity(key);
			if ( owner == null ) throw new AssertionFailure( "owner not associated with session" );
			EntityEntry e = persistenceContext.getEntry(owner);
			//only collections belonging to deleted entities are allowed to be dereferenced in the case of orphan delete
			if ( e != null && e.getStatus() != Status.DELETED && e.getStatus() != Status.GONE ) {
				throw new HibernateException( 
						"Don't dereference a collection with cascade=\"all-delete-orphan\": " + 
						coll.getCollectionSnapshot().getRole() 
				);
			}
		}

		// do the work
		entry.setCurrentPersister(null);
		entry.setCurrentKey(null);
		prepareCollectionForUpdate( coll, entry, session.getEntityMode(), session.getFactory() );

	}

	/**
	 * Initialize the role of the collection.
	 *
	 * @param coll The collection to be updated by reachibility.
	 * @param type The type of the collection.
	 * @param owner The owner of the collection.
	 * @throws HibernateException
	 */
	public static void updateReachableCollection(
			PersistentCollection coll, 
			Type type, 
			Object entity, 
			SessionImplementor session) 
	throws HibernateException {
		
		CollectionEntry ce = session.getPersistenceContext().getCollectionEntry(coll);

		if ( ce == null ) {
			// refer to comment in SessionImpl.addCollection()
			throw new HibernateException( "Found two representations of same collection" );
		}

		// The CollectionEntry.reached stuff is just to detect any silly users  
		// who set up circular or shared references between/to collections.
		if ( ce.isReached() ) {
			// We've been here before
			throw new HibernateException( "Found shared references to a collection" );
		}
		ce.setReached(true);
		
		CollectionType pctype = (CollectionType) type;
		CollectionPersister persister = session.getFactory().getCollectionPersister( pctype.getRole() );
		ce.setCurrentPersister(persister);
		ce.setCurrentKey( pctype.getKeyOfOwner(entity, session) ); //TODO: better to pass the id in as an argument?

		if ( log.isDebugEnabled() ) {
			log.debug(
					"Collection found: " +
					MessageHelper.collectionInfoString( persister, ce.getCurrentKey(), session.getFactory() ) +
					", was: " +
					MessageHelper.collectionInfoString( ce.getLoadedPersister(), ce.getLoadedKey(), session.getFactory() ) +
					( ce.isInitialized() ? " (initialized)" : " (uninitialized)" )
			);
		}

		prepareCollectionForUpdate( coll, ce, session.getEntityMode(), session.getFactory() );

	}
	
	/**
	 * 1. record the collection role that this collection is referenced by
	 * 2. decide if the collection needs deleting/creating/updating (but
	 *	don't actually schedule the action yet)
	 */
	private static void prepareCollectionForUpdate(
			PersistentCollection coll, 
			CollectionEntry entry, 
			EntityMode entityMode, 
			SessionFactoryImplementor factory) 
	throws HibernateException {

		if ( entry.isProcessed() ) throw new AssertionFailure( "collection was processed twice by flush()" );
		entry.setProcessed(true);

		if ( entry.getLoadedPersister() != null || entry.getCurrentPersister() != null ) {		// it is or was referenced _somewhere_

			boolean ownerChanged = entry.getLoadedPersister() != entry.getCurrentPersister() || 				// if either its role changed,
				!entry.getCurrentPersister().getKeyType().isEqual( entry.getLoadedKey(), entry.getCurrentKey(), entityMode, factory );   // or its key changed (for nested collections)

			if (ownerChanged) {

				// do a check
				if (
						entry.getLoadedPersister() != null &&
						entry.getCurrentPersister() != null &&
						entry.getLoadedPersister().hasOrphanDelete()
				) {
					throw new HibernateException( 
							"Don't change the reference to a collection with cascade=\"all-delete-orphan\": " + 
							coll.getCollectionSnapshot().getRole() 
					);
				}

				// do the work
				if ( entry.getCurrentPersister() != null ) entry.setDorecreate(true);			// we will need to create new entries

				if ( entry.getLoadedPersister() != null ) {
					entry.setDoremove(true);													// we will need to remove ye olde entries
					if ( entry.isDorecreate() ) {
						log.trace( "Forcing collection initialization" );
						coll.forceInitialization();												// force initialize!
					}
				}

			}
			else if ( entry.isDirty() ) {														// else if it's elements changed
				entry.setDoupdate(true);
			}

		}

	}

}
