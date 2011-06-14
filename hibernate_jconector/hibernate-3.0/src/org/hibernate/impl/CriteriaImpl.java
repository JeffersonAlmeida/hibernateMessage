//$Id: CriteriaImpl.java,v 1.28 2005/02/12 07:19:22 steveebersole Exp $
package org.hibernate.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.util.StringHelper;

/**
 * Implementation of the <tt>Criteria</tt> interface
 * @author Gavin King
 */
public class CriteriaImpl implements Criteria, Serializable {

	private FlushMode flushMode;
	private CacheMode cacheMode;
	private FlushMode sessionFlushMode;
	private CacheMode sessionCacheMode;
	private List criterionEntries = new ArrayList();
	private List orderEntries = new ArrayList();
	private Projection projection;
	private Criteria projectionCriteria;
	private Map fetchModes = new HashMap();
	private List subcriteriaList = new ArrayList();
	private final String entityOrClassName;
	private Map lockModes = new HashMap();
	private Integer maxResults;
	private Integer firstResult;
	private Integer timeout;
	private Integer fetchSize;
	private boolean cacheable;
	private String cacheRegion;
	private String comment;
	private transient SessionImpl session;
	private final String rootAlias;
	
	private ResultTransformer resultTransformer = Criteria.ROOT_ENTITY;
	
	public void setSession(SessionImpl session) {
		this.session = session;
	}
	
	public SessionImpl getSession() {
		return session;
	}

	public Iterator iterateSubcriteria() {
		return subcriteriaList.iterator();
	}
	
	public final class Subcriteria implements Criteria, Serializable {

		private String alias;
		private String path;
		private Criteria parent;
		private LockMode lockMode;
		
		public Criteria getParent() {
			return parent;
		}
		
		public String getPath() {
			return path;
		}
		
		public String toString() { 
			return "Subcriteria(" + 
				path + ":" + 
				(alias==null ? "" : alias) + 
				')'; 
		}

		private Subcriteria(Criteria parent, String path, String alias) {
			this.alias = alias;
			this.path = path;
			this.parent = parent;
			CriteriaImpl.this.subcriteriaList.add(this);
		}

		private Subcriteria(Criteria parent, String path) {
			this(parent, path, null);
		}

		public Criteria setAlias(String alias) {
			this.alias = alias;
			return this;
		}
		
		public String getAlias() {
			return alias;
		}
		
		public Criteria add(Criterion expression) {
			CriteriaImpl.this.add(this, expression);
			return this;
		}

		public Criteria createAlias(String associationPath, String alias)
			throws HibernateException {
			new Subcriteria(this, associationPath, alias);
			return this;
		}

		public Criteria addOrder(Order order) {
			CriteriaImpl.this.orderEntries.add( new OrderEntry(order, this) );
			return this;
		}

		public Criteria setCacheable(boolean cacheable) {
			CriteriaImpl.this.setCacheable(cacheable);
			return this;
		}

		public Criteria setCacheRegion(String cacheRegion) {
			CriteriaImpl.this.setCacheRegion(cacheRegion);
			return this;
		}

		public Criteria createCriteria(String associationPath)
			throws HibernateException {
			return new Subcriteria(Subcriteria.this, associationPath);
		}

		public List list() throws HibernateException {
			return CriteriaImpl.this.list();
		}

		public ScrollableResults scroll() throws HibernateException {
			return CriteriaImpl.this.scroll();
		}

		public ScrollableResults scroll(ScrollMode scrollMode) throws HibernateException {
			return CriteriaImpl.this.scroll(scrollMode);
		}

		public Object uniqueResult() throws HibernateException {
			return CriteriaImpl.this.uniqueResult();
		}

		public Criteria setFetchMode(String associationPath, FetchMode mode)
			throws HibernateException {
			CriteriaImpl.this.setFetchMode( StringHelper.qualify(path, associationPath), mode);
			return this;
		}

		public Criteria setFlushMode(FlushMode flushMode) {
			CriteriaImpl.this.setFlushMode(flushMode);
			return this;
		}
		
