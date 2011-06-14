//$Id: BasicLazyInitializer.java,v 1.11 2005/02/16 12:50:18 oneovthafew Exp $
package org.hibernate.proxy;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.hibernate.engine.EntityKey;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.util.MarkerObject;
import org.hibernate.util.ReflectHelper;

/**
 * Lazy initializer for POJOs
 * @author Gavin King
 */
public abstract class BasicLazyInitializer extends AbstractLazyInitializer {

	protected static final Object INVOKE_IMPLEMENTATION = new MarkerObject("INVOKE_IMPLEMENTATION");

	protected Class persistentClass;
	protected Method getIdentifierMethod;
	protected Method setIdentifierMethod;
	protected boolean overridesEquals;
	private Object replacement;
	protected AbstractComponentType componentIdType;

	protected BasicLazyInitializer(
			String entityName, 
			Class persistentClass, 
			Serializable id, 
			Method getIdentifierMethod, 
			Method setIdentifierMethod, 
			AbstractComponentType componentIdType,
			SessionImplementor session) {
		super(entityName, id, session);
		this.persistentClass = persistentClass;
		this.getIdentifierMethod = getIdentifierMethod;
		this.setIdentifierMethod = setIdentifierMethod;
		this.componentIdType = componentIdType;
		overridesEquals = ReflectHelper.overridesEquals(persistentClass);
	}

	protected abstract Object serializableProxy();

	protected final Object invoke(Method method, Object[] args, Object proxy) throws Throwable {

		String methodName = method.getName();
		int params = method.getParameterTypes().length;

		if ( params==0 ) {

			if ( "writeReplace".equals(methodName) ) {
				
				final Object target = getTarget();
				final SessionImplementor session = getSession();
				if ( target==null && session!=null ) {
					final EntityKey key = new EntityKey( 
						getIdentifier(), 
						session.getFactory().getEntityPersister( getEntityName() ),
						session.getEntityMode() 
					);
					setTarget( session.getPersistenceContext().getEntity(key) );
				}
				if (target==null) {
					if (replacement==null) replacement = serializableProxy();
					return replacement;
				}
				else {
					return target;
				}
				/*if (replacement==null) replacement = serializableProxy();
				replacement.setSnapshot(snapshot);
				replacement.setTarget( (Serializable) target );
				return replacement;*/

			}
			else if ( !overridesEquals && "hashCode".equals(methodName) ) {
				return new Integer( System.identityHashCode(proxy) );
			}
			else if ( isUninitialized() && method.equals(getIdentifierMethod) ) {
				return getIdentifier();
			}
			else if ( "finalize".equals(methodName) ) {
				return null;
			}
			else if ( "getHibernateLazyInitializer".equals(methodName) ) {
				return this;
			}

		}
		else if ( params==1 ) {

			if ( !overridesEquals && "equals".equals(methodName) ) {
				return args[0]==proxy ? Boolean.TRUE : Boolean.FALSE;
			}
			else if ( method.equals(setIdentifierMethod) ) {
				initialize();
				setIdentifier( (Serializable) args[0] );
				return INVOKE_IMPLEMENTATION;
			}

		}
		
		//if it is a property of an embedded component, invoke on the "identifier"
		if ( componentIdType!=null && componentIdType.isMethodOf(method) ) {
			return method.invoke( getIdentifier(), args );
		}
		
		// otherwise:
		return INVOKE_IMPLEMENTATION;

	}

	public final Class getPersistentClass() {
		return persistentClass;
	}

}
