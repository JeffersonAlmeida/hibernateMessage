//$Id: CacheEntryStructure.java,v 1.1 2005/02/13 12:46:58 oneovthafew Exp $
package org.hibernate.cache.entry;

import org.hibernate.engine.SessionFactoryImplementor;



/**
 * @author Gavin King
 */
public interface CacheEntryStructure {
	public Object structure(Object item);
	public Object destructure(Object map, SessionFactoryImplementor factory);
}
