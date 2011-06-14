// $Id: EntityMetamodel.java,v 1.7 2005/03/02 11:35:46 oneovthafew Exp $
package org.hibernate.tuple;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sf.cglib.transform.impl.InterceptFieldEnabled;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.EntityMode;
import org.hibernate.MappingException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.engine.Cascades;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.Versioning;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.ReflectHelper;

/**
 * Centralizes metamodel information about an entity.
 *
 * @author Steve Ebersole
 */
public class EntityMetamodel implements Serializable {
	
	private static final Log log = LogFactory.getLog(EntityMetamodel.class);

	private static final int NO_VERSION_INDX = -66;

	private final SessionFactoryImplementor sessionFactory;

	private final String name;
	private final String rootName;
	private final Type entityType;

	private final IdentifierProperty identifierProperty;
	private final boolean versioned;

	private final int propertySpan;
	private final int versionPropertyIndex;
	private final StandardProperty[] properties;
	// temporary ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private final String[] propertyNames;
	private final Type[] propertyTypes;
	private final boolean[] propertyLaziness;
	private final boolean[] propertyUpdateability;
	private final boolean[] propertyCheckability;
	private final boolean[] propertyInsertability;
	private final boolean[] propertyNullability;
	private final boolean[] propertyVersionability;
	private final Cascades.CascadeStyle[] cascadeStyles;
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private final Map propertyNameToIndexMap;
	private final boolean hasCollections;
	private final boolean hasLazyProperties;

	private final boolean lazy;
	private final boolean hasCascades;
	private final boolean mutable;
	private final boolean isAbstract;
	private final boolean selectBeforeUpdate;
	private final boolean dynamicUpdate;
	private final boolean dynamicInsert;
	private final int optimisticLockMode;

	private final boolean polymorphic;
	private final String superclass;  // superclass entity-name
	private final boolean explicitPolymorphism;
	private final boolean inherited;
	private final boolean hasSubclasses;
	private final Set subclassEntityNames = new HashSet();
	
	private final TuplizerLookup tuplizers;
	
	public EntityTuplizer getTuplizer(EntityMode entityMode) {
		return (EntityTuplizer) tuplizers.getTuplizer(entityMode);
	}

	public EntityTuplizer getTuplizerOrNull(EntityMode entityMode) {
		return (EntityTuplizer) tuplizers.getTuplizerOrNull(entityMode);
	}
	
	public EntityMode guessEntityMode(Object object) {
		return tuplizers.guessEntityMode(object);
	}

	public EntityMetamodel(PersistentClass persistentClass, SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;

		name = persistentClass.getEntityName();
		rootName = persistentClass.getRootClass().getEntityName();
		entityType = TypeFactory.manyToOne( name );

		identifierProperty = PropertyFactory.buildIdentifierProperty(
		        persistentClass,
		        sessionFactory.getIdentifierGenerator( rootName )
		);

		versioned = persistentClass.isVersioned();

		boolean lazyAvailable = persistentClass.hasPojoRepresentation() &&
		        InterceptFieldEnabled.class.isAssignableFrom( persistentClass.getMappedClass() );
		boolean hasLazy = false;

		propertySpan = persistentClass.getPropertyClosureSpan();
		properties = new StandardProperty[propertySpan];
		// temporary ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		propertyNames = new String[propertySpan];
		propertyTypes = new Type[propertySpan];
		propertyUpdateability = new boolean[propertySpan];
		propertyInsertability = new boolean[propertySpan];
		propertyCheckability = new boolean[propertySpan];
		propertyNullability = new boolean[propertySpan];
		propertyVersionability = new boolean[propertySpan];
		propertyLaziness = new boolean[propertySpan];
		cascadeStyles = new Cascades.CascadeStyle[propertySpan];
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


		Iterator iter = persistentClass.getPropertyClosureIterator();
		int i = 0;
		int tempVersionProperty = NO_VERSION_INDX;
		boolean foundCascade = false;
		boolean foundCollection = false;
		Map tmpPropNameToIndexMap = new HashMap();

		while ( iter.hasNext() ) {
			Property prop = ( Property ) iter.next();

			if ( prop == persistentClass.getVersion() ) {
				tempVersionProperty = i;
				properties[i] = PropertyFactory.buildVersionProperty( prop, lazyAvailable );
			}
			else {
				properties[i] = PropertyFactory.buildStandardProperty( prop, lazyAvailable );
			}

			// temporary ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			propertyNames[i] = properties[i].getName();
			propertyTypes[i] = properties[i].getType();
			propertyNullability[i] = properties[i].isNullable();
			propertyUpdateability[i] = properties[i].isUpdateable();
			propertyInsertability[i] = properties[i].isInsertable();
			propertyVersionability[i] = properties[i].isVersionable();

			boolean lazy = prop.isLazy() && lazyAvailable;
			propertyLaziness[i] = lazy;
			propertyCheckability[i] = properties[i].isUpdateable() && !lazy;
			if ( lazy ) hasLazy = true;

			cascadeStyles[i] = properties[i].getCascadeStyle();
			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

			if ( properties[i].isLazy() ) {
				hasLazy = true;
			}

			if ( properties[i].getCascadeStyle() != Cascades.STYLE_NONE ) {
				foundCascade = true;
			}

			if ( indicatesCollection( properties[i].getType() ) ) {
				foundCollection = true;
			}

			tmpPropNameToIndexMap.put( prop.getName(), new Integer(i) );
			i++;
		}

		hasCascades = foundCascade;
		versionPropertyIndex = tempVersionProperty;
		propertyNameToIndexMap = Collections.unmodifiableMap( tmpPropNameToIndexMap );
		hasLazyProperties = hasLazy;
		if (hasLazyProperties) log.info("lazy property fetching available for: " + name);

		lazy = persistentClass.isLazy();
		mutable = persistentClass.isMutable();
		isAbstract = persistentClass.isAbstract() || ( 
				persistentClass.hasPojoRepresentation() && 
				ReflectHelper.isAbstractClass( persistentClass.getMappedClass() )
			);
		selectBeforeUpdate = persistentClass.hasSelectBeforeUpdate();
		dynamicUpdate = persistentClass.useDynamicUpdate();
		dynamicInsert = persistentClass.useDynamicInsert();

		polymorphic = persistentClass.isPolymorphic();
		explicitPolymorphism = persistentClass.isExplicitPolymorphism();
		inherited = persistentClass.isInherited();
		superclass = inherited ?
				persistentClass.getSuperclass().getEntityName() :
				null;
		hasSubclasses = persistentClass.hasSubclasses();

		optimisticLockMode = persistentClass.getOptimisticLockMode();
		if ( optimisticLockMode > Versioning.OPTIMISTIC_LOCK_VERSION && !dynamicUpdate ) {
			throw new MappingException( "optimistic-lock setting requires dynamic-update=\"true\": " + name );
		}

		hasCollections = foundCollection;

		tuplizers = TuplizerLookup.create(persistentClass, this);
		
		iter = persistentClass.getSubclassIterator();
		while ( iter.hasNext() ) {
			subclassEntityNames.add( ( (PersistentClass) iter.next() ).getEntityName() );
		}
		subclassEntityNames.add(name);
		
	}
	
