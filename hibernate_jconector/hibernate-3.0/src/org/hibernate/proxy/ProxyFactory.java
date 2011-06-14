//$Id: ProxyFactory.java,v 1.3 2005/02/12 07:19:45 steveebersole Exp $
package org.hibernate.proxy;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.AbstractComponentType;

/**
 * @author Gavin King
 */
public interface ProxyFactory {

	/**
	 * Called immediately after instantiation
	 */
	public void postInstantiate(
		String entityName,
		Class persistentClass,
		Set interfaces,
		Method getIdentifierMethod,
		Method setIdentifierMethod,
		AbstractComponentType componentIdType
	) throws HibernateException;

	/**
	 * Create a new proxy
	 */
	public HibernateProxy getProxy(
		Serializable id,
		SessionImplementor session
	) throws HibernateException;

}
