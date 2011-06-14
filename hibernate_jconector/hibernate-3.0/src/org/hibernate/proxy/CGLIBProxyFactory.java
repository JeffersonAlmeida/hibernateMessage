//$Id: CGLIBProxyFactory.java,v 1.3 2005/02/12 07:19:45 steveebersole Exp $
package org.hibernate.proxy;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Set;

import net.sf.cglib.proxy.Factory;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.AbstractComponentType;

/**
 * @author Gavin King
 */
public class CGLIBProxyFactory implements ProxyFactory {

	protected static final Class[] NO_CLASSES = new Class[0];

	private Class persistentClass;
	private String entityName;
	private Class[] interfaces;
	private Method getIdentifierMethod;
	private Method setIdentifierMethod;
	private AbstractComponentType componentIdType;
	private Factory factory;

	public void postInstantiate(
		final String entityName,
		final Class persistentClass,
		final Set interfaces,
		final Method getIdentifierMethod,
		final Method setIdentifierMethod,
		AbstractComponentType componentIdType)
	throws HibernateException {
		this.entityName = entityName;
		this.persistentClass = persistentClass;
		this.interfaces = (Class[]) interfaces.toArray(NO_CLASSES);
		this.getIdentifierMethod = getIdentifierMethod;
		this.setIdentifierMethod = setIdentifierMethod;
		this.componentIdType = componentIdType;
		factory = CGLIBLazyInitializer.getProxyFactory(persistentClass, this.interfaces);
	}

	public HibernateProxy getProxy(Serializable id, SessionImplementor session)
		throws HibernateException {

		return CGLIBLazyInitializer.getProxy(
			factory, 
			entityName, 
			persistentClass, 
			interfaces, 
			getIdentifierMethod, 
			setIdentifierMethod,
			componentIdType,
			id, 
			session
		);
	}

}
