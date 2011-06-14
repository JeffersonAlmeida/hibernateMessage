//$Id: QueryCache.java,v 1.3 2004/12/22 22:29:11 oneovthafew Exp $
package org.hibernate.cache;

import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.Type;

/**
 * Defines the contract for caches capable of storing query results.  These
 * caches should only concern themselves with storing the matching result ids.
 * The transactional semantics are necessarily less strict than the semantics
 * of an item cache.
 * 
 * @author Gavin King
 */
public interface QueryCache {

	public void clear() throws CacheException;
	
	public void put(QueryKey key, Type[] returnTypes, List result, SessionImplementor session) throws HibernateException;

	public List get(QueryKey key, Type[] returnTypes, Set spaces, SessionImplementor session) throws HibernateException;

	public void destroy();

	public Cache getCache();

	public String getRegionName();
}
