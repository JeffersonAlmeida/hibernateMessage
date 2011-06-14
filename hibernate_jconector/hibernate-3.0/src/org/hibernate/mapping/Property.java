//$Id: Property.java,v 1.19 2005/02/21 04:29:25 oneovthafew Exp $
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.hibernate.MappingException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.EntityMode;
import org.hibernate.engine.Cascades;
import org.hibernate.engine.Mapping;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Setter;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;

/**
 * Represents a property as part of an entity or a component.
 *
 * @author Gavin King
 */
public class Property implements Serializable {

	private String name;
	private Value value;
	private String cascade;
	private boolean updateable = true;
	private boolean insertable = true;
	private boolean selectable = true;
	private boolean optimisticLocked = true;
	private String propertyAccessorName;
	private boolean lazy;
	private boolean optional;
	private String nodeName;
	private java.util.Map metaAttributes;
	private PersistentClass persistentClass;
	
	public boolean isBackRef() {
		return false;
	}

	public Type getType() throws MappingException {
		return value.getType();
	}
	
	public int getColumnSpan() {
		return value.getColumnSpan();
	}
	
	public Iterator getColumnIterator() {
		return value.getColumnIterator();
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isComposite() {
		return value instanceof Component;
	}

	public Value getValue() {
		return value;
	}
	
	public boolean isPrimitive(Class clazz) {
		return getGetter(clazz).getReturnType().isPrimitive();
	}

	public Cascades.CascadeStyle getCascadeStyle() throws MappingException {
		Type type = value.getType();
		if ( type.isComponentType() && !type.isAnyType() ) {
			AbstractComponentType actype = (AbstractComponentType) type;
			int length = actype.getSubtypes().length;
			for ( int i=0; i<length; i++ ) {
				if ( actype.getCascadeStyle(i)!=Cascades.STYLE_NONE ) return Cascades.STYLE_ALL;
			}
			return Cascades.STYLE_NONE;
		}
		else if ( cascade==null || cascade.equals("none") ) {
			return Cascades.STYLE_NONE;
		}
		else {
			StringTokenizer tokens = new StringTokenizer(cascade, ", ");
			Cascades.CascadeStyle[] styles = new Cascades.CascadeStyle[ tokens.countTokens() ] ;
			int i=0;
			while ( tokens.hasMoreTokens() ) {
				styles[i++] = Cascades.getCascadeStyle( tokens.nextToken() );
			}
			return new Cascades.MultipleCascadeStyle(styles);
		}
	}

	public String getCascade() {
		return cascade;
	}

	public void setCascade(String cascade) {
		this.cascade = cascade;
	}

	public void setName(String name) {
		this.name = name==null ? null : name.intern();
	}

	public void setValue(Value value) {
		this.value = value;
	}

	public boolean isUpdateable() {
		// if the property mapping consists of all formulas, 
		// make it non-updateable
		final boolean[] columnUpdateability = value.getColumnUpdateability();
		return updateable && ( 
				columnUpdateability.length==0 ||
				!ArrayHelper.isAllFalse(columnUpdateability)
		);
	}

	public boolean isInsertable() {
		// if the property mapping consists of all formulas, 
		// make it insertable
		final boolean[] columnInsertability = value.getColumnInsertability();
		return insertable && (
				columnInsertability.length==0 ||
				!ArrayHelper.isAllFalse(columnInsertability)
		);
	}

	public void setUpdateable(boolean mutable) {
		this.updateable = mutable;
	}

	public void setInsertable(boolean insertable) {
		this.insertable = insertable;
	}

	public String getPropertyAccessorName() {
		return propertyAccessorName;
	}

	public void setPropertyAccessorName(String string) {
		propertyAccessorName = string;
	}

	/**
	 * Approximate!
	 */
	boolean isNullable() {
		return value==null || value.isNullable();
	}

	public boolean isBasicPropertyAccessor() {
		return propertyAccessorName==null || "property".equals(propertyAccessorName);
	}

	public java.util.Map getMetaAttributes() {
		return metaAttributes;
	}

	public MetaAttribute getMetaAttribute(String attributeName) {
		return (MetaAttribute) metaAttributes.get(attributeName);
	}

	public void setMetaAttributes(java.util.Map metas) {
		this.metaAttributes = metas;
	}

	public boolean isValid(Mapping mapping) throws MappingException {
		return getValue().isValid(mapping);
	}

	public String toString() {
		return getClass().getName() + '(' + name + ')';
	}
	
	public void setLazy(boolean lazy) {
		this.lazy=lazy;
	}
	
	public boolean isLazy() {
		return lazy;
	}
	
	public boolean isOptimisticLocked() {
		return optimisticLocked;
	}

	public void setOptimisticLocked(boolean optimisticLocked) {
		this.optimisticLocked = optimisticLocked;
	}
	
	public boolean isOptional() {
		return optional || isNullable();
	}
	
	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	public PersistentClass getPersistentClass() {
		return persistentClass;
	}

	public void setPersistentClass(PersistentClass persistentClass) {
		this.persistentClass = persistentClass;
	}

	public boolean isSelectable() {
		return selectable;
	}
	
	public void setSelectable(boolean selectable) {
		this.selectable = selectable;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public String getAccessorPropertyName( EntityMode mode ) {
		if ( mode == EntityMode.DOM4J ) {
			return nodeName;
		}
		else {
			return getName();
		}
	}

	// todo : remove
	public Getter getGetter(Class clazz) throws PropertyNotFoundException, MappingException {
		return getPropertyAccessor(clazz).getGetter(clazz, name);
	}

	// todo : remove
	public Setter getSetter(Class clazz) throws PropertyNotFoundException, MappingException {
		return getPropertyAccessor(clazz).getSetter(clazz, name);
	}

	// todo : remove
	public PropertyAccessor getPropertyAccessor(Class clazz) throws MappingException {
		return PropertyAccessorFactory.getPropertyAccessor( clazz, getPropertyAccessorName() );
	}
}
