//$Id: PersistentSortedMap.java,v 1.4 2005/02/16 12:50:11 oneovthafew Exp $
package org.hibernate.collection;


import java.io.Serializable;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Comparator;
import java.util.Collection;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.BasicCollectionPersister;
import org.hibernate.persister.collection.CollectionPersister;


/**
 * A persistent wrapper for a <tt>java.util.SortedMap</tt>. Underlying
 * collection is a <tt>TreeMap</tt>.
 *
 * @see java.util.TreeMap
 * @author <a href="mailto:doug.currie@alum.mit.edu">e</a>
 */
public class PersistentSortedMap extends PersistentMap implements java.util.SortedMap {

	private Comparator comparator;

	protected Serializable snapshot(BasicCollectionPersister persister, EntityMode entityMode) throws HibernateException {
		TreeMap clonedMap = new TreeMap(comparator);
		Iterator iter = map.entrySet().iterator();
		while ( iter.hasNext() ) {
			java.util.Map.Entry e = (java.util.Map.Entry) iter.next();
			clonedMap.put( e.getKey(), persister.getElementType().deepCopy( e.getValue(), entityMode, persister.getFactory() ) );
		}
		return clonedMap;
	}

	public PersistentSortedMap(SessionImplementor session) {
		super(session);
	}

	public PersistentSortedMap(SessionImplementor session, CollectionPersister persister, Comparator comparator, Serializable disassembled, Object owner)
	throws HibernateException {
		super(session);
		this.comparator=comparator;
		beforeInitialize(persister);
		Serializable[] array = (Serializable[]) disassembled;
		for (int i=0; i<array.length; i+=2 ) map.put(
			persister.getIndexType().assemble( array[i], session, owner ),
			persister.getElementType().assemble( array[i+1], session, owner )
		);
		setInitialized();
	}

	public void beforeInitialize(CollectionPersister persister) {
		this.map = new TreeMap(comparator);
	}

	public void setComparator(Comparator comparator) {
		this.comparator = comparator;
	}

	//need to distinguish between the different 2-argument constructors
	/*public SortedMap(SessionImplementor session, Comparator comp) {
		super(session);
		this.map = new TreeMap(comp);
	}*/

	public PersistentSortedMap(SessionImplementor session, java.util.SortedMap map) {
		super(session, map);
		comparator = map.comparator();
	}

	public PersistentSortedMap() {} //needed for SOAP libraries, etc

	/**
	 * @see PersistentSortedMap#comparator()
	 */
	public Comparator comparator() {
		return comparator;
	}

	/**
	 * @see PersistentSortedMap#subMap(Object, Object)
	 */
	public java.util.SortedMap subMap(Object fromKey, Object toKey) {
		read();
		java.util.SortedMap m = ( (java.util.SortedMap) map ).subMap(fromKey, toKey);
		return new SortedSubMap(m);
	}

	/**
	 * @see PersistentSortedMap#headMap(Object)
	 */
	public java.util.SortedMap headMap(Object toKey) {
		read();
		java.util.SortedMap m;
		m = ( (java.util.SortedMap) map ).headMap(toKey);
		return new SortedSubMap(m);
	}

	/**
	 * @see PersistentSortedMap#tailMap(Object)
	 */
	public java.util.SortedMap tailMap(Object fromKey) {
		read();
		java.util.SortedMap m;
		m = ( (java.util.SortedMap) map ).tailMap(fromKey);
		return new SortedSubMap(m);
	}

	/**
	 * @see PersistentSortedMap#firstKey()
	 */
	public Object firstKey() {
		read();
		return ( (java.util.SortedMap) map ).firstKey();
	}

	/**
	 * @see PersistentSortedMap#lastKey()
	 */
	public Object lastKey() {
		read();
		return ( (java.util.SortedMap) map ).lastKey();
	}

	class SortedSubMap implements java.util.SortedMap {

		java.util.SortedMap submap;

		SortedSubMap(java.util.SortedMap m) {
			this.submap = m;
		}
		// from Map
		public int size() {
			return submap.size();
		}
		public boolean isEmpty() {
			return submap.isEmpty();
		}
		public boolean containsKey(Object key) {
			return submap.containsKey(key);
		}
		public boolean containsValue(Object key) {
			return submap.containsValue(key) ;
		}
		public Object get(Object key) {
			return submap.get(key);
		}
		public Object put(Object key, Object value) {
			write();
			return submap.put(key,  value);
		}
		public Object remove(Object key) {
			write();
			return submap.remove(key);
		}
		public void putAll(java.util.Map other) {
			write();
			submap.putAll(other);
		}
		public void clear() {
			write();
			submap.clear();
		}
		public java.util.Set keySet() {
			return new SetProxy( submap.keySet() );
		}
		public Collection values() {
			return new SetProxy( submap.values() );
		}
		public java.util.Set entrySet() {
			return new EntrySetProxy( submap.entrySet() );
		}
		// from SortedMap
		public Comparator comparator() {
			return submap.comparator();
		}
		public java.util.SortedMap subMap(Object fromKey, Object toKey) {
			java.util.SortedMap m;
			m = submap.subMap(fromKey, toKey);
			return new SortedSubMap( m );
		}
		public java.util.SortedMap headMap(Object toKey) {
			java.util.SortedMap m;
			m = submap.headMap(toKey);
			return new SortedSubMap(m);
		}
		public java.util.SortedMap tailMap(Object fromKey) {
			java.util.SortedMap m;
			m = submap.tailMap(fromKey);
			return new SortedSubMap(m);
		}
		public Object firstKey() {
			return  submap.firstKey();
		}
		public Object lastKey() {
			return submap.lastKey();
		}

	}

}







