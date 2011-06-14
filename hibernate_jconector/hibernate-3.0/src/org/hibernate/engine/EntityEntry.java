//$Id: EntityEntry.java,v 1.7 2005/02/16 12:50:12 oneovthafew Exp $
package org.hibernate.engine;

import java.io.Serializable;

import org.hibernate.LockMode;
import org.hibernate.EntityMode;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.pretty.MessageHelper;

/**
 * We need an entry to tell us all about the current state
 * of an object with respect to its persistent state
 * 
 * @author Gavin King
 */
public final class EntityEntry implements Serializable {

	private LockMode lockMode;
	private Status status;
	private Serializable id;
	private Object[] loadedState;
	private Object[] deletedState;
	private boolean existsInDatabase;
	private Object version;
	private transient EntityPersister persister; // for convenience to save some lookups
	private EntityMode entityMode;
	private String entityName;
	private boolean isBeingReplicated;
	private transient Object rowId;

	EntityEntry(
			final Status status,
			final Object[] loadedState,
			final Object rowId,
			final Serializable id,
			final Object version,
			final LockMode lockMode,
			final boolean existsInDatabase,
			final EntityPersister persister,
			final EntityMode entityMode,
			final boolean disableVersionIncrement) {
		this.status=status;
		this.loadedState=loadedState;
		this.id=id;
		this.rowId=rowId;
		this.existsInDatabase=existsInDatabase;
		this.version=version;
		this.lockMode=lockMode;
		this.isBeingReplicated=disableVersionIncrement;
		this.persister=persister;
		this.entityMode = entityMode;
		if ( persister != null )
			this.entityName = persister.getEntityName();
	}

	public LockMode getLockMode() {
		return lockMode;
	}

	public void setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
	}

	public Status getStatus() {
		return status;
	}

	void setStatus(Status status) {
		this.status = status;
	}

	public Serializable getId() {
		return id;
	}

	public Object[] getLoadedState() {
		return loadedState;
	}

	public Object[] getDeletedState() {
		return deletedState;
	}

	public void setDeletedState(Object[] deletedState) {
		this.deletedState = deletedState;
	}

	public boolean isExistsInDatabase() {
		return existsInDatabase;
	}

	public void setExistsInDatabase(boolean existsInDatabase) {
		this.existsInDatabase = existsInDatabase;
	}

	public Object getVersion() {
		return version;
	}

	public EntityPersister getPersister() {
		return persister;
	}

	void setPersister(EntityPersister persister) {
		this.persister = persister;
	}

	public String getEntityName() {
		return entityName;
	}

	public boolean isBeingReplicated() {
		return isBeingReplicated;
	}
	
	public Object getRowId() {
		return rowId;
	}
	
	public void postUpdate(Object obj, Object[] updatedState, Object nextVersion) {
		this.loadedState = updatedState;
		setLockMode(LockMode.WRITE);
		if ( getPersister().isVersioned() ) {
			this.version = nextVersion;
			getPersister().setPropertyValue( obj, getPersister().getVersionProperty(), nextVersion, entityMode );
		}
	}
	
	public boolean isNullifiable(boolean earlyInsert, SessionImplementor session) {
		return getStatus() == Status.SAVING || (
				earlyInsert ?
						!isExistsInDatabase() :
						session.getPersistenceContext().getNullifiableEntityKeys()
							.contains( new EntityKey( getId(), getPersister(), entityMode ) )
				);
	}
	
	public Object getLoadedValue(String propertyName) {
		return loadedState[
			( (UniqueKeyLoadable) persister ).getPropertyIndex(propertyName)
		];
	}

	public String toString() {
		return "EntityEntry" + MessageHelper.infoString(entityName, id) + '(' + status + ')';
	}

}
