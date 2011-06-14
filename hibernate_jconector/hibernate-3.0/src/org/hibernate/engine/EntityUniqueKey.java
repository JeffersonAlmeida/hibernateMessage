//$Id: EntityUniqueKey.java,v 1.6 2005/02/16 12:50:13 oneovthafew Exp $
package org.hibernate.engine;

import org.hibernate.EntityMode;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;

/**
 * @author Gavin King
 */
public class EntityUniqueKey {
	private final String uniqueKeyName;
	private final String entityName;
	private final Object key;
	private final Type keyType;
	private EntityMode entityMode;
	
	public EntityUniqueKey(
			String entityName, 
			String uniqueKeyName, 
			Object key,
			Type keyType,
			EntityMode entityMode
	) {
		this.uniqueKeyName = uniqueKeyName;
		this.entityName = entityName;
		this.key = key;
		this.keyType = keyType;
		this.entityMode = entityMode;
	}

	public String getEntityName() {
		return entityName;
	}

	public Object getKey() {
		return key;
	}

	public String getUniqueKeyName() {
		return uniqueKeyName;
	}
	
	public int hashCode() {
		int result = 17;
		result = 37 * result + entityName.hashCode();
		result = 37 * result + uniqueKeyName.hashCode();
		result = 37 * result + keyType.getHashCode(key, entityMode);
		return result;
	}
	
	public boolean equals(Object other) {
		EntityUniqueKey that = (EntityUniqueKey) other;
		return that.entityName.equals(entityName) &&
			that.uniqueKeyName.equals(uniqueKeyName) &&
			keyType.isEqual(that.key, key, entityMode);
	}
	
	public String toString() {
		return "EntityUniqueKey" + MessageHelper.infoString(entityName, uniqueKeyName, key);
	}
}
