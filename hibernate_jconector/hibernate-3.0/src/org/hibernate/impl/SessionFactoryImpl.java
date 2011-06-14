//$Id: SessionFactoryImpl.java,v 1.65 2005/03/04 10:57:47 oneovthafew Exp $
package org.hibernate.impl;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.transaction.TransactionManager;

import net.sf.cglib.core.KeyFactory;

import org.apache.commons.collections.ReferenceMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.SessionFactory;
import org.hibernate.EntityMode;
import org.hibernate.exception.SQLExceptionConverter;
import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheConcurrencyStrategy;
import org.hibernate.cache.CacheFactory;
import org.hibernate.cache.CacheKey;
import org.hibernate.cache.QueryCache;
import org.hibernate.cache.UpdateTimestampsCache;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Settings;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.NamedQueryDefinition;
import org.hibernate.engine.NamedSQLQueryDefinition;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.event.SessionEventListenerConfig;
import org.hibernate.hql.FilterTranslator;
import org.hibernate.hql.QuerySplitter;
import org.hibernate.hql.QueryTranslator;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.UUIDHexGenerator;
import org.hibernate.jdbc.BatcherFactory;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.persister.PersisterFactory;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.stat.Statistics;
import org.hibernate.stat.StatisticsImpl;
import org.hibernate.stat.StatisticsImplementor;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.transaction.TransactionFactory;
import org.hibernate.type.Type;
import org.hibernate.util.CollectionHelper;
import org.hibernate.util.ReflectHelper;


/**
 * Concrete implementation of the <tt>SessionFactory</tt> interface. Has the following
 * responsibilites
 * <ul>
 * <li>caches configuration settings (immutably)
 * <li>caches "compiled" mappings ie. <tt>EntityPersister</tt>s and
 *     <tt>CollectionPersister</tt>s (immutable)
 * <li>caches "compiled" queries (memory sensitive cache)
 * <li>manages <tt>PreparedStatement</tt>s
 * <li> delegates JDBC <tt>Connection</tt> management to the <tt>ConnectionProvider</tt>
 * <li>factory for instances of <tt>SessionImpl</tt>
 * </ul>
 * This class must appear immutable to clients, even if it does all kinds of caching
 * and pooling under the covers. It is crucial that the class is not only thread
 * safe, but also highly concurrent. Synchronization must be used extremely sparingly.
 *
 * @see org.hibernate.connection.ConnectionProvider
 * @see org.hibernate.classic.Session
 * @see org.hibernate.hql.QueryTranslator
 * @see org.hibernate.persister.entity.EntityPersister
 * @see org.hibernate.persister.collection.CollectionPersister
 * @author Gavin King
 */
public final class SessionFactoryImpl implements SessionFactory, SessionFactoryImplementor {

	private final String name;
	private final String uuid;

	private final transient Map entityPersisters;
	private final transient Map classMetadata;
	private final transient Map collectionPersisters;
	private final transient Map collectionMetadata;
	private final transient Map identifierGenerators;
	private final transient Map namedQueries;
	private final transient Map namedSqlQueries;
	private final transient Map filters;
	private final transient Map imports;
	private final transient Interceptor interceptor;
	private final transient Settings settings;
	private final transient Properties properties;
	private transient SchemaExport schemaExport;
	private final transient TransactionManager transactionManager;
	private final transient QueryCache queryCache;
	private final transient UpdateTimestampsCache updateTimestampsCache;
	private final transient Map queryCaches;
	private final transient Map allCacheRegions = new HashMap();
	private final transient StatisticsImpl statistics = new StatisticsImpl(this);

	private static final IdentifierGenerator UUID_GENERATOR = new UUIDHexGenerator();

    private SessionEventListenerConfig sessionEventListenerConfig;

	private static final Log log = LogFactory.getLog(SessionFactoryImpl.class);

