//$Id: PersistentSortedSet.java,v 1.4 2005/02/16 12:50:11 oneovthafew Exp $
package org.hibernate.collection;

import java.io.Serializable;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Comparator;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.BasicCollectionPersister;
import org.hibernate.persister.collection.CollectionPersister;


/**
 * A persistent wrapper for a <tt>java.util.SortedSet</tt>. Underlying
 * collection is a <tt>TreeSet</tt>.
 *
 * @see java.util.TreeSet
 * @author <a href="mailto:doug.currie@alum.mit.edu">e</a>
 */
public class PersistentSortedSet extends PersistentSet implements java.util.SortedSet {

	private Comparator comparator;

	protected Serializable snapshot(BasicCollectionPersister persister, EntityMode entityMode) throws HibernateException {
		//if (set==null) return new Set(session);
		TreeMap clonedSet = new TreeMap(comparator);
		Iterator iter = set.iterator();
		while ( iter.hasNext() ) {
			Object copy = persister.getElementType().deepCopy( iter.next(), entityMode, persister.getFactory() );
			clonedSet.put(copy, copy);
		}
		return clonedSet;
	}

	public void setComparator(Comparator comparator) {
		this.comparator = comparator;
	}

	public void beforeInitialize(CollectionPersister persister) {
		this.set = new TreeSet(comparator);
	}

	public PersistentSortedSet(SessionImplementor session) {
		super(session);
	}

	public PersistentSortedSet(SessionImplementor session, java.util.SortedSet set) {
		super(session, set);
		comparator = set.comparator();
	}

	public PersistentSortedSet() {} //needed for SOAP libraries, etc

	/**
	 * @see PersistentSortedSet#comparator()
	 */
	public Comparator comparator() {
		return comparator;
	}

	/**
	 * @see PersistentSortedSet#subSet(Object,Object)
	 */
	public java.util.SortedSet subSet(Object fromElement, Object toElement) {
		read();
		java.util.SortedSet s;
		s = ( (java.util.SortedSet) set ).subSet(fromElement, toElement);
		return new SubSetProxy(s);
	}

	/**
	 * @see PersistentSortedSet#headSet(Object)
	 */
	public java.util.SortedSet headSet(Object toElement) {
		read();
		java.util.SortedSet s = ( (java.util.SortedSet) set ).headSet(toElement);
		return new SubSetProxy(s);
	}

	/**
	 * @see PersistentSortedSet#tailSet(Object)
	 */
	public java.util.SortedSet tailSet(Object fromElement) {
		read();
		java.util.SortedSet s = ( (java.util.SortedSet) set ).tailSet(fromElement);
		return new SubSetProxy(s);
	}

	/**
	 * @see PersistentSortedSet#first()
	 */
	public Object first() {
		read();
		return ( (java.util.SortedSet) set ).first();
	}

	/**
	 * @see PersistentSortedSet#last()
	 */
	public Object last() {
		read();
		return ( (java.util.SortedSet) set ).last();
	}

	/** wrapper for subSets to propagate write to its backing set */
	class SubSetProxy extends SetProxy implements java.util.SortedSet {

		SubSetProxy(java.util.SortedSet s) {
			super(s);
		}

		public Comparator comparator() {
			return ( (java.util.SortedSet) this.set ).comparator();
		}

		public Object first() {
			return ( (java.util.SortedSet) this.set ).first();
		}

		public java.util.SortedSet headSet(Object toValue) {
			return new SubSetProxy( ( (java.util.SortedSet) this.set ).headSet(toValue) );
		}

		public Object last() {
			return ( (java.util.SortedSet) this.set ).last();
		}

		public java.util.SortedSet subSet(Object fromValue, Object toValue) {
			return new SubSetProxy( ( (java.util.SortedSet) this.set ).subSet(fromValue, toValue) );
		}

		public java.util.SortedSet tailSet(Object fromValue) {
			return new SubSetProxy( ( (java.util.SortedSet) this.set ).tailSet(fromValue) );
		}

	}

	public PersistentSortedSet(
		SessionImplementor session,
		CollectionPersister persister,
		Comparator comparator,
		Serializable disassembled,
		Object owner)
		throws HibernateException {

		this(session);
		this.comparator=comparator;
		beforeInitialize(persister);
		Serializable[] array = (Serializable[]) disassembled;
		for (int i=0; i<array.length; i++ ) set.add(
			persister.getElementType().assemble( array[i], session, owner )
		);
		setInitialized();
	}

}







