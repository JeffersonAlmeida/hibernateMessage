//$Id: UserCollectionType.java,v 1.5 2005/02/13 11:50:11 oneovthafew Exp $
package org.hibernate.usertype;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * A custom type for mapping user-written classes that implement <tt>PersistentCollection</tt>
 * 
 * @see org.hibernate.collection.PersistentCollection
 * @author Gavin King
 */
public interface UserCollectionType {
	
	/**
	 * Instantiate an uninitialized instance of the collection wrapper
	 */
	public PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister) 
	throws HibernateException;
	
	/**
	 * Wrap an instance of a collection
	 */
	public PersistentCollection wrap(SessionImplementor session, Object collection);
	
	/**
	 * Return an iterator over the elements of this collection - the passed collection
	 * instance may or may not be a wrapper
	 */
	public Iterator getElementsIterator(Object collection);

	/**
	 * Optional operation. Does the collection contain the entity instance?
	 */
	public boolean contains(Object collection, Object entity);
	/**
	 * Optional operation. Return the index of the entity in the collection.
	 */
	public Object indexOf(Object collection, Object entity);
	
	/**
	 * Replace the elements of a collection with the elements of another collection
	 */
	public void replaceElements(
			Object original, 
			Object target, 
			CollectionPersister persister, 
			Object owner, 
			Map copyCache, 
			SessionImplementor session)
			throws HibernateException;
	
	/**
	 * Instantiate an empty instance of the "underlying" collection (not a wrapper)
	 */
	public Object instantiate();
	
}
