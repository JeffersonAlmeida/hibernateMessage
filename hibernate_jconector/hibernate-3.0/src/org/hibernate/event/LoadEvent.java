//$Id: LoadEvent.java,v 1.6 2005/02/22 03:09:31 oneovthafew Exp $
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.LockMode;
import org.hibernate.engine.SessionImplementor;

/**
 *  Defines an event class for the loading of an entity.
 *
 * @author Steve Ebersole
 */
public class LoadEvent extends AbstractEvent {

	public static final LockMode DEFAULT_LOCK_MODE = LockMode.NONE;

	private Serializable entityId;
	private String entityClassName;
	private Object instanceToLoad;
	private LockMode lockMode;
	private boolean isAssociationFetch;

	public LoadEvent(Serializable entityId, Object instanceToLoad, SessionImplementor source) {
		this(entityId, null, instanceToLoad, null, false, source);
	}

	public LoadEvent(Serializable entityId, String entityClassName, LockMode lockMode, SessionImplementor source) {
		this(entityId, entityClassName, null, lockMode, false, source);
	}
	
	public LoadEvent(Serializable entityId, String entityClassName, boolean isAssociationFetch, SessionImplementor source) {
		this(entityId, entityClassName, null, null, isAssociationFetch, source);
	}
	
	public boolean isAssociationFetch() {
		return isAssociationFetch;
	}

	private LoadEvent(
			Serializable entityId,
			String entityClassName,
			Object instanceToLoad,
			LockMode lockMode,
			boolean isAssociationFetch,
			SessionImplementor source) {

		super(source);

		if ( entityId == null ) {
			throw new IllegalArgumentException("id to load is required for loading");
		}

		if ( lockMode == LockMode.WRITE ) {
			throw new IllegalArgumentException("Invalid lock mode for loading");
		}
		else if ( lockMode == null ) {
			lockMode = DEFAULT_LOCK_MODE;
		}

		this.entityId = entityId;
		this.entityClassName = entityClassName;
		this.instanceToLoad = instanceToLoad;
		this.lockMode = lockMode;
		this.isAssociationFetch = isAssociationFetch;
	}

	public Serializable getEntityId() {
		return entityId;
	}

	public void setEntityId(Serializable entityId) {
		this.entityId = entityId;
	}

	public String getEntityClassName() {
		return entityClassName;
	}

	public void setEntityClassName(String entityClassName) {
		this.entityClassName = entityClassName;
	}

	public Object getInstanceToLoad() {
		return instanceToLoad;
	}

	public void setInstanceToLoad(Object instanceToLoad) {
		this.instanceToLoad = instanceToLoad;
	}

	public LockMode getLockMode() {
		return lockMode;
	}

	public void setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
	}
}