//$Id: CacheKey.java,v 1.4 2005/02/16 12:50:10 oneovthafew Exp $
package org.hibernate.cache;

import java.io.Serializable;

import org.hibernate.EntityMode;
import org.hibernate.type.Type;

/**
 * Allows multiple entity classes / collection roles to be 
 * stored in the same cache region. Also allows for composite 
 * keys which do not properly implement equals()/hashCode().
 * 
 * @author Gavin King
 */
public class CacheKey implements Serializable {
	private final Serializable key;
	private final Type type;
	private final String entityOrRoleName;
	private EntityMode entityMode;
	
	/**
	 * Construct a new key for a collection or entity instance.
	 * Note that an entity name should always be the root entity 
	 * name, not a subclass entity name.
	 */
	public CacheKey(Serializable id, Type type, String entityOrRoleName, EntityMode entityMode) {
		this.key = id;
		this.type = type;
		this.entityOrRoleName = entityOrRoleName;
		this.entityMode = entityMode;
	}

	//Mainly for OSCache
	public String toString() {
		return entityOrRoleName + '#' + key.toString();//"CacheKey#" + type.toString(key, sf);
	}

	public boolean equals(Object other) {
		if ( !(other instanceof CacheKey) ) return false;
		CacheKey that = (CacheKey) other;
		return type.isEqual(key, that.key, entityMode) && 
			entityOrRoleName.equals(that.entityOrRoleName);
	}

	public int hashCode() {
		return type.getHashCode(key, entityMode);
	}
	
	public Serializable getKey() {
		return key;
	}
	
	public String getEntityOrRoleName() {
		return entityOrRoleName;
	}

}
