//$Id: ComponentType.java,v 1.29 2005/03/16 04:45:25 oneovthafew Exp $
package org.hibernate.type;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.Element;
import org.dom4j.Node;
import org.hibernate.EntityMode;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.Cascades;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.tuple.ComponentTuplizer;
import org.hibernate.tuple.TuplizerLookup;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.StringHelper;

/**
 * Handles "component" mappings
 *
 * @author Gavin King
 */
public class ComponentType extends AbstractType implements AbstractComponentType {

	private final Type[] propertyTypes;
	private final String[] propertyNames;
	private final boolean[] propertyNullability;
	protected final int propertySpan;
	private final Cascades.CascadeStyle[] cascade;
	private final FetchMode[] joinedFetch;
	private final boolean isKey;
	
	protected TuplizerLookup tuplizers;

	public int[] sqlTypes(Mapping mapping) throws MappingException {
		//Not called at runtime so doesn't matter if its slow :)
		int[] sqlTypes = new int[getColumnSpan( mapping )];
		int n = 0;
		for ( int i = 0; i < propertySpan; i++ ) {
			int[] subtypes = propertyTypes[i].sqlTypes( mapping );
			for ( int j = 0; j < subtypes.length; j++ ) {
				sqlTypes[n++] = subtypes[j];
			}
		}
		return sqlTypes;
	}

	public int getColumnSpan(Mapping mapping) throws MappingException {
		int span = 0;
		for ( int i = 0; i < propertySpan; i++ ) {
			span += propertyTypes[i].getColumnSpan( mapping );
		}
		return span;
	}

	public ComponentType(
			final String[] propertyNames,
			final Type[] propertyTypes,
			final boolean[] nullabilities,
			final FetchMode[] joinedFetch,
			final Cascades.CascadeStyle[] cascade,
			final boolean key,
			final TuplizerLookup tuplizers) 
	throws MappingException {

		this.propertyTypes = propertyTypes;
		this.propertyNullability = nullabilities;
		this.propertyNames = propertyNames;
		this.cascade = cascade;
		this.joinedFetch = joinedFetch;
		isKey = key;
		propertySpan = propertyTypes.length;
		this.tuplizers = tuplizers;
	}

	public final boolean isComponentType() {
		return true;
	}

	public Class getReturnedClass() {
		return tuplizers.getTuplizer(EntityMode.POJO).getMappedClass(); //TODO
	}

	public boolean isSame(Object x, Object y, EntityMode entityMode) throws HibernateException {
		if ( x == y ) return true;
		if ( x == null || y == null ) return false;
		Object[] xvalues = getPropertyValues(x, entityMode);
		Object[] yvalues = getPropertyValues(y, entityMode);
		for ( int i = 0; i < propertySpan; i++ ) {
			if ( !propertyTypes[i].isSame( xvalues[i], yvalues[i], entityMode ) ) {
				return false;
			}
		}
		return true;
	}

	public boolean isEqual(Object x, Object y, EntityMode entityMode) 
	throws HibernateException {
		if ( x == y ) return true;
		if ( x == null || y == null ) return false;
		Object[] xvalues = getPropertyValues(x, entityMode);
		Object[] yvalues = getPropertyValues(y, entityMode);
		for ( int i = 0; i < propertySpan; i++ ) {
			if ( !propertyTypes[i].isEqual( xvalues[i], yvalues[i], entityMode ) ) return false;
		}
		return true;
	}

	public boolean isEqual(Object x, Object y, EntityMode entityMode, SessionFactoryImplementor factory) 
	throws HibernateException {
		if ( x == y ) return true;
		if ( x == null || y == null ) return false;
		Object[] xvalues = getPropertyValues(x, entityMode);
		Object[] yvalues = getPropertyValues(y, entityMode);
		for ( int i = 0; i < propertySpan; i++ ) {
			if ( !propertyTypes[i].isEqual( xvalues[i], yvalues[i], entityMode, factory ) ) return false;
		}
		return true;
	}

	public int compare(Object x, Object y, EntityMode entityMode) {
		if ( x == y ) return 0;
		Object[] xvalues = getPropertyValues(x, entityMode);
		Object[] yvalues = getPropertyValues(y, entityMode);
		for ( int i = 0; i < propertySpan; i++ ) {
			int propertyCompare = propertyTypes[i].compare( xvalues[i], yvalues[i], entityMode );
			if ( propertyCompare != 0 ) return propertyCompare;
		}
		return 0;
	}
	
	public boolean isMethodOf(Method method) {
		return false;
	}

	public int getHashCode(Object x, EntityMode entityMode) {
		int result = 17;
		Object[] values = getPropertyValues(x, entityMode);
		for ( int i = 0; i < propertySpan; i++ ) {
			Object y = values[i];
			result *= 37;
			if ( y != null ) result += propertyTypes[i].getHashCode( y, entityMode );
		}
		return result;
	}

