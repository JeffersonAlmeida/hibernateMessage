//$Id: StandardQueryCache.java,v 1.9 2005/03/16 06:01:17 oneovthafew Exp $
package org.hibernate.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Settings;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;

/**
 * The standard implementation of the Hibernate QueryCache interface.  This
 * implementation is very good at recognizing stale query results and
 * and re-running queries when it detects this condition, recaching the new
 * results.
 *
 * @author Gavin King
 */
public class StandardQueryCache implements QueryCache {

	private static final Log log = LogFactory.getLog(StandardQueryCache.class);

	private Cache queryCache;
	private UpdateTimestampsCache updateTimestampsCache;
	private final String regionName;

	public void clear() throws CacheException {
		queryCache.clear();
	}

	public StandardQueryCache(
			final Settings settings, 
			final Properties props, 
			final UpdateTimestampsCache updateTimestampsCache, 
			String regionName)
	throws HibernateException {
		
		if (regionName==null) regionName = StandardQueryCache.class.getName();
		String prefix = settings.getCacheRegionPrefix();
		if (prefix!=null) regionName = prefix + '.' + regionName;
		
		log.info("starting query cache at region: " + regionName);
		
		this.queryCache = settings.getCacheProvider().buildCache(regionName, props);
		this.updateTimestampsCache = updateTimestampsCache;
		this.regionName = regionName;
	}

	public void put(QueryKey key, Type[] returnTypes, List result, SessionImplementor session) throws HibernateException {
		if ( log.isDebugEnabled() ) log.debug("caching query results in region: " + regionName);
		List cacheable = new ArrayList( result.size()+1 );
		cacheable.add( new Long( session.getTimestamp() ) );
		for ( int i=0; i<result.size(); i++ ) {
			if ( returnTypes.length==1 ) {
				cacheable.add( returnTypes[0].disassemble( result.get(i), session, null ) );
			}
			else {
				cacheable.add( TypeFactory.disassemble( (Object[]) result.get(i), returnTypes, session, null ) );
			}
		}
		queryCache.put(key, cacheable);
	}

	public List get(QueryKey key, Type[] returnTypes, Set spaces, SessionImplementor session) throws HibernateException {
		if ( log.isDebugEnabled() ) log.debug("checking cached query results in region: " + regionName);
		List cacheable = (List) queryCache.get(key);
		if (cacheable==null) {
			log.debug("query results were not found in cache");
			return null;
		}
		List result = new ArrayList( cacheable.size()-1 );
		Long timestamp = (Long) cacheable.get(0);
		log.debug("Checking query spaces for up-to-dateness: [" + spaces + "]");
		if ( ! isUpToDate(spaces, timestamp) ) {
			log.debug("cached query results were not up to date");
			return null;
		}
		log.debug("returning cached query results");
		for ( int i=1; i<cacheable.size(); i++ ) {
			if ( returnTypes.length==1 ) {
				result.add( returnTypes[0].assemble( (Serializable) cacheable.get(i), session, null ) );
			}
			else {
				result.add( TypeFactory.assemble( (Serializable[]) cacheable.get(i), returnTypes, session, null ) );
			}
		}
		return result;
	}

	protected boolean isUpToDate(Set spaces, Long timestamp) {
		return updateTimestampsCache.isUpToDate(spaces, timestamp);
	}

	public void destroy() {
		try {
			queryCache.destroy();
		}
		catch (Exception e) {
			log.warn("could not destroy query cache: " + regionName, e);
		}
	}
	
	public Cache getCache() {
		return queryCache;
	}
	
	public String getRegionName() {
		return regionName;
	}
	
	public String toString() {
		return "StandardQueryCache(" + regionName + ')';
	}

}
