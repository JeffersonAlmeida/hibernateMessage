//$Id: DirtyCollectionSearchVisitor.java,v 1.4 2005/03/04 10:19:53 oneovthafew Exp $
package org.hibernate.event.def;

import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.CollectionType;

/**
 * Do we have a dirty collection here?
 * 1. if it is a new application-instantiated collection, return true (does not occur anymore!)
 * 2. if it is a component, recurse
 * 3. if it is a wrappered collection, ask the collection entry
 *
 * @author Gavin King
 */
public class DirtyCollectionSearchVisitor extends AbstractVisitor {

	private boolean dirty = false;
	private boolean[] propertyVersionability;

	DirtyCollectionSearchVisitor(SessionImplementor session, boolean[] propertyVersionability) {
		super(session);
		this.propertyVersionability = propertyVersionability;
	}

	boolean wasDirtyCollectionFound() {
		return dirty;
	}

	Object processCollection(Object collection, CollectionType type)
		throws HibernateException {

		if (collection!=null) {

			SessionImplementor session = getSession();

			final PersistentCollection coll;
			if ( type.isArrayType() ) {
				 coll = session.getPersistenceContext().getCollectionHolder(collection);
				// if no array holder we found an unwrappered array (this can't occur,
				// because we now always call wrap() before getting to here)
				// return (ah==null) ? true : searchForDirtyCollections(ah, type);
			}
			else {
				// if not wrappered yet, its dirty (this can't occur, because
				// we now always call wrap() before getting to here)
				// return ( ! (obj instanceof PersistentCollection) ) ?
				//true : searchForDirtyCollections( (PersistentCollection) obj, type );
				coll = (PersistentCollection) collection;
			}

			if ( session.getPersistenceContext().getCollectionEntry(coll).isInitializedAndDirty() ) {
				dirty=true;
				return null; //NOTE: EARLY EXIT!
			}
		}

		return null;
	}

	boolean includeEntityProperty(Object[] values, int i) {
		return propertyVersionability[i] && super.includeEntityProperty(values, i);
	}
}
