//$Id: EntityType.java,v 1.39 2005/02/21 02:46:41 oneovthafew Exp $
package org.hibernate.type;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.dom4j.Element;
import org.dom4j.Node;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.EntityMode;
import org.hibernate.engine.EntityUniqueKey;
import org.hibernate.engine.ForeignKeys;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.util.ReflectHelper;

/**
 * A reference to an entity class
 * @author Gavin King
 */
public abstract class EntityType extends AbstractType implements AssociationType {

	private final String associatedEntityName;
	protected final String uniqueKeyPropertyName;
	protected final boolean isEmbeddedInXML;

	public final boolean isEntityType() {
		return true;
	}

	public final String getAssociatedEntityName() {
		return associatedEntityName;
	}

	public final boolean isSame(Object x, Object y, EntityMode entityMode) {
		return x==y;
	}
	
	public int compare(Object x, Object y, EntityMode entityMode) {
		return 0; //TODO: entities CAN be compared, by PK, fix this!
	}

	protected EntityType(String entityName, String uniqueKeyPropertyName, boolean isEmbeddedInXML) {
		this.associatedEntityName = entityName;
		this.uniqueKeyPropertyName = uniqueKeyPropertyName;
		this.isEmbeddedInXML = isEmbeddedInXML;
	}

	public Object nullSafeGet(ResultSet rs, String name, SessionImplementor session, Object owner) 
	throws HibernateException, SQLException {
		return nullSafeGet( rs, new String[] {name}, session, owner );
	}

	/**
	 * This returns the wrong class for an entity with a proxy. Theoretically
	 * it should return the proxy class, but it doesn't.
	 */
	public final Class getReturnedClass() {
		try {
			return ReflectHelper.classForName(associatedEntityName);
		}
		catch (ClassNotFoundException cnfe) {
			return java.util.Map.class;
		}
	}

	/*protected final Object getActualIdentifier(Object value, SessionImplementor session) throws HibernateException {
		return session.getEntityIdentifierIfNotUnsaved(value); //tolerates nulls
	}*/

	protected final Object getIdentifier(Object value, SessionImplementor session) 
	throws HibernateException {

		if ( isNotEmbedded(session) ) return value;
		
		if ( isReferenceToPrimaryKey() ) {
			return ForeignKeys.getEntityIdentifierIfNotUnsaved(associatedEntityName, value, session); //tolerates nulls
		}
		else if (value==null) {
			return null;
		}
		else {
			return session.getFactory()
			        .getEntityPersister( getAssociatedEntityName() )
			        .getPropertyValue( value, uniqueKeyPropertyName, session.getEntityMode() );
		}
	}

	protected boolean isNotEmbedded(SessionImplementor session) {
		return !isEmbeddedInXML && session.getEntityMode()==EntityMode.DOM4J;
	}

	/**
	 * Get the identifier value of an instance or proxy
	 */
	private static Serializable getIdentifier(Object object, EntityPersister persister, EntityMode entityMode)
	throws HibernateException {
		if (object instanceof HibernateProxy) {
			HibernateProxy proxy = (HibernateProxy) object;
			LazyInitializer li = proxy.getHibernateLazyInitializer();
			return li.getIdentifier();
		}
		else {
			return persister.getIdentifier( object, entityMode );
		}
	}	
	
	public String toLoggableString(Object value, SessionFactoryImplementor factory) 
	throws HibernateException {

		if (value==null) return "null";
		
		EntityPersister persister = factory.getEntityPersister(associatedEntityName);
		StringBuffer result = new StringBuffer()
			.append(associatedEntityName);
		
		if ( persister.hasIdentifierProperty() ) {
			//TODO: use of a guess here is bad...
			final EntityMode entityMode = persister.guessEntityMode(value);
			final Serializable id;
			if (entityMode==null) {
				if ( isEmbeddedInXML ) {
					throw new ClassCastException( value.getClass().getName() );
				}
				id = (Serializable) value;
			}
			else {
				id = getIdentifier( value, persister, entityMode );
			}
			
			result.append('#')
				.append( persister.getIdentifierType().toLoggableString(id, factory) );
		}
		
		return result.toString();
	}
	
