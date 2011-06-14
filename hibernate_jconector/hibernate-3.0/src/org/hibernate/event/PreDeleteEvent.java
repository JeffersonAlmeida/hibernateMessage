//$Id: PreDeleteEvent.java,v 1.6 2005/02/22 03:09:31 oneovthafew Exp $
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Gavin King
 */
public class PreDeleteEvent extends AbstractEvent {
	private Object entity;
	private EntityPersister persister;
	private Serializable id;
	private Object[] deletedState;
	
	public Object getEntity() {
		return entity;
	}
	public Serializable getId() {
		return id;
	}
	public EntityPersister getPersister() {
		return persister;
	}
	public Object[] getDeletedState() {
		return deletedState;
	}
	
	public PreDeleteEvent(
			Object entity, 
			Serializable id,
			Object[] deletedState,
			EntityPersister persister,
			SessionImplementor source
	) {
		super(source);
		this.entity = entity;
		this.persister = persister;
		this.id = id;
		this.deletedState = deletedState;
	}

}