	public SessionFactoryImpl(
			Configuration cfg, 
			Mapping mapping,
			Settings settings, 
			SessionEventListenerConfig listeners) 
	throws HibernateException {

		log.info("building session factory");

		this.properties = cfg.getProperties();
		this.interceptor = cfg.getInterceptor();
		this.settings = settings;
        this.sessionEventListenerConfig = listeners;
        this.filters = cfg.getFilterDefinitions();

		if ( log.isDebugEnabled() ) {
			log.debug("Session factory constructed with filter configurations : " + filters);
		}

		if ( log.isDebugEnabled() ) log.debug(
			"instantiating session factory with properties: " + properties 
		);

		// Caches
		settings.getCacheProvider().start( properties );

		//Generators:
		
		identifierGenerators = new HashMap();
		Iterator classes = cfg.getClassMappings();
		while ( classes.hasNext() ) {
			PersistentClass model = (PersistentClass) classes.next();
			if ( !model.isInherited() ) {
				IdentifierGenerator generator = model.getIdentifier().createIdentifierGenerator( 
						settings.getDialect(),
						settings.getDefaultCatalogName(),
						settings.getDefaultSchemaName(), 
						model.getEntityName()
				);
				identifierGenerators.put( model.getEntityName(), generator );
			}
		}
		
		//Persisters:

		Map caches = new HashMap();
		entityPersisters = new HashMap();
		Map classMeta = new HashMap();
		classes = cfg.getClassMappings();
		while ( classes.hasNext() ) {
			PersistentClass model = (PersistentClass) classes.next();
			String cacheRegion = model.getRootClass().getCacheRegionName();
			CacheConcurrencyStrategy cache = (CacheConcurrencyStrategy) caches.get(cacheRegion);
			if (cache==null) {
				cache = CacheFactory.createCache(
					model.getCacheConcurrencyStrategy(),
					cacheRegion,
					model.isMutable(),
					settings,
					properties
				);
				if (cache!=null) {
					caches.put(cacheRegion, cache);
					allCacheRegions.put( cache.getRegionName(), cache.getCache() );
				}
			}
			EntityPersister cp = PersisterFactory.createClassPersister(model, cache, this, mapping);
			entityPersisters.put( model.getEntityName(), cp );
			classMeta.put( model.getEntityName(), cp.getClassMetadata() );
		}
		classMetadata = Collections.unmodifiableMap(classMeta);

		collectionPersisters = new HashMap();
		Iterator collections = cfg.getCollectionMappings();
		while ( collections.hasNext() ) {
			Collection model = (Collection) collections.next();
			CacheConcurrencyStrategy cache = CacheFactory.createCache(
				model.getCacheConcurrencyStrategy(),
				model.getCacheRegionName(),
				true,
				settings,
				properties
			);
			if (cache!=null) allCacheRegions.put( cache.getRegionName(), cache.getCache() );
			CollectionPersister persister = PersisterFactory.createCollectionPersister(cfg, model, cache, this);
			collectionPersisters.put( model.getRole(), persister.getCollectionMetadata() );
		}
		collectionMetadata = Collections.unmodifiableMap(collectionPersisters);

		//Named Queries:
		// TODO: precompile and cache named queries
		namedQueries = new HashMap( cfg.getNamedQueries() );
		namedSqlQueries = new HashMap( cfg.getNamedSQLQueries() );
		imports = new HashMap( cfg.getImports() );

		// after *all* persisters and named queries are registered
		Iterator iter = entityPersisters.values().iterator();
		while ( iter.hasNext() ) {
			( (EntityPersister) iter.next() ).postInstantiate();
		}
		iter = collectionPersisters.values().iterator();
		while ( iter.hasNext() ) {
			( (CollectionPersister) iter.next() ).postInstantiate();
		}

		//JNDI + Serialization:

		name = settings.getSessionFactoryName();
		try {
			uuid = (String) UUID_GENERATOR.generate(null, null);
		}
		catch (Exception e) {
			throw new AssertionFailure("Could not generate UUID");
		}
		SessionFactoryObjectFactory.addInstance(uuid, name, this, properties);

		log.debug("instantiated session factory");

		if ( settings.isAutoCreateSchema() ) new SchemaExport(cfg).create(false, true);
		if ( settings.isAutoUpdateSchema() ) new SchemaUpdate(cfg).execute(false, true);
		if ( settings.isAutoDropSchema() ) schemaExport = new SchemaExport(cfg);

		if ( settings.getTransactionManagerLookup()!=null ) {
			log.debug("obtaining JTA TransactionManager");
			transactionManager = settings.getTransactionManagerLookup().getTransactionManager(properties);
		}
		else {
			transactionManager = null;
		}

		if ( settings.isQueryCacheEnabled() ) {
			updateTimestampsCache = new UpdateTimestampsCache(settings, properties);
			queryCache = settings.getQueryCacheFactory()
			        .getQueryCache(null, updateTimestampsCache, settings, properties);
			queryCaches = new HashMap();
			allCacheRegions.put( updateTimestampsCache.getRegionName(), updateTimestampsCache.getCache() );
			allCacheRegions.put( queryCache.getRegionName(), queryCache.getCache() );
		}
		else {
			updateTimestampsCache = null;
			queryCache = null;
			queryCaches = null;
		}

		//checking for named queries
		Map errors = checkNamedQueries();
		if ( !errors.isEmpty() ) {
			Set keys = errors.keySet();
			StringBuffer failingQueries = new StringBuffer("Errors in named queries: ");
			for ( Iterator iterator = keys.iterator() ; iterator.hasNext() ; ) {
				String queryName = (String) iterator.next();
				HibernateException e = (HibernateException) errors.get(queryName);
				failingQueries.append(queryName);
				if ( iterator.hasNext() ) failingQueries.append(", ");
				log.error("Error in named query: " + queryName, e);
			}
			throw new HibernateException( failingQueries.toString() );
		}
		
		//stats
		getStatistics().setStatisticsEnabled( settings.isStatisticsEnabled() );
	}

