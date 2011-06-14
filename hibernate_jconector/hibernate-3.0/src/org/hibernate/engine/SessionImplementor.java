//$Id: SessionImplementor.java,v 1.45 2005/03/13 16:26:31 oneovthafew Exp $
package org.hibernate.engine;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.ScrollableResults;
import org.hibernate.Transaction;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.event.SessionEventListenerConfig;
import org.hibernate.jdbc.Batcher;
import org.hibernate.loader.custom.CustomQuery;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;


/**
 * Defines the internal contract between the <tt>Session</tt> and other parts of
 * Hibernate such as implementors of <tt>Type</tt> or <tt>EntityPersister</tt>.
 *
 * @see org.hibernate.Session the interface to the application
 * @see org.hibernate.impl.SessionImpl the actual implementation
 * @author Gavin King
 */
public interface SessionImplementor extends org.hibernate.classic.Session {

	/**
	 * Retrieves the interceptor currently in use by this event source.
	 *
	 * @return The interceptor.
	 */
	public Interceptor getInterceptor();

	/**
	 * Initialize the collection (if not already initialized)
	 */
	public void initializeCollection(PersistentCollection collection, boolean writing) throws HibernateException;
	
	/**
	 * Load an instance without checking if it was deleted. If it does not exist, throw an exception.
	 * This method may create a new proxy or return an existing proxy.
	 */
	public Object internalLoad(String entityName, Serializable id) throws HibernateException;
	/**
	 * Load an instance without checking if it was deleted. If it does not exist, return <tt>null</tt>.
	 * Do not create a proxy (but do return any existing proxy).
	 */
	public Object internalLoadOneToOne(String entityName, Serializable id) throws HibernateException;
	/**
	 * Load an instance immediately. This method is only called when lazily initializing a proxy.
	 * Do not return the proxy.
	 */
	public Object immediateLoad(String entityName, Serializable id) throws HibernateException;

	/**
	 * System time before the start of the transaction
	 */
	public long getTimestamp();
	/**
	 * Get the creating <tt>SessionFactoryImplementor</tt>
	 */
	public SessionFactoryImplementor getFactory();
	/**
	 * Get the prepared statement <tt>Batcher</tt> for this session
	 */
	public Batcher getBatcher();
	
	/**
	 * Execute a <tt>find()</tt> query
	 */
	public List list(String query, QueryParameters queryParameters) throws HibernateException;
	/**
	 * Execute an <tt>iterate()</tt> query
	 */
	public Iterator iterate(String query, QueryParameters queryParameters) throws HibernateException;
	/**
	 * Execute a <tt>scroll()</tt> query
	 */
	public ScrollableResults scroll(String query, QueryParameters queryParameters) throws HibernateException;

	/**
	 * Execute a filter
	 */
	public List listFilter(Object collection, String filter, QueryParameters queryParameters) throws HibernateException;
	/**
	 * Iterate a filter
	 */
	public Iterator iterateFilter(Object collection, String filter, QueryParameters queryParameters) throws HibernateException;
	
	/**
	 * Get the <tt>EntityPersister</tt> for any instance
	 * @param entityName optional entity name
	 * @param object the entity instance
	 */
	public EntityPersister getEntityPersister(String entityName, Object object) throws HibernateException;
	
	/**
	 * Get the entity instance associated with the given <tt>Key</tt>,
	 * calling the Interceptor if necessary
	 */
	public Object getEntityUsingInterceptor(EntityKey key) throws HibernateException;

//	public boolean registerSynchronizationIfPossible();
//	public boolean registerCallbackIfNecessary();

	/**
	 * Notify the session that the transaction completed, so we no longer
	 * own the old locks. (Also we should release cache softlocks.) May
	 * be called multiple times during the transaction completion process.
	 */
	public void afterTransactionCompletion(boolean successful, Transaction tx);
	
	/**
	 * Notify the session that the transaction is about to complete
	 */
	public void beforeTransactionCompletion(Transaction tx);

	/**
	 * Return the identifier of the persistent object, or null if transient
	 */
	public Serializable getEntityIdentifier(Object obj);

	/**
	 * The best guess entity name for an entity not in an association
	 */
	public String bestGuessEntityName(Object object);
	
	/**
	 * The guessed entity name for an entity not in an association
	 */
	public String guessEntityName(Object entity) throws HibernateException;
	
	/** 
	 * Instantiate the entity class, initializing with the given identifier
	 */
	public Object instantiate(String entityName, Serializable id) throws HibernateException;
	
	/**
	 * Execute an SQL Query
	 */
	public List listCustomQuery(CustomQuery customQuery, QueryParameters queryParameters) 
	throws HibernateException;
	
	/**
	 * Execute an SQL Query
	 */
	public ScrollableResults scrollCustomQuery(CustomQuery customQuery, QueryParameters queryParameters) 
	throws HibernateException;
	
	/**
	 * Cascade merge an entity instance
	 */
	public void merge(String entityName, Object object, Map copiedAlready) throws HibernateException;
	/**
	 * Cascade persist an entity instance
	 */
	public void persist(String entityName, Object object, Map createdAlready) throws HibernateException;
	/**
	 * Cascade copy an entity instance
	 */
	public void saveOrUpdateCopy(String entityName, Object object, Map copiedAlready) throws HibernateException;
	
	/**
	 * Cascade delete an entity instance
	 */
	public void delete(String entityName, Object child, boolean isCascadeDeleteEnabled);

	/**
	 * Retreive the currently set value for a filter parameter.
	 *
	 * @param filterParameterName The filter parameter name in the format
	 * {FILTER_NAME.PARAMETER_NAME}.
	 * @return The filter parameter value.
	 */
	public Object getFilterParameterValue(String filterParameterName);

	/**
	 * Retreive the type for a given filter parrameter.
	 *
	 * @param filterParameterName The filter parameter name in the format
	 * {FILTER_NAME.PARAMETER_NAME}.
	 * @return
	 */
	public Type getFilterParameterType(String filterParameterName);

	/**
	 * Return the currently enabled filters.  The filter map is keyed by filter
	 * name, with values corresponding to the {@link org.hibernate.impl.FilterImpl}
	 * instance.
	 * @return The currently enabled filters.
	 */
	public Map getEnabledFilters();
	
	/**
	 * Force an immediate flush
	 */
	public void forceFlush(EntityEntry e) throws HibernateException;

	public int getDontFlushFromFind();
	
	/**
	 * Retrieves the configured event listeners from this event source.
	 *
	 * @return The configured event listeners.
	 */
	public SessionEventListenerConfig getListeners();
	
	/**
	 * Instantiate an entity instance, using either an interceptor,
	 * or the given persister
	 */
	public Object instantiate(EntityPersister persister, Serializable id) throws HibernateException;

	//TODO: temporary
	
	/**
	 * Get the ActionQueue for this session
	 */
	public ActionQueue getActionQueue();

	/**
	 * Get the persistence context for this session
	 */
	public PersistenceContext getPersistenceContext();

	int executeUpdate(String query, QueryParameters queryParameters) throws HibernateException;
}
