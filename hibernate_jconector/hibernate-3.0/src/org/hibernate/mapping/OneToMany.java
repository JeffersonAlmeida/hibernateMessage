//$Id: OneToMany.java,v 1.12 2005/02/20 23:18:14 oneovthafew Exp $
package org.hibernate.mapping;

import java.util.Iterator;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;

/**
 * A mapping for a one-to-many association
 * @author Gavin King
 */
public class OneToMany implements Value {

	private String referencedEntityName;
	private Table referencingTable;
	private PersistentClass associatedClass;
	private boolean embedded;

	private EntityType getEntityType() {
		return (EntityType) TypeFactory.manyToOne(referencedEntityName, null, embedded);
	}

	public OneToMany(PersistentClass owner) throws MappingException {
		this.referencingTable = (owner==null) ? null : owner.getTable();
	}

	public PersistentClass getAssociatedClass() {
		return associatedClass;
	}

    /**
     * Associated entity on the many side
     */
	public void setAssociatedClass(PersistentClass associatedClass) {
		this.associatedClass = associatedClass;
	}

	public void createForeignKey() {
		// no foreign key element of for a one-to-many
	}

	public Iterator getColumnIterator() {
		return associatedClass.getKey().getColumnIterator();
	}

	public int getColumnSpan() {
		return associatedClass.getKey().getColumnSpan();
	}

	public FetchMode getFetchMode() {
		return FetchMode.JOIN;
	}

    /** 
     * Table of the owner entity (the "one" side)
     */
	public Table getTable() {
		return referencingTable;
	}

	public Type getType() {
		return getEntityType();
	}

	public boolean isNullable() {
		return false;
	}

	public boolean isSimpleValue() {
		return false;
	}

	public boolean isAlternateUniqueKey() {
		return false;
	}

	public boolean hasFormula() {
		return false;
	}
	
	public boolean isValid(Mapping mapping) throws MappingException {
		return true;
	}

    public String getReferencedEntityName() {
		return referencedEntityName;
	}

    /** 
     * Associated entity on the "many" side
     */    
	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName==null ? null : referencedEntityName.intern();
	}

	public void setTypeUsingReflection(String className, String propertyName) {}
	
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
	
	
	public boolean[] getColumnInsertability() {
		//TODO: we could just return all false...
		throw new UnsupportedOperationException();
	}
	
	public boolean[] getColumnUpdateability() {
		//TODO: we could just return all false...
		throw new UnsupportedOperationException();
	}
	
	public boolean isEmbedded() {
		return embedded;
	}
	
	public void setEmbedded(boolean embedded) {
		this.embedded = embedded;
	}
}
