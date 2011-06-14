//$Id: LockEvent.java,v 1.5 2005/02/22 03:09:31 oneovthafew Exp $
package org.hibernate.event;

import org.hibernate.LockMode;
import org.hibernate.engine.SessionImplementor;

/**
 *  Defines an event class for the locking of an entity.
 *
 * @author Steve Ebersole
 */
public class LockEvent extends AbstractEvent {

	private Object object;
	private LockMode lockMode;
	private String entityName;

	public LockEvent(String entityName, Object original, LockMode lockMode, SessionImplementor source) {
		this(original, lockMode, source);
		this.entityName = entityName;
	}

	public LockEvent(Object object, LockMode lockMode, SessionImplementor source) {
		super(source);
		this.object = object;
		this.lockMode = lockMode;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

	public LockMode getLockMode() {
		return lockMode;
	}

	public void setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

}