	// Emulates constant time LRU/MRU algorithms for cache
	// It is better to hold strong references on some (LRU/MRU) queries
	private static final int MAX_STRONG_REF_COUNT = 128; //TODO: configurable?
	private final transient Object[] strongRefs = new Object[MAX_STRONG_REF_COUNT]; //strong reference to MRU queries
	private transient int strongRefIndex = 0;
	private final transient Map softQueryCache = new ReferenceMap(ReferenceMap.SOFT, ReferenceMap.SOFT) ;
	// both keys and values may be soft since value keeps a hard ref to the key (and there is a hard ref to MRU values)

	//returns generated class instance
	private static final QueryCacheKeyFactory QUERY_KEY_FACTORY;
	private static final FilterCacheKeyFactory FILTER_KEY_FACTORY;
	static {
		QUERY_KEY_FACTORY = (QueryCacheKeyFactory) KeyFactory.create(QueryCacheKeyFactory.class);
		FILTER_KEY_FACTORY = (FilterCacheKeyFactory) KeyFactory.create(FilterCacheKeyFactory.class);
	}

	static interface QueryCacheKeyFactory {
		//Will not recalculate hashKey for constant queries
		Object newInstance(String query, boolean scalar);
	}

	static interface FilterCacheKeyFactory {
		//Will not recalculate hashKey for constant queries
		Object newInstance(String role, String query, boolean scalar);
	}

	//TODO: this stuff can be implemented in separate class to reuse soft MRU/LRU caching
	private synchronized Object get(Object key) {
		Object result = softQueryCache.get(key);
		if( result != null ) {
			strongRefs[ ++strongRefIndex % MAX_STRONG_REF_COUNT ] = result;
		}
		return result;
	}

	private void put(Object key, Object value) {
		softQueryCache.put(key, value);
		strongRefs[ ++strongRefIndex % MAX_STRONG_REF_COUNT ] = value;
	}

	private synchronized QueryTranslator[] createQueryTranslators(
			String[] concreteQueryStrings,
			Object cacheKey,
			Map enabledFilters
	) {
		final int length = concreteQueryStrings.length;
		final QueryTranslator[] queries = new QueryTranslator[length];
		for ( int i=0; i<length; i++ ) {
			queries[i] = settings.getQueryTranslatorFactory()
				.createQueryTranslator( concreteQueryStrings[i], enabledFilters, this );
		}
		if (cacheKey != null) put(cacheKey, queries);
		return queries;
	}

	private synchronized FilterTranslator createFilterTranslator(
			String filterString, 
			Object cacheKey, 
			Map enabledFilters
	) {
        final FilterTranslator filter = settings.getQueryTranslatorFactory()
			.createFilterTranslator(filterString, enabledFilters, this);
		if (cacheKey != null) put(cacheKey, filter);
		return filter;
	}

