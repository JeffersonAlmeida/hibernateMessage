//$Id: EntityPersister.java,v 1.4 2005/03/17 05:50:13 oneovthafew Exp $
package org.hibernate.persister.entity;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.EntityMode;
import org.hibernate.cache.CacheConcurrencyStrategy;
import org.hibernate.cache.entry.CacheEntryStructure;
import org.hibernate.engine.Cascades;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;

/**
 * Concrete <tt>EntityPersister</tt>s implement mapping and persistence logic for a particular persistent class.
 * <br><br>
 * Implementors must be threadsafe (preferrably immutable) and must provide a constructor
 * of type
 * <tt>(org.hibernate.map.PersistentClass, org.hibernate.impl.SessionFactoryImplementor)</tt>.
 *
 * @author Gavin King
 */
public interface EntityPersister {

	/**
	 * The property name of the "special" identifier property in HQL
	 */
	public static final String ENTITY_ID = "id";

	/**
	 * Finish the initialization of this object, once all <tt>ClassPersisters</tt> have been instantiated.
	 *
	 * Called only once, before any other method.
	 */
	public void postInstantiate() throws MappingException;

	/**
	 * Return the SessionFactory to which this persister "belongs".
	 *
	 * @return The owning SessionFactory.
	 */
	public SessionFactoryImplementor getFactory();


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    // stuff that is persister-centric and/or EntityInfo-centric ~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Returns an object that identifies the space in which identifiers of this class hierarchy are unique.
	 *
	 * A table name, a JNDI URL, etc.
	 */
	public String getRootEntityName();

	/**
	 * The classname of the persistent class (used only for messages)
	 */
	public String getEntityName();
	
	/**
	 * Is the given entity name the name of a subclass, or this class?
	 */
	public boolean isSubclassEntityName(String entityName);

	/**
	 * Returns an array of objects that identify spaces in which properties of this class are persisted,
	 * for instances of this class only.
	 */
	public Serializable[] getPropertySpaces();

	/**
	 * Returns an array of objects that identify spaces in which properties of this class are persisted,
	 * for instances of this class and its subclasses.
	 */
	public Serializable[] getQuerySpaces();

	/**
	 * Does this class support dynamic proxies.
	 */
	public boolean hasProxy();

	/**
	 * Do instances of this class contain collections.
	 */
	public boolean hasCollections();
	
	/**
	 * Does this entity own any collections which are
	 * fetchable by subselect?
	 */
	public boolean hasSubselectLoadableCollections();

	/**
	 * Does this class declare any cascading save/update/deletes.
	 */
	public boolean hasCascades();

	/**
	 * Are instances of this class mutable.
	 */
	public boolean isMutable();

	/**
	 * Is the identifier assigned before the insert by an <tt>IDGenerator</tt>. Or
	 * is it returned by the <tt>insert()</tt> method? This determines which form
	 * of <tt>insert()</tt> will be called.
	 */
	public boolean isIdentifierAssignedByInsert();

	/**
	 * Get the type of a particular property
	 */
	public Type getPropertyType(String propertyName) throws MappingException;

	/**
	 * Compare two snapshots of the state of an instance to determine if the persistent state
	 * was modified
	 * @return <tt>null</tt> or the indices of the dirty properties
	 */
	public int[] findDirty(Object[] x, Object[] y, Object owner, SessionImplementor session)
	throws HibernateException;

	/**
	 * Compare the state of an instance to the current database state
	 * @return <tt>null</tt> or the indices of the dirty properties
	 */
	public int[] findModified(Object[] old, Object[] current, Object object, SessionImplementor session)
	throws HibernateException;

	/**
	 * Does the class have a property holding the identifier value?
	 */
	public boolean hasIdentifierProperty();
	/**
	 * Do detached instances of this class carry their own identifier value?
	 */
	public boolean hasIdentifierPropertyOrEmbeddedCompositeIdentifier();

	/**
	 * Are instances of this class versioned by a timestamp or version number column.
	 */
	public boolean isVersioned();

	/**
	 * Get the type of versioning (optional operation)
	 */
	public VersionType getVersionType();

	/**
	 * Which property holds the version number (optional operation).
	 */
	public int getVersionProperty();

	/**
	 * Return the <tt>IdentifierGenerator</tt> for the class
	 */
	public IdentifierGenerator getIdentifierGenerator() throws HibernateException;

	/**
	 * Load an instance of the persistent class.
	 */
	public Object load(Serializable id, Object optionalObject, LockMode lockMode, SessionImplementor session)
	throws HibernateException;

	/**
	 * Do a version check (optional operation)
	 */
	public void lock(Serializable id, Object version, Object object, LockMode lockMode, SessionImplementor session)
	throws HibernateException;

	/**
	 * Persist an instance
	 */
	public void insert(Serializable id, Object[] fields, Object object, SessionImplementor session)
	throws HibernateException;

	/**
	 * Persist an instance, using a natively generated identifier (optional operation)
	 */
	public Serializable insert(Object[] fields, Object object, SessionImplementor session)
	throws HibernateException;

	/**
	 * Delete a persistent instance
	 */
	public void delete(Serializable id, Object version, Object object, SessionImplementor session)
	throws HibernateException;

	/**
	 * Update a persistent instance
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
	) throws HibernateException;

	/**
	 * Get the Hibernate types of the class properties
	 */
	public Type[] getPropertyTypes();

	/**
	 * Get the names of the class properties - doesn't have to be the names of the
	 * actual Java properties (used for XML generation only)
	 */
	public String[] getPropertyNames();

	/**
	 * Get the "insertability" of the properties of this class
	 * (does the property appear in an SQL INSERT)
	 */
	public boolean[] getPropertyInsertability();

