// $Id: CollectionType.java,v 1.26 2005/03/16 04:45:25 oneovthafew Exp $
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Element;
import org.dom4j.Node;
import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.MarkerObject;

/**
 * A type that handles Hibernate <tt>PersistentCollection</tt>s (including arrays).
 * 
 * @author Gavin King
 */
public abstract class CollectionType extends AbstractType implements AssociationType {

	private static final Object NOT_NULL_COLLECTION = new MarkerObject( "NOT NULL COLLECTION" );
	public static final Object UNFETCHED_COLLECTION = new MarkerObject( "UNFETCHED COLLECTION" );

	private final String role;
	private final String foreignKeyPropertyName;
	private final boolean isEmbeddedInXML;

	public CollectionType(String role, String foreignKeyPropertyName, boolean isEmbeddedInXML) {
		this.role = role;
		this.foreignKeyPropertyName = foreignKeyPropertyName;
		this.isEmbeddedInXML = isEmbeddedInXML;
	}

	public String getRole() {
		return role;
	}

	public Object indexOf(Object collection, Object element) {
		throw new UnsupportedOperationException( "generic collections don't have indexes" );
	}

	public boolean contains(Object collection, Object childObject, CollectionPersister persister,
			SessionImplementor session) {
		// we do not have to worry about queued additions to uninitialized
		// collections, since they can only occur for inverse collections!
		Iterator elems = getElementsIterator( collection, session );
		while ( elems.hasNext() ) {
			Object element = elems.next();
			// worrying about proxies is perhaps a little bit of overkill here...
			if ( element instanceof HibernateProxy ) {
				LazyInitializer li = ( (HibernateProxy) element ).getHibernateLazyInitializer();
				if ( !li.isUninitialized() ) element = li.getImplementation();
			}
			if ( element == childObject ) return true;
		}
		return false;
	}

	public boolean isCollectionType() {
		return true;
	}

	public final boolean isEqual(Object x, Object y, EntityMode entityMode) {
		return x == y
			|| ( x instanceof PersistentCollection && ( (PersistentCollection) x ).isWrapper( y ) )
			|| ( y instanceof PersistentCollection && ( (PersistentCollection) y ).isWrapper( x ) );
	}

	public int compare(Object x, Object y, EntityMode entityMode) {
		return 0; // collections cannot be compared
	}

	public int getHashCode(Object x, EntityMode entityMode) {
		throw new UnsupportedOperationException( "cannot perform lookups on collections" );
	}

	/**
	 * Instantiate an uninitialized collection wrapper or holder. Callers MUST add the holder to the
	 * persistence context!
	 */
	public abstract PersistentCollection instantiate(SessionImplementor session,
			CollectionPersister persister, Serializable key) throws HibernateException;

	public Object nullSafeGet(ResultSet rs, String name, SessionImplementor session, Object owner)
			throws HibernateException, SQLException {
		return nullSafeGet( rs, new String[] { name }, session, owner );
	}

	public Object nullSafeGet(ResultSet rs, String[] name, SessionImplementor session, Object owner)
			throws HibernateException, SQLException {
		return resolve( null, session, owner );
	}

