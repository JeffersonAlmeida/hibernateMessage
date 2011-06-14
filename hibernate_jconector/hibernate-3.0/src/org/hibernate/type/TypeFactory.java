// $Id: TypeFactory.java,v 1.22 2005/03/16 04:45:26 oneovthafew Exp $
package org.hibernate.type;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.classic.Lifecycle;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.intercept.LazyPropertyInitializer;
import org.hibernate.property.BackrefPropertyAccessor;
import org.hibernate.tuple.StandardProperty;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;
import org.hibernate.util.ReflectHelper;

/**
 * Used internally to obtain instances of <tt>Type</tt>. Applications should use static methods
 * and constants on <tt>org.hibernate.Hibernate</tt>.
 * 
 * @see org.hibernate.Hibernate
 * @author Gavin King
 */
public final class TypeFactory {

	private static final Map BASIC_TYPES;

	static {
		HashMap basics = new HashMap();
		basics.put( boolean.class.getName(), Hibernate.BOOLEAN );
		basics.put( long.class.getName(), Hibernate.LONG );
		basics.put( short.class.getName(), Hibernate.SHORT );
		basics.put( int.class.getName(), Hibernate.INTEGER );
		basics.put( byte.class.getName(), Hibernate.BYTE );
		basics.put( float.class.getName(), Hibernate.FLOAT );
		basics.put( double.class.getName(), Hibernate.DOUBLE );
		basics.put( char.class.getName(), Hibernate.CHARACTER );
		basics.put( Hibernate.CHARACTER.getName(), Hibernate.CHARACTER );
		basics.put( Hibernate.INTEGER.getName(), Hibernate.INTEGER );
		basics.put( Hibernate.STRING.getName(), Hibernate.STRING );
		basics.put( Hibernate.DATE.getName(), Hibernate.DATE );
		basics.put( Hibernate.TIME.getName(), Hibernate.TIME );
		basics.put( Hibernate.TIMESTAMP.getName(), Hibernate.TIMESTAMP );
		basics.put( Hibernate.LOCALE.getName(), Hibernate.LOCALE );
		basics.put( Hibernate.CALENDAR.getName(), Hibernate.CALENDAR );
		basics.put( Hibernate.CALENDAR_DATE.getName(), Hibernate.CALENDAR_DATE );
		basics.put( Hibernate.CURRENCY.getName(), Hibernate.CURRENCY );
		basics.put( Hibernate.TIMEZONE.getName(), Hibernate.TIMEZONE );
		basics.put( Hibernate.CLASS.getName(), Hibernate.CLASS );
		basics.put( Hibernate.TRUE_FALSE.getName(), Hibernate.TRUE_FALSE );
		basics.put( Hibernate.YES_NO.getName(), Hibernate.YES_NO );
		basics.put( Hibernate.BINARY.getName(), Hibernate.BINARY );
		basics.put( Hibernate.TEXT.getName(), Hibernate.TEXT );
		basics.put( Hibernate.BLOB.getName(), Hibernate.BLOB );
		basics.put( Hibernate.CLOB.getName(), Hibernate.CLOB );
		basics.put( Hibernate.BIG_DECIMAL.getName(), Hibernate.BIG_DECIMAL );
		basics.put( Hibernate.SERIALIZABLE.getName(), Hibernate.SERIALIZABLE );
		basics.put( Hibernate.OBJECT.getName(), Hibernate.OBJECT );
		basics.put( Boolean.class.getName(), Hibernate.BOOLEAN );
		basics.put( Long.class.getName(), Hibernate.LONG );
		basics.put( Short.class.getName(), Hibernate.SHORT );
		basics.put( Integer.class.getName(), Hibernate.INTEGER );
		basics.put( Byte.class.getName(), Hibernate.BYTE );
		basics.put( Float.class.getName(), Hibernate.FLOAT );
		basics.put( Double.class.getName(), Hibernate.DOUBLE );
		basics.put( Character.class.getName(), Hibernate.CHARACTER );
		basics.put( String.class.getName(), Hibernate.STRING );
		basics.put( java.util.Date.class.getName(), Hibernate.TIMESTAMP );
		basics.put( Time.class.getName(), Hibernate.TIME );
		basics.put( Timestamp.class.getName(), Hibernate.TIMESTAMP );
		basics.put( java.sql.Date.class.getName(), Hibernate.DATE );
		basics.put( BigDecimal.class.getName(), Hibernate.BIG_DECIMAL );
		basics.put( Locale.class.getName(), Hibernate.LOCALE );
		basics.put( Calendar.class.getName(), Hibernate.CALENDAR );
		basics.put( GregorianCalendar.class.getName(), Hibernate.CALENDAR );
		if ( CurrencyType.CURRENCY_CLASS != null ) {
			basics.put( CurrencyType.CURRENCY_CLASS.getName(), Hibernate.CURRENCY );
		}
		basics.put( TimeZone.class.getName(), Hibernate.TIMEZONE );
		basics.put( Object.class.getName(), Hibernate.OBJECT );
		basics.put( Class.class.getName(), Hibernate.CLASS );
		basics.put( byte[].class.getName(), Hibernate.BINARY );
		basics.put( "byte[]", Hibernate.BINARY );
		basics.put( Blob.class.getName(), Hibernate.BLOB );
		basics.put( Clob.class.getName(), Hibernate.CLOB );
		basics.put( Serializable.class.getName(), Hibernate.SERIALIZABLE );
		BASIC_TYPES = Collections.unmodifiableMap( basics );
	}

