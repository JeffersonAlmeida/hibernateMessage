//$Id: Assigned.java,v 1.5 2005/02/12 07:19:22 steveebersole Exp $
package org.hibernate.id;

import java.io.Serializable;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.Type;

/**
 * <b>assigned</b><br>
 * <br>
 * An <tt>IdentifierGenerator</tt> that returns the current identifier assigned
 * to an instance.
 *
 * @author Gavin King
 */

public class Assigned implements IdentifierGenerator, Configurable {
	
	private String entityName;

	public Serializable generate(SessionImplementor session, Object obj) throws HibernateException {
		if (obj instanceof PersistentCollection) throw new IdentifierGenerationException(
			"Illegal use of assigned id generation for a toplevel collection"
		);
		final Serializable id = session.getEntityPersister( entityName, obj ).getIdentifier( obj, session.getEntityMode() );
		if (id==null) throw new IdentifierGenerationException(
			"ids for this class must be manually assigned before calling save(): " + obj.getClass().getName()
		);
		return id;
	}

	public void configure(Type type, Properties params, Dialect d)
	throws MappingException {
		entityName = params.getProperty(ENTITY_NAME);
	}
}