	/**
	 * Get the "updateability" of the properties of this class
	 * (does the property appear in an SQL UPDATE)
	 */
	public boolean[] getPropertyUpdateability();

	/**
	 * Get the nullability of the properties of this class
	 */
	public boolean[] getPropertyNullability();

	/**
	 * Get the "versionability" of the properties of this class
	 * (is the property optimistic-locked)
	 */
	public boolean[] getPropertyVersionability();
	/**
	 * Get the cascade styles of the propertes (optional operation)
	 */
	public Cascades.CascadeStyle[] getPropertyCascadeStyles();

	/**
	 * Get the identifier type
	 */
	public Type getIdentifierType();

	/**
	 * Get the name of the identifier property (or return null) - need not return the
	 * name of an actual Java property
	 */
	public String getIdentifierPropertyName();

	/**
	 * Should we always invalidate the cache instead of
	 * recaching updated state
	 */
	public boolean isCacheInvalidationRequired();
	/**
	 * Does this class have a cache.
	 */
	public boolean hasCache();
	/**
	 * Get the cache (optional operation)
	 */
	public CacheConcurrencyStrategy getCache();
	/**
	 * Get the cache structure
	 */
	public CacheEntryStructure getCacheEntryStructure();

	/**
	 * Get the user-visible metadata for the class (optional operation)
	 */
	public ClassMetadata getClassMetadata();

	/**
	 * Is batch loading enabled?
	 */
	public boolean isBatchLoadable();

	/**
	 * Is select snapshot before update enabled?
	 */
	public boolean isSelectBeforeUpdateRequired();

	/**
	 * Get the current database state of the object, in a "hydrated" form, without
	 * resolving identifiers
	 * @return null if there is no row in the database
	 */
	public Object[] getDatabaseSnapshot(Serializable id, SessionImplementor session)
	throws HibernateException;

	/**
	 * Get the current version of the object, or return null if there is no row for
	 * the given identifier. In the case of unversioned data, return any object
	 * if the row exists.
	 */
	public Object getCurrentVersion(Serializable id, SessionImplementor session)
	throws HibernateException;

	public EntityMode guessEntityMode(Object object);


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// stuff that is tuplizer-centric, but is passed a session ~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * Called just after the entities properties have been initialized
	 */
	public void afterInitialize(Object entity, boolean lazyPropertiesAreUnfetched, SessionImplementor session);

	/**
	 * Called just after the entity has been reassociated with the session
	 */
	public void afterReassociate(Object entity, SessionImplementor session);

	/**
	 * Create a new proxy instance
	 */
	public Object createProxy(Serializable id, SessionImplementor session)
	throws HibernateException;

	/**
	 * Is this a new transient instance?
	 */
	public Boolean isTransient(Object object, SessionImplementor session) throws HibernateException;

	/**
	 * Return the values of the insertable properties of the object (including backrefs)
	 */
	public Object[] getPropertyValuesToInsert(Object object, SessionImplementor session) throws HibernateException;


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// stuff that is Tuplizer-centric ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * The persistent class, or null
	 */
	public Class getMappedClass(EntityMode entityMode);

	/**
	 * Does the class implement the <tt>Lifecycle</tt> interface.
	 */
	public boolean implementsLifecycle(EntityMode entityMode);

	/**
	 * Does the class implement the <tt>Validatable</tt> interface.
	 */
	public boolean implementsValidatable(EntityMode entityMode);
	/**
	 * Get the proxy interface that instances of <em>this</em> concrete class will be 
	 * cast to (optional operation).
	 */
	public Class getConcreteProxyClass(EntityMode entityMode);

	/**
	 * Set the given values to the mapped properties of the given object
	 */
	public void setPropertyValues(Object object, Object[] values, EntityMode entityMode) throws HibernateException;

	/**
	 * Set the value of a particular property
	 */
	public void setPropertyValue(Object object, int i, Object value, EntityMode entityMode) throws HibernateException;

	/**
	 * Return the (loaded) values of the mapped properties of the object (not including backrefs)
	 */
	public Object[] getPropertyValues(Object object, EntityMode entityMode) throws HibernateException;

	/**
	 * Get the value of a particular property
	 */
	public Object getPropertyValue(Object object, int i, EntityMode entityMode) throws HibernateException;
	
	/**
	 * Get the value of a particular property
	 */
	public Object getPropertyValue(Object object, String propertyName, EntityMode entityMode) throws HibernateException;

	/**
	 * Get the identifier of an instance (throw an exception if no identifier property)
	 */
	public Serializable getIdentifier(Object object, EntityMode entityMode) throws HibernateException;

	/**
	 * Set the identifier of an instance (or do nothing if no identifier property)
	 */
	public void setIdentifier(Object object, Serializable id, EntityMode entityMode) throws HibernateException;

	/**
	 * Get the version number (or timestamp) from the object's version property (or return null if not versioned)
	 */
	public Object getVersion(Object object, EntityMode entityMode) throws HibernateException;

	/**
	 * Create a class instance initialized with the given identifier
	 */
	public Object instantiate(Serializable id, EntityMode entityMode) throws HibernateException;

	/**
	 * Is the given object an instance of this entity?
	 */
	public boolean isInstance(Object object, EntityMode entityMode);
	
	/**
	 * Does the given instance have any uninitialized lazy properties?
	 */
	public boolean hasUninitializedLazyProperties(Object object, EntityMode entityMode);
	
	/**
	 * Set the identifier and version of the given instance back 
	 * to its "unsaved" value, returning the id
	 * @param currentId TODO
	 * @param currentVersion TODO
	 */
	public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion, EntityMode entityMode);

	/**
	 * Get the persister for an instance of this class or a subclass
	 */
	public EntityPersister getSubclassEntityPersister(Object instance, SessionFactoryImplementor factory, EntityMode entityMode);
}
