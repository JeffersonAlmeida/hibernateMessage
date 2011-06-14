//$Id: EmbeddedPropertyAccessor.java,v 1.4 2005/02/19 12:58:23 oneovthafew Exp $
package org.hibernate.property;

import java.lang.reflect.Method;

import org.hibernate.HibernateException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.SessionFactoryImplementor;

/**
 * @author Gavin King
 */
public class EmbeddedPropertyAccessor implements PropertyAccessor {
	
	public static final class EmbeddedGetter implements Getter {
		
		private final Class clazz;
		
		EmbeddedGetter(Class clazz) {
			this.clazz = clazz;
		}
		
		public Object get(Object target) throws HibernateException {
			return target;
		}
		
		public Object getForInsert(Object target, SessionImplementor session) {
			return get( target );
		}

		public Method getMethod() {
			return null;
		}

		public String getMethodName() {
			return null;
		}

		public Class getReturnType() {
			return clazz;
		}
		
		public String toString() {
			return "EmbeddedGetter(" + clazz.getName() + ')';
		}
	}

	public static final class EmbeddedSetter implements Setter {
		
		private final Class clazz;
		
		EmbeddedSetter(Class clazz) {
			this.clazz = clazz;
		}
		
		public Method getMethod() {
			return null;
		}

		public String getMethodName() {
			return null;
		}

		public void set(Object target, Object value, SessionFactoryImplementor factory) throws HibernateException {}
		
		public String toString() {
			return "EmbeddedSetter(" + clazz.getName() + ')';
		}
	}

	public Getter getGetter(Class theClass, String propertyName)
	throws PropertyNotFoundException {
		return new EmbeddedGetter(theClass);
	}

	public Setter getSetter(Class theClass, String propertyName)
	throws PropertyNotFoundException {
		return new EmbeddedSetter(theClass);
	}

}