	private Map checkNamedQueries() throws HibernateException {
		// Check named queries
		Map errors = new HashMap();
		Set names = namedQueries.keySet();
		log.info("Checking " + namedQueries.size() + " named queries");
		for ( Iterator iterator = names.iterator(); iterator.hasNext(); ) {
			String queryName = (String) iterator.next();
			NamedQueryDefinition q = (NamedQueryDefinition) namedQueries.get(queryName);

			// this will throw an error if there's something wrong.
			try {
				log.debug("Checking named query: " + queryName);
				//TODO: BUG! this currently fails for named queries for non-POJO entities
				getQuery( q.getQueryString(), false, CollectionHelper.EMPTY_MAP );
			}
			catch (QueryException e) {
				errors.put(queryName, e);
			}
			catch (MappingException e) {
				errors.put(queryName, e);
			}
		}
		
		return errors;
	}

	public QueryTranslator[] getQuery(String queryString, boolean shallow, Map enabledFilters)
	throws QueryException, MappingException {

		// if there are no enabled filters, consider cached query compilations,
		// otherwise generate/compile a new set of query translators
		Object cacheKey = null;
		QueryTranslator[] queries = null;
		
		if ( enabledFilters == null || enabledFilters.isEmpty() ) {
			cacheKey = QUERY_KEY_FACTORY.newInstance(queryString, shallow);
			queries = (QueryTranslator[]) get(cacheKey);
		}

		// have to be careful to ensure that if the JVM does out-of-order execution
		// then another thread can't get an uncompiled QueryTranslator from the cache
		// we also have to be very careful to ensure that other threads can perform
		// compiled queries while another query is being compiled

		if ( queries==null ) {
			// a query that names an interface or unmapped class in the from clause
			// is actually executed as multiple queries
			String[] concreteQueryStrings = QuerySplitter.concreteQueries(queryString, this);
			queries = createQueryTranslators(concreteQueryStrings, cacheKey, enabledFilters);
		}
		for ( int i=0; i<queries.length; i++) {
//			queries[i].compile(this, settings.getQuerySubstitutions(), shallow, enabledFilters);
			queries[i].compile( settings.getQuerySubstitutions(), shallow );
		}
		// see comment above. note that QueryTranslator.compile() is synchronized
		return queries;

	}

	public FilterTranslator getFilter(
	        String filterString, 
	        String collectionRole, 
	        boolean scalar,
	        Map enabledFilters) 
	throws QueryException, MappingException {

		Object cacheKey = null;
		FilterTranslator filter = null;

		if ( enabledFilters == null || enabledFilters.isEmpty() ) {
			cacheKey = FILTER_KEY_FACTORY.newInstance(collectionRole, filterString, scalar);
			filter = (FilterTranslator) get(cacheKey);
		}

		if ( filter==null ) {
			filter = createFilterTranslator(filterString, cacheKey, enabledFilters);
		}
//		filter.compile(collectionRole, this, settings.getQuerySubstitutions(), scalar, enabledFilters);
		filter.compile( collectionRole, settings.getQuerySubstitutions(), scalar );
		// see comment above. note that FilterTranslator.compile() is synchronized
		return filter;

	}

	private org.hibernate.classic.Session openSession(
		Connection connection, 
		boolean autoClose, 
		long timestamp, 
		Interceptor sessionLocalInterceptor
	) {
		
		return new SessionImpl(
		        connection,
		        this,
		        autoClose,
		        timestamp,
		        sessionLocalInterceptor,
		        sessionEventListenerConfig,
		        settings.getDefaultEntityMode()
		);
	}

	public org.hibernate.classic.Session openSession(Connection connection, Interceptor sessionLocalInterceptor) {
		return openSession(connection, false, Long.MIN_VALUE, sessionLocalInterceptor);
	}

	public org.hibernate.classic.Session openSession(Interceptor sessionLocalInterceptor) 
	throws HibernateException {
		// note that this timestamp is not correct if the connection provider
		// returns an older JDBC connection that was associated with a
		// transaction that was already begun before openSession() was called
		// (don't know any possible solution to this!)
		long timestamp = settings.getCacheProvider().nextTimestamp();
		return openSession( null, true, timestamp, sessionLocalInterceptor );
	}

	public org.hibernate.classic.Session openSession(Connection connection) {
		return openSession(connection, interceptor); //prevents this session from adding things to cache
	}