		public Criteria setCacheMode(CacheMode cacheMode) {
			CriteriaImpl.this.setCacheMode(cacheMode);
			return this;
		}
		
		public Criteria setFirstResult(int firstResult) {
			CriteriaImpl.this.setFirstResult(firstResult);
			return this;
		}

		public Criteria setMaxResults(int maxResults) {
			CriteriaImpl.this.setMaxResults(maxResults);
			return this;
		}

		public Criteria setTimeout(int timeout) {
			CriteriaImpl.this.setTimeout(timeout);
			return this;
		}

		public Criteria setFetchSize(int fetchSize) {
			CriteriaImpl.this.setFetchSize(fetchSize);
			return this;
		}

		public Criteria createCriteria(String associationPath, String alias)
			throws HibernateException {
			return new Subcriteria(Subcriteria.this, associationPath, alias);
		}
		
		public Criteria setLockMode(LockMode lockMode) {
			this.lockMode = lockMode;
			return this;
		}
		
		public LockMode getLockMode() {
			return lockMode;
		}

		public Criteria setLockMode(String alias, LockMode lockMode) {
			CriteriaImpl.this.setLockMode(alias, lockMode);
			return this;
		}

		public Criteria setResultTransformer(ResultTransformer resultProcessor) {
			CriteriaImpl.this.setResultTransformer(resultProcessor);
			return this;
		}

		public Criteria setComment(String comment) {
			CriteriaImpl.this.setComment(comment);
			return this;
		}

		public Criteria setProjection(Projection projection) {
			CriteriaImpl.this.projection = projection;
			CriteriaImpl.this.projectionCriteria = this;
			setResultTransformer(PROJECTION);
			return this;
		}
	}

	public Criteria setFlushMode(FlushMode flushMode) {
		this.flushMode = flushMode;
		return this;
	}
	
	public Criteria setCacheMode(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
		return this;
	}
	
	public Criteria setMaxResults(int maxResults) {
		this.maxResults = new Integer(maxResults);
		return this;
	}

	public Criteria setFirstResult(int firstResult) {
		this.firstResult = new Integer(firstResult);
		return this;
	}

	public Integer getFetchSize() {
		return fetchSize;
	}

	public Criteria setFetchSize(int fetchSize) {
		this.fetchSize = new Integer(fetchSize);
		return this;
	}

	public Criteria setTimeout(int timeout) {
		this.timeout = new Integer(timeout);
		return this;
	}

	public Criteria add(Criterion expression) {
		add(this, expression);
		return this;
	}

	public String getAlias() {
		return rootAlias;
	}
	
	public Integer getMaxResults() {
		return maxResults;
	}
	public Integer getFirstResult() {
		return firstResult;
	}
	public Integer getTimeout() {
		return timeout;
	}

	public CriteriaImpl(String entityOrClassName, SessionImpl session) {
		this.session = session;
		this.entityOrClassName = entityOrClassName;
		this.cacheable = false;
		rootAlias = ROOT_ALIAS;
	}

	public CriteriaImpl(String entityOrClassName, String alias, SessionImpl session) {
		this.session = session;
		this.entityOrClassName = entityOrClassName;
		this.cacheable = false;
		this.rootAlias = alias;
	}

	public List list() throws HibernateException {
		before();
		try {
			return session.list(this);
		}
		finally {
			after();
		}
	}
	
	public ScrollableResults scroll() {
		before();
		try {
			return session.scroll(this, ScrollMode.SCROLL_INSENSITIVE);
		}
		finally {
			after();
		}
	}

	public ScrollableResults scroll(ScrollMode scrollMode) {
		before();
		try {
			return session.scroll(this, scrollMode);
		}
		finally {
			after();
		}
	}

	public boolean getCacheable() {
		return this.cacheable;
	}

	public String getCacheRegion() {
		return this.cacheRegion;
	}

	public Criteria setCacheable(boolean cacheable) {
		this.cacheable = cacheable;
		return this;
	}

	public Criteria setCacheRegion(String cacheRegion) {
		this.cacheRegion = cacheRegion.trim();
		return this;
	}

