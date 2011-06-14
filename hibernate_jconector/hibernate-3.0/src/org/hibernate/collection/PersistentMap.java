//$Id: PersistentMap.java,v 1.13 2005/03/16 04:45:22 oneovthafew Exp $
package org.hibernate.collection;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;
import org.hibernate.util.LinkedHashCollectionHelper;


/**
 * A persistent wrapper for a <tt>java.util.Map</tt>. Underlying collection
 * is a <tt>HashMap</tt>.
 *
 * @see java.util.HashMap
 * @author Gavin King
 */
public class PersistentMap extends AbstractPersistentCollection implements java.util.Map {

	java.util.Map map;

	protected Serializable snapshot(CollectionPersister persister) 
	throws HibernateException {
		EntityMode entityMode = getSession().getEntityMode();
		
		HashMap clonedMap = new HashMap( map.size() );
		Iterator iter = map.entrySet().iterator();
		while ( iter.hasNext() ) {
			java.util.Map.Entry e = (java.util.Map.Entry) iter.next();
			final Object copy = persister.getElementType()
				.deepCopy( e.getValue(), entityMode, persister.getFactory() );
			clonedMap.put( e.getKey(), copy );
		}
		return clonedMap;
	}

	public Collection getOrphans(Serializable snapshot, String entityName) throws HibernateException {
		java.util.Map sn = (java.util.Map) snapshot;
		return getOrphans( sn.values(), map.values(), entityName, getSession() );
	}

	public boolean equalsSnapshot(CollectionPersister persister) throws HibernateException {
		Type elementType = persister.getElementType();
		java.util.Map xmap = (java.util.Map) getSnapshot();
		if ( xmap.size()!=this.map.size() ) return false;
		Iterator iter = map.entrySet().iterator();
		while ( iter.hasNext() ) {
			java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
			if ( elementType.isDirty( entry.getValue(), xmap.get( entry.getKey() ), getSession() ) ) return false;
		}
		return true;
	}

	public boolean isWrapper(Object collection) {
		return map==collection;
	}
	public PersistentMap(SessionImplementor session) {
		super(session);
	}

	public PersistentMap() {} //needed for SOAP libraries, etc

	public void beforeInitialize(CollectionPersister persister) {
		this.map = persister.hasOrdering() ?
			LinkedHashCollectionHelper.createLinkedHashMap() :
			new HashMap();
	}

	public PersistentMap(SessionImplementor session, java.util.Map map) {
		super(session);
		this.map = map;
		setInitialized();
		setDirectlyAccessible(true);
	}

	/**
	 * @see java.util.Map#size()
	 */
	public int size() {
		read();
		return map.size();
	}

	/**
	 * @see java.util.Map#isEmpty()
	 */
	public boolean isEmpty() {
		read();
		return map.isEmpty();
	}

	/**
	 * @see java.util.Map#containsKey(Object)
	 */
	public boolean containsKey(Object key) {
		read();
		return map.containsKey(key);
	}

	/**
	 * @see java.util.Map#containsValue(Object)
	 */
	public boolean containsValue(Object value) {
		read();
		return map.containsValue(value) ;
	}

	/**
	 * @see java.util.Map#get(Object)
	 */
	public Object get(Object key) {
		read();
		return map.get(key);
	}

	/**
	 * @see java.util.Map#put(Object, Object)
	 */
	public Object put(Object key, Object value) {
		write();
		return map.put(key, value);
	}

	/**
	 * @see java.util.Map#remove(Object)
	 */
	public Object remove(Object key) {
		write();
		return map.remove(key);
	}

	/**
	 * @see java.util.Map#putAll(java.util.Map puts)
	 */
	public void putAll(java.util.Map puts) {
		if ( puts.size()>0 ) {
			write();
			map.putAll(puts);
		}
	}

	/**
	 * @see java.util.Map#clear()
	 */
	public void clear() {
		write();
		map.clear();
	}

	/**
	 * @see java.util.Map#keySet()
	 */
	public java.util.Set keySet() {
		read();
		return new SetProxy( map.keySet() );
	}

	/**
	 * @see java.util.Map#values()
	 */
	public Collection values() {
		read();
		return new SetProxy( map.values() );
	}

	/**
	 * @see java.util.Map#entrySet()
	 */
	public java.util.Set entrySet() {
		read();
		return new EntrySetProxy( map.entrySet() );
	}

	public boolean empty() {
		return map.isEmpty();
	}

	public String toString() {
		read();
		return map.toString();
	}

	public Object readFrom(ResultSet rs, CollectionPersister persister, Object owner) 
	throws HibernateException, SQLException {
		Object element = persister.readElement( rs, owner, getSession() );
		Object index = persister.readIndex( rs, getSession() );
		map.put(index, element);
		return element;
	}

	public Iterator entries(CollectionPersister persister) {
		return map.entrySet().iterator();
	}

