//$Id: Criteria.java,v 1.17 2005/02/12 07:19:50 steveebersole Exp $
package org.hibernate;

import java.util.List;

import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.transform.ResultTransformer;

/**
 * <tt>Criteria</tt> is a simplified API for retrieving entities
 * by composing <tt>Criterion</tt> objects. This is a very
 * convenient approach for functionality like "search" screens
 * where there is a variable number of conditions to be placed
 * upon the result set.<br>
 * <br>
 * The <tt>Session</tt> is a factory for <tt>Criteria</tt>.
 * <tt>Criterion</tt> instances are usually obtained via
 * the factory methods on <tt>Restrictions</tt>. eg.
 * <pre>
 * List cats = session.createCriteria(Cat.class)
 *     .add( Restrictions.like("name", "Iz%") )
 *     .add( Restrictions.gt( "weight", new Float(minWeight) ) )
 *     .addOrder( Order.asc("age") )
 *     .list();
 * </pre>
 * You may navigate associations using <tt>createAlias()</tt> or
 * <tt>createCriteria()</tt>.
 * <pre>
 * List cats = session.createCriteria(Cat.class)
 *     .createCriteria("kittens")
 *         .add( Restrictions.like("name", "Iz%") )
 *     .list();
 * </pre>
 * <pre>
 * List cats = session.createCriteria(Cat.class)
 *     .createAlias("kittens", "kit")
 *     .add( Restrictions.like("kit.name", "Iz%") )
 *     .list();
 * </pre>
 * You may specify projection and aggregation using <tt>Projection</tt>
 * instances obtained via the factory methods on <tt>Projections</tt>.
 * <pre>
 * List cats = session.createCriteria(Cat.class)
 *     .setProjection( Projections.projectionList()
 *         .add( Projections.rowCount() )
 *         .add( Projections.avg("weight") )
 *         .add( Projections.max("weight") )
 *         .add( Projections.min("weight") )
 *         .add( Projections.groupProperty("color") )
 *     )
 *     .addOrder( Order.asc("color") )
 *     .list();
 * </pre>
 *
 * @see Session#createCriteria(java.lang.Class)
 * @see org.hibernate.criterion.Restrictions
 * @see org.hibernate.criterion.Projections
 * @see org.hibernate.criterion.Order
 * @see org.hibernate.criterion.Criterion
 * @see org.hibernate.criterion.Projection
 * @see org.hibernate.DetachedCriteria a disconnected version of this API
 * @author Gavin King
 */
public interface Criteria extends CriteriaSpecification {

	/**
	 * Add a <tt>Criterion</tt> to constrain the results to be
	 * retrieved.
	 *
	 * @param criterion
	 * @return Criteria
	 */
	public Criteria add(Criterion criterion);
	
	/**
	 * Add an <tt>Order</tt> to the result set.
	 *
	 * @param order
	 * @return Criteria
	 */
	public Criteria addOrder(Order order);

	/**
	 * Specify an association fetching strategy for a
	 * one-to-many, many-to-one or one-to-one association, or
	 * for a collection of values.
	 *
	 * @param associationPath a dot seperated property path
	 * @param mode the fetch mode
	 * @return the Criteria object for method chaining
	 */
	public Criteria setFetchMode(String associationPath, FetchMode mode) throws HibernateException;
	/**
	 * Join an association, assigning an alias to the joined entity
	 */
	public Criteria createAlias(String associationPath, String alias) throws HibernateException;

	/**
	 * Create a new <tt>Criteria</tt>, "rooted" at the associated entity
	 */
	public Criteria createCriteria(String associationPath) throws HibernateException;

	/**
	 * Create a new <tt>Criteria</tt>, "rooted" at the associated entity,
	 * assigning the given alias
	 */
	public Criteria createCriteria(String associationPath, String alias) throws HibernateException;

	/**
	 * Set a projection of projection list, and select
	 * the <tt>PROJECTION</tt> result transformer
	 */
	public Criteria setProjection(Projection projection);
	
	/**
	 * Get the alias of the entity
	 */
	public String getAlias();

	/**
	 * Set a strategy for handling the query results. This determines the
	 * "shape" of the query result set.
	 * @see Criteria#ROOT_ENTITY
	 * @see Criteria#DISTINCT_ROOT_ENTITY
	 * @see Criteria#ALIAS_TO_ENTITY_MAP
	 * @param resultTransformer
	 */
	public Criteria setResultTransformer(ResultTransformer resultTransformer);

	/**
	 * Set a limit upon the number of objects to be
	 * retrieved.
	 *
	 * @param maxResults the maximum number of results
	 * @return Criteria
	 */
	public Criteria setMaxResults(int maxResults);
	
	/**
	 * Set the first result to be retrieved.
	 *
	 * @param firstResult the first result, numbered from <tt>0</tt>
	 * @return Criteria
	 */
	public Criteria setFirstResult(int firstResult);
	
	/**
	 * Set a fetch size for the underlying JDBC query.
	 * @param fetchSize the fetch size
	 */
	public Criteria setFetchSize(int fetchSize);

	/**
	 * Set a timeout for the underlying JDBC query.
	 *
	 * @param timeout
	 * @return Criteria
	 */
	public Criteria setTimeout(int timeout);

	/**
	 * Enable caching of this query result set
	 */
	public Criteria setCacheable(boolean cacheable);

	/**
	 * Set the name of the cache region.
	 *
	 * @param cacheRegion the name of a query cache region, or <tt>null</tt>
	 * for the default query cache
	 */
	public Criteria setCacheRegion(String cacheRegion);
	
	//public Criteria setCacheMode(CacheMode cacheMode);

	/**
	 * Get the results.
	 *
	 * @return List
	 * @throws HibernateException
	 */
	public List list() throws HibernateException;
	
	/**
	 * Get the results as an instance of <tt>ScrollableResults</tt>.
	 *
	 * @return ScrollableResults
	 * @throws HibernateException
	 */
	public ScrollableResults scroll() throws HibernateException;

	/**
	 * Get the results as an instance of <tt>ScrollableResults</tt>.
	 *
	 * @return ScrollableResults
	 * @throws HibernateException
	 */
	public ScrollableResults scroll(ScrollMode scrollMode) throws HibernateException;

	/**
	 * Convenience method to return a single instance that matches
	 * the query, or null if the query returns no results.
	 *
	 * @return the single result or <tt>null</tt>
	 * @throws HibernateException if there is more than one matching result
	 */
	public Object uniqueResult() throws HibernateException;

	/**
	 * Set the lock mode of the current entity
	 * @param lockMode the lock mode
	 */
	public Criteria setLockMode(LockMode lockMode);
	/**
	 * Set the lock mode of the aliased entity
	 * @param alias an alias
	 * @param lockMode the lock mode
	 */
	public Criteria setLockMode(String alias, LockMode lockMode);

	/**
	 * Add a comment to the generated SQL
	 * @param comment a human-readable string
	 */
	public Criteria setComment(String comment);
	
	/**
	 * Override the flush mode
	 */
	public Criteria setFlushMode(FlushMode flushMode);
	
	/**
	 * Override the cache mode
	 */
	public Criteria setCacheMode(CacheMode cacheMode);
	
}