//$Id: EntityKey.java,v 1.10 2005/02/21 11:50:29 oneovthafew Exp $
package org.hibernate.engine;

import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;

/**
 * Uniquely identifies of an entity instance in a particular session.
 *
 * Consisting of the user-visible identifier and the identifier space (eg. tablename).
 *
 * @author Gavin King
 */
public final class EntityKey implements Serializable {
	private final Serializable identifier;
	private final Serializable rootEntityName;
	private final String entityName;
	private final Type identifierType;
	private final boolean isBatchLoadable;
	private final SessionFactoryImplementor factory;
	private final int hashCode;
	private final EntityMode entityMode;

	/**
	 * Construct a unique identifier for an entity class instance
	 */
	public EntityKey(Serializable id, EntityPersister persister, EntityMode entityMode) {
		if (id==null) throw new AssertionFailure("null identifier");
		this.identifier = id; 
		this.entityMode = entityMode;
		this.rootEntityName = persister.getRootEntityName();
		this.entityName = persister.getEntityName();
		this.identifierType = persister.getIdentifierType();
		this.isBatchLoadable = persister.isBatchLoadable();
		this.factory = persister.getFactory();
		hashCode = getHashCode(); //cache the hashcode
	}
	
	public boolean isBatchLoadable() {
		return isBatchLoadable;
	}

	/**
	 * Get the user-visible identifier
	 */
	public Serializable getIdentifier() {
		return identifier;
	}

	public String getEntityName() {
		return entityName;
	}

	public boolean equals(Object other) {
		EntityKey otherKey = (EntityKey) other;
		return otherKey.rootEntityName.equals(this.rootEntityName) && 
			identifierType.isEqual(otherKey.identifier, this.identifier, entityMode, factory);
	}
	
	private int getHashCode() {
		int result = 17;
		result = 37 * result + rootEntityName.hashCode();
		result = 37 * result + identifierType.getHashCode(identifier, entityMode, factory);
		return result;
	}

	public int hashCode() {
		return hashCode;
	}

	public String toString() {
		return "EntityKey" + 
			MessageHelper.infoString( factory.getEntityPersister(entityName), identifier, factory );
	}

}
