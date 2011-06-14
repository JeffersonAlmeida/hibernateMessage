//$Id: PersistentBag.java,v 1.13 2005/03/16 04:45:20 oneovthafew Exp $
package org.hibernate.collection;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;

/**
 * An unordered, unkeyed collection that can contain the same element
 * multiple times. The Java collections API, curiously, has no <tt>Bag</tt>.
 * Most developers seem to use <tt>List</tt>s to represent bag semantics,
 * so Hibernate follows this practice.
 *
 * @author Gavin King
 */
public class PersistentBag extends AbstractPersistentCollection implements java.util.List {

	private java.util.List bag;

	public PersistentBag(SessionImplementor session) {
		super(session);
	}

	public PersistentBag(SessionImplementor session, java.util.Collection coll) {
		super(session);
		if (coll instanceof java.util.List) {
			bag = (java.util.List) coll;
		}
		else {
			bag = new ArrayList();
			Iterator iter = coll.iterator();
			while ( iter.hasNext() ) {
				bag.add( iter.next() );
			}
		}
		setInitialized();
		setDirectlyAccessible(true);
	}

	public PersistentBag() {} //needed for SOAP libraries, etc

	public boolean isWrapper(Object collection) {
		return bag==collection;
	}
	public boolean empty() {
		return bag.isEmpty();
	}
	
	public Iterator entries(CollectionPersister persister) {
		return bag.iterator();
	}

	public Object readFrom(ResultSet rs, CollectionPersister persister, Object owner) 
	throws HibernateException, SQLException {
		// note that if we load this collection from a cartesian product
		// the multiplicity would be broken ... so use an idbag instead
		Object element = persister.readElement( rs, owner, getSession() ) ;
		bag.add(element);
		return element;
	}

	public void beforeInitialize(CollectionPersister persister) {
		this.bag = new ArrayList();
	}

	public boolean equalsSnapshot(CollectionPersister persister) throws HibernateException {
		Type elementType = persister.getElementType();
		EntityMode entityMode = getSession().getEntityMode();
		java.util.List sn = (java.util.List) getSnapshot();
		if ( sn.size()!=bag.size() ) return false;
		Iterator iter = bag.iterator();
		while ( iter.hasNext() ) {
			Object elt = iter.next();
			final boolean unequal = countOccurrences(elt, bag, elementType, entityMode) !=
				countOccurrences(elt, sn, elementType, entityMode);
			if ( unequal ) return false;
		}
		return true;
	}

	private int countOccurrences(Object element, java.util.List list, Type elementType, EntityMode entityMode) 
	throws HibernateException {
		Iterator iter = list.iterator();
		int result=0;
		while ( iter.hasNext() ) {
			if ( elementType.isSame( element, iter.next(), entityMode ) ) result++;
		}
		return result;
	}

	protected Serializable snapshot(CollectionPersister persister)
	throws HibernateException {
		EntityMode entityMode = getSession().getEntityMode();
		ArrayList clonedList = new ArrayList( bag.size() );
		Iterator iter = bag.iterator();
		while ( iter.hasNext() ) {
			clonedList.add( persister.getElementType().deepCopy( iter.next(), entityMode, persister.getFactory() ) );
		}
		return clonedList;
	}

	public Collection getOrphans(Serializable snapshot, String entityName) throws HibernateException {
	    java.util.List sn = (java.util.List) snapshot;
	    return getOrphans( sn, bag, entityName, getSession() );
	}


	public Serializable disassemble(CollectionPersister persister)
	throws HibernateException {

		int length = bag.size();
		Serializable[] result = new Serializable[length];
		for ( int i=0; i<length; i++ ) {
			result[i] = persister.getElementType().disassemble( bag.get(i), getSession(), null );
		}
		return result;
	}

	public void initializeFromCache(CollectionPersister persister, Serializable disassembled, Object owner)
	throws HibernateException {
		beforeInitialize(persister);
		Serializable[] array = (Serializable[]) disassembled;
		for ( int i=0; i<array.length; i++ ) {
			bag.add( persister.getElementType().assemble( array[i], getSession(), owner ) );
		}
		setInitialized();
	}

