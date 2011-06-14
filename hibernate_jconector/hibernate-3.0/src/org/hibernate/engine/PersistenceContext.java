// $Id: PersistenceContext.java,v 1.24 2005/02/21 08:15:32 oneovthafew Exp $
package org.hibernate.engine;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.ReferenceMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.PersistentObjectException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.type.CollectionType;
import org.hibernate.util.IdentityMap;
import org.hibernate.util.MarkerObject;

/**
 * A <tt>PersistenceContext</tt> represents the state of persistent "stuff" which
 * Hibernate is tracking.  This includes persistent entities, collections,
 * as well as proxies generated.
 * </p>
 * There is meant to be a one-to-one correspondence between a SessionImpl and
 * a PersistentContext.  The SessionImpl uses the PersistentContext to track
 * the current state of its context.  Event-listeners then use the
 * PersistentContext to drive their processing.
 *
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole</a>
 */
public class PersistenceContext implements Serializable {

	// a possibility for "read-only" sessions would be to use a different 
	// PersistentContext impl that does not track the state of things

	private static final Log log = LogFactory.getLog( PersistenceContext.class );

	public static final Object NO_ROW = new MarkerObject("NO_ROW");

	private SessionImplementor session;
	
	// Loaded entity instances, by EntityKey
	private final Map entitiesByKey;

	// Loaded entity instances, by EntityUniqueKey
	private final Map entitiesByUniqueKey;
	
	// Identity map of EntityEntry instances, by the entity instance
	private transient Map entityEntries;
	
	// Entity proxies, by EntityKey
	private transient Map proxiesByKey;
	
	// Snapshots of current database state for entities
	// that have *not* been loaded
	private final Map entitySnapshotsByKey;
	
	// Identity map of array holder ArrayHolder instances, by the array instance
	private transient Map arrayHolders;
	
	// Identity map of CollectionEntry instances, by the collection wrapper
	private transient Map collectionEntries;
	
	// Collection wrappers, by the CollectionKey
	private final Map collectionsByKey; //key=CollectionKey, value=PersistentCollection
	
	// Set of EntityKeys of deleted objects
	private HashSet nullifiableEntityKeys = new HashSet();
	
	// EntityKeys that we have tried to load, and not found in the database
	private final HashSet nonExistantEntityKeys;

	// EntityUniqueKeys that we have tried to load, and not found in the database
	private final HashSet nonExistentEntityUniqueKeys;
	
	// properties that we have tried to load, and not found in the database
	private final HashSet nullAssociations;
	
	// A list of collection wrappers that were instantiating during result set
	// processing, that we will need to initialize at the end of the query
	private transient List nonlazyCollections;
	
	private transient int cascading = 0;
	private transient int loadCounter = 0;
	private transient boolean flushing = false;
	
	private boolean hasNonReadOnlyEntities = false;
	
	private transient CollectionLoadContext collectionLoadContext;
	private transient BatchFetchQueue batchFetchQueue;

	public SessionImplementor getSession() {
		return session;
	}
	
	public CollectionLoadContext getCollectionLoadContext() {
		if (collectionLoadContext==null) {
			collectionLoadContext = new CollectionLoadContext(this);
		}
		return collectionLoadContext;
	}
	
	/**
	 * Get the <tt>BatchFetchQueue</tt>, instantiating one if
	 * necessary.
	 */
	public BatchFetchQueue getBatchFetchQueue() {
		if (batchFetchQueue==null) {
			batchFetchQueue = new BatchFetchQueue(this);
		}
		return batchFetchQueue;
	}
	
	private static final int INIT_MAP_SIZE = 8;
	
