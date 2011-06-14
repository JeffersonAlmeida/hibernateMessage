//$Id: SaveOrUpdateEvent.java,v 1.8 2005/02/22 03:09:32 oneovthafew Exp $
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.SessionImplementor;

/** 
 * An event class for saveOrUpdate()
 *
 * @author Steve Ebersole
 */
public class SaveOrUpdateEvent extends AbstractEvent {

	private Object object;
	private Serializable requestedId;
	private String entityName;
	private Object entity;
	private EntityEntry entry;

	public SaveOrUpdateEvent(String entityName, Object original, SessionImplementor source) {
		this(original, source);
		this.entityName = entityName;
	}

	public SaveOrUpdateEvent(String entityName, Object original, Serializable id, SessionImplementor source) {
		this(entityName, original, source);
		this.requestedId = id;
		if ( requestedId == null ) {
			throw new IllegalArgumentException(
					"attempt to create saveOrUpdate event with null identifier"
			);
		}
	}

	public SaveOrUpdateEvent(Object object, SessionImplementor source) {
		super(source);
		if ( object == null ) {
			throw new IllegalArgumentException(
					"attempt to create saveOrUpdate event with null entity"
			);
		}
		this.object = object;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

	public Serializable getRequestedId() {
		return requestedId;
	}

	public void setRequestedId(Serializable requestedId) {
		this.requestedId = requestedId;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	public Object getEntity() {
		return entity;
	}
	
	public void setEntity(Object entity) {
		this.entity = entity;
	}
	
	public EntityEntry getEntry() {
		return entry;
	}
	
	public void setEntry(EntityEntry entry) {
		this.entry = entry;
	}
}
