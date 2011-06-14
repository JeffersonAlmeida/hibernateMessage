//$Id: CGLIBLazyInitializer.java,v 1.9 2005/03/21 16:08:28 oneovthafew Exp $
package org.hibernate.proxy;

import java.io.Serializable;
import java.lang.reflect.Method;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.util.ReflectHelper;

import org.apache.commons.logging.LogFactory;


/**
 * A <tt>LazyInitializer</tt> implemented using the CGLIB bytecode generation library
 */
public final class CGLIBLazyInitializer extends BasicLazyInitializer implements MethodInterceptor {

	private Class[] interfaces;
	private boolean constructed = false;

	static HibernateProxy getProxy(
		final String entityName,
		final Class persistentClass,
		final Class[] interfaces,
		final Method getIdentifierMethod,
		final Method setIdentifierMethod,
		AbstractComponentType componentIdType,
		final Serializable id,
		final SessionImplementor session)
	throws HibernateException {
		//note: interfaces is assumed to already contain HibernateProxy.class
		try {
			final CGLIBLazyInitializer instance = new CGLIBLazyInitializer(
				entityName, 
				persistentClass, 
				interfaces, 
				id, 
				getIdentifierMethod, 
				setIdentifierMethod, 
				componentIdType,
				session
			);
			final HibernateProxy proxy = (HibernateProxy) Enhancer.create(
				(interfaces.length==1) ?
					persistentClass :
					null,
				interfaces,
				instance
			);
			instance.constructed = true;
			return proxy;
		}
		catch (Throwable t) {
			LogFactory.getLog(BasicLazyInitializer.class).error("CGLIB Enhancement failed: " + entityName, t);
			throw new HibernateException( "CGLIB Enhancement failed: " + entityName, t );
		}
	}

	public static HibernateProxy getProxy(
		final Factory factory,
		final String entityName,
		final Class persistentClass,
		final Class[] interfaces,
		final Method getIdentifierMethod,
		final Method setIdentifierMethod,
		AbstractComponentType componentIdType,
		final Serializable id,
		final SessionImplementor session)
	throws HibernateException {
		final CGLIBLazyInitializer instance = new CGLIBLazyInitializer(
			entityName, 
			persistentClass, 
			interfaces, 
			id, 
			getIdentifierMethod, 
			setIdentifierMethod,
			componentIdType,
			session
		);
		final HibernateProxy proxy = (HibernateProxy) factory.newInstance(instance);
		instance.constructed = true;
		return proxy;
	}

	public static Factory getProxyFactory(Class persistentClass, Class[] interfaces) throws HibernateException {
		//note: interfaces is assumed to already contain HibernateProxy.class
		try {
			return (Factory) Enhancer.create(
				(interfaces.length==1) ?
					persistentClass :
					null,
				interfaces,
				NULL_METHOD_INTERCEPTOR
			);
		}
		catch (Throwable t) {
			LogFactory.getLog(BasicLazyInitializer.class).error(
					"CGLIB Enhancement failed: " + 
					persistentClass.getName(), 
					t
			);
			throw new HibernateException( "CGLIB Enhancement failed: " + persistentClass.getName(), t );
		}
	}

	private CGLIBLazyInitializer(
		final String entityName,
		final Class persistentClass,
		final Class[] interfaces,
		final Serializable id,
		final Method getIdentifierMethod,
		final Method setIdentifierMethod,
		AbstractComponentType componentIdType,
		final SessionImplementor session) {
		super(entityName, persistentClass, id, getIdentifierMethod, setIdentifierMethod, componentIdType, session);
		this.interfaces = interfaces;
	}

	public Object intercept(
		final Object proxy,
		final Method method,
		final Object[] args,
		final MethodProxy methodProxy)
	throws Throwable {
		if (constructed) {
			Object result = invoke(method, args, proxy);
			if (result==INVOKE_IMPLEMENTATION) {
				Object target = getImplementation();
				final Object returnValue;
				if ( ReflectHelper.isPublic(persistentClass, method) ) {
					returnValue = methodProxy.invoke(target, args);
				}
				else {
					if ( !method.isAccessible() ) method.setAccessible(true);
					returnValue = method.invoke(target, args);
				}
				return returnValue==target ? proxy : returnValue;
			}
			else {
				return result;
			}
		}
		else {
			//while constructor is running
			if( method.getName().equals("getHibernateLazyInitializer") ) {
				return this;
			}
			else {
				return methodProxy.invokeSuper(proxy, args);
			}
		}
	}

	protected Object serializableProxy() {
		return new SerializableProxy( 
			getEntityName(), 
			persistentClass, 
			interfaces, 
			getIdentifier(), 
			getIdentifierMethod, 
			setIdentifierMethod,
			componentIdType
		);
	}

	private static final MethodInterceptor NULL_METHOD_INTERCEPTOR = new MethodInterceptor() {
		public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
			return proxy.invokeSuper(obj, args);
		}
	};
}
