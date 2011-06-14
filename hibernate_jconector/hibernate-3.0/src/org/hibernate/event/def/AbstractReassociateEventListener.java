//$Id: AbstractReassociateEventListener.java,v 1.4 2005/02/22 03:09:33 oneovthafew Exp $
package org.hibernate.event.def;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.Status;
import org.hibernate.engine.Versioning;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.event.AbstractEvent;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.TypeFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A convenience base class for listeners that respond to requests to reassociate an entity
 * to a session ( such as through lock() or update() ).
 *
 * @author Gavin King
 */
public class AbstractReassociateEventListener extends AbstractEventListener {

	private static final Log log = LogFactory.getLog(AbstractReassociateEventListener.class);

	/**
	 * Associates a given entity (either transient or associated with another session) to
	 * the given session.
	 *
	 * @param event The event triggering the re-association
	 * @param object The entity to be associated
	 * @param id The id of the entity.
	 * @param persister The entity's persister instance.
	 * @return An EntityEntry representing the entity within this session.
	 * @throws HibernateException
	 */
	protected final EntityEntry reassociate(AbstractEvent event, Object object, Serializable id, EntityPersister persister)
	throws HibernateException {
		
		if ( log.isTraceEnabled() ) log.trace(
				"reassociating transient instance: " +
				MessageHelper.infoString( persister, id, event.getSession().getFactory() )
		);
		
		SessionImplementor source = event.getSession();
		source.getPersistenceContext().checkUniqueness(id, persister, object);
		
		//get a snapshot
		Object[] values = persister.getPropertyValues( object, source.getEntityMode() );
		TypeFactory.deepCopy( 
				values, 
				persister.getPropertyTypes(), 
				persister.getPropertyUpdateability(), 
				values, 
				source 
		);
		Object version = Versioning.getVersion(values, persister);
		
		EntityEntry newEntry = source.getPersistenceContext().addEntity(
				object, 
				Status.MANAGED, 
				values, 
				id, 
				version, 
				LockMode.NONE, 
				true, 
				persister, 
				false
		);
		
		new OnLockVisitor( source, id ).process( object, persister );
		
		persister.afterReassociate(object, source);
		
		return newEntry;
		
	}

}
