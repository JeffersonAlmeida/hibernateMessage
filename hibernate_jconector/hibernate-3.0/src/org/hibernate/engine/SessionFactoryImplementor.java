//$Id: SessionFactoryImplementor.java,v 1.22 2005/02/13 11:49:58 oneovthafew Exp $
package org.hibernate.engine;

import java.util.Map;

import javax.transaction.TransactionManager;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.cache.Cache;
import org.hibernate.cache.QueryCache;
import org.hibernate.cache.UpdateTimestampsCache;
import org.hibernate.cfg.Settings;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.dialect.Dialect;
import org.hibernate.exception.SQLExceptionConverter;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.stat.StatisticsImplementor;
import org.hibernate.type.Type;

/**
 * Defines the internal contract between the <tt>SessionFactory</tt> and other parts of
 * Hibernate such as implementors of <tt>Type</tt>.
 *
 * @see org.hibernate.SessionFactory
 * @see org.hibernate.impl.SessionFactoryImpl
 * @author Gavin King
 */
public interface SessionFactoryImplementor extends Mapping, SessionFactory {

	/**
	 * Get the persister for the named entity
	 */
	public EntityPersister getEntityPersister(String entityName) throws MappingException;
	/**
	 * Get the persister object for a collection role
	 */
	public CollectionPersister getCollectionPersister(String role) throws MappingException;

	/**
	 * Get the SQL <tt>Dialect</tt>
	 */
	public Dialect getDialect();

	/**
	 * Get the return types of a query
	 */
	public Type[] getReturnTypes(String queryString) throws HibernateException;

	/**
	 * Get the connection provider
	 */
	public ConnectionProvider getConnectionProvider();
	/**
	 * Get the names of all persistent classes that implement/extend the given interface/class
	 */
	public String[] getImplementors(String className) throws MappingException;
	/**
	 * Get a class name, using query language imports
	 */
	public String getImportedClassName(String name);


	/**
	 * Get the JTA transaction manager
	 */
	public TransactionManager getTransactionManager();


	/**
	 * Get the default query cache
	 */
	public QueryCache getQueryCache();
	/**
	 * Get a particular named query cache, or the default cache
	 * @param regionName the name of the cache region, or null for the default query cache
	 * @return the existing cache, or a newly created cache if none by that region name
	 */
	public QueryCache getQueryCache(String regionName) throws HibernateException;
	
	/**
	 * Get the cache of table update timestamps
	 */
	public UpdateTimestampsCache getUpdateTimestampsCache();
	/**
	 * Statistics SPI
	 */
	public StatisticsImplementor getStatisticsImplementor();
	
	public NamedQueryDefinition getNamedQuery(String queryName);
	public NamedSQLQueryDefinition getNamedSQLQuery(String queryName);
	
	/**
	 * Get the identifier generator for the hierarchy
	 */
	public IdentifierGenerator getIdentifierGenerator(String rootEntityName);
	
	/**
	 * Get a named second-level cache region
	 */
	public Cache getSecondLevelCacheRegion(String regionName);
	
	public Map getAllSecondLevelCacheRegions();
	
	/**
	 * Retrieves the SQLExceptionConverter in effect for this SessionFactory.
	 *
	 * @return The SQLExceptionConverter for this SessionFactory.
	 */
	public SQLExceptionConverter getSQLExceptionConverter();

	public Settings getSettings();
}
