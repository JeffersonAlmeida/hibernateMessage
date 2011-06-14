//$Id: ManyToOne.java,v 1.9 2005/02/19 12:58:23 oneovthafew Exp $
package org.hibernate.mapping;

import org.hibernate.MappingException;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;

/**
 * A many-to-one association mapping
 * @author Gavin King
 */
public class ManyToOne extends ToOne {

	public ManyToOne(Table table) {
		super(table);
	}

	public Type getType() throws MappingException {
		return TypeFactory.manyToOne( getReferencedEntityName(), referencedPropertyName, isEmbedded() );
	}

	public void createForeignKey() throws MappingException {
		// TODO: handle the case of a foreign key to something other than the pk
		if (referencedPropertyName==null && !hasFormula() ) {
			createForeignKeyOfEntity( ( (EntityType) getType() ).getAssociatedEntityName() );
		}
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
}
