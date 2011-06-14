//$Id: NamedQueryDefinition.java,v 1.7 2005/02/13 11:55:47 oneovthafew Exp $
package org.hibernate.engine;

import java.io.Serializable;

import org.hibernate.FlushMode;


/**
 * Definition of a named query, defined in the mapping metadata.
 *
 * @author Gavin King
 */
public class NamedQueryDefinition implements Serializable {
	private final String query;
	private final boolean cacheable;
	private final String cacheRegion;
	private final Integer timeout;
	private final Integer fetchSize;
	private final FlushMode flushMode;

	public NamedQueryDefinition(
		String query,
		boolean cacheable,
		String cacheRegion,
		Integer timeout,
		Integer fetchSize,
		FlushMode flushMode
	) {
		this.query = query;
		this.cacheable = cacheable;
		this.cacheRegion = cacheRegion;
		this.timeout = timeout;
		this.fetchSize = fetchSize;
		this.flushMode = flushMode;
	}

	public String getQueryString() {
		return query;
	}

	public boolean isCacheable() {
		return cacheable;
	}

	public String getCacheRegion() {
		return cacheRegion;
	}

	public Integer getFetchSize() {
		return fetchSize;
	}

	public Integer getTimeout() {
		return timeout;
	}

	public FlushMode getFlushMode() {
		return flushMode;
	}

	public String toString() {
		return getClass().getName() + '(' + query + ')';
	}
}
