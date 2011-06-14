//$Id: SQLQuery.java,v 1.4 2005/02/12 07:19:50 steveebersole Exp $
package org.hibernate;

import org.hibernate.type.Type;

/**
 * Allows the user to declare the types and select list injection
 * points of all entities returned by the query. Also allows
 * declaration of the type and column alias of any scalar results
 * of the query.
 * 
 * @author Gavin King
 */
public interface SQLQuery extends Query {
	/**
	 * Declare a "root" entity
	 */
	public SQLQuery addEntity(String alias, String entityName);
	/**
	 * Declare a "root" entity, specifying a lock mode
	 */
	public SQLQuery addEntity(String alias, String entityName, LockMode lockMode);
	/**
	 * Declare a "root" entity
	 */
	public SQLQuery addEntity(String alias, Class entityClass);
	/**
	 * Declare a "root" entity, specifying a lock mode
	 */
	public SQLQuery addEntity(String alias, Class entityClass, LockMode lockMode);
	
	/**
	 * Declare a "joined" entity
	 */
	public SQLQuery addJoin(String alias, String path);
	/**
	 * Declare a "joined" entity, specifying a lock mode
	 */
	public SQLQuery addJoin(String alias, String path, LockMode lockMode);
	
	/**
	 * Declare a scalar query result
	 */
	public SQLQuery addScalar(String columnAlias, Type type);
}
