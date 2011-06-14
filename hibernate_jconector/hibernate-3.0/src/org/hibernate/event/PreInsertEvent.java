//$Id: PreInsertEvent.java,v 1.4 2005/02/22 03:09:32 oneovthafew Exp $
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Gavin King
 */
public class PreInsertEvent extends AbstractEvent {
	private Object entity;
	private EntityPersister persister;
	private Object[] state;
	private Serializable id;

	public PreInsertEvent(
			Object entity, 
			Serializable id,
			Object[] state,
			EntityPersister persister,
			SessionImplementor source) {
		super(source);
		this.entity = entity;
		this.id = id;
		this.state = state;
		this.persister = persister;
	}

	public Object getEntity() {
		return entity;
	}
	public Serializable getId() {
		return id;
	}
	public EntityPersister getPersister() {
		return persister;
	}
	public Object[] getState() {
		return state;
	}
}
