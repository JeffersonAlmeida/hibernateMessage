//$Id: PostDeleteEvent.java,v 1.12 2005/02/22 03:09:31 oneovthafew Exp $
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Gavin King
 */
public class PostDeleteEvent extends AbstractEvent {
	private Object entity;
	private EntityPersister persister;
	private Serializable id;
	private Object[] deletedState;
	
	public PostDeleteEvent(
			Object entity, 
			Serializable id,
			Object[] deletedState,
			EntityPersister persister,
			SessionImplementor source
	) {
		super(source);
		this.entity = entity;
		this.id = id;
		this.persister = persister;
		this.deletedState = deletedState;
	}
	
	public Serializable getId() {
		return id;
	}
	public EntityPersister getPersister() {
		return persister;
	}
	public Object getEntity() {
		return entity;
	}
	public Object[] getDeletedState() {
		return deletedState;
	}
}
