// $Id: SQLQueryCollectionReturn.java,v 1.2 2005/03/19 00:29:13 maxcsaucdk Exp $
package org.hibernate.loader.custom;

import java.util.Map;

import org.hibernate.LockMode;

/**
 * Represents a return defined as part of a native sql query which
 * names a collection role in the form {classname}.{collectionrole}; it
 * is used in defining a custom sql query for loading an entity's
 * collection in non-fetching scenarios (i.e., loading the collection
 * itself as the "root" of the result).
 *
 * @author Steve
 */
public class SQLQueryCollectionReturn extends SQLQueryReturn {
	private String ownerEntityName;
	private String ownerProperty;

	public SQLQueryCollectionReturn(String alias, String ownerClass, String ownerProperty, Map propertyResults, LockMode lockMode) {
		super(alias, propertyResults, lockMode);
		this.ownerEntityName = ownerClass;
		this.ownerProperty = ownerProperty;
	}

	/**
	 * Returns the class owning the collection.
	 *
	 * @return The class owning the collection.
	 */
	public String getOwnerEntityName() {
		return ownerEntityName;
	}

	/**
	 * Returns the name of the property representing the collection from the {@link #getOwnerEntityName}.
	 *
	 * @return The name of the property representing the collection on the owner class.
	 */
	public String getOwnerProperty() {
		return ownerProperty;
	}
}