	public int getHashCode(Object x, EntityMode entityMode, SessionFactoryImplementor factory) {
		int result = 17;
		Object[] values = getPropertyValues(x, entityMode);
		for ( int i = 0; i < propertySpan; i++ ) {
			Object y = values[i];
			result *= 37;
			if ( y != null ) result += propertyTypes[i].getHashCode( y, entityMode, factory );
		}
		return result;
	}

	public boolean isDirty(Object x, Object y, SessionImplementor session) 
	throws HibernateException {
		if ( x == y ) return false;
		if ( x == null || y == null ) return true;
		EntityMode entityMode = session.getEntityMode();
		Object[] xvalues = getPropertyValues(x, entityMode);
		Object[] yvalues = getPropertyValues(y, entityMode);
		for ( int i = 0; i < xvalues.length; i++ ) {
			if ( propertyTypes[i].isDirty( xvalues[i], yvalues[i], session ) ) return true;
		}
		return false;
	}

	public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException, SQLException {
		return resolve( hydrate( rs, names, session, owner ), session, owner );
	}

	public void nullSafeSet(PreparedStatement st, Object value, int begin, SessionImplementor session)
	throws HibernateException, SQLException {

		Object[] subvalues = nullSafeGetValues( value, session.getEntityMode() );

		for ( int i = 0; i < propertySpan; i++ ) {
			propertyTypes[i].nullSafeSet( st, subvalues[i], begin, session );
			begin += propertyTypes[i].getColumnSpan( session.getFactory() );
		}
	}

	public void nullSafeSet(
			PreparedStatement st, 
			Object value, 
			int begin, 
			boolean[] settable, 
			SessionImplementor session)
	throws HibernateException, SQLException {

		Object[] subvalues = nullSafeGetValues( value, session.getEntityMode() );
		
		int loc = 0;
		for ( int i = 0; i < propertySpan; i++ ) {
			int len = propertyTypes[i].getColumnSpan( session.getFactory() );
			if (len==1) {
				if ( settable[loc] ) {
					propertyTypes[i].nullSafeSet( st, subvalues[i], begin, session );
					begin++;
				}
			}
			else {
				boolean[] subsettable = new boolean[len];
				System.arraycopy(settable, loc, subsettable, 0, len);
				propertyTypes[i].nullSafeSet( st, subvalues[i], begin, subsettable, session );
				begin += ArrayHelper.countTrue(subsettable);
			}
			loc += len;
		}
	}

	private Object[] nullSafeGetValues(Object value, EntityMode entityMode) throws HibernateException {
		if ( value == null ) {
			return new Object[propertySpan];
		}
		else {
			return getPropertyValues( value, entityMode );
		}
	}

	public Object nullSafeGet(ResultSet rs, String name, SessionImplementor session, Object owner)
			throws HibernateException, SQLException {

		return nullSafeGet( rs, new String[]{name}, session, owner );
	}

	public Object getPropertyValue(Object component, int i, SessionImplementor session)
			throws HibernateException {
		return tuplizers.getTuplizer( session.getEntityMode() ).getPropertyValue( component, i );
	}

	public Object getPropertyValue(Object component, int i)
			throws HibernateException {
		return getPropertyValue(component, i, null);
	}

	public Object[] getPropertyValues(Object component, SessionImplementor session)
			throws HibernateException {
		return getPropertyValues( component, session.getEntityMode() );
	}

	public Object[] getPropertyValues(Object component, EntityMode entityMode) 
	throws HibernateException {
		return tuplizers.getTuplizer(entityMode).getPropertyValues(component);
	}

	public void setPropertyValues(Object component, Object[] values, EntityMode entityMode) 
	throws HibernateException {
		tuplizers.getTuplizer(entityMode).setPropertyValues(component, values);
	}

	public Type[] getSubtypes() {
		return propertyTypes;
	}

	public String getName() {
		return "component" + ArrayHelper.toString(propertyNames);
	}

	public String toLoggableString(Object value, SessionFactoryImplementor factory)
			throws HibernateException {
		if ( value == null ) return "null";
		Map result = new HashMap();
		EntityMode entityMode = tuplizers.guessEntityMode(value);
		if (entityMode==null) {
			throw new ClassCastException( value.getClass().getName() );
		}
		Object[] values = getPropertyValues( value, entityMode );
		for ( int i = 0; i < propertyTypes.length; i++ ) {
			result.put( propertyNames[i], propertyTypes[i].toLoggableString( values[i], factory ) );
		}
		return StringHelper.unqualify( getName() ) + result.toString();
	}

	public String[] getPropertyNames() {
		return propertyNames;
	}

	public Object deepCopy(Object component, EntityMode entityMode, SessionFactoryImplementor factory) 
	throws HibernateException {
		if ( component == null ) return null;

		Object[] values = getPropertyValues( component, entityMode );
		for ( int i = 0; i < propertySpan; i++ ) {
			values[i] = propertyTypes[i].deepCopy( values[i], entityMode, factory );
		}

		Object result = instantiate(entityMode);
		setPropertyValues( result, values, entityMode );

		//not absolutely necessary, but helps for some
		//equals()/hashCode() implementations
		ComponentTuplizer ct = (ComponentTuplizer) tuplizers.getTuplizer(entityMode);
		if ( ct.hasParentProperty() ) {
			ct.setParent( result, ct.getParent(component), factory );
		}

		return result;
	}

