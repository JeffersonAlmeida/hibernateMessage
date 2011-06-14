//$Id: SessionImpl.java,v 1.122 2005/03/22 15:08:55 oneovthafew Exp $
package org.hibernate.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.ObjectDeletedException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.ReplicationMode;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.TransientObjectException;
import org.hibernate.UnresolvableObjectException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.ActionQueue;
import org.hibernate.engine.CollectionEntry;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.engine.NamedQueryDefinition;
import org.hibernate.engine.NamedSQLQueryDefinition;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.Status;
import org.hibernate.event.AutoFlushEvent;
import org.hibernate.event.PersistEvent;
import org.hibernate.event.DeleteEvent;
import org.hibernate.event.DirtyCheckEvent;
import org.hibernate.event.EvictEvent;
import org.hibernate.event.FlushEvent;
import org.hibernate.event.InitializeCollectionEvent;
import org.hibernate.event.LoadEvent;
import org.hibernate.event.LoadEventListener;
import org.hibernate.event.LockEvent;
import org.hibernate.event.MergeEvent;
import org.hibernate.event.RefreshEvent;
import org.hibernate.event.ReplicateEvent;
import org.hibernate.event.SaveOrUpdateEvent;
import org.hibernate.event.SessionEventListenerConfig;
import org.hibernate.hql.FilterTranslator;
import org.hibernate.hql.QuerySplitter;
import org.hibernate.hql.QueryTranslator;
import org.hibernate.jdbc.Batcher;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.loader.criteria.CriteriaLoader;
import org.hibernate.loader.custom.CustomLoader;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.CollectionHelper;
import org.hibernate.util.EmptyIterator;
import org.hibernate.util.JoinedIterator;
import org.hibernate.util.StringHelper;


/**
 * Concrete implementation of a Session, and also the central, organizing component
 * of Hibernate's internal implementation. As such, this class exposes two interfaces;
 * Session itself, to the application, and SessionImplementor, to other components
 * of Hibernate. This class is not threadsafe.
 *
 * @author Gavin King
 */
public final class SessionImpl implements SessionImplementor, JDBCContext.Context {

	// todo : need to find a clean way to handle the "event source" role
	// a seperate classs responsible for generating/dispatching events just duplicates most of the Session methods...
	// passing around seperate references to interceptor, factory, actionQueue, and persistentContext is not manageable...

	private static final Log log = LogFactory.getLog(SessionImpl.class);

	private transient SessionFactoryImpl factory;
	private EntityMode entityMode = EntityMode.POJO;

	private final long timestamp;
	private boolean closed = false;
	private FlushMode flushMode = FlushMode.AUTO;
	private CacheMode cacheMode = CacheMode.NORMAL;

	private Interceptor interceptor;

	private transient int dontFlushFromFind = 0;

	private ActionQueue actionQueue;
	private PersistenceContext persistenceContext;
	private transient JDBCContext jdbcContext;
	private SessionEventListenerConfig listeners;

	private Map enabledFilters = new HashMap();

	private boolean isRootSession = true;
	private Map childSessionsByEntityMode;


	public Session getSession(EntityMode entityMode) {
		if ( this.entityMode == entityMode ) {
			return this;
		}

		if ( childSessionsByEntityMode == null ) {
			childSessionsByEntityMode = new HashMap();
		}

		SessionImpl rtn = (SessionImpl) childSessionsByEntityMode.get( entityMode );
		if ( rtn == null ) {
			rtn = new SessionImpl( this, entityMode );
			childSessionsByEntityMode.put( entityMode, rtn );
		}

		return rtn;
	}


	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		log.trace("deserializing session");

		interceptor = (Interceptor) ois.readObject();
		factory = (SessionFactoryImpl) ois.readObject();
		jdbcContext = (JDBCContext) ois.readObject();
		ois.defaultReadObject();
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		if ( isConnected() ) throw new IllegalStateException( "Cannot serialize a Session while connected" );

		log.trace( "serializing session" );

