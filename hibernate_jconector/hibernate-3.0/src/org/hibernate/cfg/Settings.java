//$Id: Settings.java,v 1.15 2005/02/12 07:19:10 steveebersole Exp $
package org.hibernate.cfg;

import java.util.Map;

import org.hibernate.cache.CacheProvider;
import org.hibernate.cache.QueryCacheFactory;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.dialect.Dialect;
import org.hibernate.hql.QueryTranslatorFactory;
import org.hibernate.jdbc.BatcherFactory;
import org.hibernate.transaction.TransactionFactory;
import org.hibernate.transaction.TransactionManagerLookup;
import org.hibernate.exception.SQLExceptionConverter;
import org.hibernate.EntityMode;

/**
 * Settings that affect the behaviour of Hibernate at runtime.
 *
 * @author Gavin King
 */
public final class Settings {

	private boolean showSql;
	private Integer maximumFetchDepth;
	private Map querySubstitutions;
	private Dialect dialect;
	private int jdbcBatchSize;
	private int defaultBatchFetchSize;
	private boolean scrollableResultSetsEnabled;
	private boolean getGeneratedKeysEnabled;
	private String defaultSchemaName;
	private String defaultCatalogName;
	private Integer jdbcFetchSize;
	private String sessionFactoryName;
	private boolean autoCreateSchema;
	private boolean autoDropSchema;
	private boolean autoUpdateSchema;
	private boolean queryCacheEnabled;
	private boolean structuredCacheEntriesEnabled;
	private boolean secondLevelCacheEnabled;
	private String cacheRegionPrefix;
	private boolean minimalPutsEnabled;
	private boolean commentsEnabled;
	private boolean statisticsEnabled;
	private boolean jdbcBatchVersionedData;
	private boolean identifierRollbackEnabled;
	private boolean flushBeforeCompletionEnabled;
	private boolean autoCloseSessionEnabled;
	private CacheProvider cacheProvider;
	private QueryCacheFactory queryCacheFactory;
	private ConnectionProvider connectionProvider;
	private TransactionFactory transactionFactory;
	private TransactionManagerLookup transactionManagerLookup;
	private BatcherFactory batcherFactory;
	private QueryTranslatorFactory queryTranslatorFactory;
	private SQLExceptionConverter sqlExceptionConverter;
	private boolean wrapResultSetsEnabled;
	private boolean orderUpdatesEnabled;
	private EntityMode defaultEntityMode;

	public String getDefaultSchemaName() {
		return defaultSchemaName;
	}

    public String getDefaultCatalogName() {
        return defaultCatalogName;
    }

	public Dialect getDialect() {
		return dialect;
	}

	public int getJdbcBatchSize() {
		return jdbcBatchSize;
	}

	public int getDefaultBatchFetchSize() {
		return defaultBatchFetchSize;
	}

	public Map getQuerySubstitutions() {
		return querySubstitutions;
	}

	public boolean isShowSqlEnabled() {
		return showSql;
	}

	public boolean isIdentifierRollbackEnabled() {
		return identifierRollbackEnabled;
	}

	public boolean isScrollableResultSetsEnabled() {
		return scrollableResultSetsEnabled;
	}

	public boolean isGetGeneratedKeysEnabled() {
		return getGeneratedKeysEnabled;
	}

	public boolean isMinimalPutsEnabled() {
		return minimalPutsEnabled;
	}

	void setDefaultSchemaName(String string) {
		defaultSchemaName = string;
	}

    void setDefaultCatalogName(String string) {
        defaultCatalogName = string;
    }

	void setDialect(Dialect dialect) {
		this.dialect = dialect;
	}

	void setJdbcBatchSize(int i) {
		jdbcBatchSize = i;
	}

	void setDefaultBatchFetchSize(int i) {
		defaultBatchFetchSize = i;
	}

	void setQuerySubstitutions(Map map) {
		querySubstitutions = map;
	}

	void setShowSqlEnabled(boolean b) {
		showSql = b;
	}

	void setIdentifierRollbackEnabled(boolean b) {
		identifierRollbackEnabled = b;
	}

	void setMinimalPutsEnabled(boolean b) {
		minimalPutsEnabled = b;
	}

	void setScrollableResultSetsEnabled(boolean b) {
		scrollableResultSetsEnabled = b;
	}

	void setGetGeneratedKeysEnabled(boolean b) {
		getGeneratedKeysEnabled = b;
	}

	public Integer getJdbcFetchSize() {
		return jdbcFetchSize;
	}
	void setJdbcFetchSize(Integer integer) {
		jdbcFetchSize = integer;
	}

	public ConnectionProvider getConnectionProvider() {
		return connectionProvider;
	}
	void setConnectionProvider(ConnectionProvider provider) {
		connectionProvider = provider;
	}

	public TransactionFactory getTransactionFactory() {
		return transactionFactory;
	}
	void setTransactionFactory(TransactionFactory factory) {
		transactionFactory = factory;
	}

	public String getSessionFactoryName() {
		return sessionFactoryName;
	}
	void setSessionFactoryName(String string) {
		sessionFactoryName = string;
	}

