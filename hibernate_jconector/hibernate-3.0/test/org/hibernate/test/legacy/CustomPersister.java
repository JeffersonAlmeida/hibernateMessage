//$Id: CustomPersister.java,v 1.13 2005/03/17 05:54:36 oneovthafew Exp $
package org.hibernate.test.legacy;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.cache.CacheConcurrencyStrategy;
import org.hibernate.cache.entry.CacheEntryStructure;
import org.hibernate.cache.entry.UnstructuredCacheEntry;
import org.hibernate.engine.Cascades;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.TwoPhaseLoad;
import org.hibernate.event.PostLoadEvent;
import org.hibernate.event.PreLoadEvent;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.UUIDHexGenerator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.QuerySelect;
import org.hibernate.sql.Select;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;
import org.hibernate.util.EqualsHelper;

public class CustomPersister implements EntityPersister {

	private static final Hashtable INSTANCES = new Hashtable();
	private static final IdentifierGenerator GENERATOR = new UUIDHexGenerator();
	
	private SessionFactoryImplementor factory;

	public CustomPersister(
			PersistentClass model, 
			CacheConcurrencyStrategy cache, 
			SessionFactoryImplementor factory, 
			Mapping mapping) {
		this.factory = factory;
	}

	private void checkEntityMode(EntityMode entityMode) {
		if ( EntityMode.POJO != entityMode ) {
			throw new IllegalArgumentException( "Unhandled EntityMode : " + entityMode );
		}
	}

	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	public Class getMappedClass() {
		return Custom.class;
	}

	public void postInstantiate() throws MappingException {}

	public String getEntityName() {
		return Custom.class.getName();
	}
	
	public boolean isSubclassEntityName(String entityName) {
		return Custom.class.getName().equals(entityName);
	}

	public boolean hasProxy() {
		return false;
	}

	public boolean hasCollections() {
		return false;
	}

	public boolean hasCascades() {
		return false;
	}

	public boolean isMutable() {
		return true;
	}
	
	public boolean isSelectBeforeUpdateRequired() {
		return false;
	}

	public boolean isIdentifierAssignedByInsert() {
		return false;
	}

	public Boolean isTransient(Object object, SessionImplementor session) {
		return new Boolean( ( (Custom) object ).id==null );
	}

	public Object[] getPropertyValuesToInsert(Object object, SessionImplementor session)
	throws HibernateException {
		return getPropertyValues( object, session.getEntityMode() );
	}

	public Class getMappedClass(EntityMode entityMode) {
		checkEntityMode( entityMode );
		return Custom.class;
	}

	public boolean implementsLifecycle(EntityMode entityMode) {
		checkEntityMode( entityMode );
		return false;
	}

	public boolean implementsValidatable(EntityMode entityMode) {
		checkEntityMode( entityMode );
		return false;
	}

	public Class getConcreteProxyClass(EntityMode entityMode) {
		checkEntityMode( entityMode );
		return Custom.class;
	}

	public void setPropertyValues(Object object, Object[] values, EntityMode entityMode) throws HibernateException {
		checkEntityMode( entityMode );
		setPropertyValue( object, 0, values[0], entityMode );
	}

	public void setPropertyValue(Object object, int i, Object value, EntityMode entityMode) throws HibernateException {
		checkEntityMode( entityMode );
		( (Custom) object ).name = (String) value;
	}

	public Object[] getPropertyValues(Object object, EntityMode entityMode) throws HibernateException {
		checkEntityMode( entityMode );
		Custom c = (Custom) object;
		return new Object[] { c.name };
	}

	public Object getPropertyValue(Object object, int i, EntityMode entityMode) throws HibernateException {
		checkEntityMode( entityMode );
		return ( (Custom) object ).name;
	}

	public Object getPropertyValue(Object object, String propertyName, EntityMode entityMode) throws HibernateException {
		checkEntityMode( entityMode );
		return ( (Custom) object ).name;
	}

	public Serializable getIdentifier(Object object, EntityMode entityMode) throws HibernateException {
		checkEntityMode( entityMode );
		return ( (Custom) object ).id;
	}