	/*public String toXMLString(Object value, SessionFactoryImplementor factory) throws HibernateException {
		if (isEmbeddedInXML) throw new UnsupportedOperationException("entity references cannot be stringified");
		if (factory==null) throw new AssertionFailure("null factory passed to toString");
		return getIdentifierType(factory).toXMLString(value, factory);
	}

	public Object fromXMLString(String xml, Mapping factory) throws HibernateException {
		if (isEmbeddedInXML) throw new UnsupportedOperationException("entity references cannot be stringified");
		if (factory==null) throw new AssertionFailure("null factory passed to fromString");
		return getIdentifierType(factory).fromXMLString(xml, factory);
	}*/

	public String getName() { return associatedEntityName; }

	public Object deepCopy(Object value, EntityMode entityMode, SessionFactoryImplementor factory) {
		return value; //special case ... this is the leaf of the containment graph, even though not immutable
	}

	public boolean isMutable() {
		return false;
	}

	public abstract boolean isOneToOne();

	public Object replace(Object original, Object target, SessionImplementor session, Object owner, Map copyCache)
	throws HibernateException {
		if (original==null) return null;
		if (original==target) return target;
		Object cached = copyCache.get(original);
		if (cached!=null) {
			return cached;
		}
		else {
			//TODO: can this ever get called????
			Object id = getIdentifier(original, session);
			if (id==null) throw new AssertionFailure("cannot copy a reference to an object with a null id");
			id = getIdentifierOrUniqueKeyType( session.getFactory() ).replace(id, null, session, owner, copyCache);
			return resolve(id, session, owner);
		}
	}

	public boolean isAssociationType() {
		return true;
	}

