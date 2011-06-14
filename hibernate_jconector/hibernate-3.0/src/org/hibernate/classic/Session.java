//$Id: Session.java,v 1.6 2005/02/12 07:19:10 steveebersole Exp $
package org.hibernate.classic;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.type.Type;

/**
 * An extension of the <tt>Session</tt> API, including all
 * deprecated methods from Hibernate2. This interface is
 * provided to allow easier migration of existing applications.
 * New code should use <tt>org.hibernate.Session</tt>.
 * @author Gavin King
 */
public interface Session extends org.hibernate.Session {

	/**
	 * Copy the state of the given object onto the persistent object with the same
	 * identifier. If there is no persistent instance currently associated with
	 * the session, it will be loaded. Return the persistent instance. If the
	 * given instance is unsaved or does not exist in the database, save it and
	 * return it as a newly persistent instance. Otherwise, the given instance
	 * does not become associated with the session.
	 * @deprecated use org.hibernate.Session#merge(Object)
	 *
	 * @param object a transient instance with state to be copied
	 * @return an updated persistent instance
	 */
	public Object saveOrUpdateCopy(Object object) throws HibernateException;

	/**
	 * Copy the state of the given object onto the persistent object with the
	 * given identifier. If there is no persistent instance currently associated
	 * with the session, it will be loaded. Return the persistent instance. If
	 * there is no database row with the given identifier, save the given instance
	 * and return it as a newly persistent instance. Otherwise, the given instance
	 * does not become associated with the session.
	 * @deprecated use org.hibernate.Session#merge(Object, java.io.Serializable)
	 *
	 * @param object a persistent or transient instance with state to be copied
	 * @param id the identifier of the instance to copy to
	 * @return an updated persistent instance
	 */
	public Object saveOrUpdateCopy(Object object, Serializable id) throws HibernateException;

	/**
	 * Copy the state of the given object onto the persistent object with the same
	 * identifier. If there is no persistent instance currently associated with
	 * the session, it will be loaded. Return the persistent instance. If the
	 * given instance is unsaved or does not exist in the database, save it and
	 * return it as a newly persistent instance. Otherwise, the given instance
	 * does not become associated with the session.
	 * @deprecated use org.hibernate.Session#merge(String, Object)
	 *
	 * @param object a transient instance with state to be copied
	 * @return an updated persistent instance
	 */
	public Object saveOrUpdateCopy(String entityName, Object object) throws HibernateException;

	/**
	 * Copy the state of the given object onto the persistent object with the
	 * given identifier. If there is no persistent instance currently associated
	 * with the session, it will be loaded. Return the persistent instance. If
	 * there is no database row with the given identifier, save the given instance
	 * and return it as a newly persistent instance. Otherwise, the given instance
	 * does not become associated with the session.
	 * @deprecated use org.hibernate.Session#merge(String, Object, java.io.Serializable)
	 *
	 * @param object a persistent or transient instance with state to be copied
	 * @param id the identifier of the instance to copy to
	 * @return an updated persistent instance
	 */
	public Object saveOrUpdateCopy(String entityName, Object object, Serializable id) throws HibernateException;

	/**
	 * Execute a query.
	 * @deprecated use <tt>createQuery()</tt>
	 *
	 * @param query a query expressed in Hibernate's query language
	 * @return a distinct list of instances (or arrays of instances)
	 * @throws HibernateException
	 */
	public List find(String query) throws HibernateException;
			
	/**
	 * Execute a query with bind parameters.
	 * @deprecated use <tt>createQuery()</tt>
	 *
	 * Bind a value to a "?" parameter in the query string.
	 *
	 * @param query the query string
	 * @param value a value to be bound to a "?" placeholder (JDBC IN parameter).
	 * @param type the Hibernate type of the value
	 * @see org.hibernate.Hibernate for access to <tt>Type</tt> instances
	 * @return a distinct list of instances (or arrays of instances)
	 * @throws HibernateException
	 */
	public List find(String query, Object value, Type type) throws HibernateException;
	
	/**
	 * Execute a query with bind parameters.
	 * @deprecated use <tt>createQuery()</tt>
	 *
	 * Binding an array of values to "?" parameters in the query string.
	 *
	 * @param query the query string
	 * @param values an array of values to be bound to the "?" placeholders (JDBC IN parameters).
	 * @param types an array of Hibernate types of the values
	 * @see org.hibernate.Hibernate for access to <tt>Type</tt> instances
	 * @return a distinct list of instances
	 * @throws HibernateException
	 */
	public List find(String query, Object[] values, Type[] types) throws HibernateException;
	
	/**
	 * Execute a query and return the results in an iterator. If the query has multiple
	 * return values, values will be returned in an array of type <tt>Object[].</tt><br>
	 * <br>
	 * Entities returned as results are initialized on demand. The first SQL query returns
	 * identifiers only. So <tt>iterate()</tt> is usually a less efficient way to retrieve
	 * objects than <tt>find()</tt>.
	 * 
	 * @deprecated use <tt>createQuery()</tt>
	 *
	 * @param query the query string
	 * @return an iterator
	 * @throws HibernateException
	 */
	public Iterator iterate(String query) throws HibernateException;
	
