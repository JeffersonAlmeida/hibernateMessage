//$Id: PreUpdateEvent.java,v 1.4 2005/02/22 03:09:32 oneovthafew Exp $
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Gavin King
 */
public class PreUpdateEvent extends AbstractEvent {
	private Object entity;
	private EntityPersister persister;
	private Object[] state;
	private Object[] oldState;
	private Serializable id;

	public PreUpdateEvent(
			Object entity,
			Serializable id,
			Object[] state,
			Object[] oldState,
			EntityPersister persister,
			SessionImplementor source
	) {
		super(source);
		this.entity = entity;
		this.id = id;
		this.state = state;
		this.oldState = oldState;
		this.persister = persister;
	}

	public Object getEntity() {
		return entity;
	}
	public Serializable getId() {
		return id;
	}
	public Object[] getOldState() {
		return oldState;
	}
	public EntityPersister getPersister() {
		return persister;
	}
	public Object[] getState() {
		return state;
	}
}