	public final Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner)
	throws HibernateException, SQLException {
		return resolve( hydrate(rs, names, session, owner), session, owner );
	}

	public boolean isDirty(Object old, Object current, SessionImplementor session) 
	throws HibernateException {

		if ( isSame( old, current, session.getEntityMode() ) ) return false;

		Object oldid = getIdentifier(old, session);
		Object newid = getIdentifier(current, session);
		return !getIdentifierType(session).isSame( oldid, newid, session.getEntityMode() );

	}

	public Joinable getAssociatedJoinable(SessionFactoryImplementor factory) 
	throws MappingException {
		return (Joinable) factory.getEntityPersister(associatedEntityName);
	}
	
	Type getIdentifierType(Mapping factory) {
		return factory.getIdentifierType( getAssociatedEntityName() );
	}

	Type getIdentifierType(SessionImplementor session) throws MappingException {
		return getIdentifierType( session.getFactory() );
	}

	public final Type getIdentifierOrUniqueKeyType(Mapping factory) 
	throws MappingException {
		if ( isReferenceToPrimaryKey() ) {
			return getIdentifierType(factory);
		}
		else {
			return factory.getPropertyType( getAssociatedEntityName(), uniqueKeyPropertyName );
		}
	}

	public final String getIdentifierOrUniqueKeyPropertyName(Mapping factory)
	throws MappingException {
		if ( isReferenceToPrimaryKey() ) {
			return factory.getIdentifierPropertyName( getAssociatedEntityName() );
		}
		else {
			return uniqueKeyPropertyName;
		}
	}

	/**
	 * Resolve an identifier
	 */
	protected abstract Object resolveIdentifier(Serializable id, SessionImplementor session) 
	throws HibernateException;

	/**
	 * Resolve an identifier or unique key value
	 */
	public Object resolve(Object value, SessionImplementor session, Object owner)
	throws HibernateException {
		
		if ( isNotEmbedded(session) ) {
			return value;
		}

		if (value==null) {
			return null;
		}
		else {
			if ( isReferenceToPrimaryKey() ) {
				return resolveIdentifier( (Serializable) value, session );
			}
			else {
				return loadByUniqueKey( getAssociatedEntityName(), uniqueKeyPropertyName, value, session );
			}
		}
	}

	public String getAssociatedEntityName(SessionFactoryImplementor factory) {
		return getAssociatedEntityName();
	}

	/**
	 * Does this association foreign key reference the
	 * primary key of the other table?
	 */
	public boolean isReferenceToPrimaryKey() {
		return uniqueKeyPropertyName==null;
	}
	
	public String getRHSUniqueKeyPropertyName() {
		return uniqueKeyPropertyName;
	}

	public String toString() {
		return getClass().getName() + '(' + getAssociatedEntityName() + ')';
	}

	/**
	 * Load an instance by a unique key that is not the primary key.
	 */
	public Object loadByUniqueKey(
			String entityName, 
			String uniqueKeyPropertyName, 
			Object key, 
			SessionImplementor session)
	throws HibernateException {
		
		final SessionFactoryImplementor factory = session.getFactory();
		
		UniqueKeyLoadable persister = (UniqueKeyLoadable) factory.getEntityPersister(entityName);
			
		//TODO: implement caching?! proxies?!
		
		EntityUniqueKey euk = new EntityUniqueKey(
				entityName, 
				uniqueKeyPropertyName, 
				key, 
				getIdentifierOrUniqueKeyType( factory ),
				session.getEntityMode()
		);
		
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		Object result = persistenceContext.getEntity(euk);
		if ( result==null && !persistenceContext.isNonExistant(euk) ) {
			result = persister.loadByUniqueKey(uniqueKeyPropertyName, key, session);
		}
		return result==null ? null : persistenceContext.proxyFor(result);
		
	}

	public String getLHSPropertyName() {
		return null;
	}

	public String getOnCondition(String alias, SessionFactoryImplementor factory, Map enabledFilters) 
	throws MappingException {
		if ( isReferenceToPrimaryKey() ) { //TODO: this is a bit arbitrary, expose a switch to the user?
			return "";
		}
		else {
			return getAssociatedJoinable(factory).filterFragment(alias, enabledFilters);
		}
	}
	
	public int getHashCode(Object x, EntityMode entityMode, SessionFactoryImplementor factory) {
		EntityPersister persister = factory.getEntityPersister(associatedEntityName);
		if ( !persister.hasIdentifierPropertyOrEmbeddedCompositeIdentifier() ) {
			return super.getHashCode(x, entityMode);
		}
		
		if (x instanceof HibernateProxy) {
			return ( (HibernateProxy) x ).getHibernateLazyInitializer().getIdentifier().hashCode();
		}
		
		Serializable id = persister.getIdentifier(x, entityMode);
		return persister.getIdentifierType().getHashCode(id, entityMode, factory);
	}
	
	public boolean isEqual(Object x, Object y, EntityMode entityMode, SessionFactoryImplementor factory) {
		EntityPersister persister = factory.getEntityPersister(associatedEntityName);
		if ( !persister.hasIdentifierPropertyOrEmbeddedCompositeIdentifier() ) {
			return super.isEqual(x, y, entityMode);
		}
		
		Serializable xid;
		if (x instanceof HibernateProxy) {
			xid = ( (HibernateProxy) x ).getHibernateLazyInitializer().getIdentifier();
		}
		else {
			xid = persister.getIdentifier(x, entityMode);
		}
		
		Serializable yid;
		if (y instanceof HibernateProxy) {
			yid = ( (HibernateProxy) y ).getHibernateLazyInitializer().getIdentifier();
		}
		else {
			yid = persister.getIdentifier(y, entityMode);
		}
		
		return persister.getIdentifierType().isEqual(xid, yid, entityMode, factory);
	}

	public boolean isXMLElement() {
		return isEmbeddedInXML;
	}

	public Object fromXMLNode(Node xml, Mapping factory) throws HibernateException {
		if ( !isEmbeddedInXML ) {
			return getIdentifierType(factory).fromXMLNode(xml, factory);
		}
		else {
			return xml;
		}
	}

	public void setToXMLNode(Node node, Object value, SessionFactoryImplementor factory) 
	throws HibernateException {
		if ( !isEmbeddedInXML ) {
			getIdentifierType(factory).setToXMLNode(node, value, factory);
		}
		else {
			replaceNode( node, (Element) value );
		}
	}

}