	public org.hibernate.classic.Session openSession() throws HibernateException {
		return openSession(interceptor);
	}

	public EntityPersister getEntityPersister(String entityName) throws MappingException {
		EntityPersister result = (EntityPersister) entityPersisters.get(entityName);
		if (result==null) {
			throw new MappingException( "Unknown entity: " + entityName );
		}
		return result;
	}

	public CollectionPersister getCollectionPersister(String role) throws MappingException {
		CollectionPersister result = (CollectionPersister) collectionPersisters.get(role);
		if (result==null) {
			throw new MappingException( "Unknown collection role: " + role );
		}
		return result;
	}
	
	public Settings getSettings() {
		return settings;
	}

	public Dialect getDialect() {
		return settings.getDialect();
	}

	public TransactionFactory getTransactionFactory() {
		return settings.getTransactionFactory();
	}

	public TransactionManager getTransactionManager() {
		return transactionManager;
	}

	public SQLExceptionConverter getSQLExceptionConverter() {
		return settings.getSQLExceptionConverter();
	}

	// from javax.naming.Referenceable
	public Reference getReference() throws NamingException {
		log.debug("Returning a Reference to the SessionFactory");
		return new Reference(
			SessionFactoryImpl.class.getName(),
			new StringRefAddr("uuid", uuid),
			SessionFactoryObjectFactory.class.getName(),
			null
		);
	}

	private Object readResolve() throws ObjectStreamException {
		log.trace("Resolving serialized SessionFactory");
		// look for the instance by uuid
		Object result = SessionFactoryObjectFactory.getInstance(uuid);
		if (result==null) {
			// in case we were deserialized in a different JVM, look for an instance with the same name
			// (alternatively we could do an actual JNDI lookup here....)
			result = SessionFactoryObjectFactory.getNamedInstance(name);
			if (result==null) {
				throw new InvalidObjectException("Could not find a SessionFactory named: " + name);
			}
			else {
				log.debug("resolved SessionFactory by name");
			}
		}
		else {
			log.debug("resolved SessionFactory by uid");
		}
		return result;
	}

	public NamedQueryDefinition getNamedQuery(String queryName) {
		return (NamedQueryDefinition) namedQueries.get(queryName);
	}

	public NamedSQLQueryDefinition getNamedSQLQuery(String queryName) {
		return (NamedSQLQueryDefinition) namedSqlQueries.get(queryName);
	}

	public Type getIdentifierType(String className) throws MappingException {
		return getEntityPersister(className).getIdentifierType();
	}
	public String getIdentifierPropertyName(String className) throws MappingException {
		return getEntityPersister(className).getIdentifierPropertyName();
	}

