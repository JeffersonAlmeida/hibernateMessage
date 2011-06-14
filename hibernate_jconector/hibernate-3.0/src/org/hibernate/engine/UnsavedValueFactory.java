//$Id: UnsavedValueFactory.java,v 1.3 2005/02/19 07:06:04 oneovthafew Exp $
package org.hibernate.engine;

import java.io.Serializable;
import java.lang.reflect.Constructor;

import org.hibernate.InstantiationException;
import org.hibernate.MappingException;
import org.hibernate.property.Getter;
import org.hibernate.type.IdentifierType;
import org.hibernate.type.PrimitiveType;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;

/**
 * @author Gavin King
 */
public class UnsavedValueFactory {
	
	private static Object instantiate(Constructor constructor) {
		try {
			return constructor.newInstance(null);
		}
		catch (Exception e) {
			throw new InstantiationException( "could not instantiate test object", constructor.getDeclaringClass() );
		}
	}
	
	/**
	 * Return an IdentifierValue for the specified unsaved-value. If none is specified, 
	 * guess the unsaved value by instantiating a test instance of the class and
	 * reading it's id property, or if that is not possible, using the java default
	 * value for the type 
	 */
	public static Cascades.IdentifierValue getUnsavedIdentifierValue(
			String unsavedValue, 
			Getter identifierGetter,
			Type identifierType,
			Constructor constructor) {
		
		if ( unsavedValue == null ) {
			if ( identifierGetter!=null && constructor!=null ) {
				// use the id value of a newly instantiated instance as the unsaved-value
				Serializable defaultValue = (Serializable) identifierGetter.get( instantiate(constructor) );
				return new Cascades.IdentifierValue( defaultValue );
			}
			else if ( identifierGetter != null && (identifierType instanceof PrimitiveType) ) {
				Serializable defaultValue = ( ( PrimitiveType ) identifierType ).getDefaultValue();
				return new Cascades.IdentifierValue( defaultValue );
			}
			else {
				return Cascades.SAVE_NULL;
			}
		}
		else if ( "null".equals( unsavedValue ) ) {
			return Cascades.SAVE_NULL;
		}
		else if ( "undefined".equals( unsavedValue ) ) {
			return Cascades.UNDEFINED;
		}
		else if ( "none".equals( unsavedValue ) ) {
			return Cascades.SAVE_NONE;
		}
		else if ( "any".equals( unsavedValue ) ) {
			return Cascades.SAVE_ANY;
		}
		else {
			try {
				return new Cascades.IdentifierValue( ( Serializable ) ( ( IdentifierType ) identifierType ).stringToObject( unsavedValue ) );
			}
			catch ( ClassCastException cce ) {
				throw new MappingException( "Bad identifier type: " + identifierType.getName() );
			}
			catch ( Exception e ) {
				throw new MappingException( "Could not parse identifier unsaved-value: " + unsavedValue );
			}
		}
	}

	public static Cascades.VersionValue getUnsavedVersionValue(
			String versionUnsavedValue, 
			Getter versionGetter,
			VersionType versionType,
			Constructor constructor) {
		
		if ( versionUnsavedValue == null ) {
			if ( constructor!=null ) {
				Object defaultValue = versionGetter.get( instantiate(constructor) );
				// if the version of a newly instantiated object is not the same
				// as the version seed value, use that as the unsaved-value
				return versionType.isEqual( versionType.seed(), defaultValue ) ?
						Cascades.VERSION_UNDEFINED :
						new Cascades.VersionValue( defaultValue );
			}
			else {
				return Cascades.VERSION_UNDEFINED;
			}
		}
		else if ( "undefined".equals( versionUnsavedValue ) ) {
			return Cascades.VERSION_UNDEFINED;
		}
		else if ( "null".equals( versionUnsavedValue ) ) {
			return Cascades.VERSION_SAVE_NULL;
		}
		else if ( "negative".equals( versionUnsavedValue ) ) {
			return Cascades.VERSION_NEGATIVE;
		}
		else {
			// this should not happen since the DTD prevents it
			throw new MappingException( "Could not parse version unsaved-value: " + versionUnsavedValue );
		}
		
	}

}
