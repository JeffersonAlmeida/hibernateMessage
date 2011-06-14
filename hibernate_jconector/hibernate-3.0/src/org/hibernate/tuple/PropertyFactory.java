// $Id: PropertyFactory.java,v 1.3 2005/02/19 07:06:06 oneovthafew Exp $
package org.hibernate.tuple;

import java.lang.reflect.Constructor;

import org.hibernate.EntityMode;
import org.hibernate.engine.Cascades;
import org.hibernate.engine.UnsavedValueFactory;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;
import org.hibernate.util.ReflectHelper;

/**
 * Responsible for generation of runtime metamodel {@link Property} representations.
 * Makes distinction between identifier, version, and other (standard) properties.
 *
 * @author Steve Ebersole
 */
public class PropertyFactory {

	/**
	 * Generates an IdentifierProperty representation of the for a given entity mapping.
	 *
	 * @param mappedEntity The mapping definition of the entity.
	 * @param generator The identifier value generator to use for this identifier.
	 * @return The appropriate IdentifierProperty definition.
	 */
	public static IdentifierProperty buildIdentifierProperty(PersistentClass mappedEntity, IdentifierGenerator generator) {

		String mappedUnsavedValue = mappedEntity.getIdentifier().getNullValue();
		Type type = mappedEntity.getIdentifier().getType();
		Property property = mappedEntity.getIdentifierProperty();
		
		Cascades.IdentifierValue unsavedValue = UnsavedValueFactory.getUnsavedIdentifierValue(
				mappedUnsavedValue,
				getGetter( property ),
				type,
				getConstructor(mappedEntity)
			);

		if ( property == null ) {
			// this is a virtual id property...
			return new IdentifierProperty(
			        type,
					mappedEntity.hasEmbeddedIdentifier(),
					unsavedValue,
					generator
			);
		}
		else {
			return new IdentifierProperty(
					property.getName(),
					property.getNodeName(),
					type,
					mappedEntity.hasEmbeddedIdentifier(),
					unsavedValue,
					generator
			);
		}
	}

	/**
	 * Generates a VersionProperty representation for an entity mapping given its
	 * version mapping Property.
	 *
	 * @param property The version mapping Property.
	 * @param lazyAvailable Is property lazy loading currently available.
	 * @return The appropriate VersionProperty definition.
	 */
	public static VersionProperty buildVersionProperty(Property property, boolean lazyAvailable) {
		String mappedUnsavedValue = ( (KeyValue) property.getValue() ).getNullValue();
		
		Cascades.VersionValue unsavedValue = UnsavedValueFactory.getUnsavedVersionValue(
				mappedUnsavedValue, 
				getGetter( property ),
				(VersionType) property.getType(),
				getConstructor( property.getPersistentClass() )
			);

		boolean lazy = lazyAvailable && property.isLazy();

		return new VersionProperty(
		        property.getName(),
		        property.getNodeName(),
		        property.getValue().getType(),
		        lazy,
				property.isInsertable(),
				property.isUpdateable(),
				property.isOptional(),
				property.isUpdateable() && !lazy,
				property.isOptimisticLocked(),
		        property.getCascadeStyle(),
		        unsavedValue
		);
	}

	/**
	 * Generate a "standard" (i.e., non-identifier and non-version) based on the given
	 * mapped property.
	 *
	 * @param property The mapped property.
	 * @param lazyAvailable Is property lazy loading currently available.
	 * @return The appropriate StandardProperty definition.
	 */
	public static StandardProperty buildStandardProperty(Property property, boolean lazyAvailable) {
		boolean lazy = lazyAvailable && property.isLazy();

		return new StandardProperty(
				property.getName(),
				property.getNodeName(),
				property.getValue().getType(),
				lazy,
				property.isInsertable(),
				property.isUpdateable(),
				property.isOptional(),
				property.isUpdateable() && !lazy,
				property.isOptimisticLocked(),
				property.getCascadeStyle()
		);
	}

	private static Constructor getConstructor(PersistentClass persistentClass) {
		if ( persistentClass == null || !persistentClass.hasPojoRepresentation() ) {
			return null;
		}

		try {
			return ReflectHelper.getDefaultConstructor( persistentClass.getMappedClass() );
		}
		catch( Throwable t ) {
			return null;
		}
	}

	private static Getter getGetter(Property mappingProperty) {
		if ( mappingProperty == null || !mappingProperty.getPersistentClass().hasPojoRepresentation() ) {
			return null;
		}

		PropertyAccessor pa = PropertyAccessorFactory.getPropertyAccessor( mappingProperty, EntityMode.POJO );
		return pa.getGetter( mappingProperty.getPersistentClass().getMappedClass(), mappingProperty.getName() );
	}

}