	public Iterator iterateExpressionEntries() {
		return criterionEntries.iterator();
	}

	public Iterator iterateOrderings() {
		return orderEntries.iterator();
	}

	public String toString() {
		return "CriteriaImpl(" + 
			entityOrClassName + ":" + 
			(rootAlias==null ? "" : rootAlias) + 
			subcriteriaList.toString() + 
			criterionEntries.toString() + 
			( projection==null ? "" : projection.toString() ) +
			')';
	}

	public Criteria addOrder(Order ordering) {
		orderEntries.add( new OrderEntry(ordering, this) );
		return this;
	}

	public FetchMode getFetchMode(String path) {
		return (FetchMode) fetchModes.get(path);
	}

	public Criteria setFetchMode(String associationPath, FetchMode mode) {
		fetchModes.put(associationPath, mode);
		return this;
	}

	public Criteria createAlias(String associationPath, String alias) 
	throws HibernateException {
		new Subcriteria(this, associationPath, alias);
		return this;
	}

	public Criteria add(Criteria criteriaInst, Criterion expression) {
		criterionEntries.add( new CriterionEntry(expression, criteriaInst) );
		return this;
	}
	
	public static final class CriterionEntry implements Serializable {
		private final Criterion criterion;
		private final Criteria criteria;

		private CriterionEntry(Criterion criterion, Criteria criteria) {
			this.criteria = criteria;
			this.criterion = criterion;
		}

		public Criterion getCriterion() {
			return criterion;
		}

		public Criteria getCriteria() {
			return criteria;
		}

		public String toString() {
			return criterion.toString();
		}
	}

	public static final class OrderEntry implements Serializable {
		private final Order order;
		private final Criteria criteria;

		private OrderEntry(Order order, Criteria criteria) {
			this.criteria = criteria;
			this.order = order;
		}

		public Order getOrder() {
			return order;
		}

		public Criteria getCriteria() {
			return criteria;
		}

		public String toString() {
			return order.toString();
		}
	}

	public Object uniqueResult() throws HibernateException {
		return AbstractQueryImpl.uniqueElement( list() );
	}
	
	public String getEntityOrClassName() {
		return entityOrClassName;
	}

	public Criteria createCriteria(String associationPath, String alias)
		throws HibernateException {
		return new Subcriteria(this, associationPath, alias);
	}

	public Criteria createCriteria(String associationPath)
	throws HibernateException {
		return new Subcriteria(this, associationPath);
	}

	public Criteria setLockMode(LockMode lockMode) {
		return setLockMode( getAlias(), lockMode );
	}

	public Criteria setLockMode(String alias, LockMode lockMode) {
		lockModes.put(alias, lockMode);
		return this;
	}

	public Map getLockModes() {
		return lockModes;
	}

	public ResultTransformer getResultTransformer() {
		return resultTransformer;
	}

	public Criteria setResultTransformer(ResultTransformer tupleMapper) {
		this.resultTransformer = tupleMapper;
		return this;
	}

	public Criteria setComment(String comment) {
		this.comment = comment;
		return this;
	}

	public String getComment() {
		return comment;
	}

	public Criteria setProjection(Projection projection) {
		this.projection = projection;
		this.projectionCriteria = this;
		setResultTransformer(PROJECTION);
		return this;
	}
	
	public Projection getProjection() {
		return projection;
	}
	
	public Criteria getProjectionCriteria() {
		return projectionCriteria;
	}

	protected void before() {
		if ( flushMode!=null ) {
			sessionFlushMode = getSession().getFlushMode();
			getSession().setFlushMode(flushMode);
		}
		if ( cacheMode!=null ) {
			sessionCacheMode = getSession().getCacheMode();
			getSession().setCacheMode(cacheMode);
		}
	}
	
	protected void after() {
		if (sessionFlushMode!=null) {
			getSession().setFlushMode(sessionFlushMode);
			sessionFlushMode = null;
		}
		if (sessionCacheMode!=null) {
			getSession().setCacheMode(sessionCacheMode);
			sessionCacheMode = null;
		}
	}
	

}
