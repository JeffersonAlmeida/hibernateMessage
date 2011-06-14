//$Id: NamedSQLQueryDefinition.java,v 1.12 2005/03/15 23:13:50 oneovthafew Exp $
package org.hibernate.engine;

import java.util.List;

import org.hibernate.FlushMode;
import org.hibernate.loader.custom.SQLQueryReturn;
import org.hibernate.loader.custom.SQLQueryScalarReturn;

/**
 * Definition of a named native SQL query, defined
 * in the mapping metadata.
 * 
 * @author Max Andersen
 */
public class NamedSQLQueryDefinition extends NamedQueryDefinition {

	private SQLQueryReturn[] queryReturns;
	private SQLQueryScalarReturn[] scalarReturns;
	private final List querySpaces;
	private final boolean callable;

	public NamedSQLQueryDefinition(
		String query,
		SQLQueryReturn[] queryReturns,
		SQLQueryScalarReturn[] scalarReturns,
		List querySpaces,
		boolean cacheable, 
		String cacheRegion,
		Integer timeout,
		Integer fetchSize,
		FlushMode flushMode, boolean callable
	) {
		super(query, cacheable, cacheRegion, timeout, fetchSize, flushMode);
		this.queryReturns = queryReturns;
		this.scalarReturns = scalarReturns;
		this.querySpaces = querySpaces;
		this.callable = callable;
	}

	public SQLQueryReturn[] getQueryReturns() {
		return queryReturns;
	}

	public SQLQueryScalarReturn[] getScalarQueryReturns() {
		return scalarReturns;
	}

	public List getQuerySpaces() {
		return querySpaces;
	}

	public boolean isCallable() {
		return callable;
	}
}