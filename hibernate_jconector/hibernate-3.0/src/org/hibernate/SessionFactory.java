//$Id: SessionFactory.java,v 1.11 2005/02/18 03:50:59 oneovthafew Exp $
package org.hibernate;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Map;

import javax.naming.Referenceable;

import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.stat.Statistics;

/**
 * Creates <tt>Session</tt>s. Usually an application has a single <tt>SessionFactory</tt>.
 * Threads servicing client requests obtain <tt>Session</tt>s from the factory.<br>
 * <br>
 * Implementors must be threadsafe.<br>
 * <br>
 * <tt>SessionFactory</tt>s are immutable. The behaviour of a <tt>SessionFactory</tt> is
 * controlled by properties supplied at configuration time. These properties are defined
 * on <tt>Environment</tt>.
 *
 * @see Session
 * @see org.hibernate.cfg.Environment
 * @see org.hibernate.cfg.Configuration
 * @see org.hibernate.connection.ConnectionProvider
 * @see org.hibernate.transaction.TransactionFactory
 * @author Gavin King
 */
public interface SessionFactory extends Referenceable, Serializable {

	/**
	 * Open a <tt>Session</tt> on the given connection.
	 * <p>
	 * Note that the second-level cache will be disabled if you
	 * supply a JDBC connection. Hibernate will not be able to track
	 * any statements you might have executed in the same transaction.
	 * Consider implementing your own <tt>ConnectionProvider</tt>.
	 *
	 * @param connection a connection provided by the application.
	 * @return Session
	 */
	public org.hibernate.classic.Session openSession(Connection connection);

	/**
	 * Create database connection and open a <tt>Session</tt> on it, specifying an
	 * interceptor.
	 *
	 * @param interceptor a session-scoped interceptor
	 * @return Session
	 * @throws HibernateException
	 */
	public org.hibernate.classic.Session openSession(Interceptor interceptor) throws HibernateException;

	/**
	 * Open a <tt>Session</tt> on the given connection, specifying an interceptor.
	 * <p>
	 * Note that the second-level cache will be disabled if you
	 * supply a JDBC connection. Hibernate will not be able to track
	 * any statements you might have executed in the same transaction.
	 * Consider implementing your own <tt>ConnectionProvider</tt>.
	 *
	 * @param connection a connection provided by the application.
	 * @param interceptor a session-scoped interceptor
	 * @return Session
	 */
	public org.hibernate.classic.Session openSession(Connection connection, Interceptor interceptor);

	/**
	 * Create database connection and open a <tt>Session</tt> on it.
	 *
	 * @return Session
	 * @throws HibernateException
	 */
	public org.hibernate.classic.Session openSession() throws HibernateException;

	/**
	 * Get the <tt>ClassMetadata</tt> associated with the given entity class
	 *
	 * @see org.hibernate.metadata.ClassMetadata
	 */
	public ClassMetadata getClassMetadata(Class persistentClass) throws HibernateException;

	/**
	 * Get the <tt>ClassMetadata</tt> associated with the given entity name
	 *
	 * @see org.hibernate.metadata.ClassMetadata
	 * @since 3.0
	 */
	public ClassMetadata getClassMetadata(String entityName) throws HibernateException;

	/**
	 * Get the <tt>CollectionMetadata</tt> associated with the named collection role
	 *
	 * @see org.hibernate.metadata.CollectionMetadata
	 */
	public CollectionMetadata getCollectionMetadata(String roleName) throws HibernateException;


	/**
	 * Get all <tt>ClassMetadata</tt> as a <tt>Map</tt> from entityname <tt>String</tt>
	 * to metadata object
	 *
	 * @see org.hibernate.metadata.ClassMetadata
	 * @return a map from <tt>String</tt> an entity name to <tt>ClassMetaData</tt>
	 * @since 3.0 changed key from <tt>Class</tt> to <tt>String</tt>
	 */
	public Map getAllClassMetadata() throws HibernateException;

	/**
	 * Get all <tt>CollectionMetadata</tt> as a <tt>Map</tt> from role name
	 * to metadata object
	 *
	 * @see org.hibernate.metadata.CollectionMetadata
	 * @return a map from <tt>String</tt> to <tt>CollectionMetadata</tt>
	 */
	public Map getAllCollectionMetadata() throws HibernateException;
	
	/**
	 * Get the statistics for this session factory
	 */
	public Statistics getStatistics();

	/**
	 * Destroy this <tt>SessionFactory</tt> and release all resources (caches,
	 * connection pools, etc). It is the responsibility of the application
	 * to ensure that there are no open <tt>Session</tt>s before calling
	 * <tt>close()</tt>.
	 */
	public void close() throws HibernateException;

	/**
	 * Evict all entries from the second-level cache. This method occurs outside
	 * of any transaction; it performs an immediate "hard" remove, so does not respect
	 * any transaction isolation semantics of the usage strategy. Use with care.
	 */
	public void evict(Class persistentClass) throws HibernateException;
	/**
	 * Evict an entry from the second-level  cache. This method occurs outside
	 * of any transaction; it performs an immediate "hard" remove, so does not respect
	 * any transaction isolation semantics of the usage strategy. Use with care.
	 */
	public void evict(Class persistentClass, Serializable id) throws HibernateException;
	/**
	 * Evict all entries from the second-level cache. This method occurs outside
	 * of any transaction; it performs an immediate "hard" remove, so does not respect
	 * any transaction isolation semantics of the usage strategy. Use with care.
	 */
	public void evictEntity(String entityName) throws HibernateException;
	/**
	 * Evict an entry from the second-level  cache. This method occurs outside
	 * of any transaction; it performs an immediate "hard" remove, so does not respect
	 * any transaction isolation semantics of the usage strategy. Use with care.
	 */
	public void evictEntity(String entityName, Serializable id) throws HibernateException;
	/**
	 * Evict all entries from the second-level cache. This method occurs outside
	 * of any transaction; it performs an immediate "hard" remove, so does not respect
	 * any transaction isolation semantics of the usage strategy. Use with care.
	 */
	public void evictCollection(String roleName) throws HibernateException;
	/**
	 * Evict an entry from the second-level cache. This method occurs outside
	 * of any transaction; it performs an immediate "hard" remove, so does not respect
	 * any transaction isolation semantics of the usage strategy. Use with care.
	 */
	public void evictCollection(String roleName, Serializable id) throws HibernateException;

	/**
	 * Evict any query result sets cached in the default query cache region.
	 */
	public void evictQueries() throws HibernateException;
	/**
	 * Evict any query result sets cached in the named query cache region.
	 */
	public void evictQueries(String cacheRegion) throws HibernateException;


}







