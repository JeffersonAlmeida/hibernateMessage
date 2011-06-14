//$Id: HibernateProxy.java,v 1.3 2004/08/29 07:31:02 oneovthafew Exp $
package org.hibernate.proxy;

import java.io.Serializable;

/**
 * Marker interface for entity proxies
 * @author Gavin King
 */
public interface HibernateProxy extends Serializable {
	public Object writeReplace();
	public LazyInitializer getHibernateLazyInitializer();
}