	public boolean needsRecreate(CollectionPersister persister) {
		return !persister.isOneToMany();
	}


	// For a one-to-many, a <bag> is not really a bag;
	// it is *really* a set, since it can't contain the
	// same element twice. It could be considered a bug
	// in the mapping dtd that <bag> allows <one-to-many>.

	// Anyway, here we implement <set> semantics for a
	// <one-to-many> <bag>!

	public Iterator getDeletes(CollectionPersister persister, boolean indexIsFormula) throws HibernateException {
		//if ( !persister.isOneToMany() ) throw new AssertionFailure("Not implemented for Bags");
		Type elementType = persister.getElementType();
		EntityMode entityMode = getSession().getEntityMode();
		ArrayList deletes = new ArrayList();
		java.util.List sn = (java.util.List) getSnapshot();
		Iterator olditer = sn.iterator();
		int i=0;
		while ( olditer.hasNext() ) {
			Object old = olditer.next();
			Iterator newiter = bag.iterator();
			boolean found = false;
			if ( bag.size()>i && elementType.isSame( old, bag.get(i++), entityMode ) ) {
			//a shortcut if its location didn't change!
				found = true;
			}
			else {
				//search for it
				//note that this code is incorrect for other than one-to-many
				while ( newiter.hasNext() ) {
					if ( elementType.isSame( old, newiter.next(), entityMode ) ) {
						found = true;
						break;
					}
				}
			}
			if (!found) deletes.add(old);
		}
		return deletes.iterator();
	}

	public boolean needsInserting(Object entry, int i, Type elemType) throws HibernateException {
		//if ( !persister.isOneToMany() ) throw new AssertionFailure("Not implemented for Bags");
		java.util.List sn = (java.util.List) getSnapshot();
		final EntityMode entityMode = getSession().getEntityMode();
		if ( sn.size()>i && elemType.isSame( sn.get(i), entry, entityMode ) ) {
		//a shortcut if its location didn't change!
			return false;
		}
		else {
			//search for it
			//note that this code is incorrect for other than one-to-many
			Iterator olditer = sn.iterator();
			while ( olditer.hasNext() ) {
				Object old = olditer.next();
				if ( elemType.isSame( old, entry, entityMode ) ) return false;
			}
			return true;
		}
	}
	
	public boolean isRowUpdatePossible() {
		return false;
	}

	public boolean needsUpdating(Object entry, int i, Type elemType) {
		//if ( !persister.isOneToMany() ) throw new AssertionFailure("Not implemented for Bags");
		return false;
	}

	/**
	 * @see java.util.Collection#size()
	 */
	public int size() {
		read();
		return bag.size();
	}

	/**
	 * @see java.util.Collection#isEmpty()
	 */
	public boolean isEmpty() {
		read();
		return bag.isEmpty();
	}

	/**
	 * @see java.util.Collection#contains(Object)
	 */
	public boolean contains(Object o) {
		read();
		return bag.contains(o);
	}

	/**
	 * @see java.util.Collection#iterator()
	 */
	public Iterator iterator() {
		read();
		return new IteratorProxy( bag.iterator() );
	}

	/**
	 * @see java.util.Collection#toArray()
	 */
	public Object[] toArray() {
		read();
		return bag.toArray();
	}

	/**
	 * @see java.util.Collection#toArray(Object[])
	 */
	public Object[] toArray(Object[] a) {
		read();
		return bag.toArray(a);
	}

	/**
	 * @see java.util.Collection#add(Object)
	 */
	public boolean add(Object o) {
		if ( !queueAdd(o) ) {
			write();
			return bag.add(o);
		}
		else {
			return true;
		}
	}

	/**
	 * @see java.util.Collection#remove(Object)
	 */
	public boolean remove(Object o) {
		write();
		return bag.remove(o);
	}

