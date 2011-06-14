//$Id: IdentifierBag.java,v 1.8 2005/02/20 03:34:49 oneovthafew Exp $
package org.hibernate.mapping;

import org.hibernate.type.CollectionType;
import org.hibernate.type.TypeFactory;

/**
 * An <tt>IdentifierBag</tt> has a primary key consisting of
 * just the identifier column
 */
public class IdentifierBag extends IdentifierCollection {

	public IdentifierBag(PersistentClass owner) {
		super(owner);
	}

	public CollectionType getDefaultCollectionType() {
		return TypeFactory.idbag( getRole(), getReferencedPropertyName(), isEmbedded() );
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}	
}
