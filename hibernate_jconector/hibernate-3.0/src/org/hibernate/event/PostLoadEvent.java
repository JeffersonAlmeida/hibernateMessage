//$Id: PostLoadEvent.java,v 1.9 2005/02/22 03:09:31 oneovthafew Exp $
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Occurs after an an entity instance is fully loaded.
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>, Gavin King
 */
public class PostLoadEvent extends AbstractEvent {
	private Object entity;
	private Serializable id;
	private EntityPersister persister;

	public PostLoadEvent(
			/*Object entity, Serializable id, EntityPersister persister,*/ 
			SessionImplementor source) {
		super( source );
		/*this.entity = entity;
		this.id = id;
		this.persister = persister;*/
	}

	public Object getEntity() {
		return entity;
	}
	
	public EntityPersister getPersister() {
		return persister;
	}
	
	public Serializable getId() {
		return id;
	}

	public PostLoadEvent setEntity(Object entity) {
		this.entity = entity;
		return this;
	}
	

	public PostLoadEvent setId(Serializable id) {
		this.id = id;
		return this;
	}
	

	public PostLoadEvent setPersister(EntityPersister persister) {
		this.persister = persister;
		return this;
	}
	
}
