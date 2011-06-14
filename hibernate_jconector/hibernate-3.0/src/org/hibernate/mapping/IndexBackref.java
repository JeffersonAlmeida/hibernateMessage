//$Id: IndexBackref.java,v 1.3 2005/02/21 04:29:25 oneovthafew Exp $
package org.hibernate.mapping;

import org.hibernate.property.IndexPropertyAccessor;
import org.hibernate.property.PropertyAccessor;

/**
 * @author Gavin King
 */
public class IndexBackref extends Property {
	private String collectionRole;
	
	public boolean isBackRef() {
		return true;
	}
	public String getCollectionRole() {
		return collectionRole;
	}
	public void setCollectionRole(String collectionRole) {
		this.collectionRole = collectionRole;
	}

	public boolean isBasicPropertyAccessor() {
		return false;
	}

	public PropertyAccessor getPropertyAccessor(Class clazz) {
		return new IndexPropertyAccessor(collectionRole);
	}
}
