// $Id: AbstractTuplizer.java,v 1.12 2005/02/21 02:08:45 oneovthafew Exp $
package org.hibernate.tuple;

import java.io.Serializable;
import java.util.Iterator;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.Assigned;
import org.hibernate.intercept.LazyPropertyInitializer;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.AbstractComponentType;


/**
 * Support base class for EntityTuplizer implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractTuplizer implements EntityTuplizer {

	//TODO: currently keeps Getters and Setters (instead of PropertyAccessors) because 
	//      of the way getGetter() and getSetter() are implemented currently; yuck!

	private final EntityMetamodel entityMetamodel;

	private final Getter idGetter;
	private final Setter idSetter;

	protected final Getter[] getters;
	protected final Setter[] setters;
	protected final int propertySpan;
	protected final boolean hasCustomAccessors;
	private final Instantiator instantiator;
	private final ProxyFactory proxyFactory;

	protected abstract EntityMode getEntityMode();

	protected abstract Getter buildPropertyGetter(Property mappedProperty, PersistentClass mappedEntity);
	protected abstract Setter buildPropertySetter(Property mappedProperty, PersistentClass mappedEntity);
	protected abstract Instantiator buildInstantiator(PersistentClass mappingInfo);
	protected abstract ProxyFactory buildProxyFactory(PersistentClass mappingInfo, Getter idGetter, Setter idSetter);
	
	public AbstractTuplizer(EntityMetamodel entityMetamodel, PersistentClass mappedEntity) {
		this.entityMetamodel = entityMetamodel;

		if ( !entityMetamodel.getIdentifierProperty().isVirtual() ) {
			idGetter = buildPropertyGetter( mappedEntity.getIdentifierProperty(), mappedEntity );
			idSetter = buildPropertySetter( mappedEntity.getIdentifierProperty(), mappedEntity );
		}
		else {
			idGetter = null;
			idSetter = null;
		}

		propertySpan = entityMetamodel.getPropertySpan();

        getters = new Getter[propertySpan];
		setters = new Setter[propertySpan];

		Iterator iter = mappedEntity.getPropertyClosureIterator();
		boolean foundCustomAccessor=false;
		int i=0;
		while ( iter.hasNext() ) {
			//TODO: redesign how PropertyAccessors are acquired...
			Property property = (Property) iter.next();
			getters[i] = buildPropertyGetter(property, mappedEntity);
			setters[i] = buildPropertySetter(property, mappedEntity);
			if ( !property.isBasicPropertyAccessor() ) foundCustomAccessor = true;
			i++;
		}
		hasCustomAccessors = foundCustomAccessor;

        instantiator = buildInstantiator( mappedEntity );

		if ( entityMetamodel.isLazy() ) {
			proxyFactory = buildProxyFactory( mappedEntity, idGetter, idSetter );
		}
		else {
			proxyFactory = null;
		}
	}

	public String getEntityName() {
		return entityMetamodel.getName();
	}

	public Serializable getIdentifier(Object entity) throws HibernateException {
		final Object id;
		if ( entityMetamodel.getIdentifierProperty().isEmbedded() ) {
			id = entity;
		}
		else {
			if ( idGetter == null ) {
				throw new HibernateException( "The class has no identifier property: " + getEntityName() );
			}
			id = idGetter.get( entity );
		}

		try {
			return ( Serializable ) id;
		}
		catch ( ClassCastException cce ) {
			StringBuffer msg = new StringBuffer( "Identifier classes must be serializable. " );
			if ( id != null ) {
				msg.append( id.getClass().getName() + " is not serializable. " );
			}
			if ( cce.getMessage() != null ) {
				msg.append( cce.getMessage() );
			}
			throw new ClassCastException( msg.toString() );
		}
	}


	public void setIdentifier(Object entity, Serializable id) throws HibernateException {
		if ( entityMetamodel.getIdentifierProperty().isEmbedded() ) {
			if ( entity != id ) {
				AbstractComponentType copier = (AbstractComponentType) entityMetamodel.getIdentifierProperty().getType();
				copier.setPropertyValues( entity, copier.getPropertyValues( id, getEntityMode() ), getEntityMode() );
			}
		}
		else if ( idSetter != null ) {
			idSetter.set( entity, id, getFactory() );
		}
	}

	public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion) {
		if ( entityMetamodel.getIdentifierProperty().getIdentifierGenerator() instanceof Assigned ) {
			//return currentId;
		}
		else {
			//reset the id
			Serializable result = entityMetamodel.getIdentifierProperty().getUnsavedValue()
					.getDefaultValue( currentId );
			setIdentifier( entity, result );
			//reset the version
			VersionProperty versionProperty = entityMetamodel.getVersionProperty();
			if ( entityMetamodel.isVersioned() ) {
				setPropertyValue(
				        entity,
				        entityMetamodel.getVersionPropertyIndex(),
						versionProperty.getUnsavedValue().getDefaultValue( currentVersion )
				);
			}
			//return the id, so we can use it to reset the proxy id
			//return result;
		}
	}

	public Object getVersion(Object entity) throws HibernateException {
		if ( !entityMetamodel.isVersioned() ) return null;
		return getters[ entityMetamodel.getVersionPropertyIndex() ].get( entity );
	}

	protected boolean shouldGetAllProperties(Object entity) {
		return !hasUninitializedLazyProperties( entity );
	}

	public Object[] getPropertyValues(Object entity) throws HibernateException {
		boolean getAll = shouldGetAllProperties( entity );
		final int span = entityMetamodel.getPropertySpan();
		final Object[] result = new Object[span];

		for ( int j = 0; j < span; j++ ) {
			StandardProperty property = entityMetamodel.getProperties()[j];
			if ( getAll || !property.isLazy() ) {
				result[j] = getters[j].get( entity );
			}
			else {
				result[j] = LazyPropertyInitializer.UNFETCHED_PROPERTY;
			}
		}
		return result;
	}

	public Object[] getPropertyValuesToInsert(Object entity, SessionImplementor session) 
	throws HibernateException {
		final int span = entityMetamodel.getPropertySpan();
		final Object[] result = new Object[span];

		for ( int j = 0; j < span; j++ ) {
			result[j] = getters[j].getForInsert( entity, session );
		}
		return result;
	}

	public Object getPropertyValue(Object entity, int i) throws HibernateException {
		return getters[i].get( entity );
	}

	public Object getPropertyValue(Object entity, String propertyName) throws HibernateException {
		return getPropertyValue( entity, entityMetamodel.getPropertyIndex( propertyName ) );
	}

	public void setPropertyValues(Object entity, Object[] values) throws HibernateException {
		boolean setAll = !entityMetamodel.hasLazyProperties();

		for ( int j = 0; j < entityMetamodel.getPropertySpan(); j++ ) {
			if ( setAll || values[j] != LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
				setters[j].set( entity, values[j], getFactory() );
			}
		}
	}

	public void setPropertyValue(Object entity, int i, Object value) throws HibernateException {
		setters[i].set( entity, value, getFactory() );
	}

	public void setPropertyValue(Object entity, String propertyName, Object value) throws HibernateException {
		setters[ entityMetamodel.getPropertyIndex( propertyName ) ].set( entity, value, getFactory() );
	}

	public final Object instantiate(Serializable id) throws HibernateException {
		Object result = instantiator.instantiate( id );
		if ( id != null ) {
			setIdentifier( result, id );
		}
		return result;
	}

	public final Object instantiate() throws HibernateException {
		return instantiator.instantiate(null);
	}

	public void afterInitialize(Object entity, boolean lazyPropertiesAreUnfetched, SessionImplementor session) {}

	public boolean hasUninitializedLazyProperties(Object entity) {
		// the default is to simply not lazy fetch properties for now...
		return false;
	}

	public final boolean isInstance(Object object) {
        return instantiator.isInstance( object );
	}

	public boolean hasProxy() {
		return entityMetamodel.isLazy();
	}

	public Object createProxy(Serializable id, SessionImplementor session) 
	throws HibernateException {
		return getProxyFactory().getProxy( id, session );
	}

	public boolean isLifecycleImplementor() {
		return false;
	}

	public boolean isValidatableImplementor() {
		return false;
	}
	
	protected final EntityMetamodel getEntityMetamodel() {
		return entityMetamodel;
	}

	protected final SessionFactoryImplementor getFactory() {
		return entityMetamodel.getSessionFactory();
	}

	protected final Instantiator getInstantiator() {
		return instantiator;
	}

	protected final ProxyFactory getProxyFactory() {
		return proxyFactory;
	}
	
	public String toString() {
		return getClass().getName() + '(' + getEntityMetamodel().getName() + ')';
	}

}
