//$Id: SettingsFactory.java,v 1.24 2005/02/21 13:45:11 oneovthafew Exp $
package org.hibernate.cfg;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.EntityMode;
import org.hibernate.cache.CacheProvider;
import org.hibernate.cache.QueryCacheFactory;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.connection.ConnectionProviderFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.exception.SQLExceptionConverter;
import org.hibernate.exception.SQLExceptionConverterFactory;
import org.hibernate.hql.QueryTranslatorFactory;
import org.hibernate.jdbc.BatcherFactory;
import org.hibernate.jdbc.BatchingBatcherFactory;
import org.hibernate.jdbc.NonBatchingBatcherFactory;
import org.hibernate.transaction.TransactionFactory;
import org.hibernate.transaction.TransactionFactoryFactory;
import org.hibernate.transaction.TransactionManagerLookup;
import org.hibernate.transaction.TransactionManagerLookupFactory;
import org.hibernate.util.PropertiesHelper;
import org.hibernate.util.ReflectHelper;
import org.hibernate.util.StringHelper;

/**
 * Reads configuration properties and configures a <tt>Settings</tt> instance.
 *
 * @author Gavin King
 */
public class SettingsFactory implements Serializable {
	
	private static final Log log = LogFactory.getLog(SettingsFactory.class);

	protected SettingsFactory() throws HibernateException {}
	
	public Settings buildSettings(Properties props) {
		Settings settings = new Settings();
		
		//SQL Dialect:

		Dialect dialect = Dialect.getDialect(props);
		
		settings.setDialect(dialect);
		
		//use dialect default properties
		final Properties properties = new Properties();
		properties.putAll( dialect.getDefaultProperties() );
		properties.putAll(props);
		
		//SessionFactory name:
		
		String sessionFactoryName = properties.getProperty(Environment.SESSION_FACTORY_NAME);
		settings.setSessionFactoryName(sessionFactoryName);

		//SQL Exception converter:
		
		SQLExceptionConverter sqlExceptionConverter;
		try {
			sqlExceptionConverter = SQLExceptionConverterFactory.buildSQLExceptionConverter( dialect, properties );
		}
		catch(HibernateException e) {
			log.warn("Error building SQLExceptionConverter; using minimal converter");
			sqlExceptionConverter = SQLExceptionConverterFactory.buildMinimalSQLExceptionConverter();
		}
		settings.setSQLExceptionConverter(sqlExceptionConverter);

		//SQL Generation settings:

		String defaultSchema = properties.getProperty(Environment.DEFAULT_SCHEMA);
		String defaultCatalog = properties.getProperty(Environment.DEFAULT_CATALOG);
		if (defaultSchema!=null) log.info("Default schema: " + defaultSchema);
		if (defaultCatalog!=null) log.info("Default catalog: " + defaultCatalog);
		settings.setDefaultSchemaName(defaultSchema);
		settings.setDefaultCatalogName(defaultCatalog);

		Integer maxFetchDepth = PropertiesHelper.getInteger(Environment.MAX_FETCH_DEPTH, properties);
		if (maxFetchDepth!=null) log.info("Maximum outer join fetch depth: " + maxFetchDepth);
		settings.setMaximumFetchDepth(maxFetchDepth);
		int batchFetchSize = PropertiesHelper.getInt(Environment.DEFAULT_BATCH_FETCH_SIZE, properties, 1);
		log.info("Default batch fetch size: " + batchFetchSize);
		settings.setDefaultBatchFetchSize(batchFetchSize);

		boolean comments = PropertiesHelper.getBoolean(Environment.USE_SQL_COMMENTS, properties);
		log.info( "Generate SQL with comments: " + enabledDisabled(comments) );
		settings.setCommentsEnabled(comments);
		
		boolean orderUpdates = PropertiesHelper.getBoolean(Environment.ORDER_UPDATES, properties);
		log.info( "Order SQL updates by primary key: " + enabledDisabled(orderUpdates) );
		settings.setOrderUpdatesEnabled(orderUpdates);
		
		//Query parser settings:
		
		settings.setQueryTranslatorFactory( createQueryTranslatorFactory(properties) );

		Map querySubstitutions = PropertiesHelper.toMap(Environment.QUERY_SUBSTITUTIONS, " ,=;:\n\t\r\f", properties);
		log.info("Query language substitutions: " + querySubstitutions);
		settings.setQuerySubstitutions(querySubstitutions);
		
		//JDBC and connection settings:

		ConnectionProvider connections = createConnectionProvider(properties);
		settings.setConnectionProvider(connections);
		
		boolean metaSupportsScrollable = false;
		boolean metaSupportsGetGeneratedKeys = false;
		int batchSize = PropertiesHelper.getInt(Environment.STATEMENT_BATCH_SIZE, properties, 0);
		try {
			Connection conn = connections.getConnection();
			try {
				DatabaseMetaData meta = conn.getMetaData();
				metaSupportsScrollable = meta.supportsResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE);
				if ( ( batchSize > 0 ) && !meta.supportsBatchUpdates() ) batchSize = 0;
				if ( Environment.jvmSupportsGetGeneratedKeys() ) {
					try {
						Boolean result = (Boolean) DatabaseMetaData.class.getMethod("supportsGetGeneratedKeys", null)
							.invoke(meta, null);
						metaSupportsGetGeneratedKeys = result.booleanValue();
					}
					catch (AbstractMethodError ame) {
						metaSupportsGetGeneratedKeys = false;
					}
					catch (Exception e) {
						metaSupportsGetGeneratedKeys = false;
					}
				}
			}
			finally {
				connections.closeConnection(conn);
			}
		}
		catch (SQLException sqle) {
			log.warn("Could not obtain connection metadata", sqle);
		}
		catch (UnsupportedOperationException uoe) {
			// user supplied JDBC connections
		}
		
