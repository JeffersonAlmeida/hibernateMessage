// $Id: SQLQueryRootReturn.java,v 1.2 2005/03/19 00:29:13 maxcsaucdk Exp $
package org.hibernate.loader.custom;

import java.util.Collections;
import java.util.Map;

import org.hibernate.LockMode;

/**
 * Represents a return defined as part of a native sql query which
 * names a "root" entity.  A root entity means it is explicitly a
 * "column" in the result, as opposed to a fetched relationship or role.
 *
 * @author Steve
 */
public class SQLQueryRootReturn extends SQLQueryReturn {
	private String returnEntityName;
	public SQLQueryRootReturn(String alias, String returnEntityName, LockMode lockMode) {
		this(alias, returnEntityName, null, lockMode);
	}

	public SQLQueryRootReturn(String alias, String entityName, Map propertyResults, LockMode lockMode) {
		super(alias, propertyResults, lockMode);
		this.returnEntityName = entityName;
		
	}

	public String getReturnEntityName() {
		return returnEntityName;
	}
	
}