		oos.writeObject(interceptor);
		oos.writeObject(factory);
		oos.writeObject(jdbcContext);
		oos.defaultWriteObject();

	}

	public void clear() {
		persistenceContext.clear();
		actionQueue.clear();
	}

	private SessionImpl(SessionImpl parent, EntityMode entityMode) {
		this.factory = parent.factory;

		this.timestamp = parent.timestamp;

		this.jdbcContext = parent.jdbcContext;

		this.interceptor = parent.interceptor;
		this.listeners = parent.listeners;

		this.actionQueue = new ActionQueue(this);

		this.entityMode = entityMode;
		this.persistenceContext = new PersistenceContext(this);

		this.isRootSession = false;

		if ( factory.getStatistics().isStatisticsEnabled() ) {
			factory.getStatisticsImplementor().openSession();
		}
		
		log.debug( "opened session [" + entityMode + "]" );
	}

	SessionImpl(
			final Connection connection,
			final SessionFactoryImpl factory,
			final boolean autoclose,
			final long timestamp,
			final Interceptor interceptor,
			final SessionEventListenerConfig listeners,
			final EntityMode entityMode) {
		this.factory = factory;

		this.timestamp = timestamp;

		this.entityMode = entityMode;

		this.interceptor = interceptor;
		this.listeners = listeners;

		this.actionQueue = new ActionQueue( this );
		this.persistenceContext = new PersistenceContext( this );

		this.isRootSession = true;

		this.jdbcContext = new JDBCContext( this, connection, autoclose );

		if ( factory.getStatistics().isStatisticsEnabled() ) {
			factory.getStatisticsImplementor().openSession();
		}
		
		if ( log.isDebugEnabled() ) log.debug( "opened session at timestamp: " + timestamp );
	}

	public Batcher getBatcher() {
		return jdbcContext.getBatcher();
	}

	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public Connection close() throws HibernateException {

		log.trace( "closing session" );

		if ( factory.getStatistics().isStatisticsEnabled() ) 
			factory.getStatisticsImplementor().closeSession();

		try {
			try {
				if ( childSessionsByEntityMode != null ) {
					Iterator childSessions = childSessionsByEntityMode.values().iterator();
					while ( childSessions.hasNext() ) {
						final SessionImpl child = ( SessionImpl ) childSessions.next();
						child.close();
					}
				}
			}
			catch( Throwable t ) {
				// just ignore
			}

			if ( isRootSession ) {
				return jdbcContext.release();
			}
			else {
				return null;
			}
		}
		finally {
			closed = true;
			cleanup();
		}
	}

	public boolean isOpen() {
		return !closed;
	}

	public boolean isFlushModeNever() {
		return getFlushMode() == FlushMode.NEVER;
	}

	public boolean isFlushBeforeCompletionEnabled() {
		return getFactory().getSettings().isFlushBeforeCompletionEnabled();
	}

	public void managedFlush() {

		log.trace("automatically flushing session");
		flush();
		
		if ( childSessionsByEntityMode != null ) {
			Iterator iter = childSessionsByEntityMode.values().iterator();
			while ( iter.hasNext() ) {
				( (Session) iter.next() ).flush();
			}
		}
		
	}

	public boolean shouldAutoClose() {
		return getFactory().getSettings().isAutoCloseSessionEnabled() && isOpen();
	}

	public void managedClose() {
		log.trace("automatically closing session");
		close();
	}

	public Connection connection() throws HibernateException {
		return jdbcContext.connection();
	}

	public boolean isConnected() {
		return jdbcContext.isConnected();
	}

	public Connection disconnect() throws HibernateException {
		log.debug( "disconnecting session" );
        return jdbcContext.disconnect();
	}

	public void reconnect() throws HibernateException {
		log.debug( "reconnecting session" );
		jdbcContext.reconnect();
	}

	public void reconnect(Connection conn) throws HibernateException {
		log.debug( "reconnecting session" );
		jdbcContext.reconnect( conn );
	}

	public void beforeTransactionCompletion(Transaction tx) {
		log.trace( "before transaction completion" );

		if ( !isRootSession ) {
			log.trace( "skipping beforeTransactionCompletion processing as this is not root session" );
			return;
		}

		try {
			interceptor.beforeTransactionCompletion(tx);
		}
		catch (Throwable t) {
			log.error("exception in interceptor beforeTransactionCompletion()", t);
		}
	}

	public void afterTransactionCompletion(boolean success, Transaction tx) {
		log.trace( "after transaction completion" );

		persistenceContext.afterTransactionCompletion();
		actionQueue.afterTransactionCompletion(success);

		if ( isRootSession ) {

			try {
				interceptor.afterTransactionCompletion(tx);
			}
			catch (Throwable t) {
				log.error("exception in interceptor beforeTransactionCompletion()", t);
			}
			
		}
		
	}
	
	/**
	 * clear all the internal collections, just 
	 * to help the garbage collector, does not
	 * clear anything that is needed during the
	 * afterTransactionCompletion() phase
	 */
	private void cleanup() {
		persistenceContext.clear();
	}

	public LockMode getCurrentLockMode(Object object) throws HibernateException {
		if ( object == null ) throw new NullPointerException( "null object passed to getCurrentLockMode()" );
		if ( object instanceof HibernateProxy ) {
			object = ( (HibernateProxy) object ).getHibernateLazyInitializer().getImplementation(this);
			if ( object == null ) return LockMode.NONE;
		}
		EntityEntry e = persistenceContext.getEntry(object);
		if ( e == null ) throw new TransientObjectException( "Given object not associated with the session" );
		if ( e.getStatus() != Status.MANAGED ) throw new ObjectDeletedException( 
				"The given object was deleted", 
				e.getId(), e.getPersister().getEntityName() 
		);
		return e.getLockMode();
	}

	public Object getEntityUsingInterceptor(EntityKey key) throws HibernateException {
		// todo : should this get moved to PersistentContext?
		// logically, is PersistentContext the "thing" to which an interceptor gets attached?
		final Object result = persistenceContext.getEntity(key);
		if ( result == null ) {
			final Object newObject = interceptor.getEntity( key.getEntityName(), key.getIdentifier() );
			if ( newObject != null ) lock(newObject, LockMode.NONE);
			return newObject;
		}
		else {
			return result;
		}
	}

	public void saveOrUpdate(Object object) throws HibernateException {
		saveOrUpdate(null, object);
	}

	public void saveOrUpdate(String entityName, Object obj) throws HibernateException {
		SaveOrUpdateEvent event = new SaveOrUpdateEvent(entityName, obj, this);
		listeners.getSaveOrUpdateEventListener().onSaveOrUpdate(event);
	}

	public void save(Object obj, Serializable id) throws HibernateException {
		save(null, obj, id);
	}

	public Serializable save(Object obj) throws HibernateException {
		return save(null, obj);
	}

	public Serializable save(String entityName, Object object) throws HibernateException {
		SaveOrUpdateEvent event = new SaveOrUpdateEvent(entityName, object, this);
		return listeners.getSaveEventListener().onSaveOrUpdate(event);
	}

	public void save(String entityName, Object object, Serializable id) throws HibernateException {
		SaveOrUpdateEvent event = new SaveOrUpdateEvent(entityName, object, id, this);
		listeners.getSaveEventListener().onSaveOrUpdate(event);
	}

	public void update(Object obj) throws HibernateException {
		update(null, obj);
	}

	public void update(Object obj, Serializable id) throws HibernateException {
		update(null, obj, id);
	}

	public void update(String entityName, Object object) throws HibernateException {
		SaveOrUpdateEvent event = new SaveOrUpdateEvent(entityName, object, this);
		listeners.getUpdateEventListener().onSaveOrUpdate(event);
	}

	public void update(String entityName, Object object, Serializable id) throws HibernateException {
		SaveOrUpdateEvent event = new SaveOrUpdateEvent(entityName, object, id, this);
		listeners.getUpdateEventListener().onSaveOrUpdate(event);
	}

	public void lock(Object object, LockMode lockMode) throws HibernateException {
        listeners.getLockEventListener().onLock( new LockEvent(object, lockMode, this) );
	}

	public void lock(String entityName, Object object, LockMode lockMode) throws HibernateException {
		LockEvent event = new LockEvent(entityName, object, lockMode, this);
		listeners.getLockEventListener().onLock(event);
	}

	public void persist(String entityName, Object object, Map copiedAlready)
	throws HibernateException {
		PersistEvent event = new PersistEvent(entityName, object, this);
		listeners.getCreateEventListener().onPersist(event, copiedAlready);
	}

	public void persist(String entityName, Object object)
	throws HibernateException {
		PersistEvent event = new PersistEvent(entityName, object, this);
		listeners.getCreateEventListener().onPersist(event);
	}

	public void persist(Object object) throws HibernateException {
		persist(null, object);
	}

	public Object merge(String entityName, Object object)
	throws HibernateException {
		MergeEvent event = new MergeEvent(entityName, object, this);
		return listeners.getMergeEventListener().onMerge(event);
	}

	public Object merge(Object object) throws HibernateException {
		return merge(null, object);
	}

	public void merge(String entityName, Object object, Map copiedAlready) throws HibernateException {
		MergeEvent event = new MergeEvent(entityName, object, this);
		listeners.getMergeEventListener().onMerge(event, copiedAlready);
	}

	public Object saveOrUpdateCopy(String entityName, Object object)
	throws HibernateException {
		MergeEvent event = new MergeEvent(entityName, object, this);
		return listeners.getSaveOrUpdateCopyEventListener().onMerge(event);
	}

	public Object saveOrUpdateCopy(Object object) throws HibernateException {
		return saveOrUpdateCopy(null, object);
	}

	public Object saveOrUpdateCopy(String entityName, Object object, Serializable id) 
	throws HibernateException {
		MergeEvent event = new MergeEvent(entityName, object, id, this);
		return listeners.getSaveOrUpdateCopyEventListener().onMerge(event);
	}

	public Object saveOrUpdateCopy(Object object, Serializable id) 
	throws HibernateException {
		return saveOrUpdateCopy(null, object, id);
	}

	public void saveOrUpdateCopy(String entityName, Object object, Map copiedAlready) 
	throws HibernateException {
		MergeEvent event = new MergeEvent(entityName, object, this);
		listeners.getSaveOrUpdateCopyEventListener().onMerge(event, copiedAlready);
	}

	/**
	 * Delete a persistent object
	 */
	public void delete(Object object) throws HibernateException {
		DeleteEvent event = new DeleteEvent(object, this);
        listeners.getDeleteEventListener().onDelete(event);
	}

	/**
	 * Delete a persistent object
	 */
	public void delete(String entityName, Object object, boolean isCascadeDeleteEnabled) throws HibernateException {
		DeleteEvent event = new DeleteEvent(entityName, object, isCascadeDeleteEnabled, this);
        listeners.getDeleteEventListener().onDelete(event);
	}

	public void load(Object object, Serializable id) throws HibernateException {
        LoadEvent event = new LoadEvent(id, object, this);
        listeners.getLoadEventListener().onLoad(event, null);
	}

	public Object load(Class entityClass, Serializable id) throws HibernateException {
		return load( entityClass.getName(), id );
	}

	public Object load(String entityName, Serializable id) throws HibernateException {
        LoadEvent event = new LoadEvent(id, entityName, false, this);
        Object result = listeners.getLoadEventListener().onLoad(event, LoadEventListener.LOAD);

		ObjectNotFoundException.throwIfNull(result, id, entityName);
		return result;
	}

	public Object get(Class entityClass, Serializable id) throws HibernateException {
		return get( entityClass.getName(), id );
	}

	public Object get(String entityName, Serializable id) throws HibernateException {
        LoadEvent event = new LoadEvent(id, entityName, false, this);
        return listeners.getLoadEventListener().onLoad(event, LoadEventListener.GET);
	}

	/**
	 * Load the data for the object with the specified id into a newly created object.
	 * This is only called when lazily initializing a proxy.
	 * Do NOT return a proxy.
	 */
	public Object immediateLoad(String entityName, Serializable id) throws HibernateException {
		
		if ( log.isDebugEnabled() ) {
			EntityPersister persister = getFactory().getEntityPersister(entityName);
			log.debug( "initializing proxy: " + MessageHelper.infoString( persister, id, getFactory() ) );
		}
		
        LoadEvent event = new LoadEvent(id, entityName, true, this);
        Object result = listeners.getLoadEventListener().onLoad(event, LoadEventListener.IMMEDIATE_LOAD);

		ObjectNotFoundException.throwIfNull(result, id, entityName); //should it be UnresolvableObject?
		return result;
	}

	/**
	 * Return the object with the specified id or null if no row with that id exists. Do not defer the load
	 * or return a new proxy (but do return an existing proxy). Do not check if the object was deleted.
	 */
	public Object internalLoadOneToOne(String entityName, Serializable id) throws HibernateException {
		// todo : remove ( currently used from OneToOneType )
        LoadEvent event = new LoadEvent(id, entityName, true, this);
        return listeners.getLoadEventListener().onLoad(event, LoadEventListener.INTERNAL_LOAD_ONE_TO_ONE);
	}

	/**
	 * Return the object with the specified id or throw exception if no row with that id exists. Defer the load,
	 * return a new proxy or return an existing proxy if possible. Do not check if the object was deleted.
	 */
	public Object internalLoad(String entityName, Serializable id) throws HibernateException {
        LoadEvent event = new LoadEvent(id, entityName, true, this);
        Object result = listeners.getLoadEventListener().onLoad(event, LoadEventListener.INTERNAL_LOAD);

		UnresolvableObjectException.throwIfNull(result, id, entityName);
		return result;
	}

	public Object load(Class entityClass, Serializable id, LockMode lockMode) throws HibernateException {
		return load( entityClass.getName(), id, lockMode );
	}

	public Object load(String entityName, Serializable id, LockMode lockMode) throws HibernateException {
        LoadEvent event = new LoadEvent(id, entityName, lockMode, this);
        return listeners.getLoadEventListener().onLoad(event, LoadEventListener.LOAD);
	}

	public Object get(Class entityClass, Serializable id, LockMode lockMode) throws HibernateException {
		return get( entityClass.getName(), id, lockMode );
	}

	public Object get(String entityName, Serializable id, LockMode lockMode) throws HibernateException {
        LoadEvent event = new LoadEvent(id, entityName, lockMode, this);
        return listeners.getLoadEventListener().onLoad(event, LoadEventListener.GET);
	}

	public void refresh(Object object) throws HibernateException {
		listeners.getRefreshEventListener().onRefresh( new RefreshEvent(object, this) );
	}

	public void refresh(Object object, LockMode lockMode) throws HibernateException {
		listeners.getRefreshEventListener().onRefresh( new RefreshEvent(object, lockMode, this) );
	}

	public void replicate(Object obj, ReplicationMode replicationMode) throws HibernateException {
		ReplicateEvent event = new ReplicateEvent(obj, replicationMode, this);
		listeners.getReplicateEventListener().onReplicate(event);
	}

	public void replicate(String entityName, Object obj, ReplicationMode replicationMode)
	throws HibernateException {
		ReplicateEvent event = new ReplicateEvent(entityName, obj, replicationMode, this);
		listeners.getReplicateEventListener().onReplicate(event);
	}

	/**
	 * remove any hard references to the entity that are held by the infrastructure
	 * (references held by application or other persistant instances are okay)
	 */
	public void evict(Object object) throws HibernateException {
        listeners.getEvictEventListener().onEvict( new EvictEvent(object, this) );
	}

	/**
	 * detect in-memory changes, determine if the changes are to tables
	 * named in the query and, if so, complete execution the flush
	 */
	private boolean autoFlushIfRequired(Set querySpaces) throws HibernateException {
		AutoFlushEvent event = new AutoFlushEvent(querySpaces, this);
		return listeners.getAutoFlushEventListener().onAutoFlush(event);
	}

	public boolean isDirty() throws HibernateException {
		log.debug("checking session dirtiness");
		if ( actionQueue.areInsertionsOrDeletionsQueued() ) {
			log.debug("session dirty (scheduled updates and insertions)");
			return true;
		}
		else {
			DirtyCheckEvent event = new DirtyCheckEvent(this);
			return listeners.getDirtyCheckEventListener().onDirtyCheck(event);
		}
	}

	public void flush() throws HibernateException {
		if ( persistenceContext.getCascadeLevel() > 0 ) {
			throw new HibernateException("Flush during cascade is dangerous");
		}
		listeners.getFlushEventListener().onFlush( new FlushEvent(this) );
	}

	public void forceFlush(EntityEntry e) throws HibernateException {
		if ( log.isDebugEnabled() ) {
			log.debug(
				"flushing to force deletion of re-saved object: " +
				MessageHelper.infoString( e.getPersister(), e.getId(), getFactory() )
			);
		}

		if ( persistenceContext.getCascadeLevel() > 0 ) {
			throw new ObjectDeletedException(
				"deleted object would be re-saved by cascade (remove deleted object from associations)",
				e.getId(),
				e.getPersister().getEntityName()
			);
		}

		flush();
	}

	public Filter enableFilter(String filterName) {
        FilterImpl filter = new FilterImpl( factory.getFilterDefinition(filterName) );
		enabledFilters.put(filterName, filter);
		return filter;
	}

	public Filter getEnabledFilter(String filterName) {
		return (Filter) enabledFilters.get(filterName);
	}

	public void disableFilter(String filterName) {
		enabledFilters.remove(filterName);
	}

	public Object getFilterParameterValue(String filterParameterName) {
        String[] parsed = parseFilterParameterName(filterParameterName);
		FilterImpl filter = (FilterImpl) enabledFilters.get( parsed[0] );
		if (filter == null) {
			throw new IllegalArgumentException("Filter [" + parsed[0] + "] currently not enabled");
		}
		return filter.getParameter( parsed[1] );
	}

	public Type getFilterParameterType(String filterParameterName) {
		String[] parsed = parseFilterParameterName(filterParameterName);
		FilterDefinition filterDef = factory.getFilterDefinition( parsed[0] );
		if (filterDef == null) {
			throw new IllegalArgumentException("Filter [" + parsed[0] + "] not defined");
		}
		Type type = filterDef.getParameterType( parsed[1] );
		if (type == null) {
			// this is an internal error of some sort...
			throw new InternalError("Unable to locate type for filter parameter");
		}
		return type;
	}

	public Map getEnabledFilters() {
		// First, validate all the enabled filters...
		//TODO: this implementation has bad performance
		Iterator itr = enabledFilters.values().iterator();
		while ( itr.hasNext() ) {
			final Filter filter = (Filter) itr.next();
			filter.validate();
		}
		return enabledFilters;
	}

	private String[] parseFilterParameterName(String filterParameterName) {
		int dot = filterParameterName.indexOf('.');
		if (dot <= 0) {
			throw new IllegalArgumentException("Invalid filter-parameter name format"); // TODO: what type?
		}
		String filterName = filterParameterName.substring(0, dot);
		String parameterName = filterParameterName.substring(dot+1);
		return new String[] {filterName, parameterName};
	}


	/**
	 * Retrieve a list of persistent objects using a hibernate query
	 */
	public List find(String query) throws HibernateException {
		return list( query, new QueryParameters() );
	}

	public List find(String query, Object value, Type type) throws HibernateException {
		return list( query, new QueryParameters(type, value) );
	}

	public List find(String query, Object[] values, Type[] types) throws HibernateException {
		return list( query, new QueryParameters(types, values) );
	}

	public List list(String query, QueryParameters queryParameters) throws HibernateException {

		if ( log.isTraceEnabled() ) {
			log.trace( "find: " + query );
			queryParameters.traceParameters(factory);
		}

		queryParameters.validateParameters();
		QueryTranslator[] q = getQueries(query, false);

		List results = CollectionHelper.EMPTY_LIST;

		dontFlushFromFind++;   //stops flush being called multiple times if this method is recursively called

		//execute the queries and return all result lists as a single list
		try {
			for ( int i = 0; i < q.length; i++ ) {
				List currentResults = q[i].list(this, queryParameters);
				currentResults.addAll(results);
				results = currentResults;
			}
		}
		finally {
			dontFlushFromFind--;
		}
		return results;
	}

	public int executeUpdate(String query, QueryParameters queryParameters) throws HibernateException {

		if ( log.isTraceEnabled() ) {
			log.trace( "executeUpdate: " + query );
			queryParameters.traceParameters(factory);
		}

		queryParameters.validateParameters();
		QueryTranslator[] q = getQueries(query, false);

		// there should be only one QT
		if ( q.length > 1 ) {
			log.warn( "update query returned multiple translators" );
			//TODO: iterate over them all????
		}

		return q[0].executeUpdate( queryParameters, this );
	}

	private QueryTranslator[] getQueries(String query, boolean scalar) throws HibernateException {

		// take the union of the query spaces (ie. the queried tables)
		QueryTranslator[] q = factory.getQuery( query, scalar, getEnabledFilters() );
		return prepareQueries(q);

	}

	private QueryTranslator[] prepareQueries(QueryTranslator[] q) {
		HashSet qs = new HashSet();
		for ( int i = 0; i < q.length; i++ ) {
			qs.addAll( q[i].getQuerySpaces() );
		}

		autoFlushIfRequired(qs);

		return q;
	}

	public Iterator iterate(String query) throws HibernateException {
		return iterate( query, new QueryParameters() );
	}

	public Iterator iterate(String query, Object value, Type type) throws HibernateException {
		return iterate( query, new QueryParameters(type, value) );
	}

	public Iterator iterate(String query, Object[] values, Type[] types) throws HibernateException {
		return iterate( query, new QueryParameters(types, values) );
	}

	public Iterator iterate(String query, QueryParameters queryParameters) throws HibernateException {

		if ( log.isTraceEnabled() ) {
			log.trace( "iterate: " + query );
			queryParameters.traceParameters(factory);
		}

		queryParameters.validateParameters();
		QueryTranslator[] q = getQueries(query, true);

		if ( q.length == 0 ) return EmptyIterator.INSTANCE;

		Iterator result = null;
		Iterator[] results = null;
		boolean many = q.length > 1;
		if (many) results = new Iterator[q.length];

		dontFlushFromFind++; //stops flush being called multiple times if this method is recursively called

		try {

			//execute the queries and return all results as a single iterator
			for ( int i = 0; i < q.length; i++ ) {
				result = q[i].iterate(queryParameters, this);
				if (many) results[i] = result;
			}

			return many ? new JoinedIterator(results) : result;

		}
		finally {
			dontFlushFromFind--;
		}
	}

	public ScrollableResults scroll(String query, QueryParameters queryParameters) throws HibernateException {

		if ( log.isTraceEnabled() ) {
			log.trace( "scroll: " + query );
			queryParameters.traceParameters( factory );
		}

		QueryTranslator[] q = factory.getQuery( query, false, getEnabledFilters() );
		if ( q.length != 1 ) throw new QueryException( "implicit polymorphism not supported for scroll() queries" );
		autoFlushIfRequired( q[0].getQuerySpaces() );

		dontFlushFromFind++; //stops flush being called multiple times if this method is recursively called
		try {
			return q[0].scroll(queryParameters, this);
		}
		finally {
			dontFlushFromFind--;
		}
	}

	public int delete(String query) throws HibernateException {
		return delete( query, ArrayHelper.EMPTY_OBJECT_ARRAY, ArrayHelper.EMPTY_TYPE_ARRAY );
	}

	public int delete(String query, Object value, Type type) throws HibernateException {
		return delete( query, new Object[]{value}, new Type[]{type} );
	}

	public int delete(String query, Object[] values, Type[] types) throws HibernateException {
		if ( query == null ) {
			throw new IllegalArgumentException("attempt to perform delete-by-query with null query");
		}

		if ( log.isTraceEnabled() ) {
			log.trace( "delete: " + query );
			if ( values.length != 0 ) {
				log.trace( "parameters: " + StringHelper.toString( values ) );
			}
		}

		List list = find( query, values, types );
		int deletionCount = list.size();
		for ( int i = 0; i < deletionCount; i++ ) {
			delete( list.get( i ) );
		}

		return deletionCount;
	}

	public Query createFilter(Object collection, String queryString) {
		return new CollectionFilterImpl(queryString, collection, this);
	}
	
	public Query createQuery(String queryString) {
		return new QueryImpl(queryString, this);
	}

	private Query createQuery(String queryString, FlushMode queryFlushMode) {
		return new QueryImpl(queryString, queryFlushMode, this);
	}
	
	public Query getNamedQuery(String queryName) throws MappingException {
		NamedQueryDefinition nqd = factory.getNamedQuery(queryName);
		final Query query;
		if ( nqd != null ) {
			query = createQuery( 
					nqd.getQueryString(), 
					nqd.getFlushMode()
			);
			if ( factory.getSettings().isCommentsEnabled() ) {
				query.setComment("named query " + queryName);
			}
		}
		else {
			NamedSQLQueryDefinition nsqlqd = factory.getNamedSQLQuery( queryName );
			if (nsqlqd==null) {
				throw new MappingException("Named query not known: " + queryName);
			}
			query = new SQLQueryImpl(nsqlqd, this);
			nqd = nsqlqd;
			if ( factory.getSettings().isCommentsEnabled() ) {
				query.setComment("named SQL query " + queryName);
			}
		}
		query.setCacheable( nqd.isCacheable() );
		query.setCacheRegion( nqd.getCacheRegion() );
		if ( nqd.getTimeout()!=null ) query.setTimeout( nqd.getTimeout().intValue() );
		if ( nqd.getFetchSize()!=null ) query.setFetchSize( nqd.getFetchSize().intValue() );
		return query;
	}

	public Object instantiate(String entityName, Serializable id) throws HibernateException {
		return instantiate( factory.getEntityPersister(entityName), id );
	}

	/**
	 * give the interceptor an opportunity to override the default instantiation
	 */
	public Object instantiate(EntityPersister persister, Serializable id) throws HibernateException {
		Object result = interceptor.instantiate( persister.getEntityName(), entityMode, id );
		if ( result == null ) result = persister.instantiate( id, entityMode );
		return result;
	}

	public EntityMode getEntityMode() {
		return entityMode;
	}

	public void setFlushMode(FlushMode flushMode) {
		if ( log.isTraceEnabled() ) log.trace("setting flush mode to: " + flushMode);
		this.flushMode = flushMode;
	}
	
	public FlushMode getFlushMode() {
		return flushMode;
	}

	public CacheMode getCacheMode() {
		return cacheMode;
	}
	
	public void setCacheMode(CacheMode cacheMode) {
		if ( log.isTraceEnabled() ) log.trace("setting cache mode to: " + cacheMode);
		this.cacheMode= cacheMode; 
	}

	public Transaction beginTransaction() throws HibernateException {
		if ( !isRootSession ) {
			log.warn("Transaction started on non-root session");
		}
		Transaction tx = jdbcContext.beginTransaction();
		interceptor.afterTransactionBegin(tx);
		return tx;
	}

	public EntityPersister getEntityPersister(final String entityName, final Object object) {
		if (entityName==null) {
			return factory.getEntityPersister( guessEntityName(object) );
		}
		else {
			return factory.getEntityPersister( entityName ).getSubclassEntityPersister( object, getFactory(), entityMode );
		}
	}

	// not for internal use:
	public Serializable getIdentifier(Object object) throws HibernateException {
		if ( object instanceof HibernateProxy ) {
			LazyInitializer li = ( (HibernateProxy) object ).getHibernateLazyInitializer();
			if ( li.getSession() != this ) {
				throw new TransientObjectException( "The proxy was not associated with this session" );
			}
			return li.getIdentifier();
		}
		else {
			EntityEntry entry = persistenceContext.getEntry(object);
			if ( entry == null ) {
				throw new TransientObjectException( "The instance was not associated with this session" );
			}
			return entry.getId();
		}
	}

	/**
	 * Get the id value for an object that is actually associated with the session. This
	 * is a bit stricter than getEntityIdentifierIfNotUnsaved().
	 */
	public Serializable getEntityIdentifier(Object object) {
		if ( object instanceof HibernateProxy ) {
			return getProxyIdentifier(object);
		}
		else {
			EntityEntry entry = persistenceContext.getEntry(object);
			return entry != null ? entry.getId() : null;
		}
	}
	
	private Serializable getProxyIdentifier(Object proxy) {
		return ( (HibernateProxy) proxy ).getHibernateLazyInitializer().getIdentifier();
	}

	public Collection filter(Object collection, String filter) throws HibernateException {
		return listFilter( collection, filter, new QueryParameters( new Type[1], new Object[1] ) );
	}

	public Collection filter(Object collection, String filter, Object value, Type type) throws HibernateException {
		return listFilter( collection, filter, new QueryParameters( new Type[]{null, type}, new Object[]{null, value} ) );
	}

	public Collection filter(Object collection, String filter, Object[] values, Type[] types) 
	throws HibernateException {
		Object[] vals = new Object[values.length + 1];
		Type[] typs = new Type[types.length + 1];
		System.arraycopy( values, 0, vals, 1, values.length );
		System.arraycopy( types, 0, typs, 1, types.length );
		return listFilter( collection, filter, new QueryParameters( typs, vals ) );
	}

	/**
	 * 1. determine the collection role of the given collection (this may require a flush, if the
	 *    collecion is recorded as unreferenced)
	 * 2. obtain a compiled filter query
	 * 3. autoflush if necessary
	 */
	private FilterTranslator getFilterTranslator(
			Object collection, 
			String filter, 
			QueryParameters parameters, 
			boolean scalar)
	throws HibernateException {

		if ( collection == null ) throw new NullPointerException( "null collection passed to filter" );

		if ( log.isTraceEnabled() ) {
			log.trace( "filter: " + filter );
			parameters.traceParameters(factory);
		}

		CollectionEntry entry = persistenceContext.getCollectionEntryOrNull(collection);
		final CollectionPersister roleBeforeFlush = (entry == null) ? null : entry.getLoadedPersister();

		FilterTranslator filterTranslator;
		if ( roleBeforeFlush == null ) {
			// if it was previously unreferenced, we need
			// to flush in order to get its state into the
			// database to query
			flush();
			entry = persistenceContext.getCollectionEntryOrNull(collection);
			CollectionPersister roleAfterFlush = (entry == null) ? null : entry.getLoadedPersister();
			if ( roleAfterFlush == null ) throw new QueryException( "The collection was unreferenced" );
			filterTranslator = factory.getFilter( filter, roleAfterFlush.getRole(), scalar, getEnabledFilters() );
		}
		else {
			// otherwise, we only need to flush if there are
			// in-memory changes to the queried tables
			filterTranslator = factory.getFilter( filter, roleBeforeFlush.getRole(), scalar, getEnabledFilters() );
			if ( autoFlushIfRequired( filterTranslator.getQuerySpaces() ) ) {
				// might need to run a different filter entirely after the flush
				// because the collection role may have changed
				entry = persistenceContext.getCollectionEntryOrNull(collection);
				CollectionPersister roleAfterFlush = (entry == null) ? null : entry.getLoadedPersister();
				if ( roleBeforeFlush != roleAfterFlush ) {
					if ( roleAfterFlush == null ) throw new QueryException( "The collection was dereferenced" );
					filterTranslator = factory.getFilter( filter, roleAfterFlush.getRole(), scalar, getEnabledFilters() );
				}
			}
		}

		parameters.getPositionalParameterValues()[0] = entry.getLoadedKey();
		parameters.getPositionalParameterTypes()[0] = entry.getLoadedPersister().getKeyType();

		return filterTranslator;
	}

	public List listFilter(Object collection, String filter, QueryParameters queryParameters) 
	throws HibernateException {

		String[] concreteFilters = QuerySplitter.concreteQueries( filter, factory );
		FilterTranslator[] filters = new FilterTranslator[concreteFilters.length];

		for ( int i = 0; i < concreteFilters.length; i++ ) {
			filters[i] = getFilterTranslator( collection, concreteFilters[i], queryParameters, false );
		}

		dontFlushFromFind++;   //stops flush being called multiple times if this method is recursively called

		List results = CollectionHelper.EMPTY_LIST;
		try {
			for ( int i = 0; i < concreteFilters.length; i++ ) {
				List currentResults = filters[i].list( this, queryParameters );
				currentResults.addAll(results);
				results = currentResults;
			}
		}
		finally {
			dontFlushFromFind--;
		}
		return results;

	}

	public Iterator iterateFilter(Object collection, String filter, QueryParameters queryParameters) 
	throws HibernateException {

		String[] concreteFilters = QuerySplitter.concreteQueries(filter, factory);
		FilterTranslator[] filters = new FilterTranslator[concreteFilters.length];

		for ( int i=0; i<concreteFilters.length; i++ ) {
			filters[i] = getFilterTranslator( collection, concreteFilters[i], queryParameters, true );
		}

		if ( filters.length == 0 ) return EmptyIterator.INSTANCE;

		Iterator result = null;
		Iterator[] results = null;
		boolean many = filters.length > 1;
		if (many) results = new Iterator[filters.length];

		//execute the queries and return all results as a single iterator
		for ( int i=0; i<filters.length; i++ ) {
			result = filters[i].iterate(queryParameters, this);
			if (many) results[i] = result;
		}

		return many ? new JoinedIterator(results) : result;

	}

	public Criteria createCriteria(Class persistentClass, String alias) {
		return new CriteriaImpl( persistentClass.getName(), alias, this );
	}

	public Criteria createCriteria(String entityName, String alias) {
		return new CriteriaImpl(entityName, alias, this);
	}

	public Criteria createCriteria(Class persistentClass) {
		return new CriteriaImpl( persistentClass.getName(), this );
	}

	public Criteria createCriteria(String entityName) {
		return new CriteriaImpl(entityName, this);
	}

	public ScrollableResults scroll(CriteriaImpl criteria, ScrollMode scrollMode) {
		String entityName = criteria.getEntityOrClassName();
		CriteriaLoader loader = new CriteriaLoader(
				getOuterJoinLoadable(entityName),
				factory,
				criteria,
				entityName,
		        getEnabledFilters()
		);
		autoFlushIfRequired( loader.getQuerySpaces() );
		dontFlushFromFind++;
		try {
			return loader.scroll(this, scrollMode);
		}
		finally {
			dontFlushFromFind--;
		}
	}

	public List list(CriteriaImpl criteria) throws HibernateException {

		String[] implementors = factory.getImplementors( criteria.getEntityOrClassName() );
		int size = implementors.length;

		CriteriaLoader[] loaders = new CriteriaLoader[size];
		Set spaces = new HashSet();
		for( int i=0; i <size; i++ ) {

			loaders[i] = new CriteriaLoader(
					getOuterJoinLoadable( implementors[i] ),
					factory,
					criteria,
					implementors[i],
			        getEnabledFilters()
			);

			spaces.addAll( loaders[i].getQuerySpaces() );

		}

		autoFlushIfRequired(spaces);

		List results = Collections.EMPTY_LIST;
		dontFlushFromFind++;
		try {
			for( int i=0; i<size; i++ ) {
				final List currentResults = loaders[i].list(this);
				currentResults.addAll(results);
				results = currentResults;
			}
		}
		finally {
			dontFlushFromFind--;
		}

		return results;
	}

	private OuterJoinLoadable getOuterJoinLoadable(String entityName) throws MappingException {
		EntityPersister persister = factory.getEntityPersister(entityName);
		if ( !(persister instanceof OuterJoinLoadable) ) {
			throw new MappingException( "class persister is not OuterJoinLoadable: " + entityName );
		}
		return ( OuterJoinLoadable ) persister;
	}

	public boolean contains(Object object) {
		if ( object instanceof HibernateProxy ) {
			//do not use proxiesByKey, since not all
			//proxies that point to this session's
			//instances are in that collection!
			LazyInitializer li = ( (HibernateProxy) object ).getHibernateLazyInitializer();
			if ( li.isUninitialized() ) {
				//if it is an uninitialized proxy, pointing
				//with this session, then when it is accessed,
				//the underlying instance will be "contained"
				return li.getSession()==this;
			}
			else {
				//if it is initialized, see if the underlying
				//instance is contained, since we need to 
				//account for the fact that it might have been
				//evicted
				object = li.getImplementation();
			}
		}
		return persistenceContext.isEntryFor(object);
	}
	
	public SQLQuery createSQLQuery(String sql) {
		return new SQLQueryImpl(sql, this);
	}

	public Query createSQLQuery(String sql, String returnAlias, Class returnClass) {
		return new SQLQueryImpl(sql, new String[] { returnAlias }, new Class[] { returnClass }, this);
	}

	public Query createSQLQuery(String sql, String returnAliases[], Class returnClasses[]) {
		return new SQLQueryImpl(sql, returnAliases, returnClasses, this);
	}

	public ScrollableResults scrollCustomQuery(CustomQuery customQuery, QueryParameters queryParameters) 
	throws HibernateException {

		if ( log.isTraceEnabled() ) {
			log.trace( "scroll SQL query: " + customQuery.getSQL() );
		}

		CustomLoader loader = new CustomLoader( customQuery, getFactory() );

		autoFlushIfRequired( loader.getQuerySpaces() );

		dontFlushFromFind++; //stops flush being called multiple times if this method is recursively called
		try {
			return loader.scroll(queryParameters, this);
		}
		finally {
			dontFlushFromFind--;
		}
	}

	// basically just an adapted copy of find(CriteriaImpl)
	public List listCustomQuery(CustomQuery customQuery, QueryParameters queryParameters) 
	throws HibernateException {

		if ( log.isTraceEnabled() ) log.trace( "SQL query: " + customQuery.getSQL() );
		
		CustomLoader loader = new CustomLoader( customQuery, getFactory() );

		autoFlushIfRequired( loader.getQuerySpaces() );

		dontFlushFromFind++;
		try {
			return loader.list(this, queryParameters);
		}
		finally {
			dontFlushFromFind--;
		}
	}

	public SessionFactory getSessionFactory() {
		return factory;
	}
	
	public void initializeCollection(PersistentCollection collection, boolean writing) 
	throws HibernateException {
		listeners.getInitializeCollectionEventListener()
			.onInitializeCollection( new InitializeCollectionEvent(collection, this) );
	}

	public String bestGuessEntityName(Object object) {
		if (object instanceof HibernateProxy) {
			object = ( (HibernateProxy) object ).getHibernateLazyInitializer().getImplementation();
		}
		EntityEntry entry = persistenceContext.getEntry(object);
		if (entry==null) {
			return guessEntityName(object);
		}
		else {
			return entry.getPersister().getEntityName();
		}
	}
	
	public String getEntityName(Object object) {
		if (object instanceof HibernateProxy) {
			if ( !persistenceContext.containsProxy( object ) ) {
				throw new TransientObjectException("proxy was not associated with the session");
			}
			object = ( (HibernateProxy) object ).getHibernateLazyInitializer().getImplementation();
		}

		EntityEntry entry = persistenceContext.getEntry(object);
		if (entry==null) throwTransientObjectException(object);
		return entry.getPersister().getEntityName();
	}

	private void throwTransientObjectException(Object object) throws HibernateException {
		throw new TransientObjectException(
				"object references an unsaved transient instance - save the transient instance before flushing: " +
				guessEntityName(object)
		);
	}

	public String guessEntityName(Object object) throws HibernateException {
		String entity = interceptor.getEntityName(object);
		if ( entity == null ) {
			if ( object instanceof Map ) {
				entity = (String) ( (Map) object ).get( "type" );
				if ( entity == null ) throw new HibernateException( "could not determine type of dynamic entity" );
			}
			else if ( object instanceof Element ) {
				return ( (Element) object ).getName();
			}
			else {
				entity = object.getClass().getName();
			}
		}
		return entity;
	}

	public void cancelQuery() throws HibernateException {
		getBatcher().cancelLastQuery();
	}

	public Interceptor getInterceptor() {
		return interceptor;
	}

	public int getDontFlushFromFind() {
		return dontFlushFromFind;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer(500)
			.append( getClass().getName() )
			.append('(');
		if ( isOpen() ) {
			buf.append(persistenceContext)
				.append(" ")
				.append(actionQueue);
		}
		else {
			buf.append("<closed>");
		}
		return buf.append(')').toString();
	}

	public SessionEventListenerConfig getListeners() {
		return listeners;
	}

	public ActionQueue getActionQueue() {
		return actionQueue;
	}
	
	public PersistenceContext getPersistenceContext() {
		return persistenceContext;
	}

}