		if (batchSize>0) log.info("JDBC batch size: " + batchSize);
		settings.setJdbcBatchSize(batchSize);
		boolean jdbcBatchVersionedData = PropertiesHelper.getBoolean(Environment.BATCH_VERSIONED_DATA, properties, false);
		if (batchSize>0) log.info("JDBC batch updates for versioned data: " + enabledDisabled(jdbcBatchVersionedData) );
		settings.setJdbcBatchVersionedData(jdbcBatchVersionedData);
		settings.setBatcherFactory( createBatcherFactory(properties, batchSize) );
		
		boolean useScrollableResultSets = PropertiesHelper.getBoolean(Environment.USE_SCROLLABLE_RESULTSET, properties, metaSupportsScrollable);
		log.info("Scrollable result sets: " + enabledDisabled(useScrollableResultSets) );
		settings.setScrollableResultSetsEnabled(useScrollableResultSets);

		boolean wrapResultSets = PropertiesHelper.getBoolean(Environment.WRAP_RESULT_SETS, properties, false);
		log.debug( "Wrap result sets: " + enabledDisabled(wrapResultSets) );
		settings.setWrapResultSetsEnabled(wrapResultSets);

		boolean useGetGeneratedKeys = PropertiesHelper.getBoolean(Environment.USE_GET_GENERATED_KEYS, properties, metaSupportsGetGeneratedKeys);
		log.info("JDBC3 getGeneratedKeys(): " + enabledDisabled(useGetGeneratedKeys) );
		settings.setGetGeneratedKeysEnabled(useGetGeneratedKeys);

		Integer statementFetchSize = PropertiesHelper.getInteger(Environment.STATEMENT_FETCH_SIZE, properties);
		if (statementFetchSize!=null) log.info("JDBC result set fetch size: " + statementFetchSize);
		settings.setJdbcFetchSize(statementFetchSize);

		// Transaction settings:
		
		settings.setTransactionFactory( createTransactionFactory(properties) );
		settings.setTransactionManagerLookup( createTransactionManagerLookup(properties) );
		boolean flushBeforeCompletion = PropertiesHelper.getBoolean(Environment.FLUSH_BEFORE_COMPLETION, properties);
		log.info("Automatic flush during beforeCompletion(): " + enabledDisabled(flushBeforeCompletion) );
		settings.setFlushBeforeCompletionEnabled(flushBeforeCompletion);
		boolean autoCloseSession = PropertiesHelper.getBoolean(Environment.AUTO_CLOSE_SESSION, properties);
		log.info("Automatic session close at end of transaction: " + enabledDisabled(autoCloseSession) );
		settings.setAutoCloseSessionEnabled(autoCloseSession);
		
		// Second-level / query cache:
		
		settings.setCacheProvider( createCacheProvider(properties) );
		
		boolean useSecondLevelCache = PropertiesHelper.getBoolean(Environment.USE_SECOND_LEVEL_CACHE, properties, true);
		log.info( "Second-level cache: " + enabledDisabled(useSecondLevelCache) );
		settings.setSecondLevelCacheEnabled(useSecondLevelCache);
		boolean useMinimalPuts = PropertiesHelper.getBoolean(
				Environment.USE_MINIMAL_PUTS, properties, settings.getCacheProvider().isMinimalPutsEnabledByDefault() 
		);
		log.info( "Optimize cache for minimal puts: " + enabledDisabled(useMinimalPuts) );
		settings.setMinimalPutsEnabled(useMinimalPuts);
		String prefix = properties.getProperty(Environment.CACHE_REGION_PREFIX);
		if ( StringHelper.isEmpty(prefix) ) prefix=null;
		if (prefix!=null) log.info("Cache region prefix: "+ prefix);
		settings.setCacheRegionPrefix(prefix);
		boolean useStructuredCacheEntries = PropertiesHelper.getBoolean(Environment.USE_STRUCTURED_CACHE, properties, false);
		log.info( "Structured second-level cache entries: " + enabledDisabled(useSecondLevelCache) );
		settings.setStructuredCacheEntriesEnabled(useStructuredCacheEntries);
		