	private TypeFactory() {
		throw new UnsupportedOperationException();
	}

	/**
	 * A one-to-one association type for the given class
	 */
	public static Type oneToOne(String persistentClass, ForeignKeyDirection foreignKeyType,
			String uniqueKeyPropertyName, boolean isEmbeddedInXML) {
		return new OneToOneType(
			persistentClass,
			foreignKeyType,
			uniqueKeyPropertyName,
			isEmbeddedInXML );
	}

	/**
	 * A many-to-one association type for the given class
	 */
	public static Type manyToOne(String persistentClass) {
		return new ManyToOneType( persistentClass );
	}

	/**
	 * A many-to-one association type for the given class
	 */
	public static Type manyToOne(String persistentClass, String uniqueKeyPropertyName,
			boolean isEmbeddedInXML) {
		return new ManyToOneType( persistentClass, uniqueKeyPropertyName, isEmbeddedInXML );
	}

	/**
	 * Given the name of a Hibernate basic type, return an instance of
	 * <tt>org.hibernate.type.Type</tt>.
	 */
	public static Type basic(String name) {
		return (Type) BASIC_TYPES.get( name );
	}

	/**
	 * Uses heuristics to deduce a Hibernate type given a string naming the type or Java class.
	 * Return an instance of <tt>org.hibernate.type.Type</tt>.
	 */
	public static Type heuristicType(String typeName) throws MappingException {
		return heuristicType( typeName, null );
	}

	/**
	 * Uses heuristics to deduce a Hibernate type given a string naming the type or Java class.
	 * Return an instance of <tt>org.hibernate.type.Type</tt>.
	 */
	public static Type heuristicType(String typeName, Properties parameters)
			throws MappingException {
		Type type = TypeFactory.basic( typeName );
		if ( type == null ) {
			Class typeClass;
			try {
				typeClass = ReflectHelper.classForName( typeName );
			}
			catch (ClassNotFoundException cnfe) {
				typeClass = null;
			}
			if ( typeClass != null ) {
				if ( Type.class.isAssignableFrom( typeClass ) ) {
					try {
						type = (Type) typeClass.newInstance();
					}
					catch (Exception e) {
						throw new MappingException( "Could not instantiate Type "
							+ typeClass.getName()
							+ ": "
							+ e );
					}
				}
				else if ( CompositeUserType.class.isAssignableFrom( typeClass ) ) {
					type = new CompositeCustomType( typeClass, parameters );
				}
				else if ( UserType.class.isAssignableFrom( typeClass ) ) {
					type = new CustomType( typeClass, parameters );
				}
				else if ( Lifecycle.class.isAssignableFrom( typeClass ) ) {
					type = Hibernate.entity( typeClass );
				}
				else if ( Serializable.class.isAssignableFrom( typeClass ) ) {
					type = Hibernate.serializable( typeClass );
				}
			}
		}
		return type;

	}