	public void setIdentifier(Object object, Serializable id, EntityMode entityMode) throws HibernateException {
		checkEntityMode( entityMode );
		( (Custom) object ).id = (String) id;
	}

	public Object getVersion(Object object, EntityMode entityMode) throws HibernateException {
		checkEntityMode( entityMode );
		return null;
	}

	public Object instantiate(Serializable id, EntityMode entityMode) throws HibernateException {
		checkEntityMode( entityMode );
		Custom c = new Custom();
		c.id = (String) id;
		return c;
	}

	public boolean isInstance(Object object, EntityMode entityMode) {
		checkEntityMode( entityMode );
		return object instanceof Custom;
	}

	public boolean hasUninitializedLazyProperties(Object object, EntityMode entityMode) {
		checkEntityMode( entityMode );
		return false;
	}

	public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion, EntityMode entityMode) {
		checkEntityMode( entityMode );
		( ( Custom ) entity ).id = ( String ) currentId;
	}

	public EntityPersister getSubclassEntityPersister(Object instance, SessionFactoryImplementor factory, EntityMode entityMode) {
		checkEntityMode( entityMode );
		return this;
	}

	public int[] findDirty(
		Object[] x,
		Object[] y,
		Object owner,
		SessionImplementor session
	) throws HibernateException {
		if ( !EqualsHelper.equals( x[0], y[0] ) ) {
			return new int[] { 0 };
		}
		else {
			return null;
		}
	}

	public int[] findModified(
		Object[] x,
		Object[] y,
		Object owner,
		SessionImplementor session
	) throws HibernateException {
		if ( !EqualsHelper.equals( x[0], y[0] ) ) {
			return new int[] { 0 };
		}
		else {
			return null;
		}
	}

	/**
	 * @see EntityPersister#hasIdentifierProperty()
	 */
	public boolean hasIdentifierProperty() {
		return true;
	}


	/**
	 * @see EntityPersister#isVersioned()
	 */
	public boolean isVersioned() {
		return false;
	}

	/**
	 * @see EntityPersister#getVersionType()
	 */
	public VersionType getVersionType() {
		return null;
	}

	/**
	 * @see EntityPersister#getVersionProperty()
	 */
	public int getVersionProperty() {
		return 0;
	}

	/**
	 * @see EntityPersister#getIdentifierGenerator()
	 */
	public IdentifierGenerator getIdentifierGenerator()
	throws HibernateException {
		return GENERATOR;
	}

	/**
	 * @see EntityPersister#load(Serializable, Object, LockMode, SessionImplementor)
	 */
	public Object load(
		Serializable id,
		Object optionalObject,
		LockMode lockMode,
		SessionImplementor session
	) throws HibernateException {

		// fails when optional object is supplied

		Custom clone = null;
		Custom obj = (Custom) INSTANCES.get(id);
		if (obj!=null) {
			clone = (Custom) obj.clone();
			TwoPhaseLoad.addUninitializedEntity(id, clone, this, LockMode.NONE, session);
			TwoPhaseLoad.postHydrate(this, id, new String[] { obj.name }, null, clone, LockMode.NONE, session);
			TwoPhaseLoad.initializeEntity( clone, false, session, new PreLoadEvent(session), new PostLoadEvent(session) );
		}
		return clone;
	}

	/**
	 * @see EntityPersister#lock(Serializable, Object, Object, LockMode, SessionImplementor)
	 */
	public void lock(
		Serializable id,
		Object version,
		Object object,
		LockMode lockMode,
		SessionImplementor session
	) throws HibernateException {

		throw new UnsupportedOperationException();
	}

	public void insert(
		Serializable id,
		Object[] fields,
		Object object,
		SessionImplementor session
	) throws HibernateException {

		INSTANCES.put(id, ( (Custom) object ).clone() );
	}

	public Serializable insert(Object[] fields, Object object, SessionImplementor session)
	throws HibernateException {

		throw new UnsupportedOperationException();
	}

	public void delete(
		Serializable id,
		Object version,
		Object object,
		SessionImplementor session
	) throws HibernateException {

		INSTANCES.remove(id);
	}

	/**
	 * @see EntityPersister
	 */
	public void update(
		Serializable id,
		Object[] fields,
		int[] dirtyFields,
		boolean hasDirtyCollection,
		Object[] oldFields,
		Object oldVersion,
		Object object,
		Object rowId,
		SessionImplementor session
	) throws HibernateException {

		INSTANCES.put( id, ( (Custom) object ).clone() );

	}

	private static final Type[] TYPES = new Type[] { Hibernate.STRING };
	private static final String[] NAMES = new String[] { "name" };
	private static final boolean[] MUTABILITY = new boolean[] { true };

	/**
	 * @see EntityPersister#getPropertyTypes()
	 */
	public Type[] getPropertyTypes() {
		return TYPES;
	}

	/**
	 * @see EntityPersister#getPropertyNames()
	 */
	public String[] getPropertyNames() {
		return NAMES;
	}

	/**
	 * @see EntityPersister#getPropertyCascadeStyles()
	 */
	public Cascades.CascadeStyle[] getPropertyCascadeStyles() {
		return null;
	}

	/**
	 * @see EntityPersister#getIdentifierType()
	 */
	public Type getIdentifierType() {
		return Hibernate.LONG;
	}

	/**
	 * @see EntityPersister#getIdentifierPropertyName()
	 */
	public String getIdentifierPropertyName() {
		return "id";
	}

	/**
	 * @see EntityPersister#hasCache()
	 */
	public boolean hasCache() {
		return false;
	}

	/**
	 * @see EntityPersister#getCache()
	 */
	public CacheConcurrencyStrategy getCache() {
		return null;
	}

	/**
	 * @see EntityPersister#getRootEntityName()
	 */
	public String getRootEntityName() {
		return "CUSTOMS";
	}

	public Serializable[] getPropertySpaces() {
		return new String[] { "CUSTOMS" };
	}

	public Serializable[] getQuerySpaces() {
		return new String[] { "CUSTOMS" };
	}

	/**
	 * @see EntityPersister#getClassMetadata()
	 */
	public ClassMetadata getClassMetadata() {
		return null;
	}

	public boolean[] getPropertyUpdateability() {
		return MUTABILITY;
	}

	/**
	 * @see EntityPersister#getPropertyInsertability()
	 */
	public boolean[] getPropertyInsertability() {
		return MUTABILITY;
	}

	public boolean hasIdentifierPropertyOrEmbeddedCompositeIdentifier() {
		return true;
	}

	public boolean isBatchLoadable() {
		return false;
	}

	public Type getPropertyType(String propertyName) {
		throw new UnsupportedOperationException();
	}

	public Object getPropertyValue(Object object, String propertyName)
		throws HibernateException {
		throw new UnsupportedOperationException();
	}

	public Object createProxy(Serializable id, SessionImplementor session)
		throws HibernateException {
		throw new UnsupportedOperationException("no proxy for this class");
	}

	public Object getCurrentVersion(
		Serializable id,
		SessionImplementor session)
		throws HibernateException {

		return INSTANCES.get(id);
	}

	public EntityMode guessEntityMode(Object object) {
		if ( !isInstance(object, EntityMode.POJO) ) {
			return null;
		}
		else {
			return EntityMode.POJO;
		}
	}

	public boolean[] getPropertyNullability() {
		return MUTABILITY;
	}

	public boolean isDynamic() {
		return false;
	}

	public boolean isCacheInvalidationRequired() {
		return false;
	}

	public void applyFilters(QuerySelect select, String alias, Map filters) {
	}

	public void applyFilters(Select select, String alias, Map filters) {
	}
	
	
	public void afterInitialize(Object entity, boolean fetched, SessionImplementor session) {
	}

	public void afterReassociate(Object entity, SessionImplementor session) {
	}

	public Object[] getDatabaseSnapshot(Serializable id, SessionImplementor session) 
	throws HibernateException {
		return null;
	}
	
	public boolean[] getPropertyVersionability() {
		return MUTABILITY;
	}

	public CacheEntryStructure getCacheEntryStructure() {
		return new UnstructuredCacheEntry();
	}

	public boolean hasSubselectLoadableCollections() {
		return false;
	}
}






