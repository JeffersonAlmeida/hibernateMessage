// $Id: SQLQueryReturn.java,v 1.2 2005/03/19 00:29:13 maxcsaucdk Exp $
package org.hibernate.loader.custom;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.LockMode;

/**
 * Represents the base information for a return defined as part of
 * a native sql query.
 *
 * @author Steve
 */
public abstract class SQLQueryReturn implements Serializable {
	private String alias;
	private LockMode lockMode;
	protected Map propertyResults = new HashMap();

	protected SQLQueryReturn(String alias, Map propertyResults, LockMode lockMode) {
		this.alias = alias;
		this.lockMode = lockMode;
		if(propertyResults!=null) {
			this.propertyResults = propertyResults;
		}
	}

	public String getAlias() {
		return alias;
	}

	public LockMode getLockMode() {
		return lockMode;
	}
	
	public Map getPropertyResultsMap() {
		return Collections.unmodifiableMap(propertyResults);
	}
	
}
