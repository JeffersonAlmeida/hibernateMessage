//$Id: PreLoadEvent.java,v 1.5 2005/02/22 03:09:32 oneovthafew Exp $
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Called before injecting property values into a newly 
 * loaded entity instance.
 *
 * @author Gavin King
 */
public class PreLoadEvent extends AbstractEvent {
	private Object entity;
	private Object[] state;
	private Serializable id;
	private EntityPersister persister;

	public PreLoadEvent(
		/*final Object entity, 
		final Object[] state, 
		final Serializable id, 
		final EntityPersister persister, */
		final SessionImplementor source
	) {
		super( source );
		/*this.entity = entity;
		this.state = state;
		this.id = id;
		this.persister = persister;*/
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

	public PreLoadEvent setEntity(Object entity) {
		this.entity = entity;
		return this;
	}
	

	public PreLoadEvent setId(Serializable id) {
		this.id = id;
		return this;
	}
	

	public PreLoadEvent setPersister(EntityPersister persister) {
		this.persister = persister;
		return this;
	}
	

	public PreLoadEvent setState(Object[] state) {
		this.state = state;
		return this;
	}
	
}
