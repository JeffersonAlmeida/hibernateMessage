//$Id: SQLQueryImpl.java,v 1.23 2005/03/19 00:29:16 maxcsaucdk Exp $
package org.hibernate.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.NamedSQLQueryDefinition;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.loader.custom.SQLCustomQuery;
import org.hibernate.loader.custom.SQLQueryJoinReturn;
import org.hibernate.loader.custom.SQLQueryReturn;
import org.hibernate.loader.custom.SQLQueryRootReturn;
import org.hibernate.loader.custom.SQLQueryScalarReturn;
import org.hibernate.type.Type;
import org.hibernate.util.CollectionHelper;

/**
 * Implements SQL query passthrough.
 *
 * <pre>
 * <sql-query name="mySqlQuery">
 * <return alias="person" class="eg.Person"/>
 *   SELECT {person}.NAME AS {person.name}, {person}.AGE AS {person.age}, {person}.SEX AS {person.sex}
 *   FROM PERSON {person} WHERE {person}.NAME LIKE 'Hiber%'
 * </sql-query>
 * </pre>
 *
 * @author Max Andersen
 */
public class SQLQueryImpl extends AbstractQueryImpl implements SQLQuery {

	private final List queryReturns;
	private final List scalarQueryReturns;
	private final Collection querySpaces;
	private final boolean callable;

	/**
	 * Constructs a SQLQueryImpl given a sql query defined in the mappings.
	 *
	 * @param queryDef The representation of the defined <sql-query/>.
	 * @param session The session to which this SQLQueryImpl belongs.
	 */
	SQLQueryImpl(NamedSQLQueryDefinition queryDef, SessionImplementor session) {
		this(
		        queryDef.getQueryString(),
		        Arrays.asList( queryDef.getQueryReturns() ),
		        Arrays.asList( queryDef.getScalarQueryReturns() ),
		        queryDef.getQuerySpaces(),
		        queryDef.getFlushMode(),
		        queryDef.isCallable(),
		        session
		);
	}

	SQLQueryImpl(
		final String sql,
		final List queryReturns,
		final List scalarQueryReturns,
		final Collection querySpaces,
		final FlushMode flushMode,
		boolean callable, final SessionImplementor session
	) {
		super(sql, flushMode, session);
		this.queryReturns = queryReturns;
		this.scalarQueryReturns = scalarQueryReturns;
		this.querySpaces = querySpaces;
		this.callable = callable;
	}

	SQLQueryImpl(
		final String sql,
		final String returnAliases[],
		final Class returnClasses[],
		final LockMode[] lockModes,
		final SessionImplementor session, 
		final Collection querySpaces, 
		final FlushMode flushMode
	) {
		super(sql, flushMode, session);
		scalarQueryReturns=null;
		queryReturns = new ArrayList(returnAliases.length);
		for ( int i=0; i<returnAliases.length; i++ ) {
			SQLQueryRootReturn ret = new SQLQueryRootReturn(
					returnAliases[i],
					returnClasses[i].getName(),
					lockModes==null ? LockMode.NONE : lockModes[i]
			);
			queryReturns.add(ret);
		}
		this.querySpaces = querySpaces;
		this.callable = false;
	}

	SQLQueryImpl(
		final String sql,
		final String returnAliases[],
		final Class returnClasses[], 
		final SessionImplementor session
	) {
		this(sql, returnAliases, returnClasses, null, session, null, null);
	}
	
	SQLQueryImpl(String sql, SessionImplementor session) {
		super(sql, null, session);
		queryReturns = new ArrayList();
		scalarQueryReturns = new ArrayList();
		querySpaces = null;
		callable = false;
	}
	
	private static final SQLQueryReturn[] NO_SQL_RETURNS = new SQLQueryReturn[0];
	private static final SQLQueryScalarReturn[] NO_SQL_SCALAR_RETURNS = new SQLQueryScalarReturn[0];
	
