// $Id: StandardProperty.java,v 1.2 2005/02/18 20:04:16 steveebersole Exp $
package org.hibernate.tuple;

import org.hibernate.engine.Cascades;
import org.hibernate.type.Type;

/**
 * Represents a basic property within the Hibernate runtime-metamodel.
 *
 * @author Steve Ebersole
 */
public class StandardProperty extends Property {

	private boolean lazy;
	private boolean insertable;
	private boolean updateable;
	private boolean nullable;
	private boolean checkable;
	private boolean versionable;
	private Cascades.CascadeStyle cascadeStyle;

	/**
	 * Constructs StandardProperty instances.
	 *
	 * @param name The name by which the property can be referenced within
	 * its owner.
	 * @param node The node name to use for XML-based representation of this
	 * property.
	 * @param type The Hibernate Type of this property.
	 * @param lazy Should this property be handled lazily?
	 * @param insertable Is this property an insertable value?
	 * @param updateable Is this property an updateable value?
	 * @param nullable Is this property a nullable value?
	 * @param checkable Is this property a checkable value?
	 * @param versionable Is this property a versionable value?
	 * @param cascadeStyle The cascade style for this property's value.
	 */
	public StandardProperty(
	        String name,
	        String node,
	        Type type,
	        boolean lazy,
	        boolean insertable,
	        boolean updateable,
	        boolean nullable,
	        boolean checkable,
	        boolean versionable,
	        Cascades.CascadeStyle cascadeStyle) {
		super(name, node, type);
		this.lazy = lazy;
		this.insertable = insertable;
		this.updateable = updateable;
		this.nullable = nullable;
		this.checkable = checkable;
		this.versionable = versionable;
		this.cascadeStyle = cascadeStyle;
	}

	public boolean isLazy() {
		return lazy;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public boolean isInsertable() {
		return insertable;
	}

	public void setInsertable(boolean insertable) {
		this.insertable = insertable;
	}

	public boolean isUpdateable() {
		return updateable;
	}

	public void setUpdateable(boolean updateable) {
		this.updateable = updateable;
	}

	public boolean isNullable() {
		return nullable;
	}

	public void setNullable(boolean nullable) {
		this.nullable = nullable;
	}

	public boolean isCheckable() {
		return checkable;
	}

	public void setCheckable(boolean checkable) {
		this.checkable = checkable;
	}

	public boolean isVersionable() {
		return versionable;
	}

	public void setVersionable(boolean versionable) {
		this.versionable = versionable;
	}

	public Cascades.CascadeStyle getCascadeStyle() {
		return cascadeStyle;
	}

	public void setCascadeStyle(Cascades.CascadeStyle cascadeStyle) {
		this.cascadeStyle = cascadeStyle;
	}
}
