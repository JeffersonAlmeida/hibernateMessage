//$Id: PojoComponentTuplizer.java,v 1.4 2005/02/21 02:46:41 oneovthafew Exp $
package org.hibernate.tuple;

import java.lang.reflect.Method;

import net.sf.cglib.beans.BulkBean;
import net.sf.cglib.reflect.FastClass;

import org.hibernate.HibernateException;
import org.hibernate.PropertyAccessException;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Setter;
import org.hibernate.util.ReflectHelper;

/**
 * @author Gavin King
 */
public class PojoComponentTuplizer extends AbstractComponentTuplizer {
	
	private final Class componentClass;
	private transient BulkBean optimizer;
	private transient FastClass fastClass;
	private final Getter parentGetter;
	private final Setter parentSetter;
	
	public Class getMappedClass() {
		return componentClass;
	}
	
	public Object[] getPropertyValues(Object component) throws HibernateException {
		if ( optimizer != null ) {
			try {
				return optimizer.getPropertyValues( component );
			}
			catch ( Throwable t ) {
				throw new PropertyAccessException( t,
						ReflectHelper.PROPERTY_GET_EXCEPTION,
						false,
						componentClass,
						ReflectHelper.getPropertyName( t, optimizer ) );
			}
		}
		else {
			return super.getPropertyValues(component);
		}
	}

	public void setPropertyValues(Object component, Object[] values)
			throws HibernateException {

		if ( optimizer != null ) {
			try {
				optimizer.setPropertyValues( component, values );
				return;
			}
			catch ( Throwable t ) {
				throw new PropertyAccessException( t,
						ReflectHelper.PROPERTY_SET_EXCEPTION,
						true,
						componentClass,
						ReflectHelper.getPropertyName( t, optimizer ) );
			}
		}
		else {
			super.setPropertyValues(component, values);
		}

	}
	
	public Object getParent(Object component) {
		return parentGetter.get( component );
	}
	
	public boolean hasParentProperty() {
		return parentGetter!=null;
	}
	
	public boolean isMethodOf(Method method) {
		for ( int i=0; i<propertySpan; i++ ) {
			final Method getterMethod = getters[i].getMethod();
			if ( getterMethod!=null && getterMethod.equals(method) ) return true;
		}
		return false;
	}
	
	public void setParent(Object component, Object parent, SessionFactoryImplementor factory) {
		parentSetter.set(component, parent, factory);
	}
	
	protected Instantiator buildInstantiator(Component component) {
		return new PojoInstantiator( 
				component.getComponentClass(),
				null,
				fastClass, 
				false 
			);
	}
	protected Getter buildGetter(Component component, Property prop) {
		return prop.getGetter( component.getComponentClass() );
	}
	protected Setter buildSetter(Component component, Property prop) {
		return prop.getSetter( component.getComponentClass() );
	}

	public PojoComponentTuplizer(Component component) {
		super(component);
		
		this.componentClass = component.getComponentClass();

		String[] getterNames = new String[propertySpan];
		String[] setterNames = new String[propertySpan];
		Class[] propTypes = new Class[propertySpan];
		for ( int i = 0; i < propertySpan; i++ ) {
			getterNames[i] = getters[i].getMethodName();
			setterNames[i] = setters[i].getMethodName();
			propTypes[i] = getters[i].getReturnType();
		}
		
		final String parentPropertyName = component.getParentProperty();
		if ( parentPropertyName == null ) {
			parentSetter = null;
			parentGetter = null;
		}
		else {
			PropertyAccessor pa = PropertyAccessorFactory.getPropertyAccessor( null );
			parentSetter = pa.getSetter( componentClass, parentPropertyName );
			parentGetter = pa.getGetter( componentClass, parentPropertyName );
		}

		if ( hasCustomAccessors || !Environment.useReflectionOptimizer() ) {
			fastClass = null;
			optimizer = null;
		}
		else {
			fastClass = ReflectHelper.getFastClass( componentClass );
			optimizer = ReflectHelper.getBulkBean( componentClass, getterNames, setterNames, propTypes, fastClass );
			if (optimizer==null) fastClass = null;
		}
		
	}

}
