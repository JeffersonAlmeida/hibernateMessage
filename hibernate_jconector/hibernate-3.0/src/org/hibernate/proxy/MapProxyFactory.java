//$Id: MapProxyFactory.java,v 1.4 2005/02/12 07:19:45 steveebersole Exp $
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
public class MapProxyFactory implements ProxyFactory {

	private String entityName;

	public void postInstantiate(
		final String entityName, 
		final Class persistentClass,
		final Set interfaces, 
		final Method getIdentifierMethod,
		final Method setIdentifierMethod,
		AbstractComponentType componentIdType) 
	throws HibernateException {
		
		this.entityName = entityName;

	}

	public HibernateProxy getProxy(
		final Serializable id, 
		final SessionImplementor session)
	throws HibernateException {
		return new MapProxy( new MapLazyInitializer(entityName, id, session) );
	}

}
