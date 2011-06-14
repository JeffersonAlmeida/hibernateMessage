// $Id: VersionProperty.java,v 1.2 2005/02/18 20:04:16 steveebersole Exp $
package org.hibernate.tuple;

import org.hibernate.engine.Cascades;
import org.hibernate.type.Type;

/**
 * Represents a version property within the Hibernate runtime-metamodel.
 *
 * @author Steve Ebersole
 */
public class VersionProperty extends StandardProperty {

	private Cascades.VersionValue unsavedValue;

	/**
	 * Constructs VersionProperty instances.
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
	 * @param unsavedValue The value which, if found as the value of
	 * this (i.e., the version) property, represents new (i.e., un-saved)
	 * instances of the owning entity.
	 */
	public VersionProperty(
	        String name,
	        String node,
	        Type type,
	        boolean lazy,
	        boolean insertable,
	        boolean updateable,
	        boolean nullable,
	        boolean checkable,
	        boolean versionable,
	        Cascades.CascadeStyle cascadeStyle,
	        Cascades.VersionValue unsavedValue) {
		super(name, node, type, lazy, insertable, updateable, nullable, checkable, versionable, cascadeStyle);
		this.unsavedValue = unsavedValue;
	}

	public Cascades.VersionValue getUnsavedValue() {
		return unsavedValue;
	}
}