	/**
	 * Constructs a PersistentContext, bound to the given session.
	 *
	 * @param session The session "owning" this context.
	 */
	public PersistenceContext(SessionImplementor session) {
		this.session = session;

		entitiesByKey = new HashMap(INIT_MAP_SIZE);
		entitiesByUniqueKey = new HashMap(INIT_MAP_SIZE);
		proxiesByKey = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);
		entitySnapshotsByKey = new HashMap(INIT_MAP_SIZE);
		nonExistantEntityKeys = new HashSet(INIT_MAP_SIZE);
		nonExistentEntityUniqueKeys = new HashSet(INIT_MAP_SIZE);
		nullAssociations = new HashSet(INIT_MAP_SIZE);
		entityEntries = IdentityMap.instantiateSequenced(INIT_MAP_SIZE);
		collectionEntries = IdentityMap.instantiateSequenced(INIT_MAP_SIZE);
		collectionsByKey = new HashMap(INIT_MAP_SIZE);
		arrayHolders = IdentityMap.instantiate(INIT_MAP_SIZE);

		initTransientState();
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		log.trace( "deserializing persistent-context" );
		ois.defaultReadObject();

		entityEntries = IdentityMap.deserialize( ois.readObject() );
		collectionEntries = IdentityMap.deserialize( ois.readObject() );
		arrayHolders = IdentityMap.deserialize( ois.readObject() );

		initTransientState();

		proxiesByKey = new ReferenceMap( ReferenceMap.HARD, ReferenceMap.WEAK );
		Map map = ( Map ) ois.readObject();
		proxiesByKey.putAll( map );

		// we need to reconnect all proxies and collections to this session
		// the association is transient because serialization is used for
		// different things.

		Iterator iter = collectionEntries.entrySet().iterator();
		while ( iter.hasNext() ) {
			try {
				Map.Entry e = ( Map.Entry ) iter.next();
				( ( PersistentCollection ) e.getKey() ).setCurrentSession( session );
				CollectionEntry ce = ( CollectionEntry ) e.getValue();
				if ( ce.getRole() != null ) {
					ce.setLoadedPersister( session.getFactory().getCollectionPersister( ce.getRole() ) );
				}
			}
			catch (HibernateException he) {
				throw new InvalidObjectException( he.getMessage() );
			}
		}

		iter = proxiesByKey.values().iterator();
		while ( iter.hasNext() ) {
			Object proxy = iter.next();
			if ( proxy instanceof HibernateProxy ) {
				( ( HibernateProxy ) proxy ).getHibernateLazyInitializer().setSession( session );
			}
			else {
				iter.remove(); //the proxy was pruned during the serialization process
			}
		}

		iter = entityEntries.entrySet().iterator();
		while ( iter.hasNext() ) {
			EntityEntry e = ( EntityEntry ) ( ( Map.Entry ) iter.next() ).getValue();
			try {
				e.setPersister( session.getFactory().getEntityPersister( e.getEntityName() ) );
			}
			catch (MappingException me) {
				throw new InvalidObjectException( me.getMessage() );
			}
		}
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		log.trace( "serializing persistent-context" );

		oos.defaultWriteObject();

		oos.writeObject( IdentityMap.serialize(entityEntries) );
		oos.writeObject( IdentityMap.serialize(collectionEntries) );
		oos.writeObject( IdentityMap.serialize(arrayHolders) );