		boolean useQueryCache = PropertiesHelper.getBoolean(Environment.USE_QUERY_CACHE, properties);
		log.info( "Query cache: " + enabledDisabled(useQueryCache) );
		if (useQueryCache) settings.setQueryCacheFactory( createQueryCacheFactory(properties) );
		settings.setQueryCacheEnabled(useQueryCache);
		
		//Statistics and logging:

		boolean showSql = PropertiesHelper.getBoolean(Environment.SHOW_SQL, properties);
		if (showSql) log.info("Echoing all SQL to stdout");
		settings.setShowSqlEnabled(showSql);

		boolean useStatistics = PropertiesHelper.getBoolean(Environment.GENERATE_STATISTICS, properties);
		log.info( "Statistics: " + enabledDisabled(useStatistics) );
		settings.setStatisticsEnabled(useStatistics);
		
		boolean useIdentifierRollback = PropertiesHelper.getBoolean(Environment.USE_IDENTIFIER_ROLLBACK, properties);
		log.info( "Deleted entity synthetic identifier rollback: " + enabledDisabled(useIdentifierRollback) );
		settings.setIdentifierRollbackEnabled(useIdentifierRollback);
		
		//Schema export:
		
		String autoSchemaExport = properties.getProperty(Environment.HBM2DDL_AUTO);
		if ( "update".equals(autoSchemaExport) ) settings.setAutoUpdateSchema(true);
		if ( "create".equals(autoSchemaExport) ) settings.setAutoCreateSchema(true);
		if ( "create-drop".equals(autoSchemaExport) ) {
			settings.setAutoCreateSchema(true);
			settings.setAutoDropSchema(true);
		}

		EntityMode defaultEntityMode = EntityMode.parse( properties.getProperty( Environment.DEFAULT_ENTITY_MODE ) );
		log.info( "Default entity-mode: " + defaultEntityMode );
		settings.setDefaultEntityMode( defaultEntityMode );

		return settings;

	}
	
	private static final String enabledDisabled(boolean value) {
		return value ? "enabled" : "disabled";
	}
	
	protected QueryCacheFactory createQueryCacheFactory(Properties properties) {
		String queryCacheFactoryClassName = PropertiesHelper.getString(
				Environment.QUERY_CACHE_FACTORY, properties, "org.hibernate.cache.StandardQueryCacheFactory"
		);
		log.info("Query cache factory: " + queryCacheFactoryClassName);
		try {
			return (QueryCacheFactory) ReflectHelper.classForName(queryCacheFactoryClassName).newInstance();
		}
		catch (Exception cnfe) {
			throw new HibernateException("could not instantiate QueryCacheFactory: " + queryCacheFactoryClassName, cnfe);
		}
	}
	
	protected CacheProvider createCacheProvider(Properties properties) {
		String cacheClassName = PropertiesHelper.getString(
				Environment.CACHE_PROVIDER, properties, "org.hibernate.cache.EhCacheProvider"
		);
		log.info("Cache provider: " + cacheClassName);
		try {
			return (CacheProvider) ReflectHelper.classForName(cacheClassName).newInstance();
		}
		catch (Exception cnfe) {
			throw new HibernateException("could not instantiate CacheProvider: " + cacheClassName, cnfe);
		}
	}
	
	protected QueryTranslatorFactory createQueryTranslatorFactory(Properties properties) {
		String className = PropertiesHelper.getString(
				Environment.QUERY_TRANSLATOR, properties, "org.hibernate.hql.ast.ASTQueryTranslatorFactory"
		);
		log.info("Query translator: " + className);
		try {
			return (QueryTranslatorFactory) ReflectHelper.classForName(className).newInstance();
		}
		catch (Exception cnfe) {
			throw new HibernateException("could not instantiate QueryTranslatorFactory: " + className, cnfe);
		}
	}
	
	protected BatcherFactory createBatcherFactory(Properties properties, int batchSize) {
		String batcherClass = properties.getProperty(Environment.BATCH_STRATEGY);
		if (batcherClass==null) {
			return batchSize==0 ?
					(BatcherFactory) new NonBatchingBatcherFactory() :
					(BatcherFactory) new BatchingBatcherFactory();
		}
		else {
			log.info("Batcher factory: " + batcherClass);
			try {
				return (BatcherFactory) ReflectHelper.classForName(batcherClass).newInstance();
			}
			catch (Exception cnfe) {
				throw new HibernateException("could not instantiate BatcherFactory: " + batcherClass, cnfe);
			}
		}
	}
	
	protected ConnectionProvider createConnectionProvider(Properties properties) {
		return ConnectionProviderFactory.newConnectionProvider(properties);
	}
	
	protected TransactionFactory createTransactionFactory(Properties properties) {
		return TransactionFactoryFactory.buildTransactionFactory(properties);
	}
	
	protected TransactionManagerLookup createTransactionManagerLookup(Properties properties) {
		return TransactionManagerLookupFactory.getTransactionManagerLookup(properties);		
	}
}
