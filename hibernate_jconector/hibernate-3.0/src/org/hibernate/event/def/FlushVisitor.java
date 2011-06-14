//$Id: FlushVisitor.java,v 1.6 2005/03/04 17:25:10 oneovthafew Exp $
package org.hibernate.event.def;

import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.Collections;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.CollectionType;

/**
 * Process collections reachable from an entity. This
 * visitor assumes that wrap was already performed for
 * the entity.
 *
 * @author Gavin King
 */
public class FlushVisitor extends AbstractVisitor {
	
	private Object owner;

	Object processCollection(Object collection, CollectionType type)
	throws HibernateException {
		
		if (collection==CollectionType.UNFETCHED_COLLECTION) {
			return null;
		}

		if (collection!=null) {
			final PersistentCollection coll;
			if ( type.hasHolder( getSession().getEntityMode() ) ) {
				coll = getSession().getPersistenceContext().getCollectionHolder(collection);
			}
			else {
				coll = (PersistentCollection) collection;
			}

			Collections.updateReachableCollection( coll, type, owner, getSession() );
		}

		return null;

	}

	FlushVisitor(SessionImplementor session, Object owner) {
		super(session);
		this.owner = owner;
	}

}