		HashMap map = new HashMap(INIT_MAP_SIZE);
		map.putAll(proxiesByKey);
		oos.writeObject(map);
	}

	private void initTransientState() {
		nonlazyCollections = new ArrayList(INIT_MAP_SIZE);
	}

	public void clear() {
		arrayHolders.clear();
		entitiesByKey.clear();
		entitiesByUniqueKey.clear();
		entityEntries.clear();
		entitySnapshotsByKey.clear();
		collectionsByKey.clear();
		collectionEntries.clear();
		proxiesByKey.clear();
		nonExistantEntityKeys.clear();
		nullifiableEntityKeys.clear();
		nonExistentEntityUniqueKeys.clear();
		if (batchFetchQueue!=null) batchFetchQueue.clear();
		hasNonReadOnlyEntities=false;
	}
	
	public boolean hasNonReadOnlyEntities() {
		return hasNonReadOnlyEntities;
	}
	
	public void setEntryStatus(EntityEntry entry, Status status) {
		entry.setStatus(status);
		setHasNonReadOnlyEnties(status);
	}
	
	private void setHasNonReadOnlyEnties(Status status) {
		if ( status==Status.DELETED || status==Status.MANAGED || status==Status.SAVING ) {
			hasNonReadOnlyEntities = true;
		}
	}

	public void afterTransactionCompletion() {
		// Downgrade locks
		Iterator iter = entityEntries.values().iterator();
		while ( iter.hasNext() ) {
			( (EntityEntry) iter.next() ).setLockMode(LockMode.NONE);
		}
	}

	/**
	 * Get the current state of the entity as known to the underlying
	 * database, or null if there is no corresponding row 
	 */
	public Object[] getDatabaseSnapshot(Serializable id, EntityPersister persister)
	throws HibernateException {
		EntityKey key = new EntityKey( id, persister, session.getEntityMode() );
		Object cached = entitySnapshotsByKey.get(key);
		if (cached!=null) {
			return cached==NO_ROW ? null : (Object[]) cached;
		}
		else {
			Object[] snapshot = persister.getDatabaseSnapshot( id, session );
			entitySnapshotsByKey.put( key, snapshot==null ? NO_ROW : snapshot );
			return snapshot;
		}
	}

	public Object[] getCachedDatabaseSnapshot(EntityKey key) {
		//TODO: assertion failure if NO_ROW
		return (Object[]) entitySnapshotsByKey.get(key);
	}

	/*public void removeDatabaseSnapshot(EntityKey key) {
		entitySnapshotsByKey.remove(key);
	}*/

	public void addEntity(EntityKey key, Object object) {
		entitiesByKey.put(key, object);
		getBatchFetchQueue().removeBatchLoadableEntityKey(key);
	}

	/**
	 * Get the entity instance associated with the given 
	 * <tt>EntityKey</tt>
	 */
	public Object getEntity(EntityKey key) {
		return entitiesByKey.get(key);
	}

	public boolean containsEntity(EntityKey key) {
		return entitiesByKey.containsKey(key);
	}

	/**
	 * Remove an entity from the session cache, also clear
	 * up other state associated with the entity, all except
	 * for the <tt>EntityEntry</tt>
	 */
	public Object removeEntity(EntityKey key) {
		Object entity = entitiesByKey.remove(key);
		Iterator iter = entitiesByUniqueKey.values().iterator();
		while ( iter.hasNext() ) {
			if ( iter.next()==entity ) iter.remove();
		}
		entitySnapshotsByKey.remove(key);
		nullifiableEntityKeys.remove(key);
		getBatchFetchQueue().removeBatchLoadableEntityKey(key);
		getBatchFetchQueue().removeSubquery(key);
		return entity;
	}

	/**
	 * Get an entity cached by unique key
	 */
	public Object getEntity(EntityUniqueKey euk) {
		return entitiesByUniqueKey.get(euk);
	}

	/**
	 * Add an entity to the cache by unique key
	 */
	public void addEntity(EntityUniqueKey euk, Object entity) {
		entitiesByUniqueKey.put(euk, entity);
	}

	/**
	 * Retreive the EntityEntry representation of the given entity.
	 *
	 * @param object The entity for which to locate the EntityEntry.
	 * @return The EntityEntry for the given entity.
	 */
	public EntityEntry getEntry(Object object) {
		return (EntityEntry) entityEntries.get(object);
	}

	/**
	 * Remove an entity entry from the session cache
	 */
	public EntityEntry removeEntry(Object object) {
		return (EntityEntry) entityEntries.remove(object);
	}

	/**
	 * Is there an EntityEntry for this instance?
	 */
	public boolean isEntryFor(Object object) {
		return entityEntries.containsKey(object);
	}

	/**
	 * Get the collection entry for a persistent collection
	 */
	public CollectionEntry getCollectionEntry(PersistentCollection coll) {
		return (CollectionEntry) collectionEntries.get(coll);
	}

	/**
	 * Adds an entity to the internal caches.
	 */
	public EntityEntry addEntity(
	        final Object object,
			final Status status,
			final Object[] loadedState,
			final Serializable id,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final EntityPersister persister,
			final boolean disableVersionIncrement
	) {
		
		addEntity( new EntityKey( id, persister, session.getEntityMode() ), object );
		
		return addEntry(object,
		        status,
		        loadedState,
		        null,
		        id,
		        version,
		        lockMode,
		        existsInDatabase,
		        persister,
		        disableVersionIncrement);
	}


	/**
	 * Generates an appropriate EntityEntry instance and adds it 
	 * to the event source's internal caches.
	 */
	public EntityEntry addEntry(
	        final Object object,
			final Status status,
			final Object[] loadedState,
			final Object rowId,
			final Serializable id,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final EntityPersister persister,
			final boolean disableVersionIncrement) {
		EntityEntry e = new EntityEntry(
		        status,
		        loadedState,
		        rowId,
		        id,
		        version,
		        lockMode,
		        existsInDatabase,
		        persister,
		        session.getEntityMode(),
		        disableVersionIncrement
		);
		entityEntries.put(object, e);
		setHasNonReadOnlyEnties(status);
		return e;
	}
	
	public LockMode getLockMode(Object entity) {
		return getEntry(entity).getLockMode();
	}

	public void setLockMode(Object entity, LockMode lockMode) {
		getEntry(entity).setLockMode(lockMode);
	}

	public boolean containsProxy(Object object) {
		return proxiesByKey.values().contains( object );
	}

	/**
	 * Takes the given object and, if it represents a proxy, reassociates it with this event source.
	 *
	 * @param value The possible proxy to be reassociated.
	 * @return Whether the passed value represented an actual proxy which got initialized.
	 * @throws MappingException
	 */
	public boolean reassociateIfUninitializedProxy(Object value) throws MappingException {
		if ( !Hibernate.isInitialized(value) ) {
			HibernateProxy proxy = (HibernateProxy) value;
			LazyInitializer li = proxy.getHibernateLazyInitializer();
			reassociateProxy(li, proxy);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * If a deleted entity instance is re-saved, and it has a proxy, we need to
	 * reset the identifier of the proxy 
	 */
	public void reassociateProxy(Object value, Serializable id) throws MappingException {
		if ( value instanceof HibernateProxy ) {
			if ( log.isDebugEnabled() ) log.debug("setting proxy identifier: " + id);
			HibernateProxy proxy = (HibernateProxy) value;
			LazyInitializer li = proxy.getHibernateLazyInitializer();
			li.setIdentifier(id);
			reassociateProxy(li, proxy);
		}
	}

	/**
	 * Associate a proxy that was instantiated by another session with this session
	 */
	public void reassociateProxy(LazyInitializer li, HibernateProxy proxy) throws HibernateException {
		if ( li.getSession() != this ) {
			EntityPersister persister = session.getFactory().getEntityPersister( li.getEntityName() );
			EntityKey key = new EntityKey( li.getIdentifier(), persister, session.getEntityMode() );
			if ( !proxiesByKey.containsKey(key) ) proxiesByKey.put(key, proxy); // any earlier proxy takes precedence
			proxy.getHibernateLazyInitializer().setSession(session);
		}
	}

	/**
	 * Get the entity instance underlying the given proxy, throwing
	 * an exception if the proxy is uninitialized. If the given object
	 * is not a proxy, simply return the argument.
	 */
	public Object unproxy(Object maybeProxy) throws HibernateException {
		if ( maybeProxy instanceof HibernateProxy ) {
			HibernateProxy proxy = (HibernateProxy) maybeProxy;
			LazyInitializer li = proxy.getHibernateLazyInitializer();
			if ( li.isUninitialized() ) {
				throw new PersistentObjectException(
						"object was an uninitialized proxy for " +
						li.getEntityName()
				);
			}
			return li.getImplementation(); //unwrap the object
		}
		else {
			return maybeProxy;
		}
	}

	/**
	 * Possibly unproxy the given reference and reassociate it with the current session.
	 *
	 * @param maybeProxy The reference to be unproxied if it currently represents a proxy.
	 * @return The unproxied instance.
	 * @throws HibernateException
	 */
	public Object unproxyAndReassociate(Object maybeProxy) throws HibernateException {
		if ( maybeProxy instanceof HibernateProxy ) {
			HibernateProxy proxy = (HibernateProxy) maybeProxy;
			LazyInitializer li = proxy.getHibernateLazyInitializer();
			reassociateProxy(li, proxy);
			return li.getImplementation(); //initialize + unwrap the object
		}
		else {
			return maybeProxy;
		}
	}

	/**
	 * Attempts to check whether the given key represents an entity already loaded within the
	 * current session.
	 * @param id The key to be checked.
	 * @param object The entity reference against which to perform the uniqueness check.
	 * @throws HibernateException
	 */
	public void checkUniqueness(Serializable id, EntityPersister persister, Object object) throws HibernateException {
		Object entity = getEntity( new EntityKey( id, persister, session.getEntityMode() ) );
		if ( entity == object ) {
			throw new AssertionFailure( "object already associated, but no entry was found" );
		}
		if ( entity != null ) {
			throw new NonUniqueObjectException( id, persister.getEntityName() );
		}
	}

	/**
	 * If the existing proxy is insufficiently "narrow" (derived), instantiate a new proxy
	 * and overwrite the registration of the old one. This breaks == and occurs only for
	 * "class" proxies rather than "interface" proxies.
	 *
	 * @param proxy The proxied object to be narrowed.
	 * @param persister The persister for the proxied entity.
	 * @param key The internal cache key for the proxied entity.
	 * @param object (optional) the actual proxied entity instance.
	 * @return An appropriately narrowed instance.
	 * @throws HibernateException
	 */
	public Object narrowProxy(Object proxy, EntityPersister persister, EntityKey key, Object object)
	throws HibernateException {
		if ( !persister.getConcreteProxyClass( session.getEntityMode() ).isAssignableFrom( proxy.getClass() ) ) {

			if ( log.isWarnEnabled() )
				log.warn(
						"Narrowing proxy to " +
						persister.getConcreteProxyClass( session.getEntityMode() ) +
						" - this operation breaks =="
				);

			if ( object != null ) {
				proxiesByKey.remove(key);
				return object; //return the proxied object
			}
			else {
				proxy = persister.createProxy( key.getIdentifier(), session );
				proxiesByKey.put(key, proxy); //overwrite old proxy
				return proxy;
			}

		}
		else {
			return proxy;
		}
	}

	/**
	 * Return the existing proxy associated with the given <tt>EntityKey</tt>, or the
	 * second argument (the entity associated with the key) if no proxy exists.
	 */
	public Object proxyFor(EntityPersister persister, EntityKey key, Object impl) 
	throws HibernateException {
		if ( !persister.hasProxy() ) return impl;
		Object proxy = proxiesByKey.get(key);
		if ( proxy != null ) {
			return narrowProxy(proxy, persister, key, impl);
		}
		else {
			return impl;
		}
	}

	/**
	 * Return the existing proxy associated with the given <tt>EntityKey</tt>, or the
	 * second argument (the entity associated with the key) if no proxy exists.
	 * (slower than the form above)
	 */
	public Object proxyFor(Object impl) throws HibernateException {
		EntityEntry e = getEntry(impl);
		EntityPersister p = e.getPersister();
		return proxyFor( p, new EntityKey( e.getId(), p, session.getEntityMode() ), impl );
	}

	/**
	 * Get the entity that owns this persistent collection
	 */
	public Object getCollectionOwner(Serializable key, CollectionPersister collectionPersister) throws MappingException {
		return getEntity( new EntityKey( key, collectionPersister.getOwnerEntityPersister(), session.getEntityMode() ) );
	}

	/**
	 * add a collection we just loaded up (still needs initializing)
	 */
	public void addUninitializedCollection(PersistentCollection collection, CollectionPersister persister, Serializable id, EntityMode em) {
		CollectionEntry ce = new CollectionEntry(persister, id, flushing);
		collection.setCollectionSnapshot(ce);
		addCollection(collection, ce, id, em);
	}

	/**
	 * add a detached uninitialized collection
	 */
	public void addUninitializedDetachedCollection(PersistentCollection collection, CollectionPersister persister, Serializable id, EntityMode em) {
		CollectionEntry ce = new CollectionEntry(persister, id);
		collection.setCollectionSnapshot(ce);
		addCollection(collection, ce, id, em);
	}

	/**
	 * Add a new collection (ie. a newly created one, just instantiated by the
	 * application, with no database state or snapshot)
	 *
	 * @param collection The collection to be associated with the persistence context
	 */
	public void addNewCollection(PersistentCollection collection, CollectionPersister persister)
	throws HibernateException {
		CollectionEntry ce = addCollection(collection);
		if ( persister.hasOrphanDelete() ) {
			ce.initSnapshot(collection, persister);
		}
	}
	
	/**
	 * Add an collection to the cache, with a given collection entry
	 */
	private void addCollection(PersistentCollection coll, CollectionEntry entry, Serializable key, EntityMode em) {
		collectionEntries.put(coll, entry);
		PersistentCollection old = (PersistentCollection) collectionsByKey.put(
				new CollectionKey( entry.getLoadedPersister(), key, em ),
				coll
		);
		if ( old != null ) {
			if (old==coll) throw new AssertionFailure("bug adding collection twice");
			// or should it actually throw an exception?
			old.unsetSession(session);
			collectionEntries.remove(old);
			// watch out for a case where old is still referenced
			// somewhere in the object graph! (which is a user error)
		}
	}

	/**
	 * Add a collection to the cache, creating a new collection entry for it
	 */
	private CollectionEntry addCollection(PersistentCollection collection) 
	throws HibernateException {
		CollectionEntry ce = new CollectionEntry();
		collectionEntries.put(collection, ce);
		collection.setCollectionSnapshot(ce);
		return ce;
	}

	/**
	 * add an (initialized) collection that was created by another session and passed
	 * into update() (ie. one with a snapshot and existing state on the database)
	 */
	public void addInitializedDetachedCollection(PersistentCollection collection, CollectionSnapshot cs, EntityMode em) 
	throws HibernateException {
		if ( cs.wasDereferenced() ) {
			addCollection(collection);
		}
		else {
			CollectionEntry ce = new CollectionEntry( cs, session.getFactory() );
			collection.setCollectionSnapshot(ce);
			addCollection( collection, ce, cs.getKey(), em );
		}
	}

	/**
	 * add a collection we just pulled out of the cache (does not need initializing)
	 */
	public CollectionEntry addInitializedCollection(PersistentCollection collection, CollectionPersister persister, Serializable id, EntityMode em)
	throws HibernateException {
		CollectionEntry ce = new CollectionEntry(persister, id, flushing);
		ce.postInitialize(collection);
		collection.setCollectionSnapshot(ce);
		addCollection(collection, ce, id, em);
		return ce;
	}
	
	/**
	 * Get the collection instance associated with the <tt>CollectionKey</tt>
	 */
	public PersistentCollection getCollection(CollectionKey collectionKey) {
		return (PersistentCollection) collectionsByKey.get(collectionKey);
	}
	
	/**
	 * Register a collection for non-lazy loading at the end of the
	 * two-phase load
	 */
	public void addNonLazyCollection(PersistentCollection collection) {
		nonlazyCollections.add(collection);
	}

	/**
	 * Force initialization of all non-lazy collections encountered during
	 * the current two-phase load (actually, this is a no-op, unless this
	 * is the "outermost" load)
	 */
	public void initializeNonLazyCollections() throws HibernateException {
		if ( loadCounter == 0 ) {
			log.debug( "initializing non-lazy collections" );
			//do this work only at the very highest level of the load
			loadCounter++; //don't let this method be called recursively
			try {
				int size;
				while ( ( size = nonlazyCollections.size() ) > 0 ) {
					//note that each iteration of the loop may add new elements
					( (PersistentCollection) nonlazyCollections.remove( size - 1 ) ).forceInitialization();
				}
			}
			finally {
				loadCounter--;
				clearNullProperties();
			}
		}
	}


	/**
	 * Get the <tt>PersistentCollection</tt> object for an array
	 */
	public PersistentCollection getCollectionHolder(Object array) {
		return (PersistentCollection) arrayHolders.get(array);
	}

	/**
	 * Register a <tt>PersistentCollection</tt> object for an array.
	 * Associates a holder with an array - MUST be called after loading 
	 * array, since the array instance is not created until endLoad().
	 */
	public void addCollectionHolder(PersistentCollection holder) {
		//TODO:refactor + make this method private
		arrayHolders.put( holder.getValue(), holder );
	}

	public PersistentCollection removeCollectionHolder(Object array) {
		return (PersistentCollection) arrayHolders.remove(array);
	}

	/**
	 * Get the snapshot of the pre-flush collection state
	 */
	public Serializable getSnapshot(PersistentCollection coll) {
		return getCollectionEntry(coll).getSnapshot();
	}

	/**
	 * Get the pre-flush identifier of the collection
	 */
	public Serializable getLoadedCollectionKey(PersistentCollection coll) {
		return getCollectionEntry(coll).getLoadedKey();
	}

	/**
	 * Is this the "inverse" end of a bidirectional association?
	 */
	public boolean isInverseCollection(PersistentCollection collection) {
		CollectionEntry ce = getCollectionEntry(collection);
		return ce != null && ce.getLoadedPersister().isInverse();
	}

	/**
	 * Get the collection entry for a collection passed to filter,
	 * which might be a collection wrapper, an array, or an unwrapped
	 * collection. Return null if there is no entry.
	 */
	public CollectionEntry getCollectionEntryOrNull(Object collection) {
		PersistentCollection coll;
		if ( collection instanceof PersistentCollection ) {
			coll = (PersistentCollection) collection;
			//if (collection==null) throw new TransientObjectException("Collection was not yet persistent");
		}
		else {
			coll = getCollectionHolder(collection);
			if ( coll == null ) {
				//it might be an unwrapped collection reference!
				//try to find a wrapper (slowish)
				Iterator wrappers = IdentityMap.keyIterator(collectionEntries);
				while ( wrappers.hasNext() ) {
					PersistentCollection pc = (PersistentCollection) wrappers.next();
					if ( pc.isWrapper(collection) ) {
						coll = pc;
						break;
					}
				}
			}
		}

		return (coll == null) ? null : getCollectionEntry(coll);
	}

	/**
	 * Get an existing proxy by key
	 */
	public Object getProxy(EntityKey key) {
		return proxiesByKey.get(key);
	}

	/**
	 * Add a proxy to the session cache
	 */
	public void addProxy(EntityKey key, Object proxy) {
		proxiesByKey.put(key, proxy);
	}

	/**
	 * Remove a proxy from the session cache
	 */
	public Object removeProxy(EntityKey key) {
		return proxiesByKey.remove(key);
	}

	/**
	 * Record the fact that an entity does not exist in the database
	 * 
	 * @param key the primary key of the entity
	 */
	public void addNonExistantEntityKey(EntityKey key) {
		nonExistantEntityKeys.add(key);
	}

	/**
	 * Record the fact that an entity does not exist in the database
	 * 
	 * @param key a unique key of the entity
	 */
	public void addNonExistantEntityUniqueKey(EntityUniqueKey key) {
		nonExistentEntityUniqueKeys.add(key);
	}

	public void removeNonExist(EntityKey key) {
		nonExistantEntityKeys.remove(key);
	}

	/** 
	 * Retrieve the set of EntityKeys representing nullifiable references
	 */
	public HashSet getNullifiableEntityKeys() {
		return nullifiableEntityKeys;
	}

	public void setNullifiableEntityKeys(HashSet nullifiables) {
		// todo : yick !!!
		this.nullifiableEntityKeys = nullifiables;
	}

	public Map getEntitiesByKey() {
		return entitiesByKey;
	}

	public Map getEntityEntries() {
		return entityEntries;
	}

	public Map getCollectionEntries() {
		return collectionEntries;
	}

	public Map getCollectionsByKey() {
		return collectionsByKey;
	}

	/**
	 * Do we already know that the entity does not exist in the
	 * database?
	 */
	public boolean isNonExistant(EntityKey key) {
		return nonExistantEntityKeys.contains(key);
	}

	/**
	 * Do we already know that the entity does not exist in the
	 * database?
	 */
	public boolean isNonExistant(EntityUniqueKey key) {
		return nonExistentEntityUniqueKeys.contains(key);
	}

	public int getCascadeLevel() {
		return cascading;
	}

	public int incrementCascadeLevel() {
		return ++cascading;
	}

	public int decrementCascadeLevel() {
		return --cascading;
	}

	public boolean isFlushing() {
		return flushing;
	}

	public void setFlushing(boolean flushing) {
		this.flushing = flushing;
	}

	/**
	 * Call this before begining a two-phase load
	 */
	public void beforeLoad() {
		loadCounter++;
	}

	/**
	 * Call this after finishing a two-phase load
	 */
	public void afterLoad() {
		loadCounter--;
	}

	/**
	 * Returns a string representation of the object.
	 *
	 * @return a string representation of the object.
	 */
	public String toString() {
		return new StringBuffer()
				.append("PersistentContext[entitiesByKey=")
				.append(entitiesByKey)
				.append("]")
				.toString();
	}
	
	/**
	 * Search the persistence context for an owner for the child object,
	 * given a collection role
	 */
	public Serializable getOwnerId(String role, Object childObject) {
		
		int loc = role.lastIndexOf('.');
		String entity = role.substring(0, loc);
		String property = role.substring( loc+1, role.length() );
		EntityPersister persister = session.getFactory().getEntityPersister(entity);
		final CollectionPersister collectionPersister = session.getFactory().getCollectionPersister(role);
		CollectionType type = collectionPersister.getCollectionType();
		
		Iterator entities = entityEntries.entrySet().iterator();
		while ( entities.hasNext() ) {
			Map.Entry me = (Map.Entry) entities.next();
			EntityEntry ee = (EntityEntry) me.getValue();
			if ( persister.isSubclassEntityName( ee.getEntityName() ) ) {
				Object collection = persister.getPropertyValue( me.getKey(), property, session.getEntityMode() );
				if ( collection!=null && type.contains(collection, childObject, collectionPersister, session) ) {
					return ee.getId();
				}
			}
		}
		return null;
	}

	/**
	 * Search the persistence context for an index of the child object,
	 * given a collection role
	 */
	public Object getIndexInOwner(String role, Object childObject) {
		int loc = role.lastIndexOf('.');
		String entity = role.substring(0, loc);
		String property = role.substring( loc+1, role.length() );
		EntityPersister persister = session.getFactory().getEntityPersister(entity);
		CollectionPersister cp = session.getFactory().getCollectionPersister(role);
		Iterator entities = entityEntries.entrySet().iterator();
		while ( entities.hasNext() ) {
			Map.Entry me = (Map.Entry) entities.next();
			EntityEntry ee = (EntityEntry) me.getValue();
			if ( persister.isSubclassEntityName( ee.getEntityName() ) ) {
				Object collection = persister.getPropertyValue( me.getKey(), property, session.getEntityMode() );
				if ( collection!=null ) {
					if ( Hibernate.isInitialized(collection) ) {
						Object index = cp.getCollectionType().indexOf(collection, childObject);
						if (index!=null) return index;
					}
				}
			}
		}
		return null;
	}
	
	private static final class AssociationKey implements Serializable {
		private EntityKey ownerKey;
		private String propertyName;
		private AssociationKey(EntityKey ownerKey, String propertyName) {
			this.ownerKey = ownerKey;
			this.propertyName = propertyName;
		}
		public boolean equals(Object that) {
			AssociationKey key = (AssociationKey) that;
			return key.propertyName.equals(propertyName) && 
				key.ownerKey.equals(ownerKey);
		}
		public int hashCode() {
			return ownerKey.hashCode() + propertyName.hashCode();
		}
	}

	public void addNullProperty(EntityKey ownerKey, String propertyName) {
		nullAssociations.add( new AssociationKey(ownerKey, propertyName) );
	}
	
	public boolean isPropertyNull(EntityKey ownerKey, String propertyName) {
		return nullAssociations.contains( new AssociationKey(ownerKey, propertyName) );
	}
	
	public void clearNullProperties() {
		nullAssociations.clear();
	}

}
