//$Id: AbstractPersistentCollection.java,v 1.16 2005/03/18 04:34:27 oneovthafew Exp $
package org.hibernate.collection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.LazyInitializationException;
import org.hibernate.engine.CollectionSnapshot;
import org.hibernate.engine.ForeignKeys;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.TypedValue;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.Type;
import org.hibernate.util.EmptyIterator;

/**
 * Base class implementing <tt>PersistentCollection</tt>
 * @see PersistentCollection
 * @author Gavin King
 */
public abstract class AbstractPersistentCollection 
	implements Serializable, PersistentCollection {

	private transient SessionImplementor session;
	private boolean initialized;
	private transient List additions;
	private CollectionSnapshot collectionSnapshot;
	private transient boolean directlyAccessible;
	private transient boolean initializing;

	//Careful: these methods do not initialize the collection.
	/**
	 * Is the initialized collection empty?
	 */
	public abstract boolean empty();
	/**
	 * Called by any read-only method of the collection interface
	 */
	protected final void read() {
		initialize(false);
	}
	/**
	 * Is the collection currently connected to an open session?
	 */
	private final boolean isConnectedToSession() {
		return session!=null && session.isOpen();
	}

	/**
	 * Called by any writer method of the collection interface
	 */
	protected final void write() {
		initialize(true);
		collectionSnapshot.setDirty();
	}
	/**
	 * Is this collection in a state that would allow us to
	 * "queue" additions?
	 */
	private boolean isQueueAdditionEnabled() {
		return !initialized &&
			isConnectedToSession() &&
			session.getPersistenceContext().isInverseCollection(this);
	}
	/**
	 * Queue an addition
	 */
	protected final boolean queueAdd(Object element) {
		if ( isQueueAdditionEnabled() ) {
			if (additions==null) additions = new ArrayList(10);
			additions.add(element);
			collectionSnapshot.setDirty(); //needed so that we remove this collection from the second-level cache
			return true;
		}
		else {
			return false;
		}
	}
	/**
	 * Queue additions
	 */
	protected final boolean queueAddAll(Collection coll) {
		if ( isQueueAdditionEnabled() ) {
			if (additions==null) additions = new ArrayList(20);
			additions.addAll(coll);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * After reading all existing elements from the database,
	 * add the queued elements to the underlying collection.
	 */
	public void delayedAddAll(Collection coll) {
		throw new AssertionFailure("Collection does not support delayed initialization");
	}

	/**
	 * After flushing, clear any "queued" additions, since the
	 * database state is now synchronized with the memory state.
	 */
	public void postFlush() {
		if (additions!=null) additions=null;
	}

	/**
	 * Not called by Hibernate, but used by non-JDK serialization,
	 * eg. SOAP libraries.
	 */
	public AbstractPersistentCollection() {}

	protected AbstractPersistentCollection(SessionImplementor session) {
		this.session = session;
	}

	/**
	 * return the user-visible collection (or array) instance
	 */
	public Object getValue() {
		return this;
	}

	/**
	 * Called just before reading any rows from the JDBC result set
	 */
	public void beginRead() {
		// override on some subclasses
		initializing = true;
	}

	/**
	 * Called after reading all rows from the JDBC result set
	 */
	public boolean endRead() {
		//override on some subclasses

		setInitialized();
		//do this bit after setting initialized to true or it will recurse
		if (additions!=null) {
			delayedAddAll(additions);
			additions=null;
			return false;
		}
		else {
			return true;
		}
	}

	/**
	 * Initialize the collection, if possible, wrapping any exceptions
	 * in a runtime exception
	 * @param writing currently obsolete
	 * @throws LazyInitializationException if we cannot initialize
	 */
	protected final void initialize(boolean writing) {
		if (!initialized) {
			if (initializing) throw new LazyInitializationException("cannot access loading collection");
			if ( isConnectedToSession() )  {
				if ( session.isConnected() ) {
					session.initializeCollection(this, writing);
				}
				else {
                    String name = (getCollectionSnapshot()!=null)?"("+getCollectionSnapshot().getRole()+")":"";
					throw new LazyInitializationException("failed to lazily initialize a collection " + name + " - session is disconnected");
				}
			}
			else {
                String name = (getCollectionSnapshot()!=null)?"("+getCollectionSnapshot().getRole()+")":"";
				throw new LazyInitializationException("failed to lazily initialize a collection " + name + " - no session or session was closed");
			}
		}
	}

	protected final void setInitialized() {
		this.initializing = false;
		this.initialized = true;
	}

	protected final void setDirectlyAccessible(boolean directlyAccessible) {
		this.directlyAccessible = directlyAccessible;
	}

	/**
	 * Could the application possibly have a direct reference to
	 * the underlying collection implementation?
	 */
	public boolean isDirectlyAccessible() {
		return directlyAccessible;
	}

	/**
	 * Disassociate this collection from the given session.
	 * @return true if this was currently associated with the given session
	 */
	public final boolean unsetSession(SessionImplementor currentSession) {
		if (currentSession==this.session) {
			this.session=null;
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * Associate the collection with the given session.
	 * @return false if the collection was already associated with the session
	 * @throws HibernateException if the collection was already associated
	 * with another open session
	 */
	public final boolean setCurrentSession(SessionImplementor session) throws HibernateException {
		if (session==this.session) {
			return false;
		}
		else {
			if ( isConnectedToSession() ) {
				throw new HibernateException("Illegal attempt to associate a collection with two open sessions");
			}
			else {
				this.session = session;
				return true;
			}
		}
	}

	/**
	 * Do we need to completely recreate this collection when it changes?
	 */
	public boolean needsRecreate(CollectionPersister persister) {
		return false;
	}
	/**
	 * Return a new snapshot of the current state of the collection,
	 * or null if no persister is passed
	 */
	public final Serializable getSnapshot(CollectionPersister persister) throws HibernateException {
		return (persister==null) ? null : snapshot(persister);
	}

	/**
	 * Return a new snapshot of the current state
	 */
	protected abstract Serializable snapshot(CollectionPersister persister) throws HibernateException;
	
	/**
	 * To be called internally by the session, forcing
	 * immediate initialization.
	 */
	public final void forceInitialization() throws HibernateException {
		if (!initialized) {
			if (initializing) {
				throw new AssertionFailure("force initialize loading collection");
			}
			if (session==null) {
				throw new HibernateException("collection is not associated with any session");
			}
			if ( !session.isConnected() ) {
				throw new HibernateException("disconnected session");
			}
			session.initializeCollection(this, false);
		}
	}


	/**
	 * Get the current snapshot from the session
	 */
	protected final Serializable getSnapshot() {
		return session.getPersistenceContext().getSnapshot(this);
	}

	/**
	 * Is this instance initialized?
	 */
	public final boolean wasInitialized() {
		return initialized;
	}
	
	public boolean isRowUpdatePossible() {
		return true;
	}

	/**
	 * Does this instance have any "queued" additions?
	 */
	public final boolean hasQueuedAdditions() {
		return additions!=null;
	}
	/**
	 * Iterate the "queued" additions
	 */
	public final Iterator queuedAdditionIterator() {
		return hasQueuedAdditions() ?
			additions.iterator() :
			EmptyIterator.INSTANCE;
	}

	/**
	 * Returns the collectionSnapshot.
	 * @return CollectionSnapshot
	 */
	public CollectionSnapshot getCollectionSnapshot() {
		return collectionSnapshot;
	}

	/**
	 * Sets the collectionSnapshot.
	 * @param collectionSnapshot The collectionSnapshot to set
	 */
	public void setCollectionSnapshot(CollectionSnapshot collectionSnapshot) {
		this.collectionSnapshot = collectionSnapshot;
	}

	/**
	 * Called before inserting rows, to ensure that any surrogate keys
	 * are fully generated
	 */
	public void preInsert(CollectionPersister persister) throws HibernateException {}
	/**
	 * Called after inserting a row, to fetch the natively generated id
	 */
	public void afterRowInsert(CollectionPersister persister, Object entry, int i) throws HibernateException {}
	/**
	 * get all "orphaned" elements
	 * @param entityName TODO
	 */
	public abstract Collection getOrphans(Serializable snapshot, String entityName) throws HibernateException;

	/**
	 * Get the current session
	 */
	protected final SessionImplementor getSession() {
		return session;
	}

	final class IteratorProxy implements Iterator {
		private final Iterator iter;
		IteratorProxy(Iterator iter) {
			this.iter=iter;
		}
		public boolean hasNext() {
			return iter.hasNext();
		}

		public Object next() {
			return iter.next();
		}

		public void remove() {
			write();
			iter.remove();
		}

	}

	final class ListIteratorProxy implements ListIterator {
		private final ListIterator iter;
		ListIteratorProxy(ListIterator iter) {
			this.iter = iter;
		}
		public void add(Object o) {
			write();
			iter.add(o);
		}

		public boolean hasNext() {
			return iter.hasNext();
		}

		public boolean hasPrevious() {
			return iter.hasPrevious();
		}

		public Object next() {
			return iter.next();
		}

		public int nextIndex() {
			return iter.nextIndex();
		}

		public Object previous() {
			return iter.previous();
		}

		public int previousIndex() {
			return iter.previousIndex();
		}

		public void remove() {
			write();
			iter.remove();
		}

		public void set(Object o) {
			write();
			iter.set(o);
		}

	}

	class SetProxy implements java.util.Set {

		final Collection set;

		SetProxy(Collection set) {
			this.set=set;
		}
		public boolean add(Object o) {
			write();
			return set.add(o);
		}

		public boolean addAll(Collection c) {
			write();
			return set.addAll(c);
		}

		public void clear() {
			write();
			set.clear();
		}

		public boolean contains(Object o) {
			return set.contains(o);
		}

		public boolean containsAll(Collection c) {
			return set.containsAll(c);
		}

		public boolean isEmpty() {
			return set.isEmpty();
		}

		public Iterator iterator() {
			return new IteratorProxy( set.iterator() );
		}

		public boolean remove(Object o) {
			write();
			return set.remove(o);
		}

		public boolean removeAll(Collection c) {
			write();
			return set.removeAll(c);
		}

		public boolean retainAll(Collection c) {
			write();
			return set.retainAll(c);
		}

		public int size() {
			return set.size();
		}

		public Object[] toArray() {
			return set.toArray();
		}

		public Object[] toArray(Object[] array) {
			return set.toArray(array);
		}

	}

	final class ListProxy implements java.util.List {

		private final java.util.List list;

		ListProxy(java.util.List list) {
			this.list = list;
		}

		public void add(int index, Object value) {
			write();
			list.add(index, value);
		}

		/**
		 * @see java.util.Collection#add(Object)
		 */
		public boolean add(Object o) {
			write();
			return list.add(o);
		}

		/**
		 * @see java.util.Collection#addAll(Collection)
		 */
		public boolean addAll(Collection c) {
			write();
			return list.addAll(c);
		}

		/**
		 * @see java.util.List#addAll(int, Collection)
		 */
		public boolean addAll(int i, Collection c) {
			write();
			return list.addAll(i, c);
		}

		/**
		 * @see java.util.Collection#clear()
		 */
		public void clear() {
			write();
			list.clear();
		}

		/**
		 * @see java.util.Collection#contains(Object)
		 */
		public boolean contains(Object o) {
			return list.contains(o);
		}

		/**
		 * @see java.util.Collection#containsAll(Collection)
		 */
		public boolean containsAll(Collection c) {
			return list.containsAll(c);
		}

		/**
		 * @see java.util.List#get(int)
		 */
		public Object get(int i) {
			return list.get(i);
		}

		/**
		 * @see java.util.List#indexOf(Object)
		 */
		public int indexOf(Object o) {
			return list.indexOf(o);
		}

		/**
		 * @see java.util.Collection#isEmpty()
		 */
		public boolean isEmpty() {
			return list.isEmpty();
		}

		/**
		 * @see java.util.Collection#iterator()
		 */
		public Iterator iterator() {
			return new IteratorProxy( list.iterator() );
		}

		/**
		 * @see java.util.List#lastIndexOf(Object)
		 */
		public int lastIndexOf(Object o) {
			return list.lastIndexOf(o);
		}

		/**
		 * @see java.util.List#listIterator()
		 */
		public ListIterator listIterator() {
			return new ListIteratorProxy( list.listIterator() );
		}

		/**
		 * @see java.util.List#listIterator(int)
		 */
		public ListIterator listIterator(int i) {
			return new ListIteratorProxy( list.listIterator(i) );
		}

		/**
		 * @see java.util.List#remove(int)
		 */
		public Object remove(int i) {
			write();
			return list.remove(i);
		}

		/**
		 * @see java.util.Collection#remove(Object)
		 */
		public boolean remove(Object o) {
			write();
			return list.remove(o);
		}

		/**
		 * @see java.util.Collection#removeAll(Collection)
		 */
		public boolean removeAll(Collection c) {
			write();
			return list.removeAll(c);
		}

		/**
		 * @see java.util.Collection#retainAll(Collection)
		 */
		public boolean retainAll(Collection c) {
			write();
			return list.retainAll(c);
		}

		/**
		 * @see java.util.List#set(int, Object)
		 */
		public Object set(int i, Object o) {
			write();
			return list.set(i, o);
		}

		/**
		 * @see java.util.Collection#size()
		 */
		public int size() {
			return list.size();
		}

		/**
		 * @see java.util.List#subList(int, int)
		 */
		public List subList(int i, int j) {
			return list.subList(i, j);
		}

		/**
		 * @see java.util.Collection#toArray()
		 */
		public Object[] toArray() {
			return list.toArray();
		}

		/**
		 * @see java.util.Collection#toArray(Object[])
		 */
		public Object[] toArray(Object[] array) {
			return list.toArray(array);
		}

	}


	protected static Collection getOrphans(
			Collection oldElements, 
			Collection currentElements, 
			String entityName, 
			SessionImplementor session)
	throws HibernateException {

		// short-circuit(s)
		if ( currentElements.size()==0 ) return oldElements; // no new elements, the old list contains only Orphans
		if ( oldElements.size()==0) return oldElements; // no old elements, so no Orphans neither
		
		Type idType = session.getFactory().getEntityPersister(entityName).getIdentifierType();

		// create the collection holding the Orphans
		Collection res = new ArrayList();

		// collect EntityIdentifier(s) of the *current* elements - add them into a HashSet for fast access
		java.util.Set currentIds = new HashSet();
		for ( Iterator it=currentElements.iterator(); it.hasNext(); ) {
			Object current = it.next();
			if ( current!=null && ForeignKeys.isNotTransient(entityName, current, null, session) ) {
				Serializable currentId = ForeignKeys.getEntityIdentifierIfNotUnsaved(entityName, current, session);
				currentIds.add( new TypedValue( idType, currentId, session.getEntityMode() ) );
			}
		}

		// iterate over the *old* list
		for ( Iterator it=oldElements.iterator(); it.hasNext(); ) {
			Object old = it.next();
			Serializable oldId = ForeignKeys.getEntityIdentifierIfNotUnsaved(entityName, old, session);
			if ( !currentIds.contains( new TypedValue( idType, oldId, session.getEntityMode() ) ) ) res.add(old);
		}

		return res;
	}

	static void identityRemove(
			Collection list, 
			Object object, 
			String entityName, 
			SessionImplementor session)
	throws HibernateException {

		if ( object!=null && ForeignKeys.isNotTransient(entityName, object, null, session) ) {
			
			Type idType = session.getFactory().getEntityPersister(entityName).getIdentifierType();

			Serializable idOfCurrent = ForeignKeys.getEntityIdentifierIfNotUnsaved(entityName, object, session);
			Iterator iter = list.iterator();
			while ( iter.hasNext() ) {
				Serializable idOfOld = ForeignKeys.getEntityIdentifierIfNotUnsaved(entityName, iter.next(), session);
				if ( idType.isEqual( idOfCurrent, idOfOld, session.getEntityMode(), session.getFactory() ) ) {
					iter.remove();
					break;
				}
			}

		}
	}
	
	public Object getIdentifier(Object entry, int i) {
		throw new UnsupportedOperationException();
	}
	
}
