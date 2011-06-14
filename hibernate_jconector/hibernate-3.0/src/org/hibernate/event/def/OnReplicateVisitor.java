//$Id: OnReplicateVisitor.java,v 1.5 2005/02/21 13:15:25 oneovthafew Exp $
package org.hibernate.event.def;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentCollection;
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
public class OnReplicateVisitor extends ReattachVisitor {

	OnReplicateVisitor(SessionImplementor session, Serializable key) {
		super(session, key);
	}

	Object processCollection(Object collection, CollectionType type)
		throws HibernateException {
		
		if (collection==CollectionType.UNFETCHED_COLLECTION) return null;

		SessionImplementor session = getSession();
		Serializable key = getKey();
		CollectionPersister persister = session.getFactory().getCollectionPersister( type.getRole() );

		removeCollection(persister, key, session);
		if ( collection!=null && (collection instanceof PersistentCollection) ) {
			PersistentCollection wrapper = (PersistentCollection) collection;
			wrapper.setCurrentSession(session);
			if ( wrapper.wasInitialized() ) {
				session.getPersistenceContext().addNewCollection(wrapper, persister);
			}
			else {
				reattachCollection( wrapper, wrapper.getCollectionSnapshot() );
			}
		}
		else {
			// otherwise a null or brand new collection
			// this will also (inefficiently) handle arrays, which
			// have no snapshot, so we can't do any better
			//processArrayOrNewCollection(collection, type);
		}

		return null;

	}

}