	public Object replace(
			Object original,
			Object target,
			SessionImplementor session,
			Object owner, 
			Map copyCache)
		throws HibernateException {

		if ( original == null ) return null;
		if ( original == target ) return target;

		final Object result = target == null ?
				instantiate( owner, session ) :
				target;
				
		final EntityMode entityMode = session.getEntityMode();
		Object[] values = TypeFactory.replace( getPropertyValues( original, entityMode ),
				getPropertyValues( result, entityMode ),
				propertyTypes,
				session,
				owner, copyCache );

		setPropertyValues( result, values, entityMode );
		return result;
	}

	/**
	 * This method does not populate the component parent
	 */
	private Object instantiate(EntityMode entityMode) throws HibernateException {
		return tuplizers.getTuplizer(entityMode).instantiate();
	}

	public Object instantiate(Object parent, SessionImplementor session)
	throws HibernateException {
		
		Object result = instantiate( session.getEntityMode() );

		ComponentTuplizer ct = (ComponentTuplizer) tuplizers.getTuplizer( session.getEntityMode() );
		if ( ct.hasParentProperty() && parent != null ) {
			ct.setParent( 
					result, 
					session.getPersistenceContext().proxyFor( parent ), 
					session.getFactory() 
				);
		}
		
		return result;
	}

	public Cascades.CascadeStyle getCascadeStyle(int i) {
		return cascade[i];
	}

	public boolean isMutable() {
		return true;
	}

	public Serializable disassemble(Object value, SessionImplementor session, Object owner)
			throws HibernateException {

		if ( value == null ) {
			return null;
		}
		else {
			Object[] values = getPropertyValues( value, session.getEntityMode() );
			for ( int i = 0; i < propertyTypes.length; i++ ) {
				values[i] = propertyTypes[i].disassemble( values[i], session, owner );
			}
			return values;
		}
	}

	public Object assemble(Serializable object, SessionImplementor session, Object owner)
			throws HibernateException {

		if ( object == null ) {
			return null;
		}
		else {
			Object[] values = (Object[]) object;
			Object[] assembled = new Object[values.length];
			for ( int i = 0; i < propertyTypes.length; i++ ) {
				assembled[i] = propertyTypes[i].assemble( ( Serializable ) values[i], session, owner );
			}
			Object result = instantiate( owner, session );
			setPropertyValues( result, assembled, session.getEntityMode() );
			return result;
		}
	}

	public FetchMode getFetchMode(int i) {
		return joinedFetch[i];
	}

	public Object hydrate(
			final ResultSet rs,
			final String[] names,
			final SessionImplementor session,
			final Object owner)
	throws HibernateException, SQLException {

		int begin = 0;
		boolean notNull = false;
		Object[] values = new Object[propertySpan];
		for ( int i = 0; i < propertySpan; i++ ) {
			int length = propertyTypes[i].getColumnSpan( session.getFactory() );
			String[] range = ArrayHelper.slice( names, begin, length ); //cache this
			Object val = propertyTypes[i].hydrate( rs, range, session, owner );
			if ( val == null ) {
				if (isKey) return null; //different nullability rules for pk/fk
			}
			else {
				notNull = true;
			}
			values[i] = val;
			begin += length;
		}

		return notNull ? values : null;
	}

	public Object resolve(Object value, SessionImplementor session, Object owner)
		throws HibernateException {

		if ( value != null ) {
			Object result = instantiate( owner, session );
			Object[] values = ( Object[] ) value;
			Object[] resolvedValues = new Object[values.length]; //only really need new array during semiresolve!
			for ( int i = 0; i < values.length; i++ ) {
				resolvedValues[i] = propertyTypes[i].resolve( values[i], session, owner );
			}
			setPropertyValues( result, resolvedValues, session.getEntityMode() );
			return result;
		}
		else {
			return null;
		}
	}

	public Object semiResolve(Object value, SessionImplementor session, Object owner)
			throws HibernateException {
		//note that this implementation is kinda broken
		//for components with many-to-one associations
		return resolve( value, session, owner );
	}

	public boolean isModified(Object old, Object current, SessionImplementor session)
			throws HibernateException {

		if ( current == null ) return old != null;
		if ( old == null ) return current != null;
		Object[] currentValues = getPropertyValues( current, session );
		Object[] oldValues = ( Object[] ) old;
		for ( int i = 0; i < currentValues.length; i++ ) {
			if ( propertyTypes[i].isModified( oldValues[i], currentValues[i], session ) ) {
				return true;
			}
		}
		return false;

	}

	public boolean[] getPropertyNullability() {
		return propertyNullability;
	}

	public boolean isXMLElement() {
		return true;
	}

	public Object fromXMLNode(Node xml, Mapping factory) throws HibernateException {
		return xml;
	}

	public void setToXMLNode(Node node, Object value, SessionFactoryImplementor factory) throws HibernateException {
		replaceNode( node, (Element) value );
	}

}
