//$Id: Getter.java,v 1.5 2005/02/19 12:58:23 oneovthafew Exp $
package org.hibernate.property;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;

/**
 * Gets values of a particular property
 *
 * @author Gavin King
 */
public interface Getter extends Serializable {
	/**
	 * Get the property value from the given instance .
	 * @param owner The instance containing the value to be retreived.
	 * @return The extracted value.
	 * @throws HibernateException
	 */
	public Object get(Object owner) throws HibernateException;

	/**
	 * Get the property value from the given owner instance.
	 *
	 * @param owner The instance containing the value to be retreived.
	 * @param session The session from which this request originated.
	 * @return The extracted value.
	 * @throws HibernateException
	 */
	public Object getForInsert(Object owner, SessionImplementor session) 
	throws HibernateException;

	/**
	 * Get the declared Java type
	 */
	public Class getReturnType();

	/**
	 * Optional operation (return null)
	 */
	public String getMethodName();

	/**
	 * Optional operation (return null)
	 */
	public Method getMethod();
}