	public final void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable,
			SessionImplementor session) throws HibernateException, SQLException {
		//NOOP
	}

	public void nullSafeSet(PreparedStatement st, Object value, int index,
			SessionImplementor session) throws HibernateException, SQLException {
	}

	public int[] sqlTypes(Mapping session) throws MappingException {
		return ArrayHelper.EMPTY_INT_ARRAY;
	}

	public int getColumnSpan(Mapping session) throws MappingException {
		return 0;
	}

	public String toLoggableString(Object value, SessionFactoryImplementor factory)
			throws HibernateException {

		if ( value == null ) return "null";
		
		if ( Hibernate.isInitialized( value ) ) {
			if ( getReturnedClass().isInstance(value) ) {
				List list = new ArrayList();
				Type elemType = getElementType( factory );
				Iterator iter = getElementsIterator( value );
				while ( iter.hasNext() ) {
					list.add( elemType.toLoggableString( iter.next(), factory ) );
				}
				return list.toString();
			}
			else {
				// for DOM4J "collections" only
				return ( (Element) value ).asXML(); //TODO: it would be better if this was done at the higher level by Printer
			}
		}
		else {
			return "<uninitialized>";
		}
		
	}

	public Object deepCopy(Object value, EntityMode entityMode, SessionFactoryImplementor factory)
			throws HibernateException {
		return value;
	}

	public String getName() {
		return getReturnedClass().getName() + '(' + getRole() + ')';
	}

	/**
	 * Get an iterator over the element set of the collection, which may not yet be wrapped
	 */
	public Iterator getElementsIterator(Object collection, SessionImplementor session) {
		if ( session.getEntityMode()==EntityMode.DOM4J ) {
			final SessionFactoryImplementor factory = session.getFactory();
			final CollectionPersister persister = factory.getCollectionPersister( getRole() );
			final Type elementType = persister.getElementType();
			
			List elements = ( (Element) collection ).elements( persister.getElementNodeName() );
			ArrayList results = new ArrayList();
			for ( int i=0; i<elements.size(); i++ ) {
				Element value = (Element) elements.get(i);
				results.add( elementType.fromXMLNode( value, factory ) );
			}
			return results.iterator();
		}
		else {
			return getElementsIterator(collection);
		}
	}

	/**
	 * Get an iterator over the element set of the collection in POJO mode
	 */
	protected Iterator getElementsIterator(Object collection) {
		return ( (Collection) collection ).iterator();
	}

	public boolean isMutable() {
		return false;
	}

	public Serializable disassemble(Object value, SessionImplementor session, Object owner)
			throws HibernateException {
		//remember the uk value
		
		//This solution would allow us to eliminate the owner arg to disassemble(), but
		//what if the collection was null, and then later had elements added? seems unsafe
		//session.getPersistenceContext().getCollectionEntry( (PersistentCollection) value ).getKey();
		
		final Serializable key = getKeyOfOwner(owner, session);
		if (key==null) {
			return null;
		}
		else {
			return getPersister(session)
				.getKeyType()
				.disassemble( key, session, owner );
		}
	}

	public Object assemble(Serializable cached, SessionImplementor session, Object owner)
			throws HibernateException {
		//we must use the "remembered" uk value, since it is 
		//not available from the EntityEntry during assembly
		if (cached==null) {
			return null;
		}
		else {
			final Serializable key = (Serializable) getPersister(session)
				.getKeyType()
				.assemble( cached, session, owner);
			return resolveKey( key, session );
		}
	}

	/**
	 * Is the owning entity versioned?
	 */
	private boolean isOwnerVersioned(SessionImplementor session) throws MappingException {
		return getPersister( session )
			.getOwnerEntityPersister()
			.isVersioned();
	}

	private CollectionPersister getPersister(SessionImplementor session) {
		return session
			.getFactory()
			.getCollectionPersister( role );
	}

	public boolean isDirty(Object old, Object current, SessionImplementor session)
			throws HibernateException {

		// collections don't dirty an unversioned parent entity

		// TODO: I don't really like this implementation; it would be better if
		// this was handled by searchForDirtyCollections()
		return isOwnerVersioned( session ) && super.isDirty( old, current, session );
		// return false;

	}

	/**
	 * Wrap the naked collection instance in a wrapper, or instantiate a holder. Callers MUST add
	 * the holder to the persistence context!
	 */
	public abstract PersistentCollection wrap(SessionImplementor session, Object collection);

	/**
	 * Note: return true because this type is castable to <tt>AssociationType</tt>. Not because
	 * all collections are associations.
	 */
	public boolean isAssociationType() {
		return true;
	}

	public ForeignKeyDirection getForeignKeyDirection() {
		return ForeignKeyDirection.FOREIGN_KEY_TO_PARENT;
	}

	/**
	 * Get the key value from the owning entity instance, usually the identifier, but might be some
	 * other unique key, in the case of property-ref
	 */
	public Serializable getKeyOfOwner(Object owner, SessionImplementor session) {
		
		EntityEntry e = session.getPersistenceContext().getEntry( owner );
		if ( e == null ) return null; // This just handles a particular case of component
									  // projection, perhaps get rid of it and throw an exception
		
		if ( foreignKeyPropertyName == null ) {
			return e.getId();
		}
		else {
			// TODO: at the point where we are resolving collection references, we don't
			// know if the uk value has been resolved (depends if it was earlier or
			// later in the mapping document) - now, we could try and use e.getStatus()
			// to decide to semiResolve(), trouble is that initializeEntity() reuses
			// the same array for resolved and hydrated values
			Object id = e.getLoadedValue( foreignKeyPropertyName );

			// NOTE VERY HACKISH WORKAROUND!!
			Type keyType = getPersister( session ).getKeyType();
			if ( !keyType.getReturnedClass().isInstance( id ) ) {
				id = (Serializable) keyType.semiResolve(
					e.getLoadedValue( foreignKeyPropertyName ),
					session,
					owner );
			}

			return (Serializable) id;
		}
	}

	public Object hydrate(ResultSet rs, String[] name, SessionImplementor session, Object owner) {
		// can't just return null here, since that would
		// cause an owning component to become null
		return NOT_NULL_COLLECTION;
	}

	public Object resolve(Object value, SessionImplementor session, Object owner)
			throws HibernateException {
		
		return resolveKey( getKeyOfOwner( owner, session ), session );
	}
	
	private Object resolveKey(Serializable key, SessionImplementor session) {
		// if (key==null) throw new AssertionFailure("owner identifier unknown when re-assembling
		// collection reference");
		return key == null ? null : // TODO: can this case really occur??
			getCollection( key, session );
	}

	public Object semiResolve(Object value, SessionImplementor session, Object owner)
			throws HibernateException {
		throw new UnsupportedOperationException(
			"collection mappings may not form part of a property-ref" );
	}

	public boolean isArrayType() {
		return false;
	}

	public boolean useLHSPrimaryKey() {
		return foreignKeyPropertyName == null;
	}

	public String getRHSUniqueKeyPropertyName() {
		return null;
	}

	public Joinable getAssociatedJoinable(SessionFactoryImplementor factory)
			throws MappingException {
		return (Joinable) factory.getCollectionPersister( role );
	}

	public boolean isModified(Object old, Object current, SessionImplementor session) throws HibernateException {
		return false;
	}

	public String getAssociatedEntityName(SessionFactoryImplementor factory)
			throws MappingException {
		try {
			
			QueryableCollection collectionPersister = (QueryableCollection) factory
				.getCollectionPersister( role );
			
			if ( !collectionPersister.getElementType().isEntityType() ) {
				throw new MappingException( "collection was not an association: "
					+ collectionPersister.getRole() );
			}
			
			return collectionPersister.getElementPersister().getEntityName();
			
		}
		catch (ClassCastException cce) {
			throw new MappingException( "collection role is not queryable " + role );
		}
	}

	/**
	 * Replace the elements of a collection with the elements of another collection
	 */
	public Object replaceElements(Object original, Object target, Object owner, Map copyCache,
			SessionImplementor session) throws HibernateException {

		// TODO: does not work for EntityMode.DOM4J yet!

		java.util.Collection result = (java.util.Collection) target;

		result.clear();

		// copy elements into newly empty target collection
		Type elemType = getElementType( session.getFactory() );
		Iterator iter = ( (java.util.Collection) original ).iterator();
		while ( iter.hasNext() ) {
			result.add( elemType.replace( iter.next(), null, session, owner, copyCache ) );
		}

		return result;

	}

	/**
	 * Instantiate an empty instance of the "underlying" collection (not a wrapper)
	 */
	public abstract Object instantiate(Object original);

	public Object replace(final Object original, final Object target,
			final SessionImplementor session, final Object owner, final Map copyCache)
			throws HibernateException {

		if ( original == null ) return null;
		if ( !Hibernate.isInitialized( original ) ) return target;
		if ( original == target ) return target; // is this really a Good Thing?

		Object result = target == null ? instantiate( original ) : target;
		return replaceElements( original, result, owner, copyCache, session );

	}

	/**
	 * Get the Hibernate type of the collection elements
	 */
	public final Type getElementType(SessionFactoryImplementor factory) throws MappingException {
		return factory.getCollectionPersister( getRole() ).getElementType();
	}

	public String toString() {
		return getClass().getName() + '(' + getRole() + ')';
	}

	public String getOnCondition(String alias, SessionFactoryImplementor factory, Map enabledFilters)
			throws MappingException {
		return getAssociatedJoinable( factory ).filterFragment( alias, enabledFilters );
	}

	/**
	 * instantiate a collection wrapper (called when loading an object)
	 */
	public Object getCollection(final Serializable key, final SessionImplementor session)
			throws HibernateException {

		// note: there cannot possibly be a collection already registered,
		// because this method is called while first loading the entity
		// that references it

		CollectionPersister persister = getPersister( session );
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final EntityMode entityMode = session.getEntityMode();

		PersistentCollection collection = persistenceContext
			.getCollectionLoadContext()
			.getLoadingCollection( persister, key, entityMode );
		
		if ( collection != null ) {
			// the collection is currently being loaded, return the existing wrapper
			//TODO: (big) what if is is not supposed to be embedded in the XML???
		}
		else {

			if (entityMode==EntityMode.DOM4J && !isEmbeddedInXML) {
				return UNFETCHED_COLLECTION;
			}

			// create a new collection wrapper, to be initialized later
			collection = instantiate( session, persister, key );

			persistenceContext.addUninitializedCollection( collection, persister, key, entityMode );

			// some collections are not lazy:
			if ( initializeImmediately( entityMode ) ) {
				session.initializeCollection( collection, false );
			}
			else if ( !persister.isLazy() ) {
				persistenceContext.addNonLazyCollection( collection );
			}

			if ( hasHolder( entityMode ) ) {
				session.getPersistenceContext().addCollectionHolder( collection );
			}
		}

		return collection.getValue();
	}

	public boolean hasHolder(EntityMode entityMode) {
		return entityMode == EntityMode.DOM4J;
	}

	protected boolean initializeImmediately(EntityMode entityMode) {
		return entityMode == EntityMode.DOM4J;
	}

	public String getLHSPropertyName() {
		return foreignKeyPropertyName;
	}

	public boolean isXMLElement() {
		return true;
	}

	public Object fromXMLNode(Node xml, Mapping factory) throws HibernateException {
		return xml;
	}

	public void setToXMLNode(Node node, Object value, SessionFactoryImplementor factory) 
	throws HibernateException {
		if ( !isEmbeddedInXML ) {
			node.detach();
		}
		else {
			replaceNode( node, (Element) value );
		}
	}
	
}
