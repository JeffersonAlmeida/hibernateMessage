//$Id: PostInsertIdentifierGenerator.java,v 1.3 2005/02/12 07:19:22 steveebersole Exp $
package org.hibernate.id;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;

/**
 * @author Gavin King
 */
public interface PostInsertIdentifierGenerator extends IdentifierGenerator {
	public Serializable getGenerated(SessionImplementor session, Object object, PostInsertIdentityPersister persister) 
	throws HibernateException;
}
