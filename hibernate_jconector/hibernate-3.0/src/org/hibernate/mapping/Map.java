//$Id: Map.java,v 1.9 2005/02/20 03:34:49 oneovthafew Exp $
package org.hibernate.mapping;

import org.hibernate.MappingException;
import org.hibernate.type.CollectionType;
import org.hibernate.type.TypeFactory;

/**
 * A map has a primary key consisting of
 * the key columns + index columns.
 */
public class Map extends IndexedCollection {

	public Map(PersistentClass owner) {
		super(owner);
	}
	
	public boolean isMap() {
		return true;
	}

	public CollectionType getDefaultCollectionType() {
		return isSorted() ?
			TypeFactory.sortedMap( getRole(), getReferencedPropertyName(), isEmbedded(), getComparator() ) :
			TypeFactory.map( getRole(), getReferencedPropertyName(), isEmbedded() );
	}


	public void createAllKeys() throws MappingException {
		super.createAllKeys();
		if ( !isInverse() ) getIndex().createForeignKey();
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
}