	public static CollectionType customCollection(String typeName, String role, String propertyRef,
			boolean embedded) {
		Class typeClass;
		try {
			typeClass = ReflectHelper.classForName( typeName );
		}
		catch (ClassNotFoundException cnfe) {
			throw new MappingException( "user colllection type class not found: " + typeName, cnfe );
		}
		return new CustomCollectionType( typeClass, role, propertyRef, embedded );
	}

	// Collection Types:

	public static CollectionType array(String role, String propertyRef, boolean embedded,
			Class elementClass) {
		return new ArrayType( role, propertyRef, elementClass, embedded );
	}

	public static CollectionType list(String role, String propertyRef, boolean embedded) {
		return new ListType( role, propertyRef, embedded );
	}

	public static CollectionType bag(String role, String propertyRef, boolean embedded) {
		return new BagType( role, propertyRef, embedded );
	}

	public static CollectionType idbag(String role, String propertyRef, boolean embedded) {
		return new IdentifierBagType( role, propertyRef, embedded );
	}

	public static CollectionType map(String role, String propertyRef, boolean embedded) {
		return new MapType( role, propertyRef, embedded );
	}

	public static CollectionType set(String role, String propertyRef, boolean embedded) {
		return new SetType( role, propertyRef, embedded );
	}

	public static CollectionType sortedMap(String role, String propertyRef, boolean embedded,
			Comparator comparator) {
		return new SortedMapType( role, propertyRef, comparator, embedded );
	}

	public static CollectionType sortedSet(String role, String propertyRef, boolean embedded,
			Comparator comparator) {
		return new SortedSetType( role, propertyRef, comparator, embedded );
	}