	public boolean isAutoCreateSchema() {
		return autoCreateSchema;
	}
	public boolean isAutoDropSchema() {
		return autoDropSchema;
	}

	public boolean isAutoUpdateSchema() {
		return autoUpdateSchema;
	}
	void setAutoCreateSchema(boolean b) {
		autoCreateSchema = b;
	}

	void setAutoDropSchema(boolean b) {
		autoDropSchema = b;
	}
	void setAutoUpdateSchema(boolean b) {
		autoUpdateSchema = b;
	}

	public Integer getMaximumFetchDepth() {
		return maximumFetchDepth;
	}
	void setMaximumFetchDepth(Integer i) {
		maximumFetchDepth = i;
	}

	public CacheProvider getCacheProvider() {
		return cacheProvider;
	}
	void setCacheProvider(CacheProvider cacheProvider) {
		this.cacheProvider = cacheProvider;
	}

	public TransactionManagerLookup getTransactionManagerLookup() {
		return transactionManagerLookup;
	}
	void setTransactionManagerLookup(TransactionManagerLookup lookup) {
		transactionManagerLookup = lookup;
	}

	public boolean isQueryCacheEnabled() {
		return queryCacheEnabled;
	}
	void setQueryCacheEnabled(boolean b) {
		queryCacheEnabled = b;
	}

	public boolean isCommentsEnabled() {
		return commentsEnabled;
	}
	void setCommentsEnabled(boolean commentsEnabled) {
		this.commentsEnabled = commentsEnabled;
	}

	public boolean isSecondLevelCacheEnabled() {
		return secondLevelCacheEnabled;
	}
	void setSecondLevelCacheEnabled(boolean secondLevelCacheEnabled) {
		this.secondLevelCacheEnabled = secondLevelCacheEnabled;
	}

	public String getCacheRegionPrefix() {
		return cacheRegionPrefix;
	}
	void setCacheRegionPrefix(String cacheRegionPrefix) {
		this.cacheRegionPrefix = cacheRegionPrefix;
	}

	public QueryCacheFactory getQueryCacheFactory() {
		return queryCacheFactory;
	}
	public void setQueryCacheFactory(QueryCacheFactory queryCacheFactory) {
		this.queryCacheFactory = queryCacheFactory;
	}

	public boolean isStatisticsEnabled() {
		return statisticsEnabled;
	}
	void setStatisticsEnabled(boolean statisticsEnabled) {
		this.statisticsEnabled = statisticsEnabled;
	}

	public boolean isJdbcBatchVersionedData() {
		return jdbcBatchVersionedData;
	}
	void setJdbcBatchVersionedData(boolean jdbcBatchVersionedData) {
		this.jdbcBatchVersionedData = jdbcBatchVersionedData;
	}

	public boolean isFlushBeforeCompletionEnabled() {
		return flushBeforeCompletionEnabled;
	}
	void setFlushBeforeCompletionEnabled(boolean flushBeforeCompletionEnabled) {
		this.flushBeforeCompletionEnabled = flushBeforeCompletionEnabled;
	}

	public BatcherFactory getBatcherFactory() {
		return batcherFactory;
	}
	void setBatcherFactory(BatcherFactory batcher) {
		this.batcherFactory = batcher;
	}
	
	public boolean isAutoCloseSessionEnabled() {
		return autoCloseSessionEnabled;
	}
	void setAutoCloseSessionEnabled(boolean autoCloseSessionEnabled) {
		this.autoCloseSessionEnabled = autoCloseSessionEnabled;
	}
	
	public QueryTranslatorFactory getQueryTranslatorFactory() {
		return queryTranslatorFactory;
	}
	
	void setQueryTranslatorFactory(QueryTranslatorFactory queryTranslatorFactory) {
		this.queryTranslatorFactory = queryTranslatorFactory;
	}

	public SQLExceptionConverter getSQLExceptionConverter() {
		return sqlExceptionConverter;
	}

	void setSQLExceptionConverter(SQLExceptionConverter sqlExceptionConverter) {
		this.sqlExceptionConverter = sqlExceptionConverter;
	}

	public boolean isWrapResultSetsEnabled() {
		return wrapResultSetsEnabled;
	}

	void setWrapResultSetsEnabled(boolean wrapResultSetsEnabled) {
		this.wrapResultSetsEnabled = wrapResultSetsEnabled;
	}
	
	public boolean isOrderUpdatesEnabled() {
		return orderUpdatesEnabled;
	}
	void setOrderUpdatesEnabled(boolean orderUpdatesEnabled) {
		this.orderUpdatesEnabled = orderUpdatesEnabled;
	}

	public boolean isStructuredCacheEntriesEnabled() {
		return structuredCacheEntriesEnabled;
	}
	void setStructuredCacheEntriesEnabled(boolean structuredCacheEntriesEnabled) {
		this.structuredCacheEntriesEnabled = structuredCacheEntriesEnabled;
	}

	public EntityMode getDefaultEntityMode() {
		return defaultEntityMode;
	}

	public void setDefaultEntityMode(EntityMode defaultEntityMode) {
		this.defaultEntityMode = defaultEntityMode;
	}

}
