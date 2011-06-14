//$Id: IndexPropertyAccessor.java,v 1.5 2005/02/19 12:58:23 oneovthafew Exp $
package org.hibernate.property;

import java.lang.reflect.Method;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.SessionFactoryImplementor;

/**
 * Represents a "back-reference" to the index of a collection.
 *
 * @author Gavin King
 */
public class IndexPropertyAccessor implements PropertyAccessor {
	
	private final String collectionRole;

	/**
	 * Constructs a new instance of IndexPropertyAccessor.
	 *
	 * @param collectionRole The collection role which this back ref references.
	 */
	public IndexPropertyAccessor(String collectionRole) {
		this.collectionRole = collectionRole;
	}

	public Setter getSetter(Class theClass, String propertyName) {
		return new IndexSetter();
	}

	public Getter getGetter(Class theClass, String propertyName) {
		return new IndexGetter();
	}


	/**
	 * The Setter implementation for index backrefs.
	 */
	public static final class IndexSetter implements Setter {

		public Method getMethod() {
			return null;
		}

		public String getMethodName() {
			return null;
		}

		public void set(Object target, Object value) {
			// do nothing...
		}

		public void set(Object target, Object value, SessionFactoryImplementor factory) throws HibernateException {
			// do nothing...
		}

	}


	/**
	 * The Getter implementation for index backrefs.
	 */
	public class IndexGetter implements Getter {
		
		public Object getForInsert(Object target, SessionImplementor session) throws HibernateException {
			if (session==null) {
				return BackrefPropertyAccessor.UNKNOWN;
			}
			else {
				return session.getPersistenceContext().getIndexInOwner(collectionRole, target);
			}
		}

		public Object get(Object target)  {
			return BackrefPropertyAccessor.UNKNOWN;
		}

		public Method getMethod() {
			return null;
		}

		public String getMethodName() {
			return null;
		}

		public Class getReturnType() {
			return Object.class;
		}
	}
}
