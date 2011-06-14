//$Id: AbstractComponentType.java,v 1.7 2005/02/16 12:50:18 oneovthafew Exp $
package org.hibernate.type;

import java.lang.reflect.Method;

import org.hibernate.EntityMode;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.Cascades;
import org.hibernate.engine.SessionImplementor;

/**
 * Enables other Component-like types to hold collections and have cascades, etc.
 *
 * @see ComponentType
 * @see AnyType
 * @author Gavin King
 */
public interface AbstractComponentType extends Type {
	public Type[] getSubtypes();
	public String[] getPropertyNames();
	/**
	 * Optional operation
	 * @return nullability of component properties
	 */
	public boolean[] getPropertyNullability();
	public Object[] getPropertyValues(Object component, SessionImplementor session) throws HibernateException;
	/**
	 * Optional operation
	 * @param entityMode
	 */
	public Object[] getPropertyValues(Object component, EntityMode entityMode) throws HibernateException;
	/**
	 * Optional operation
	 * @param entityMode TODO
	 */
	public void setPropertyValues(Object component, Object[] values, EntityMode entityMode) throws HibernateException;
	public Object getPropertyValue(Object component, int i, SessionImplementor session) throws HibernateException;
	//public Object instantiate(Object parent, SessionImplementor session) throws HibernateException;
	public Cascades.CascadeStyle getCascadeStyle(int i);
	public FetchMode getFetchMode(int i);
	public boolean isMethodOf(Method method);
}
