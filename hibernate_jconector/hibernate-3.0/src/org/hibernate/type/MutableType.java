//$Id: MutableType.java,v 1.6 2005/02/16 12:50:19 oneovthafew Exp $
package org.hibernate.type;

import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;

/**
 * Superclass for mutable nullable types
 * @author Gavin King
 */
public abstract class MutableType extends NullableType {

	public final boolean isMutable() {
		return true;
	}

	protected abstract Object deepCopyNotNull(Object value) throws HibernateException;

	public final Object deepCopy(Object value, EntityMode entityMode, SessionFactoryImplementor factory) throws HibernateException {
		return (value==null) ? null : deepCopyNotNull(value);
	}

	public Object replace(
		Object original,
		Object target,
		SessionImplementor session,
		Object owner, 
		Map copyCache)
	throws HibernateException {
		return deepCopy( original, session.getEntityMode(), session.getFactory() );
	}

}