	private SQLQueryReturn[] getQueryReturns() {
		return (SQLQueryReturn[]) queryReturns.toArray(NO_SQL_RETURNS);
	}

	private SQLQueryScalarReturn[] getQueryScalarReturns() {
		return scalarQueryReturns==null ? 
			null : 
			(SQLQueryScalarReturn[]) scalarQueryReturns.toArray(NO_SQL_SCALAR_RETURNS);
	}

	public List list() throws HibernateException {
		verifyParameters();
		Map namedParams = getNamedParams();
		before();

		SQLCustomQuery cq = new SQLCustomQuery(
				getQueryReturns(),
		        getQueryScalarReturns(),
		        bindParameterLists(namedParams),
		        querySpaces,
				getSession().getFactory()
		);
		
		try {
			return getSession().listCustomQuery( cq, getQueryParameters(namedParams) );
		}
		finally {
			after();
		}
	}
	
	public QueryParameters getQueryParameters(Map namedParams) {
		QueryParameters qp = super.getQueryParameters(namedParams);
		qp.setCallable(callable);
		return qp;
	}

	protected void verifyParameters() {
		verifyParameters(callable);
	}
	
	public ScrollableResults scroll(ScrollMode scrollMode) throws HibernateException {
		verifyParameters();
		Map namedParams = getNamedParams();
		before();

		SQLCustomQuery cq = new SQLCustomQuery(
				getQueryReturns(),
		        getQueryScalarReturns(),
		        bindParameterLists(namedParams),
		        querySpaces,
				getSession().getFactory()
		);
		
		QueryParameters qp = getQueryParameters(namedParams);
		qp.setScrollMode(scrollMode);
		
		try {
			return getSession().scrollCustomQuery(cq, qp);
		}
		finally {
			after();
		}
	}

	public ScrollableResults scroll() throws HibernateException {
		return scroll(ScrollMode.SCROLL_INSENSITIVE);
	}

	public Iterator iterate() throws HibernateException {
		throw new UnsupportedOperationException("SQL queries do not currently support iteration");
	}

	public Type[] getReturnTypes() throws HibernateException {
		throw new UnsupportedOperationException("not yet implemented for SQL queries");
	}
	
	public Query setLockMode(String alias, LockMode lockMode) {
		throw new UnsupportedOperationException("cannot set the lock mode for a native SQL query");
	}
	
	protected Map getLockModes() {
		//we never need to apply locks to the SQL
		return CollectionHelper.EMPTY_MAP;
	}

	public SQLQuery addScalar(String columnAlias, Type type) {
		scalarQueryReturns.add( new SQLQueryScalarReturn(columnAlias, type) );
		return this;
	}

	public SQLQuery addJoin(String alias, String path) {
		return addJoin(alias, path, LockMode.READ);
	}

	public SQLQuery addEntity(String alias, String entityName) {
		return addEntity(alias, entityName, LockMode.READ);
	}

	public SQLQuery addEntity(String alias, Class entityClass) {
		return addEntity( alias, entityClass.getName() );
	}
	
	public SQLQuery addJoin(String alias, String path, LockMode lockMode) {
		int loc = path.indexOf('.');
		if ( loc<0 ) throw new QueryException("not a property path: " + path);
		String ownerAlias = path.substring(0, loc);
		String role = path.substring(loc+1);
		queryReturns.add( new SQLQueryJoinReturn(alias, ownerAlias, role, CollectionHelper.EMPTY_MAP, lockMode) );
		return this;
	}

	public SQLQuery addEntity(String alias, String entityName, LockMode lockMode) {
		queryReturns.add( new SQLQueryRootReturn(alias, entityName, lockMode) );
		return this;
	}

	public SQLQuery addEntity(String alias, Class entityClass, LockMode lockMode) {
		return addEntity( alias, entityClass.getName(), lockMode );
	}
	
}
