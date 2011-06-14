//$Id: CollectionEntry.java,v 1.10 2005/02/20 10:07:39 oneovthafew Exp $
package org.hibernate.engine;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.util.CollectionHelper;

/**
 * We need an entry to tell us all about the current state
 * of a collection with respect to its persistent state
 * 
 * @author Gavin King
 */
public final class CollectionEntry implements CollectionSnapshot, Serializable {

	private static final Log log = LogFactory.getLog(CollectionEntry.class);

	// collections detect changes made via
	// their public interface and mark
	// themselves as dirty
	private boolean dirty;
	// during flush, we navigate the object graph to
	// collections and decide what to do with them
	private transient boolean reached;
	private transient boolean processed;
	private transient boolean doupdate;
	private transient boolean doremove;
	private transient boolean dorecreate;
	// if we instantiate a collection during the flush() process,
	// we must ignore it for the rest of the flush()
	private transient boolean ignore;
	// collections might be lazily initialized
	private boolean initialized;
	// "current" means the reference that was found
	// during flush() and "loaded" means the reference
	// that is consistent with the current database
	// state
	private transient CollectionPersister currentPersister;
	private transient Serializable currentKey;
	private transient CollectionPersister loadedPersister;
	private Serializable loadedKey;
	// session-start/post-flush persistent state
	private Serializable snapshot;
	// allow the snapshot to be serialized
	private String role;

	public CollectionEntry() {
		this.dirty=false; //a newly wrapped collection is NOT dirty (or we get unnecessary version updates)
		this.initialized=true;
		// new collections that get found + wrapped
		// during flush shouldn't be ignored
		this.ignore=false;
	}

	CollectionEntry(CollectionPersister loadedPersister, Serializable loadedKey) {
		// detached collection wrappers that get found + reattached
		// during flush shouldn't be ignored
		this(loadedPersister, loadedKey, false);
	}

	CollectionEntry(CollectionPersister loadedPersister, Serializable loadedKey, boolean ignore) {
		this.dirty=false;
		this.initialized=false;
		this.ignore=ignore;
		this.loadedKey=loadedKey;
		setLoadedPersister(loadedPersister);
	}

	CollectionEntry(CollectionSnapshot cs, SessionFactoryImplementor factory)
	throws MappingException {
		this.dirty=cs.getDirty();
		this.initialized=true;
		// detached collections that get found + reattached
		// during flush shouldn't be ignored
		this.ignore=false;

		this.loadedKey=cs.getKey();
		setLoadedPersister( factory.getCollectionPersister( cs.getRole() ) );

		this.snapshot=cs.getSnapshot();
	}

	private boolean isDirty(PersistentCollection coll) throws HibernateException {
		if ( isDirty() ) {
			return true;
		}
		else if (
			!coll.isDirectlyAccessible() &&
			!getLoadedPersister().getElementType().isMutable()
		) {
			return false;
		}
		else {
			return !coll.equalsSnapshot( getLoadedPersister() );
		}
	}

	public void preFlush(PersistentCollection collection) throws HibernateException {

		this.dirty = ( isInitialized() && getLoadedPersister() != null && isDirty(collection) ) ||
			( !isInitialized() && isDirty() );

		if ( log.isDebugEnabled() && isDirty() && getLoadedPersister() != null ) {
			log.debug(
					"Collection dirty: " +
					MessageHelper.collectionInfoString( getLoadedPersister().getRole(), getLoadedKey() )
			);
		}

		setDoupdate(false);
		setDoremove(false);
		setDorecreate(false);
		setReached(false);
		setProcessed(false);
	}

	public void postInitialize(PersistentCollection collection) throws HibernateException {
		this.initialized = true;
		this.snapshot = collection.getSnapshot( getLoadedPersister() );
	}