	/**
	 * @see java.util.Collection#containsAll(Collection)
	 */
	public boolean containsAll(Collection c) {
		read();
		return bag.containsAll(c);
	}

	/**
	 * @see java.util.Collection#addAll(Collection)
	 */
	public boolean addAll(Collection c) {
		if ( c.size()==0 ) return false;
		if ( !queueAddAll(c) ) {
			write();
			return bag.addAll(c);
		}
		else {
			return c.size()>0;
		}
	}

	public void delayedAddAll(Collection c) {
		bag.addAll(c);
	}

	/**
	 * @see java.util.Collection#removeAll(Collection)
	 */
	public boolean removeAll(Collection c) {
		if ( c.size()>0 ) {
			write();
			return bag.removeAll(c);
		}
		else {
			return false;
		}
	}

	/**
	 * @see java.util.Collection#retainAll(Collection)
	 */
	public boolean retainAll(Collection c) {
		write();
		return bag.retainAll(c);
	}

	/**
	 * @see java.util.Collection#clear()
	 */
	public void clear() {
		write();
		bag.clear();
	}

	public Object getIndex(Object entry, int i, CollectionPersister persister) {
		throw new UnsupportedOperationException("Bags don't have indexes");
	}

	public Object getElement(Object entry) {
		return entry;
	}

	public Object getSnapshotElement(Object entry, int i) {
		java.util.List sn = (java.util.List) getSnapshot();
		return sn.get(i);
	}

	public int occurrences(Object o) {
		read();
		Iterator iter = bag.iterator();
		int result=0;
		while ( iter.hasNext() ) {
			if ( o.equals( iter.next() ) ) result++;
		}
		return result;
	}

	// List OPERATIONS:

	/**
	 * @see java.util.List#add(int, Object)
	 */
	public void add(int i, Object o) {
		write();
		bag.add(i, o);
	}

	/**
	 * @see java.util.List#addAll(int, Collection)
	 */
	public boolean addAll(int i, Collection c) {
		if ( c.size()>0 ) {
			write();
			return bag.addAll(i, c);
		}
		else {
			return false;
		}
	}

	/**
	 * @see java.util.List#get(int)
	 */
	public Object get(int i) {
		read();
		return bag.get(i);
	}

	/**
	 * @see java.util.List#indexOf(Object)
	 */
	public int indexOf(Object o) {
		read();
		return bag.indexOf(o);
	}

	/**
	 * @see java.util.List#lastIndexOf(Object)
	 */
	public int lastIndexOf(Object o) {
		read();
		return bag.lastIndexOf(o);
	}

	/**
	 * @see java.util.List#listIterator()
	 */
	public ListIterator listIterator() {
		read();
		return new ListIteratorProxy( bag.listIterator() );
	}

	/**
	 * @see java.util.List#listIterator(int)
	 */
	public ListIterator listIterator(int i) {
		read();
		return new ListIteratorProxy( bag.listIterator(i) );
	}

	/**
	 * @see java.util.List#remove(int)
	 */
	public Object remove(int i) {
		write();
		return bag.remove(i);
	}

	/**
	 * @see java.util.List#set(int, Object)
	 */
	public Object set(int i, Object o) {
		write();
		return bag.set(i, o);
	}

	/**
	 * @see java.util.List#subList(int, int)
	 */
	public List subList(int start, int end) {
		read();
		return new ListProxy( bag.subList(start, end) );
	}

	public String toString() {
		read();
		return bag.toString();
	}

	/*public boolean equals(Object other) {
		read();
		return bag.equals(other);
	}

	public int hashCode(Object other) {
		read();
		return bag.hashCode();
	}*/

	public boolean entryExists(Object entry, int i) {
		return entry!=null;
	}

	/**
	 * Bag does not respect the collection API and do an
	 * JVM instance comparison to do the equals.
	 * The semantic is broken not to have to initialize a
	 * collection for a simple equals() operation.
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return super.hashCode();
	}

}