	private final void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		log.trace("deserializing");
		in.defaultReadObject();
		log.debug("deserialized: " + uuid);
	}
	private final void writeObject(ObjectOutputStream out) throws IOException {
		log.debug("serializing: " + uuid);
		out.defaultWriteObject();
		log.trace("serialized");
	}

	public Type[] getReturnTypes(String queryString) throws HibernateException {
		String[] queries = QuerySplitter.concreteQueries(queryString, this);
		if ( queries.length==0 ) throw new HibernateException("Query does not refer to any persistent classes: " + queryString);
		return getQuery( queries[0], true, CollectionHelper.EMPTY_MAP )[0].getReturnTypes();
	}

	public ClassMetadata getClassMetadata(Class persistentClass) throws HibernateException {
		return getClassMetadata( persistentClass.getName() );
	}

	public CollectionMetadata getCollectionMetadata(String roleName) throws HibernateException {
		return (CollectionMetadata) collectionMetadata.get(roleName);
	}

	public ClassMetadata getClassMetadata(String entityName) throws HibernateException {
		return (ClassMetadata) classMetadata.get(entityName);
	}

	/**
	 * Return the names of all persistent (mapped) classes that extend or implement the
	 * given class or interface, accounting for implicit/explicit polymorphism settings
	 * and excluding mapped subclasses/joined-subclasses of other classes in the result.
	 */
	public String[] getImplementors(String className) throws MappingException {

		final Class clazz;
		try {
			clazz = ReflectHelper.classForName(className);
		}
		catch (ClassNotFoundException cnfe) {
			return new String[] { className }; //for a dynamic-class
		}

		ArrayList results = new ArrayList();
		Iterator iter = entityPersisters.values().iterator();
		while ( iter.hasNext() ) {
			//test this entity to see if we must query it
			EntityPersister testPersister = (EntityPersister) iter.next();
			if ( testPersister instanceof Queryable ) {
				Queryable testQueryable = (Queryable) testPersister;
				String testClassName = testQueryable.getEntityName();
				boolean isMappedClass = className.equals(testClassName);
				if ( testQueryable.isExplicitPolymorphism() ) {
					if (isMappedClass) return new String[] { className }; //NOTE EARLY EXIT
				}
				else {
					if (isMappedClass) {
						results.add(testClassName);
					}
					else {
						final Class mappedClass = testQueryable.getMappedClass( EntityMode.POJO );
						if ( mappedClass!=null && clazz.isAssignableFrom( mappedClass ) ) {
							final boolean assignableSuperclass;
							if ( testQueryable.isInherited() ) {
								Class mappedSuperclass = getEntityPersister( testQueryable.getMappedSuperclass() ).getMappedClass( EntityMode.POJO);
								assignableSuperclass = clazz.isAssignableFrom(mappedSuperclass);
							}
							else {
								assignableSuperclass = false;
							}
							if (!assignableSuperclass) results.add(testClassName);
						}
					}
				}
			}
		}
		return (String[]) results.toArray( new String[ results.size() ] );
	}

	public String getImportedClassName(String className) {
		String result = (String) imports.get(className);
		if (result==null) {
			try {
				ReflectHelper.classForName(className);
				return className;
			}
			catch (ClassNotFoundException cnfe) {
				return null;
			}
		}
		else {
			return result;
		}
	}

	public Map getAllClassMetadata() throws HibernateException {
		return classMetadata;
	}

	public Map getAllCollectionMetadata() throws HibernateException {
		return collectionMetadata;
	}

	/**
	 * Closes the session factory, releasing all held resources.
	 *
	 * <ol>
	 * <li>cleans up used cache regions and "stops" the cache provider.
	 * <li>close the JDBC connection
	 * <li>remove the JNDI binding
	 * </ol>
	 */
	public void close() throws HibernateException {

		log.info("closing");

		Iterator iter = entityPersisters.values().iterator();
		while ( iter.hasNext() ) {
			EntityPersister p = (EntityPersister) iter.next();
			if ( p.hasCache() ) p.getCache().destroy();
		}

		iter = collectionPersisters.values().iterator();
		while ( iter.hasNext() ) {
			CollectionPersister p = (CollectionPersister) iter.next();
			if ( p.hasCache() ) p.getCache().destroy();
		}

		if ( settings.isQueryCacheEnabled() )  {
			queryCache.destroy();

			iter = queryCaches.values().iterator();
			while ( iter.hasNext() ) {
				QueryCache cache = (QueryCache) iter.next();
				cache.destroy();
			}
			updateTimestampsCache.destroy();
		}

		settings.getCacheProvider().stop();

		try {
			settings.getConnectionProvider().close();
		}
		finally {
			SessionFactoryObjectFactory.removeInstance(uuid, name, properties);
		}

		if ( settings.isAutoDropSchema() ) schemaExport.drop(false, true);

	}

	public void evictEntity(String entityName, Serializable id) throws HibernateException {
		EntityPersister p = getEntityPersister(entityName);
		if ( p.hasCache() ) {
			if ( log.isDebugEnabled() ) {
				log.debug( "evicting second-level cache: " + MessageHelper.infoString(p, id, this) );
			}
			p.getCache().remove( new CacheKey( id, p.getIdentifierType(), p.getRootEntityName(), EntityMode.POJO ) );
		}
	}

	public void evictEntity(String entityName) throws HibernateException {
		EntityPersister p = getEntityPersister(entityName);
		if ( p.hasCache() ) {
			if ( log.isDebugEnabled() ) {
				log.debug( "evicting second-level cache: " + p.getEntityName() );
			}
			p.getCache().clear();
		}
	}

	public void evict(Class persistentClass, Serializable id) throws HibernateException {
		EntityPersister p = getEntityPersister( persistentClass.getName() );
		if ( p.hasCache() ) {
			if ( log.isDebugEnabled() ) {
				log.debug( "evicting second-level cache: " + MessageHelper.infoString(p, id, this) );
			}
			p.getCache().remove( new CacheKey( id, p.getIdentifierType(), p.getRootEntityName(), EntityMode.POJO ) );
		}
	}

	public void evict(Class persistentClass) throws HibernateException {
		EntityPersister p = getEntityPersister( persistentClass.getName() );
		if ( p.hasCache() ) {
			if ( log.isDebugEnabled() ) {
				log.debug( "evicting second-level cache: " + p.getEntityName() );
			}
			p.getCache().clear();
		}
	}

	public void evictCollection(String roleName, Serializable id) throws HibernateException {
		CollectionPersister p = getCollectionPersister(roleName);
		if ( p.hasCache() ) {
			if ( log.isDebugEnabled() ) {
				log.debug( "evicting second-level cache: " + MessageHelper.collectionInfoString(p, id, this) );
			}
			p.getCache().remove( new CacheKey( id, p.getKeyType(), p.getRole(), EntityMode.POJO ) );
		}
	}

	public void evictCollection(String roleName) throws HibernateException {
		CollectionPersister p = getCollectionPersister(roleName);
		if ( p.hasCache() ) {
			if ( log.isDebugEnabled() ) log.debug( "evicting second-level cache: " + p.getRole() );
			p.getCache().clear();
		}
	}

	public Type getPropertyType(String className, String propertyName)
		throws MappingException {
		return getEntityPersister(className).getPropertyType(propertyName);
	}

	public ConnectionProvider getConnectionProvider() {
		return settings.getConnectionProvider();
	}

	public UpdateTimestampsCache getUpdateTimestampsCache() {
		return updateTimestampsCache;
	}
	
	public QueryCache getQueryCache() {
		return queryCache;
	}

	public QueryCache getQueryCache(String cacheRegion) throws HibernateException {
		if (cacheRegion==null) {
			return getQueryCache();
		}
		
		if ( !settings.isQueryCacheEnabled() ) {
			return null;
		}

		synchronized (allCacheRegions) {
			QueryCache currentQueryCache = (QueryCache) queryCaches.get(cacheRegion);
			if (currentQueryCache==null) {
				currentQueryCache = settings.getQueryCacheFactory()
					.getQueryCache(cacheRegion, updateTimestampsCache, settings, properties);
				queryCaches.put(cacheRegion, currentQueryCache);
				allCacheRegions.put( currentQueryCache.getRegionName(), currentQueryCache.getCache() );
			}
			return currentQueryCache;
		}
	}
	
	public Cache getSecondLevelCacheRegion(String regionName) {
		synchronized (allCacheRegions) {
			return (Cache) allCacheRegions.get(regionName);
		}
	}
	
	public Map getAllSecondLevelCacheRegions() {
		synchronized (allCacheRegions) {
			return new HashMap(allCacheRegions);
		}
	}

	public Statistics getStatistics() {
		return statistics;
	}
	
	public StatisticsImplementor getStatisticsImplementor() {
		return statistics;
	}
	
	public void evictQueries() throws HibernateException {
		if ( settings.isQueryCacheEnabled() ) {
			queryCache.clear();
		}
	}

	public void evictQueries(String cacheRegion) throws HibernateException {
		if (cacheRegion==null) {
			throw new NullPointerException("use the zero-argument form to evict the default query cache");
		}
		else {
			synchronized (allCacheRegions) {
				if ( settings.isQueryCacheEnabled() ) {
					QueryCache currentQueryCache = (QueryCache) queryCaches.get(cacheRegion);
					if (currentQueryCache!=null) currentQueryCache.clear();
				}
			}			
		}
	}

	public FilterDefinition getFilterDefinition(String filterName) throws IllegalArgumentException {
		FilterDefinition def = (FilterDefinition) filters.get(filterName);
		if (def == null) {
			// TODO: what should be the actual type thrown?
			throw new IllegalArgumentException("No such filter configured [" + filterName + "]");
		}
		return def;
	}

	public BatcherFactory getBatcherFactory() {
		return settings.getBatcherFactory();
	}
	
	public IdentifierGenerator getIdentifierGenerator(String rootEntityName) {
		return (IdentifierGenerator) identifierGenerators.get(rootEntityName);
	}

}
