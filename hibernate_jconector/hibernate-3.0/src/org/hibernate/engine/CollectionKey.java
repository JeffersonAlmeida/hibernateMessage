//$Id: CollectionKey.java,v 1.6 2005/02/16 12:50:12 oneovthafew Exp $
package org.hibernate.engine;

import org.hibernate.EntityMode;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;



import java.io.Serializable;

/**
 * Uniquely identifies a collection instance in a particular session.
 *
 * @author Gavin King
 */
public final class CollectionKey implements Serializable {

	private final String role;
	private final Serializable key;
	private final Type keyType;
	private final SessionFactoryImplementor factory;
	private final int hashCode;
	private EntityMode entityMode;

	public CollectionKey(CollectionPersister persister, Serializable key, EntityMode em) {
		this.entityMode = em;
		this.role = persister.getRole();
		this.key = key;
		this.keyType = persister.getKeyType();
		this.factory = persister.getFactory();
		this.hashCode = getHashCode(); //cache the hashcode
	}

	public boolean equals(Object other) {
		CollectionKey that = (CollectionKey) other;
		return that.role.equals(role) && 
			keyType.isEqual(that.key, key, entityMode, factory);
	}

	public int getHashCode() {
		int result = 17;
		result = 37 * result + role.hashCode();
		result = 37 * result + keyType.getHashCode(key, entityMode, factory);
		return result;
	}

	public int hashCode() {
		return hashCode;
	}

	public String getRole() {
		return role;
	}

	public Serializable getKey() {
		return key;
	}

	public String toString() {
		return "CollectionKey" + 
			MessageHelper.collectionInfoString( factory.getCollectionPersister(role), key, factory );
	}
}