	/**
	 * Execute a query and return the results in an iterator. Write the given value to "?"
	 * in the query string. If the query has multiple return values, values will be returned
	 * in an array of type <tt>Object[]</tt>.<br>
	 * <br>
	 * Entities returned as results are initialized on demand. The first SQL query returns
	 * identifiers only. So <tt>iterate()</tt> is usually a less efficient way to retrieve
	 * objects than <tt>find()</tt>.
	 * 
	 * @deprecated use <tt>createQuery()</tt>
	 *
	 * @param query the query string
	 * @param value a value to be witten to a "?" placeholder in the query string
	 * @param type the hibernate type of value
	 * @return an iterator
	 * @throws HibernateException
	 */
	public Iterator iterate(String query, Object value, Type type) throws HibernateException;
	
	/**
	 * Execute a query and return the results in an iterator. Write the given values to "?"
	 * in the query string. If the query has multiple return values, values will be returned
	 * in an array of type <tt>Object[]</tt>.<br>
	 * <br>
	 * Entities returned as results are initialized on demand. The first SQL query returns
	 * identifiers only. So <tt>iterate()</tt> is usually a less efficient way to retrieve
	 * objects than <tt>find()</tt>.
	 * 
	 * @deprecated use <tt>createQuery()</tt>
	 *
	 * @param query the query string
	 * @param values a list of values to be written to "?" placeholders in the query
	 * @param types a list of Hibernate types of the values
	 * @return an iterator
	 * @throws HibernateException
	 */
	public Iterator iterate(String query, Object[] values, Type[] types) throws HibernateException;
	
	/**
	 * Apply a filter to a persistent collection. A filter is a Hibernate query that may refer to
	 * <tt>this</tt>, the collection element. Filters allow efficient access to very large lazy
	 * collections. (Executing the filter does not initialize the collection.)
	 * 
	 * @deprecated use <tt>createQuery()</tt>
	 *
	 * @param collection a persistent collection to filter
	 * @param filter a filter query string
	 * @return Collection the resulting collection
	 * @throws HibernateException
	 */
	public Collection filter(Object collection, String filter) throws HibernateException;
	
	/**
	 * Apply a filter to a persistent collection. A filter is a Hibernate query that may refer to
	 * <tt>this</tt>, the collection element.
	 * 
	 * @deprecated use <tt>createQuery()</tt>
	 *
	 * @param collection a persistent collection to filter
	 * @param filter a filter query string
	 * @param value a value to be witten to a "?" placeholder in the query string
	 * @param type the hibernate type of value
	 * @return Collection
	 * @throws HibernateException
	 */
	public Collection filter(Object collection, String filter, Object value, Type type) throws HibernateException;
	
	/**
	 * Apply a filter to a persistent collection.
	 *
	 * Bind the given parameters to "?" placeholders. A filter is a Hibernate query that
	 * may refer to <tt>this</tt>, the collection element.
	 * 
	 * @deprecated use <tt>createQuery()</tt>
	 *
	 * @param collection a persistent collection to filter
	 * @param filter a filter query string
	 * @param values a list of values to be written to "?" placeholders in the query
	 * @param types a list of Hibernate types of the values
	 * @return Collection
	 * @throws HibernateException
	 */
	public Collection filter(Object collection, String filter, Object[] values, Type[] types) throws HibernateException;
	
	/**
	 * Delete all objects returned by the query. Return the number of objects deleted.
	 * 
	 * @deprecated use <tt>createQuery()</tt>
	 *
	 * @param query the query string
	 * @return the number of instances deleted
	 * @throws HibernateException
	 */
	public int delete(String query) throws HibernateException;
	
	/**
	 * Delete all objects returned by the query. Return the number of objects deleted.
	 * 
	 * @deprecated use <tt>createQuery()</tt>
	 *
	 * @param query the query string
	 * @param value a value to be witten to a "?" placeholder in the query string.
	 * @param type the hibernate type of value.
	 * @return the number of instances deleted
	 * @throws HibernateException
	 */
	public int delete(String query, Object value, Type type) throws HibernateException;
	
	/**
	 * Delete all objects returned by the query. Return the number of objects deleted.
	 * 
	 * @deprecated use <tt>createQuery()</tt>
	 *
	 * @param query the query string
	 * @param values a list of values to be written to "?" placeholders in the query.
	 * @param types a list of Hibernate types of the values
	 * @return the number of instances deleted
	 * @throws HibernateException
	 */
	public int delete(String query, Object[] values, Type[] types) throws HibernateException;


	/**
	 * Create a new instance of <tt>Query</tt> for the given SQL string.
	 * @deprecated will be replaced with a more Query like interface in later release
	 *
	 * @param sql a query expressed in SQL
	 * @param returnAlias a table alias that appears inside <tt>{}</tt> in the SQL string
	 * @param returnClass the returned persistent class
	 */
	public Query createSQLQuery(String sql, String returnAlias, Class returnClass);
	
	/**
	 * Create a new instance of <tt>Query</tt> for the given SQL string.
	 * @deprecated will be replaced with a more Query like interface in later release
	 *
	 * @param sql a query expressed in SQL
	 * @param returnAliases an array of table aliases that appear inside <tt>{}</tt> in the SQL string
	 * @param returnClasses the returned persistent classes
	 */
	public Query createSQLQuery(String sql, String[] returnAliases, Class[] returnClasses);
	
	
}
