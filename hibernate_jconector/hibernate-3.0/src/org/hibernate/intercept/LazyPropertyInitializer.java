//$Id: LazyPropertyInitializer.java,v 1.2 2004/08/13 01:20:04 oneovthafew Exp $
package org.hibernate.intercept;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;

/**
 * @author Gavin King
 */
public interface LazyPropertyInitializer {
	
	/**
	 * Marker value for uninitialized properties
	 */
	public static final Serializable UNFETCHED_PROPERTY = new Serializable() {
		public String toString() { return "<lazy>"; }
		public Object readResolve() {
			return UNFETCHED_PROPERTY;
		}
	};

	/**
	 * Initialize the property, and return its new value
	 */
	public Object initializeLazyProperty(String fieldName, Object entity, SessionImplementor session)
	throws HibernateException;

}