	/**
	 * Deep copy values in the first array into the second
	 */
	public static void deepCopy(Object[] values, Type[] types, boolean[] copy, Object[] target,
			SessionImplementor session) throws HibernateException {
		for ( int i = 0; i < types.length; i++ ) {
			if ( copy[i] ) {
				if ( values[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY
					|| values[i] == BackrefPropertyAccessor.UNKNOWN ) {
					target[i] = values[i];
				}
				else {
					target[i] = types[i].deepCopy( values[i], session.getEntityMode(), session
						.getFactory() );
				}
			}
		}
	}

	/**
	 * Determine if any of the given field values are dirty, returning an array containing indexes
	 * of the dirty fields or <tt>null</tt> if no fields are dirty.
	 */
	public static int[] findDirty(Type[] types, Object[] x, Object[] y, boolean[] check,
			SessionImplementor session) throws HibernateException {
		int[] results = null;
		int count = 0;
		for ( int i = 0; i < types.length; i++ ) {
			if ( check[i] && types[i].isDirty( x[i], y[i], session ) ) {
				if ( results == null ) results = new int[types.length];
				results[count++] = i;
			}
		}
		if ( count == 0 ) {
			return null;
		}
		else {
			int[] trimmed = new int[count];
			System.arraycopy( results, 0, trimmed, 0, count );
			return trimmed;
		}
	}

	/**
	 * Determine if any of the given field values are modified, returning an array containing
	 * indexes of the dirty fields or <tt>null</tt> if no fields are dirty.
	 */
	public static int[] findModified(Type[] types, Object[] old, Object[] current, boolean[] check,
			SessionImplementor session) throws HibernateException {
		int[] results = null;
		int count = 0;
		for ( int i = 0; i < types.length; i++ ) {
			if ( check[i]
				&& types[i].isModified( old[i], current[i], session ) ) {
				if ( results == null ) results = new int[types.length];
				results[count++] = i;
			}
		}
		if ( count == 0 ) {
			return null;
		}
		else {
			int[] trimmed = new int[count];
			System.arraycopy( results, 0, trimmed, 0, count );
			return trimmed;
		}
	}

	public static Object[] assemble(Serializable[] row, Type[] types, SessionImplementor session,
			Object owner) throws HibernateException {
		Object[] assembled = new Object[row.length];
		for ( int i = 0; i < types.length; i++ ) {
			if ( row[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY
				|| row[i] == BackrefPropertyAccessor.UNKNOWN ) {
				assembled[i] = row[i];
			}
			else {
				assembled[i] = types[i].assemble( row[i], session, owner );
			}
		}
		return assembled;
	}

	public static Serializable[] disassemble(Object[] row, Type[] types, SessionImplementor session,
			Object owner) throws HibernateException {
		Serializable[] disassembled = new Serializable[row.length];
		for ( int i = 0; i < row.length; i++ ) {
			if ( row[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY
				|| row[i] == BackrefPropertyAccessor.UNKNOWN ) {
				disassembled[i] = (Serializable) row[i];
			}
			else {
				disassembled[i] = types[i].disassemble( row[i], session, owner );
			}
		}
		return disassembled;
	}

	public static Object[] replace(Object[] original, Object[] target, Type[] types,
			SessionImplementor session, Object owner, Map copyCache) throws HibernateException {
		Object[] copied = new Object[original.length];
		for ( int i = 0; i < types.length; i++ ) {
			if ( original[i] == LazyPropertyInitializer.UNFETCHED_PROPERTY
				|| original[i] == BackrefPropertyAccessor.UNKNOWN ) {
				copied[i] = target[i];
			}
			else {
				copied[i] = types[i].replace( original[i], target[i], session, owner, copyCache );
			}
		}
		return copied;
	}

	/**
	 * Return <tt>-1</tt> if non-dirty, or the index of the first dirty value otherwise
	 */
	/*
	 * public static int findDirty(Type[] types, Object[] x, Object[] y, Object owner,
	 * SessionFactoryImplementor factory) throws HibernateException { for (int i=0; i<types.length;
	 * i++) { if ( types[i].isDirty( x[i], y[i], owner, factory ) ) { return i; } } return -1; }
	 */

	public static int[] findDirty(final StandardProperty[] properties, final Object[] x,
			final Object[] y, final boolean anyUninitializedProperties,
			final SessionImplementor session) throws HibernateException {

		int[] results = null;
		int count = 0;
		int span = properties.length;

		for ( int i = 0; i < span; i++ ) {

			final boolean dirty = isCheckable( properties[i], anyUninitializedProperties )
				&& properties[i].getType().isDirty( x[i], y[i], session );

			if ( dirty ) {
				if ( results == null ) {
					results = new int[span];
				}
				results[count++] = i;
			}

		}

		if ( count == 0 ) {
			return null;
		}
		else {
			int[] trimmed = new int[count];
			System.arraycopy( results, 0, trimmed, 0, count );
			return trimmed;
		}
	}

	/**
	 * Determine if any of the given field values are modified, returning an array containing
	 * indexes of the dirty fields or <tt>null</tt> if no fields are dirty.
	 */
	public static int[] findModified(StandardProperty[] properties, Object[] x, Object[] y,
			boolean anyUninitializedProperties, SessionImplementor session)
			throws HibernateException {

		int[] results = null;
		int count = 0;
		int span = properties.length;

		for ( int i = 0; i < span; i++ ) {

			final boolean modified = isCheckable( properties[i], anyUninitializedProperties )
				&& properties[i].getType().isModified( x[i], y[i], session );

			if ( modified ) {
				if ( results == null ) {
					results = new int[span];
				}
				results[count++] = i;
			}

		}

		if ( count == 0 ) {
			return null;
		}
		else {
			int[] trimmed = new int[count];
			System.arraycopy( results, 0, trimmed, 0, count );
			return trimmed;
		}
	}

	private static boolean isCheckable(StandardProperty property, boolean anyUninitializedProperties) {
		return anyUninitializedProperties ? property.isCheckable() : property.isUpdateable();
	}
}