	/**
	 * Called after a successful flush, returning true if
	 * the collection has been dereferenced, and the 
	 * entry can be removed.
	 */
	public boolean postFlush(PersistentCollection collection) throws HibernateException {

		if ( isIgnore() ) {
			this.ignore = false;
		}
		else {
			if ( !isProcessed() ) throw new AssertionFailure( "collection was not processed by flush()" );
			this.loadedKey = getCurrentKey();
			setLoadedPersister( getCurrentPersister() );
			this.dirty = false;
			collection.postFlush();
			if ( isInitialized() && ( isDoremove() || isDorecreate() || isDoupdate() ) ) {
				initSnapshot( collection, getLoadedPersister() ); //re-snapshot
			}
		}
		
		return getLoadedPersister() == null;

	}

	public void initSnapshot(PersistentCollection collection, CollectionPersister persister)
	throws HibernateException {
		this.snapshot = collection.getSnapshot( persister );
	}

	public boolean getDirty() {
		return isDirty();
	}

	public Serializable getKey() {
		return getLoadedKey();
	}

	public String getRole() {
		return role;
	}

	public Serializable getSnapshot() {
		return snapshot;
	}

	public boolean snapshotIsEmpty() {
		//TODO: implementation here is non-extensible ...
		//should use polymorphism
		return isInitialized() && getSnapshot() != null && (
			(getSnapshot() instanceof Collection && ( ( Collection ) getSnapshot() ).size() == 0) || // if snapshot is a collection
			(getSnapshot() instanceof Map && ( ( Map ) getSnapshot() ).size() == 0) || // if snapshot is a map
			(getSnapshot().getClass().isArray() && Array.getLength( getSnapshot() ) == 0)// if snapshot is an array
		);
	}

	public void setDirty() {
		this.dirty = true;
	}

	void setLoadedPersister(CollectionPersister persister) {
		loadedPersister = persister;
		setRole( persister == null ? null : persister.getRole() );
	}

	public boolean isNew() {
		return isInitialized() && getSnapshot() == null; //TODO: is this a correct implementation?
	}

	public boolean wasDereferenced() {
		return getLoadedKey() == null;
	}

	public boolean isDirty() {
		return dirty;
	}

	public boolean isReached() {
		return reached;
	}

	public void setReached(boolean reached) {
		this.reached = reached;
	}

	public boolean isProcessed() {
		return processed;
	}

	public void setProcessed(boolean processed) {
		this.processed = processed;
	}

	public boolean isDoupdate() {
		return doupdate;
	}

	public void setDoupdate(boolean doupdate) {
		this.doupdate = doupdate;
	}

	public boolean isDoremove() {
		return doremove;
	}

	public void setDoremove(boolean doremove) {
		this.doremove = doremove;
	}

	public boolean isDorecreate() {
		return dorecreate;
	}

	public void setDorecreate(boolean dorecreate) {
		this.dorecreate = dorecreate;
	}

	public boolean isIgnore() {
		return ignore;
	}

	public boolean isInitialized() {
		return initialized;
	}

	public CollectionPersister getCurrentPersister() {
		return currentPersister;
	}

	public void setCurrentPersister(CollectionPersister currentPersister) {
		this.currentPersister = currentPersister;
	}

	/**
	 * This is only available late during the flush
	 * cycle
	 */
	public Serializable getCurrentKey() {
		return currentKey;
	}

	public void setCurrentKey(Serializable currentKey) {
		this.currentKey = currentKey;
	}
	
	/**
	 * This is only available late during the flush
	 * cycle
	 */
	public CollectionPersister getLoadedPersister() {
		return loadedPersister;
	}

	public Serializable getLoadedKey() {
		return loadedKey;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String toString() {
		String result = "CollectionEntry" + MessageHelper.collectionInfoString( loadedPersister.getRole(), loadedKey );
		if (currentPersister!=null) {
			result += "->" + MessageHelper.collectionInfoString( currentPersister.getRole(), currentKey );
		}
		return result;
	}

	public boolean isInitializedAndDirty() {
		return isInitialized() && isDirty();
	}

	/**
	 * Get the collection orphans (entities which were removed from the collection)
	 */
	public Collection getOrphans(String entityName, PersistentCollection coll) 
	throws HibernateException {
		return isNew() ? CollectionHelper.EMPTY_COLLECTION : coll.getOrphans( getSnapshot(), entityName );
	}

}