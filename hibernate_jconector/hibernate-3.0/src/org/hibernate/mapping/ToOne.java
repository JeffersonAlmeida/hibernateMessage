//$Id: ToOne.java,v 1.8 2005/02/20 23:18:15 oneovthafew Exp $
package org.hibernate.mapping;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.type.Type;
import org.hibernate.util.ReflectHelper;

/**
 * A simple-point association (ie. a reference to another entity).
 * @author Gavin King
 */
public abstract class ToOne extends SimpleValue implements Fetchable {

	private FetchMode fetchMode;
	protected String referencedPropertyName;
	private String referencedEntityName;
	private boolean embedded;

	protected ToOne(Table table) {
		super(table);
	}

	public FetchMode getFetchMode() {
		return fetchMode;
	}

	public void setFetchMode(FetchMode fetchMode) {
		this.fetchMode=fetchMode;
	}

	public abstract void createForeignKey() throws MappingException;
	public abstract Type getType() throws MappingException;

	public String getReferencedPropertyName() {
		return referencedPropertyName;
	}

	public void setReferencedPropertyName(String name) {
		referencedPropertyName = name==null ? null : name.intern();
	}

	public String getReferencedEntityName() {
		return referencedEntityName;
	}

	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName==null ? 
				null : referencedEntityName.intern();
	}

	public void setTypeUsingReflection(String className, String propertyName)
	throws MappingException {
		if (referencedEntityName==null) {
			referencedEntityName = ReflectHelper.reflectedPropertyClass(className, propertyName).getName();
		}
	}

	public boolean isTypeSpecified() {
		return referencedEntityName!=null;
	}
	
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
	
	public boolean isEmbedded() {
		return embedded;
	}
	
	public void setEmbedded(boolean embedded) {
		this.embedded = embedded;
	}
}







