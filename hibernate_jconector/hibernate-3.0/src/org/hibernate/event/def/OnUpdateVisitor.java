//$Id: OnUpdateVisitor.java,v 1.3 2005/02/21 13:15:25 oneovthafew Exp $
package org.hibernate.event.def;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.CollectionSnapshot;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.CollectionType;

/**
 * When an entity is passed to update(), we must inspect all its collections and
 * 1. associate any uninitialized PersistentCollections with this session
 * 2. associate any initialized PersistentCollections with this session, using the
 *    existing snapshot
 * 3. execute a collection removal (SQL DELETE) for each null collection property
 *    or "new" collection
 *
 * @author Gavin King
 */
public class OnUpdateVisitor extends ReattachVisitor {

	OnUpdateVisitor(SessionImplementor session, Serializable key) {
		super(session, key);
	}

	Object processCollection(Object collection, CollectionType type)
		throws HibernateException {
		
		if (collection==CollectionType.UNFETCHED_COLLECTION) return null;

		SessionImplementor session = getSession();
		Serializable key = getKey();
		CollectionPersister persister = session.getFactory().getCollectionPersister( type.getRole() );

		if ( collection!=null && (collection instanceof PersistentCollection) ) {
			PersistentCollection wrapper = (PersistentCollection) collection;

			if ( wrapper.setCurrentSession(session) ) {
				//a "detached" collection!
				CollectionSnapshot snapshot = wrapper.getCollectionSnapshot();

				if ( !isOwnerUnchanged(snapshot, persister, key) ) {
					// if the collection belonged to a different entity,
					// clean up the existing state of the collection
					removeCollection(persister, key, session);
				}

				reattachCollection(wrapper, snapshot);
			}
			else {
				// a collection loaded in the current session
				// can not possibly be the collection belonging
				// to the entity passed to update()
				removeCollection(persister, key, session);
			}

		}
		else {
			// null or brand new collection
			// this will also (inefficiently) handle arrays, which have
			// no snapshot, so we can't do any better
			removeCollection(persister, key, session);
			//processArrayOrNewCollection(collection, type);
		}

		return null;

	}

}