	/** a wrapper for Map.Entry sets */
	class EntrySetProxy implements java.util.Set {
		private final java.util.Set set;
		EntrySetProxy(java.util.Set set) {
			this.set=set;
		}
		public boolean add(Object entry) {
			//write(); -- doesn't
			return set.add(entry);
		}
		public boolean addAll(Collection entries) {
			//write(); -- doesn't
			return set.addAll(entries);
		}
		public void clear() {
			write();
			set.clear();
		}
		public boolean contains(Object entry) {
			return set.contains(entry);
		}
		public boolean containsAll(Collection entries) {
			return set.containsAll(entries);
		}
		public boolean isEmpty() {
			return set.isEmpty();
		}
		public Iterator iterator() {
			return new EntryIteratorProxy( set.iterator() );
		}
		public boolean remove(Object entry) {
			write();
			return set.remove(entry);
		}
		public boolean removeAll(Collection entries) {
			write();
			return set.removeAll(entries);
		}
		public boolean retainAll(Collection entries) {
			write();
			return set.retainAll(entries);
		}
		public int size() {
			return set.size();
		}
		// amazingly, these two will work because AbstractCollection
		// uses iterator() to fill the array
		public Object[] toArray() {
			return set.toArray();
		}
		public Object[] toArray(Object[] array) {
			return set.toArray(array);
		}
	}
	final class EntryIteratorProxy implements Iterator {
		private final Iterator iter;
		EntryIteratorProxy(Iterator iter) {
			this.iter=iter;
		}
		public boolean hasNext() {
			return iter.hasNext();
		}
		public Object next() {
			return new MapEntryProxy( (java.util.Map.Entry) iter.next() );
		}
		public void remove() {
			write();
			iter.remove();
		}
	}

	final class MapEntryProxy implements java.util.Map.Entry {
		private final java.util.Map.Entry me;
		MapEntryProxy( java.util.Map.Entry me ) {
			this.me = me;
		}
		public Object getKey() { return me.getKey(); }
		public Object getValue() { return me.getValue(); }
		public boolean equals(Object o) { return me.equals(o); }
		public int hashCode() { return me.hashCode(); }
		// finally, what it's all about...
		public Object setValue(Object value) {
			write();
			return me.setValue(value);
		}
	}

	public void initializeFromCache(CollectionPersister persister, Serializable disassembled, Object owner)
	throws HibernateException {
		beforeInitialize(persister);
		Serializable[] array = (Serializable[]) disassembled;
		for (int i=0; i<array.length; i+=2 ) map.put(
			persister.getIndexType().assemble( array[i], getSession(), owner ),
			persister.getElementType().assemble( array[i+1], getSession(), owner )
		);
		setInitialized();
	}

	public Serializable disassemble(CollectionPersister persister) throws HibernateException {

		Serializable[] result = new Serializable[ map.size() * 2 ];
		Iterator iter = map.entrySet().iterator();
		int i=0;
		while ( iter.hasNext() ) {
			java.util.Map.Entry e = (java.util.Map.Entry) iter.next();
			result[i++] = persister.getIndexType().disassemble( e.getKey(), getSession(), null );
			result[i++] = persister.getElementType().disassemble( e.getValue(), getSession(), null );
		}
		return result;

	}

	public Iterator getDeletes(CollectionPersister persister, boolean indexIsFormula) 
	throws HibernateException {
		java.util.List deletes = new ArrayList();
		Iterator iter = ( (java.util.Map) getSnapshot() ).entrySet().iterator();
		while ( iter.hasNext() ) {
			java.util.Map.Entry e = (java.util.Map.Entry) iter.next();
			Object key = e.getKey();
			if ( e.getValue()!=null && map.get(key)==null ) {
				deletes.add( indexIsFormula ? e.getValue() : key );
			}
		}
		return deletes.iterator();
	}

	public boolean needsInserting(Object entry, int i, Type elemType) 
	throws HibernateException {
		final java.util.Map sn = (java.util.Map) getSnapshot();
		java.util.Map.Entry e = (java.util.Map.Entry) entry;
		return e.getValue()!=null && sn.get( e.getKey() )==null;
	}

	public boolean needsUpdating(Object entry, int i, Type elemType) 
	throws HibernateException {
		final java.util.Map sn = (java.util.Map) getSnapshot();
		java.util.Map.Entry e = (java.util.Map.Entry) entry;
		Object snValue = sn.get( e.getKey() );
		return e.getValue()!=null &&
			snValue!=null &&
			elemType.isDirty( snValue, e.getValue(), getSession() );
	}


	public Object getIndex(Object entry, int i, CollectionPersister persister) {
		return ( (java.util.Map.Entry) entry ).getKey();
	}

	public Object getElement(Object entry) {
		return ( (java.util.Map.Entry) entry ).getValue();
	}

	public Object getSnapshotElement(Object entry, int i) {
		final java.util.Map sn = (java.util.Map) getSnapshot();
		return sn.get( ( (java.util.Map.Entry) entry ).getKey() );
	}

	public boolean equals(Object other) {
		read();
		return map.equals(other);
	}

	public int hashCode() {
		read();
		return map.hashCode();
	}

	public boolean entryExists(Object entry, int i) {
		return ( (Map.Entry) entry ).getValue()!=null;
	}

}
