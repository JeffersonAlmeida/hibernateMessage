//$Id: TreeCache.java,v 1.9 2005/03/16 20:31:42 oneovthafew Exp $
package org.hibernate.cache;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.cache.Fqn;

/**
 * Represents a particular region within the given JBossCache TreeCache.
 *
 * @author Gavin King
 */
public class TreeCache implements Cache {

	private static final String ITEM = "item";

	private org.jboss.cache.TreeCache cache;
	private final String regionName;
	private final String userRegionName;
	private final TransactionManager transactionManager;

	public TreeCache(org.jboss.cache.TreeCache cache, String regionName, TransactionManager transactionManager) 
	throws CacheException {
		this.cache = cache;
		userRegionName = regionName;
		this.regionName = '/' + regionName.replace('.', '/');
		this.transactionManager = transactionManager;
	}

	public Object get(Object key) throws CacheException {
		try {
			return cache.get( new Fqn( new Object[] { regionName, key } ), ITEM );
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}

	public void update(Object key, Object value) throws CacheException {
		try {
			cache.put( new Fqn( new Object[] { regionName, key } ), ITEM, value );
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}

	public void put(Object key, Object value) throws CacheException {
		try {
			//do the failfast put outside the scope of the JTA txn
			Transaction tx = transactionManager==null ? null : transactionManager.suspend();
			cache.putFailFast( new Fqn( new Object[] { regionName, key } ), ITEM, value, 0 );
			if (tx!=null) transactionManager.resume(tx);
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}

	public void remove(Object key) throws CacheException {
		try {
			cache.remove( new Fqn( new Object[] { regionName, key } ) );
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}

	public void clear() throws CacheException {
		try {
			cache.remove( new Fqn(regionName) );
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}

	public void destroy() throws CacheException {
		clear();
	}

	public void lock(Object key) throws CacheException {
		throw new UnsupportedOperationException("TreeCache is a fully transactional cache" + regionName);
	}

	public void unlock(Object key) throws CacheException {
		throw new UnsupportedOperationException("TreeCache is a fully transactional cache: " + regionName);
	}

	public long nextTimestamp() {
		return System.currentTimeMillis() / 100;
	}

	public int getTimeout() {
		return 600; //60 seconds
	}

	public String getRegionName() {
		return userRegionName;
	}

	public long getSizeInMemory() {
		return -1;
	}

	public long getElementCountInMemory() {
		try {
			return cache.getChildrenNames( new Fqn(regionName) ).size();
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}

	public long getElementCountOnDisk() {
		return 0;
	}
	
	public Map toMap() {
		try {
			Map result = new HashMap();
			Iterator iter = cache.getChildrenNames( new Fqn(regionName) ).iterator();
			while ( iter.hasNext() ) {
				Object key = iter.next();
				result.put( key, cache.get( new Fqn( new Object[] { regionName, key } ), ITEM ) );
			}
			return result;
		}
		catch (Exception e) {
			throw new CacheException(e);
		}
	}
	
	public String toString() {
		return "TreeCache(" + userRegionName + ')';
	}
	
}