	public Set getSubclassEntityNames() {
		return subclassEntityNames;
	}

	private boolean indicatesCollection(Type type) {
		if ( type.isCollectionType() ) {
			return true;
		}
		else if ( type.isComponentType() ) {
			Type[] subtypes = ( ( AbstractComponentType ) type ).getSubtypes();
			for ( int i = 0; i < subtypes.length; i++ ) {
				if ( indicatesCollection( subtypes[i] ) ) {
					return true;
				}
			}
		}
		return false;
	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	public String getName() {
		return name;
	}

	public String getRootName() {
		return rootName;
	}

	public Type getEntityType() {
		return entityType;
	}

	public IdentifierProperty getIdentifierProperty() {
		return identifierProperty;
	}

	public int getPropertySpan() {
		return propertySpan;
	}

	public int getVersionPropertyIndex() {
		return versionPropertyIndex;
	}

	public VersionProperty getVersionProperty() {
		if ( NO_VERSION_INDX == versionPropertyIndex ) {
			return null;
		}
		else {
			return ( VersionProperty ) properties[ versionPropertyIndex ];
		}
	}

	public StandardProperty[] getProperties() {
		return properties;
	}

	public int getPropertyIndex(StandardProperty property) throws PropertyNotFoundException {
		return getPropertyIndex( property.getName() );
	}

	public int getPropertyIndex(String propertyName) throws PropertyNotFoundException {
		Integer index = ( Integer ) propertyNameToIndexMap.get( propertyName );
		if ( index == null ) {
			throw new PropertyNotFoundException(
			        "Unable to resolve property [name=" + propertyName + "] to corresponding index"
			);
		}

		return index.intValue();
	}

	public boolean hasCollections() {
		return hasCollections;
	}

	public boolean hasLazyProperties() {
		return hasLazyProperties;
	}

	public boolean hasCascades() {
		return hasCascades;
	}

	public boolean isMutable() {
		return mutable;
	}

	public boolean isSelectBeforeUpdate() {
		return selectBeforeUpdate;
	}

	public boolean isDynamicUpdate() {
		return dynamicUpdate;
	}

	public boolean isDynamicInsert() {
		return dynamicInsert;
	}

	public int getOptimisticLockMode() {
		return optimisticLockMode;
	}

	public boolean isPolymorphic() {
		return polymorphic;
	}

	public String getSuperclass() {
		return superclass;
	}

	public boolean isExplicitPolymorphism() {
		return explicitPolymorphism;
	}

	public boolean isInherited() {
		return inherited;
	}

	public boolean hasSubclasses() {
		return hasSubclasses;
	}

	public boolean isLazy() {
		return lazy;
	}

	public boolean isVersioned() {
		return versioned;
	}

	public boolean isAbstract() {
		return isAbstract;
	}
	
	public String toString() {
		return "EntityMetamodel(" + name + ':' + ArrayHelper.toString(properties) + ')';
	}
	
	// temporary ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	public String[] getPropertyNames() {
		return propertyNames;
	}

	public Type[] getPropertyTypes() {
		return propertyTypes;
	}

	public boolean[] getPropertyLaziness() {
		return propertyLaziness;
	}

	public boolean[] getPropertyUpdateability() {
		return propertyUpdateability;
	}

	public boolean[] getPropertyCheckability() {
		return propertyCheckability;
	}

	public boolean[] getPropertyInsertability() {
		return propertyInsertability;
	}

	public boolean[] getPropertyNullability() {
		return propertyNullability;
	}

	public boolean[] getPropertyVersionability() {
		return propertyVersionability;
	}

	public Cascades.CascadeStyle[] getCascadeStyles() {
		return cascadeStyles;
	}
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
}
