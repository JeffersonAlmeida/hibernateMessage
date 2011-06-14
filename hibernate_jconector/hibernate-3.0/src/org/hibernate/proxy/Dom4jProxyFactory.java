// $Id: Dom4jProxyFactory.java,v 1.2 2005/02/12 07:19:45 steveebersole Exp $
package org.hibernate.proxy;

import org.hibernate.HibernateException;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.engine.SessionImplementor;

import java.util.Set;
import java.lang.reflect.Method;
import java.io.Serializable;

/**
 * Builds proxies for "dom4j" entity representations.
 *
 * @author Steve Ebersole
 */
public class Dom4jProxyFactory implements ProxyFactory {

	private String entityName;

	/**
	 * Called immediately after instantiation
	 */
	public void postInstantiate(
	        String entityName,
	        Class persistentClass,
	        Set interfaces,
	        Method getIdentifierMethod,
	        Method setIdentifierMethod,
	        AbstractComponentType componentIdType) throws HibernateException {
		this.entityName = entityName;
	}

	/**
	 * Create a new proxy
	 */
	public HibernateProxy getProxy(Serializable id, SessionImplementor session) throws HibernateException {
		return new Dom4jProxy( new Dom4jLazyInitializer( entityName, id, session ) );
	}
}
