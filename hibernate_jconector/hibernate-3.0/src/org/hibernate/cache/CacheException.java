//$Id: CacheException.java,v 1.1 2004/06/03 16:30:05 steveebersole Exp $
package org.hibernate.cache;

import org.hibernate.HibernateException;

/**
 * Something went wrong in the cache
 */
public class CacheException extends HibernateException {
	
	public CacheException(String s) {
		super(s);
	}
	
	public CacheException(Exception e) {
		super(e);
	}
	
}






