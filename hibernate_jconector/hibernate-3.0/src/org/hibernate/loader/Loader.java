//$Id: Loader.java,v 1.94 2005/03/21 20:08:46 oneovthafew Exp $
package org.hibernate.loader;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.AssertionFailure;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.QueryException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.StaleObjectStateException;
import org.hibernate.WrongClassException;
import org.hibernate.cache.QueryCache;
import org.hibernate.cache.QueryKey;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.EntityUniqueKey;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.RowSelection;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.SubselectFetch;
import org.hibernate.engine.TwoPhaseLoad;
import org.hibernate.event.PostLoadEvent;
import org.hibernate.event.PreLoadEvent;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.impl.ScrollableResultsImpl;
import org.hibernate.jdbc.ColumnNameCache;
import org.hibernate.jdbc.ResultSetWrapper;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;
import org.hibernate.type.VersionType;
import org.hibernate.util.StringHelper;

/**
 * Abstract superclass of object loading (and querying) strategies. This class implements
 * useful common functionality that concrete loaders delegate to. It is not intended that this
 * functionality would be directly accessed by client code. (Hence, all methods of this class
 * are declared <tt>protected</tt> or <tt>private</tt>.) This class relies heavily upon the
 * <tt>Loadable</tt> interface, which is the contract between this class and
 * <tt>EntityPersister</tt>s that may be loaded by it.<br>
 * <br>
 * The present implementation is able to load any number of columns of entities and at most
 * one collection role per query.
 *
 * @author Gavin King
 * @see org.hibernate.persister.entity.Loadable
 */
public abstract class Loader {

	private static final Log log = LogFactory.getLog( Loader.class );

	private final SessionFactoryImplementor factory;
	private ColumnNameCache columnNameCache;

	public Loader(SessionFactoryImplementor factory) {
		this.factory = factory;
	}

	/**
	 * The SQL query string to be called; implemented by all subclasses
	 */
	protected abstract String getSQLString();

	/**
	 * An array of persisters of entity classes contained in each row of results;
	 * implemented by all subclasses
	 */
	protected abstract Loadable[] getEntityPersisters();

	/**
	 * The suffix identifies a particular column of results in the SQL <tt>ResultSet</tt>;
	 * implemented by all subclasses
	 */
	//protected abstract String[] getSuffixes();

	/**
	 * An array of indexes of the entity that owns a one-to-one association
	 * to the entity at the given index (-1 if there is no "owner")
	 */
	protected int[] getOwners() {
		return null;
	}

	/**
	 * An array of unique key property names by which the corresponding
	 * entities are referenced by other entities in the result set
	 */
	protected AssociationType[] getOwnerAssociationTypes() {
		return null;
	}

	/**
	 * An (optional) persister for a collection to be initialized; only collection loaders
	 * return a non-null value
	 */
	protected CollectionPersister getCollectionPersister() {
		return null;
	}

	/**
	 * Get the index of the entity that owns the collection, or -1
	 * if there is no owner in the query results (ie. in the case of a
	 * collection initializer) or no collection.
	 */
	protected int getCollectionOwner() {
		return -1;
	}

	/**
	 * What lock mode does this load entities with?
	 *
	 * @param lockModes a collection of lock modes specified dynamically via the Query interface
	 */
	protected abstract LockMode[] getLockModes(Map lockModes);

	/**
	 * Append <tt>FOR UPDATE OF</tt> clause, if necessary. This
	 * empty superclass implementation merely returns its first
	 * argument.
	 */
	protected String applyLocks(String sql, Map lockModes, Dialect dialect) throws HibernateException {
		return sql;
	}

	/**
	 * Does this query return objects that might be already cached
	 * by the session, whose lock mode may need upgrading
	 */
	protected boolean upgradeLocks() {
		return false;
	}

	/**
	 * Return false is this loader is a batch entity loader
	 */
	protected boolean isSingleRowLoader() {
		return false;
	}

	/**
	 * Get the SQL table aliases of entities whose
	 * associations are subselect-loadable, returning
	 * null if this loader does not support subselect
	 * loading
	 */
	protected String[] getAliases() {
		return null;
	}

	/**
	 * Modify the SQL, adding lock hints and comments, if necessary
	 */
	protected String preprocessSQL(String sql, QueryParameters parameters, Dialect dialect)
			throws HibernateException {
		sql = applyLocks( sql, parameters.getLockModes(), dialect );
		String comment = parameters.getComment();
		if ( comment == null ) {
			return sql;
		}
		else {
			return new StringBuffer( comment.length() + sql.length() + 5 )
					.append( "/*" )
					.append( comment )
					.append( "*/ " )
					.append( sql )
					.toString();
		}
	}

	/**
	 * Execute an SQL query and attempt to instantiate instances of the class mapped by the given
	 * persister from each row of the <tt>ResultSet</tt>. If an object is supplied, will attempt to
	 * initialize that object. If a collection is supplied, attempt to initialize that collection.
	 */
	private List doQueryAndInitializeNonLazyCollections(final SessionImplementor session,
														final QueryParameters queryParameters,
														final boolean returnProxies) 
		throws HibernateException, SQLException {

		final PersistenceContext persistenceContext = session.getPersistenceContext();
		persistenceContext.beforeLoad();
		List result;
		try {
			result = doQuery( session, queryParameters, returnProxies );
		}
		finally {
			persistenceContext.afterLoad();
		}
		persistenceContext.initializeNonLazyCollections();
		return result;
	}

	public Object loadSingleRow(final ResultSet resultSet,
								final SessionImplementor session,
								final QueryParameters queryParameters,
								final boolean returnProxies)
			throws HibernateException {

		final int entitySpan = getEntityPersisters().length;
		final List hydratedObjects = entitySpan == 0 ? null : new ArrayList( entitySpan );

		final Object result;
		try {
			result = getRowFromResultSet( resultSet,
					session,
					queryParameters,
					getLockModes( queryParameters.getLockModes() ),
					null,
					hydratedObjects,
					new EntityKey[entitySpan],
					returnProxies );
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
			        factory.getSQLExceptionConverter(),
			        sqle,
			        "could not read next row of results",
			        getSQLString()
			);
		}

		initializeEntitiesAndCollections( hydratedObjects, resultSet, session, queryParameters.isReadOnly() );
		session.getPersistenceContext().initializeNonLazyCollections();
		return result;
	}

	private static EntityKey getOptionalObjectKey(QueryParameters queryParameters, SessionImplementor session) {
		final Object optionalObject = queryParameters.getOptionalObject();
		final Serializable optionalId = queryParameters.getOptionalId();
		final String optionalEntityName = queryParameters.getOptionalEntityName();

		if ( optionalObject != null && optionalEntityName != null ) {
			return new EntityKey( optionalId,
					session.getEntityPersister( optionalEntityName, optionalObject ), 
					session.getEntityMode()
			);
		}
		else {
			return null;
		}

	}

	private Object getRowFromResultSet(final ResultSet resultSet,
									   final SessionImplementor session,
									   final QueryParameters queryParameters,
									   final LockMode[] lockModeArray,
									   final EntityKey optionalObjectKey,
									   final List hydratedObjects,
									   final EntityKey[] keys,
									   boolean returnProxies)
			throws SQLException, HibernateException {

		final Loadable[] persisters = getEntityPersisters();
		final int entitySpan = persisters.length;

		for ( int i = 0; i < entitySpan; i++ ) {
			keys[i] = getKeyFromResultSet( i,
					persisters[i],
					i == entitySpan - 1 ?
					queryParameters.getOptionalId() :
					null,
					resultSet,
					session );
			//TODO: the i==entitySpan-1 bit depends upon subclass implementation (very bad)
		}

		registerNonExists( keys, persisters, session );

		// this call is side-effecty
		Object[] row = getRow( resultSet,
				persisters,
				keys,
				queryParameters.getOptionalObject(),
				optionalObjectKey,
				lockModeArray,
				hydratedObjects,
				session );

		readCollectionElements( row, resultSet, session );

		if ( returnProxies ) {
			// now get an existing proxy for each row element (if there is one)
			for ( int i = 0; i < entitySpan; i++ ) {
				row[i] = session.getPersistenceContext().proxyFor( persisters[i], keys[i], row[i] );
				// force the proxy to resolve itself
				Hibernate.initialize( row[i] ); //TODO: if there is an uninitialized proxy, this involves a hashmap lookup that could be avoided
			}
		}

		return getResultColumnOrRow( row, resultSet, session );

	}

	/**
	 * Read any collection elements contained in a single row of the result set
	 */
	private void readCollectionElements(Object[] row, ResultSet resultSet, SessionImplementor session)
			throws SQLException, HibernateException {

		//TODO: make this handle multiple collection roles!

		final CollectionPersister collectionPersister = getCollectionPersister();
		if ( collectionPersister != null ) {

			final int collectionOwner = getCollectionOwner();
			final boolean hasCollectionOwners = collectionOwner >= 0;
			//true if this is a query and we are loading multiple instances of the same collection role
			//otherwise this is a CollectionInitializer and we are loading up a single collection or batch

			final Object owner = hasCollectionOwners ?
					row[collectionOwner] :
					null; //if null, owner will be retrieved from session

			final Serializable key;
			if ( owner == null ) {
				key = null;
			}
			else {
				key = collectionPersister.getCollectionType().getKeyOfOwner( owner, session );
				//TODO: old version did not require hashmap lookup:
				//keys[collectionOwner].getIdentifier()
			}

			readCollectionElement( owner, key, resultSet, session );

		}
	}

	private List doQuery(final SessionImplementor session,
						 final QueryParameters queryParameters,
						 final boolean returnProxies) throws SQLException, HibernateException {

		final RowSelection selection = queryParameters.getRowSelection();
		final int maxRows = hasMaxRows( selection ) ?
				selection.getMaxRows().intValue() :
				Integer.MAX_VALUE;

		final int entitySpan = getEntityPersisters().length;

		final ArrayList hydratedObjects = entitySpan == 0 ? null : new ArrayList( entitySpan * 10 );
		final List results = new ArrayList();
		final PreparedStatement st = prepareQueryStatement( queryParameters, false, session );
		final ResultSet rs = getResultSet( st, queryParameters.isCallable(), selection, session );

		final LockMode[] lockModeArray = getLockModes( queryParameters.getLockModes() );
		final EntityKey optionalObjectKey = getOptionalObjectKey( queryParameters, session );

		final boolean createSubselects = isSubselectLoadingEnabled();
		final List subselectResultKeys = createSubselects ? new ArrayList() : null;

		try {

			handleEmptyCollections( queryParameters.getCollectionKeys(), rs, session );

			EntityKey[] keys = new EntityKey[entitySpan]; //we can reuse it for each row

			if ( log.isTraceEnabled() ) log.trace( "processing result set" );

			int count;
			for ( count = 0; count < maxRows && rs.next(); count++ ) {
				
				if ( log.isTraceEnabled() ) log.debug("result set row: " + count);

				Object result = getRowFromResultSet( rs,
						session,
						queryParameters,
						lockModeArray,
						optionalObjectKey,
						hydratedObjects,
						keys,
						returnProxies );
				results.add( result );

				if ( createSubselects ) {
					subselectResultKeys.add(keys);
					keys = new EntityKey[entitySpan]; //can't reuse in this case
				}
				
			}

			if ( log.isTraceEnabled() ) log.trace( "done processing result set (" + count + " rows)" );

		}
		finally {
			session.getBatcher().closeQueryStatement( st, rs );
		}

		initializeEntitiesAndCollections( hydratedObjects, rs, session, queryParameters.isReadOnly() );

		if ( createSubselects ) createSubselects( subselectResultKeys, queryParameters, session );

		return results; //getResultList(results);

	}

	protected boolean isSubselectLoadingEnabled() {
		return false;
	}
	
	protected boolean hasSubselectLoadableCollections() {
		final Loadable[] loadables = getEntityPersisters();
		for (int i=0; i<loadables.length; i++ ) {
			if ( loadables[i].hasSubselectLoadableCollections() ) return true;
		}
		return false;
	}
	
	private static EntityKey[][] transpose( EntityKey[][] keys ) {
		EntityKey[][] result = new EntityKey[ keys[0].length ][];
		for ( int j=0; j<result.length; j++ ) {
			result[j] = new EntityKey[keys.length];
			for ( int i=0; i<keys.length; i++ ) {
				result[j][i] = keys[i][j];
			}
		}
		return result;
	}

	private void createSubselects(List keys, QueryParameters queryParameters, SessionImplementor session) {
		if ( keys.size() > 1 ) { //if we only returned one entity, query by key is more efficient
			
			//TODO: remove unnecessary creation of outer array
			EntityKey[][] keyArray = transpose( ( EntityKey[][] ) keys.toArray( new EntityKey[ keys.size() ][] ) );
			
			final Loadable[] loadables = getEntityPersisters();
			final String[] aliases = getAliases();
			final Iterator iter = keys.iterator();
			while ( iter.hasNext() ) {
				
				final EntityKey[] rowKeys = (EntityKey[]) iter.next();
				for ( int i=0; i<rowKeys.length; i++ ) {
					
					if ( rowKeys[i]!=null && loadables[i].hasSubselectLoadableCollections() ) {
						
						SubselectFetch subselectFetch = new SubselectFetch( 
								//getSQLString(), 
								aliases[i], 
								loadables[i], 
								queryParameters, 
								keyArray[i] 
							);
						
						session.getPersistenceContext()
							.getBatchFetchQueue()
							.addSubselect( rowKeys[i], subselectFetch );
					}
					
				}
				
			}
		}
	}

	private void initializeEntitiesAndCollections(
			final List hydratedObjects,
			final Object resultSetId,
			final SessionImplementor session,
			final boolean readOnly) 
	throws HibernateException {
		
		//important: reuse the same event instances for performance!
		final PreLoadEvent pre = new PreLoadEvent(session);
		final PostLoadEvent post = new PostLoadEvent(session);
		
		if ( getEntityPersisters().length > 0 ) { //if no persisters, hydratedObjects is null
			int hydratedObjectsSize = hydratedObjects.size();
			if ( log.isTraceEnabled() ) log.trace( "total objects hydrated: " + hydratedObjectsSize );
			for ( int i = 0; i < hydratedObjectsSize; i++ ) {
				TwoPhaseLoad.initializeEntity( hydratedObjects.get(i), readOnly, session, pre, post );
			}
		}
		
		final CollectionPersister collectionPersister = getCollectionPersister();
		if ( collectionPersister != null ) {
			//this is a query and we are loading multiple instances of the same collection role
			session.getPersistenceContext().getCollectionLoadContext()
				.endLoadingCollections( collectionPersister, resultSetId, session.getEntityMode() );
		}
	}

	protected List getResultList(List results) throws QueryException {
		return results;
	}

	/**
	 * Get the actual object that is returned in the user-visible result list.
	 * This empty implementation merely returns its first argument. This is
	 * overridden by some subclasses.
	 */
	protected Object getResultColumnOrRow(Object[] row, ResultSet rs, SessionImplementor session)
			throws SQLException, HibernateException {
		return row;
	}

	/**
	 * For missing objects associated by one-to-one with another object in the
	 * result set, register the fact that the the object is missing with the
	 * session.
	 */
	private void registerNonExists(final EntityKey[] keys,
								   final Loadable[] persisters,
								   final SessionImplementor session) {
		
		final int[] owners = getOwners();
		if ( owners != null ) {
			
			AssociationType[] ownerAssociationTypes = getOwnerAssociationTypes();
			for ( int i = 0; i < keys.length; i++ ) {
				
				int owner = owners[i];
				if ( owner > -1 ) {
					EntityKey ownerKey = keys[owner];
					if ( keys[i] == null && ownerKey != null ) {
						
						final PersistenceContext persistenceContext = session.getPersistenceContext();
						
						final boolean isPrimaryKey;
						final boolean isSpecialOneToOne;
						if ( ownerAssociationTypes == null || ownerAssociationTypes[i] == null ) {
							isPrimaryKey = true;
							isSpecialOneToOne = false;
						}
						else {
							isPrimaryKey = ownerAssociationTypes[i].getRHSUniqueKeyPropertyName()==null;
							isSpecialOneToOne = ownerAssociationTypes[i].getLHSPropertyName()!=null;
						}
						
						//TODO: can we *always* use the "null property" approach for everything?
						if ( isPrimaryKey && !isSpecialOneToOne ) {
							persistenceContext.addNonExistantEntityKey( 
									new EntityKey( ownerKey.getIdentifier(), persisters[i], session.getEntityMode() ) 
							);
						}
						else if ( isSpecialOneToOne ) {
							persistenceContext.addNullProperty( ownerKey, 
									ownerAssociationTypes[i].getLHSPropertyName() );
						}
						else {
							persistenceContext.addNonExistantEntityUniqueKey( new EntityUniqueKey( 
									persisters[i].getEntityName(),
									ownerAssociationTypes[i].getRHSUniqueKeyPropertyName(),
									ownerKey.getIdentifier(),
									persisters[owner].getIdentifierType(),
									session.getEntityMode()
							) );
						}
					}
				}
			}
		}
	}

	/**
	 * Read one collection element from the current row of the JDBC result set
	 */
	private void readCollectionElement(final Object optionalOwner,
									   final Serializable optionalKey,
									   final ResultSet rs,
									   final SessionImplementor session)
			throws HibernateException, SQLException {

		final CollectionPersister collectionPersister = getCollectionPersister();
		final Serializable collectionRowKey = ( Serializable ) collectionPersister.readKey( rs, session );
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		if ( collectionRowKey != null ) {
			// we found a collection element in the result set

			if ( log.isDebugEnabled() ) {
				log.debug( "found row of collection: " +
						MessageHelper.collectionInfoString( collectionPersister, collectionRowKey, getFactory() ) );
			}

			Object owner = optionalOwner;
			if ( owner == null ) {
				owner = persistenceContext.getCollectionOwner( collectionRowKey, collectionPersister );
				if ( owner == null ) {
					//TODO: This is assertion is disabled because there is a bug that means the
					//	  original owner of a transient, uninitialized collection is not known
					//	  if the collection is re-referenced by a different object associated
					//	  with the current Session
					//throw new AssertionFailure("bug loading unowned collection");
				}
			}

			PersistentCollection rowCollection = persistenceContext.getCollectionLoadContext()
				.getLoadingCollection( collectionPersister, collectionRowKey, rs, session.getEntityMode() );
			if ( rowCollection != null ) rowCollection.readFrom( rs, collectionPersister, owner );

		}
		else if ( optionalKey != null ) {
			// we did not find a collection element in the result set, so we
			// ensure that a collection is created with the owner's identifier,
			// since what we have is an empty collection

			if ( log.isDebugEnabled() ) {
				log.debug( "result set contains (possibly empty) collection: " +
						MessageHelper.collectionInfoString( collectionPersister, optionalKey, getFactory() ) );
			}

			persistenceContext.getCollectionLoadContext()
				.getLoadingCollection( collectionPersister, optionalKey, rs, session.getEntityMode() ); //handle empty collection

		}

		// else no collection element, but also no owner
	}

	/**
	 * If this is a collection initializer, we need to tell the session that a collection
	 * is being initilized, to account for the possibility of the collection having
	 * no elements (hence no rows in the result set).
	 */
	private void handleEmptyCollections(final Serializable[] keys,
										final Object resultSetId,
										final SessionImplementor session)
			throws HibernateException {

		if ( keys != null ) {
			// this is a collection initializer, so we must create a collection
			// for each of the passed-in keys, to account for the possibility
			// that the collection is empty and has no rows in the result set

			CollectionPersister collectionPersister = getCollectionPersister();
			for ( int i = 0; i < keys.length; i++ ) {
				//handle empty collections

				if ( log.isDebugEnabled() ) {
					log.debug( "result set contains (possibly empty) collection: " +
							MessageHelper.collectionInfoString( collectionPersister, keys[i], getFactory() ) );
				}

				session.getPersistenceContext()
					.getCollectionLoadContext()
					.getLoadingCollection( collectionPersister, keys[i], resultSetId, session.getEntityMode() );
			}

		}

		// else this is not a collection initializer (and empty collections will
		// be detected by looking for the owner's identifier in the result set)
	}

	/**
	 * Read a row of <tt>Key</tt>s from the <tt>ResultSet</tt> into the given array.
	 * Warning: this method is side-effecty.
	 * <p/>
	 * If an <tt>id</tt> is given, don't bother going to the <tt>ResultSet</tt>.
	 */
	private EntityKey getKeyFromResultSet(final int i,
										  final Loadable persister,
										  final Serializable id,
										  final ResultSet rs,
										  final SessionImplementor session)
			throws HibernateException, SQLException {

		Serializable resultId;

		// if we know there is exactly 1 row, we can skip.
		// it would be great if we could _always_ skip this;
		// it is a problem for <key-many-to-one>

		if ( isSingleRowLoader() && id != null ) {
			resultId = id;
		}
		else {
			
			Type idType = persister.getIdentifierType();
			resultId = (Serializable) idType.nullSafeGet(
					rs,
					getEntityAliases()[i].getSuffixedKeyAliases(),
					session,
					null //problematic for <key-many-to-one>!
			);
			
			final boolean idIsResultId = id != null && 
					resultId != null && 
					idType.isEqual( id, resultId, session.getEntityMode(), factory );
			
			if ( idIsResultId ) resultId = id; //use the id passed in
		}

		return resultId == null ?
				null :
				new EntityKey( resultId, persister, session.getEntityMode() );
	}

	/**
	 * Check the version of the object in the <tt>ResultSet</tt> against
	 * the object version in the session cache, throwing an exception
	 * if the version numbers are different
	 */
	private void checkVersion(final int i,
							  final Loadable persister,
							  final Serializable id,
							  final Object entity,
							  final ResultSet rs,
							  final SessionImplementor session)
			throws HibernateException, SQLException {

		Object version = session.getPersistenceContext().getEntry( entity ).getVersion();

		if ( version != null ) { //null version means the object is in the process of being loaded somewhere else in the ResultSet
			VersionType versionType = persister.getVersionType();
			Object currentVersion = versionType.nullSafeGet(
					rs,
					getEntityAliases()[i].getSuffixedVersionAliases(),
					session,
					null
			);
			if ( !versionType.isEqual(version, currentVersion) ) {
				throw new StaleObjectStateException( persister.getEntityName(), id );
			}
		}

	}

	/**
	 * Resolve any ids for currently loaded objects, duplications within the
	 * <tt>ResultSet</tt>, etc. Instantiate empty objects to be initialized from the
	 * <tt>ResultSet</tt>. Return an array of objects (a row of results) and an
	 * array of booleans (by side-effect) that determine whether the corresponding
	 * object should be initialized.
	 */
	private Object[] getRow(final ResultSet rs,
							final Loadable[] persisters,
							final EntityKey[] keys,
							final Object optionalObject,
							final EntityKey optionalObjectKey,
							final LockMode[] lockModes,
							final List hydratedObjects,
							final SessionImplementor session)
			throws HibernateException, SQLException {

		final int cols = persisters.length;
		final EntityAliases[] descriptors = getEntityAliases();

		if ( log.isDebugEnabled() ) log.debug( "result row: " + StringHelper.toString( keys ) );

		final Object[] rowResults = new Object[cols];

		for ( int i = 0; i < cols; i++ ) {

			Object object = null;
			EntityKey key = keys[i];

			if ( keys[i] == null ) {
				//do nothing
			}
			else {

				//If the object is already loaded, return the loaded one
				object = session.getEntityUsingInterceptor( key ); //TODO: should it be getSessionEntity() ?
				if ( object != null ) {
					//its already loaded so don't need to hydrate it
					instanceAlreadyLoaded( rs,
							i,
							persisters[i],
							key,
							object,
							lockModes[i],
							session );
				}
				else {
					object = instanceNotYetLoaded( rs,
							i,
							persisters[i],
							descriptors[i].getRowIdAlias(),
							key,
							lockModes[i],
							optionalObjectKey,
							optionalObject,
							hydratedObjects,
							session );
				}

			}

			rowResults[i] = object;

		}

		return rowResults;

	}

	/**
	 * The entity instance is already in the session cache
	 */
	private void instanceAlreadyLoaded(final ResultSet rs,
									   final int i,
									   final Loadable persister,
									   final EntityKey key,
									   final Object object,
									   final LockMode lockMode,
									   final SessionImplementor session)
			throws HibernateException, SQLException {

		if ( !persister.isInstance( object, session.getEntityMode() ) ) {
			throw new WrongClassException( "loaded object was of wrong class", key.getIdentifier(), persister.getEntityName() );
		}

		if ( LockMode.NONE != lockMode && upgradeLocks() ) { //no point doing this if NONE was requested

			final boolean isVersionCheckNeeded = persister.isVersioned() &&
					session.getPersistenceContext().getLockMode( object ).lessThan( lockMode );
			// we don't need to worry about existing version being uninitialized
			// because this block isn't called by a re-entrant load (re-entrant
			// loads _always_ have lock mode NONE)
			if (isVersionCheckNeeded) {
				//we only check the version when _upgrading_ lock modes
				checkVersion( i, persister, key.getIdentifier(), object, rs, session );
				//we need to upgrade the lock mode to the mode requested
				session.getPersistenceContext().setLockMode( object, lockMode );
			}
		}
	}

	/**
	 * The entity instance is not in the session cache
	 */
	private Object instanceNotYetLoaded(final ResultSet rs,
										final int i,
										final Loadable persister,
										final String rowIdAlias,
										final EntityKey key,
										final LockMode lockMode,
										final EntityKey optionalObjectKey,
										final Object optionalObject,
										final List hydratedObjects,
										final SessionImplementor session)
			throws HibernateException, SQLException {

		final String instanceClass = getInstanceClass( rs, i, persister, key.getIdentifier(), session );

		final Object object;
		if ( optionalObjectKey != null && key.equals( optionalObjectKey ) ) {
			//its the given optional object
			object = optionalObject;
		}
		else {
			// instantiate a new instance
			object = session.instantiate( instanceClass, key.getIdentifier() );
		}

		//need to hydrate it.

		// grab its state from the ResultSet and keep it in the Session
		// (but don't yet initialize the object itself)
		// note that we acquire LockMode.READ even if it was not requested
		LockMode acquiredLockMode = lockMode == LockMode.NONE ? LockMode.READ : lockMode;
		loadFromResultSet( rs, i, object, instanceClass, key, rowIdAlias, acquiredLockMode, persister, session );

		//materialize associations (and initialize the object) later
		hydratedObjects.add( object );

		return object;
	}


	/**
	 * Hydrate the state an object from the SQL <tt>ResultSet</tt>, into
	 * an array or "hydrated" values (do not resolve associations yet),
	 * and pass the hydrates state to the session.
	 */
	private void loadFromResultSet(final ResultSet rs,
								   final int i,
								   final Object object,
								   final String instanceEntityName,
								   final EntityKey key,
								   final String rowIdAlias,
								   final LockMode lockMode,
								   final Loadable rootPersister,
								   final SessionImplementor session)
			throws SQLException, HibernateException {

		final Serializable id = key.getIdentifier();

		// Get the persister for the _subclass_
		final Loadable persister = (Loadable) getFactory().getEntityPersister( instanceEntityName );

		if ( log.isTraceEnabled() ) {
			log.trace( "Initializing object from ResultSet: " + MessageHelper.infoString(persister, id, getFactory()) );
		}

		// add temp entry so that the next step is circular-reference
		// safe - only needed because some types don't take proper
		// advantage of two-phase-load (esp. components)
		TwoPhaseLoad.addUninitializedEntity( id, object, persister, lockMode, session );

		//This is not very nice (and quite slow):
		final String[][] cols = persister == rootPersister ?
				getEntityAliases()[i].getSuffixedPropertyAliases() :
				getEntityAliases()[i].getSuffixedPropertyAliases(persister);

		final Object[] values = persister.hydrate( rs, id, object, rootPersister, session, cols );

		final Object rowId = persister.hasRowId() ? rs.getObject(rowIdAlias) : null;

		final AssociationType[] ownerAssociationTypes = getOwnerAssociationTypes();
		if ( ownerAssociationTypes != null && ownerAssociationTypes[i] != null ) {
			String ukName = ownerAssociationTypes[i].getRHSUniqueKeyPropertyName();
			if (ukName!=null) {
				final int index = ( (UniqueKeyLoadable) persister ).getPropertyIndex(ukName);
				final Type type = persister.getPropertyTypes()[index];
	
				// polymorphism not really handled completely correctly,
				// perhaps...well, actually its ok, assuming that the
				// entity name used in the lookup is the same as the
				// the one used here, which it will be
	
				EntityUniqueKey euk = new EntityUniqueKey( 
						rootPersister.getEntityName(), //polymorphism comment above
						ukName,
						type.semiResolve( values[index], session, object ),
						type,
						session.getEntityMode()
				);
				session.getPersistenceContext().addEntity( euk, object );
			}
		}

		TwoPhaseLoad.postHydrate( persister, id, values, rowId, object, lockMode, session );

	}

	/**
	 * Determine the concrete class of an instance in the <tt>ResultSet</tt>
	 */
	private String getInstanceClass(final ResultSet rs,
									final int i,
									final Loadable persister,
									final Serializable id,
									final SessionImplementor session)
			throws HibernateException, SQLException {

		if ( persister.hasSubclasses() ) {

			// Code to handle subclasses of topClass
			Object discriminatorValue = persister.getDiscriminatorType().nullSafeGet(
					rs,
					getEntityAliases()[i].getSuffixedDiscriminatorAlias(),
					session,
					null
			);

			final String result = persister.getSubclassForDiscriminatorValue( discriminatorValue );

			if ( result == null ) {
				//woops we got an instance of another class hierarchy branch
				throw new WrongClassException( 
						"Discriminator: " + discriminatorValue,
						id,
						persister.getEntityName() 
				);
			}

			return result;

		}
		else {
			return persister.getEntityName();
		}
	}

	/**
	 * Advance the cursor to the first required row of the <tt>ResultSet</tt>
	 */
	private void advance(final ResultSet rs, final RowSelection selection)
			throws SQLException {

		final int firstRow = getFirstRow( selection );
		if ( firstRow != 0 ) {
			if ( getFactory().getSettings().isScrollableResultSetsEnabled() ) {
				// we can go straight to the first required row
				rs.absolute( firstRow );
			}
			else {
				// we need to step through the rows one row at a time (slow)
				for ( int m = 0; m < firstRow; m++ ) rs.next();
			}
		}
	}

	private static boolean hasMaxRows(RowSelection selection) {
		return selection != null && selection.getMaxRows() != null;
	}

	private static int getFirstRow(RowSelection selection) {
		if ( selection == null || selection.getFirstRow() == null ) {
			return 0;
		}
		else {
			return selection.getFirstRow().intValue();
		}
	}

	/**
	 * Should we pre-process the SQL string, adding a dialect-specific
	 * LIMIT clause.
	 */
	private static boolean useLimit(final RowSelection selection, final Dialect dialect) {
		return dialect.supportsLimit() && hasMaxRows( selection );
	}

	/**
	 * Bind positional parameter values to the <tt>PreparedStatement</tt>
	 * (these are parameters specified by a JDBC-style ?).
	 */
	protected int bindPositionalParameters(final PreparedStatement st,
										   final QueryParameters queryParameters,
										   final int start,
										   final SessionImplementor session)
			throws SQLException, HibernateException {

		final Object[] values = queryParameters.getFilteredPositionalParameterValues();
		final Type[] types = queryParameters.getFilteredPositionalParameterTypes();
		int span = 0;
		for ( int i = 0; i < values.length; i++ ) {
			types[i].nullSafeSet( st, values[i], start + span, session );
			span += types[i].getColumnSpan( getFactory() );
		}
		return span;
	}

	/**
	 * Obtain a <tt>PreparedStatement</tt> with all parameters pre-bound.
	 * Bind JDBC-style <tt>?</tt> parameters, named parameters, and
	 * limit parameters.
	 */
	protected final PreparedStatement prepareQueryStatement(final QueryParameters queryParameters,
															final boolean scroll,
															final SessionImplementor session)
			throws SQLException, HibernateException {

		queryParameters.processFilters( getSQLString(), session );
		String sql = queryParameters.getFilteredSQL();
		final Dialect dialect = getFactory().getDialect();
		final RowSelection selection = queryParameters.getRowSelection();
		boolean useLimit = useLimit( selection, dialect );
		boolean hasFirstRow = getFirstRow( selection ) > 0;
		boolean useOffset = hasFirstRow && useLimit && dialect.supportsLimitOffset();
		boolean callable = queryParameters.isCallable();
		
		boolean useScrollableResultSetToSkip = hasFirstRow &&
				!useOffset &&
				getFactory().getSettings().isScrollableResultSetsEnabled();
		ScrollMode scrollMode = scroll ? queryParameters.getScrollMode() : ScrollMode.SCROLL_INSENSITIVE;

		if ( useLimit ) {
			sql = dialect.getLimitString( 
					sql.trim(), //use of trim() here is ugly?
					useOffset ? getFirstRow(selection) : 0, 
					getMaxOrLimit(selection, dialect) 
			);
		}

		sql = preprocessSQL( sql, queryParameters, dialect );
		
		PreparedStatement st = null;
		
		if (callable) {
			st = session.getBatcher()
				.prepareCallableQueryStatement( sql, scroll || useScrollableResultSetToSkip, scrollMode );
		} 
		else {
			st = session.getBatcher()
				.prepareQueryStatement( sql, scroll || useScrollableResultSetToSkip, scrollMode );
		}
				

		try {

			int col = 1;
			//TODO: can we limit stored procedures ?!
			if ( useLimit && dialect.bindLimitParametersFirst() ) {
				col += bindLimitParameters( st, col, selection );
			}
			if (callable) {
				col = dialect.registerResultSetOutParameter( (CallableStatement)st, col );
			}
			col += bindPositionalParameters( st, queryParameters, col, session );
			col += bindNamedParameters( st, queryParameters.getNamedParameters(), col, session );

			if ( useLimit && !dialect.bindLimitParametersFirst() ) {
				col += bindLimitParameters( st, col, selection );
			}

			if ( !useLimit ) setMaxRows( st, selection );
			if ( selection != null ) {
				if ( selection.getTimeout() != null ) {
					st.setQueryTimeout( selection.getTimeout().intValue() );
				}
				if ( selection.getFetchSize() != null ) {
					st.setFetchSize( selection.getFetchSize().intValue() );
				}
			}
		}
		catch ( SQLException sqle ) {
			session.getBatcher().closeQueryStatement( st, null );
			throw sqle;
		}
		catch ( HibernateException he ) {
			session.getBatcher().closeQueryStatement( st, null );
			throw he;
		}

		return st;
	}

	/**
	 * Some dialect-specific LIMIT clauses require the maximium last row number,
	 * others require the maximum returned row count.
	 */
	private static int getMaxOrLimit(final RowSelection selection, final Dialect dialect) {
		final int firstRow = getFirstRow( selection );
		final int lastRow = selection.getMaxRows().intValue();
		if ( dialect.useMaxForLimit() ) {
			return lastRow + firstRow;
		}
		else {
			return lastRow;
		}
	}

	/**
	 * Bind parameters needed by the dialect-specific LIMIT clause
	 */
	private int bindLimitParameters(final PreparedStatement st, final int index, final RowSelection selection)
			throws SQLException {

		Dialect dialect = getFactory().getDialect();
		if ( !dialect.supportsVariableLimit() ) return 0;
		if ( !hasMaxRows( selection ) ) throw new AssertionFailure( "no max results set" );
		int firstRow = getFirstRow( selection );
		int lastRow = getMaxOrLimit( selection, dialect );
		boolean hasFirstRow = firstRow > 0 && dialect.supportsLimitOffset();
		boolean reverse = dialect.bindLimitParametersInReverseOrder();
		if ( hasFirstRow ) st.setInt( index + ( reverse ? 1 : 0 ), firstRow );
		st.setInt( index + ( reverse || !hasFirstRow ? 0 : 1 ), lastRow );
		return hasFirstRow ? 2 : 1;
	}

	/**
	 * Use JDBC API to limit the number of rows returned by the SQL query if necessary
	 */
	private void setMaxRows(final PreparedStatement st, final RowSelection selection)
			throws SQLException {
		if ( hasMaxRows( selection ) ) {
			st.setMaxRows( selection.getMaxRows().intValue() + getFirstRow( selection ) );
		}
	}

	protected final ResultSet getResultSet(final PreparedStatement st,
			   final RowSelection selection,
			   final SessionImplementor session) throws HibernateException, SQLException {
		return getResultSet(st, false, selection, session);
	}

	/**
	 * Fetch a <tt>PreparedStatement</tt>, call <tt>setMaxRows</tt> and then execute it,
	 * advance to the first result and return an SQL <tt>ResultSet</tt>
	 */
	protected final ResultSet getResultSet(final PreparedStatement st,
										   final boolean callable,
										   final RowSelection selection,
										   final SessionImplementor session)
			throws SQLException, HibernateException {
	
		ResultSet rs = null;
		try {
			if(callable) {
				rs = session.getBatcher().getResultSet( (CallableStatement)st, getFactory().getDialect() );
			} else {
				rs = session.getBatcher().getResultSet( st );
			}
			rs = wrapResultSetIfEnabled( rs , session );
			Dialect dialect = getFactory().getDialect();
			if ( !dialect.supportsLimitOffset() || !useLimit( selection, dialect ) ) {
				advance( rs, selection );
			}
			return rs;
		}
		catch ( SQLException sqle ) {
			session.getBatcher().closeQueryStatement( st, rs );
			throw sqle;
		}
	}

	private synchronized ResultSet wrapResultSetIfEnabled(final ResultSet rs, final SessionImplementor session) {
		// synchronized to avoid multi-thread access issues; defined as method synch to avoid
		// potential deadlock issues due to nature of code.
		if ( session.getFactory().getSettings().isWrapResultSetsEnabled() ) {
			try {
				log.debug("Wrapping result set [" + rs + "]");
				return new ResultSetWrapper( rs, retreiveColumnNameToIndexCache( rs ) );
			}
			catch(SQLException e) {
				log.info("Error wrapping result set", e);
				return rs;
			}
		}
		else {
			return rs;
		}
	}

	private ColumnNameCache retreiveColumnNameToIndexCache(ResultSet rs) throws SQLException {
		if ( columnNameCache == null ) {
			log.trace("Building columnName->columnIndex cache");
			columnNameCache = new ColumnNameCache( rs.getMetaData().getColumnCount() );
		}

		return columnNameCache;
	}

	/**
	 * Bind named parameters to the <tt>PreparedStatement</tt>. This has an empty
	 * implementation on this superclass and should be implemented by subclasses
	 * (queries) which allow named parameters.
	 */
	protected int bindNamedParameters(PreparedStatement st,
									  Map namedParams,
									  int start,
									  SessionImplementor session)
			throws SQLException, HibernateException {
		return 0;
	}

	/**
	 * Called by subclasses that load entities
	 * @param persister only needed for logging
	 */
	protected final List loadEntity(final SessionImplementor session,
									final Object id,
									final Type identifierType,
									final Object optionalObject,
									final String optionalEntityName,
									final Serializable optionalIdentifier, 
									final EntityPersister persister) 
			throws HibernateException {
		
		if ( log.isDebugEnabled() ) {
			log.debug( "loading entity: " + MessageHelper.infoString( persister, id, identifierType, getFactory() ) );
		}

		List result;
		try {
			result = doQueryAndInitializeNonLazyCollections( 
					session,
					new QueryParameters( 
							new Type[]{identifierType},
							new Object[]{id},
							optionalObject,
							optionalEntityName,
							optionalIdentifier 
						),
					false 
				);
		}
		catch ( SQLException sqle ) {
			final Loadable[] persisters = getEntityPersisters();
			throw JDBCExceptionHelper.convert(
			        factory.getSQLExceptionConverter(),
			        sqle,
			        "could not load an entity: " + 
			        MessageHelper.infoString( persisters[persisters.length-1], id, identifierType, getFactory() ),
			        getSQLString()
			);
		}

		log.debug("done entity load");
		
		return result;
		
	}

	/**
	 * Called by wrappers that batch load entities
	 * @param persister only needed for logging
	 */
	public final List loadEntityBatch(final SessionImplementor session,
									  final Serializable[] ids,
									  final Type idType,
									  final Object optionalObject,
									  final String optionalEntityName,
									  final Serializable optionalId, 
									  final EntityPersister persister) 
			throws HibernateException {

		if ( log.isDebugEnabled() ) {
			log.debug( "batch loading entity: " + MessageHelper.infoString(persister, ids, getFactory() ) );
		}

		Type[] types = new Type[ids.length];
		Arrays.fill( types, idType );
		List result;
		try {
			result = doQueryAndInitializeNonLazyCollections( 
					session,
					new QueryParameters( types, ids, optionalObject, optionalEntityName, optionalId ),
					false 
				);
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
			        factory.getSQLExceptionConverter(),
			        sqle,
			        "could not load an entity batch: " + 
			        MessageHelper.infoString( getEntityPersisters()[0], ids, getFactory() ),
			        getSQLString()
			);
		}

		log.debug("done entity batch load");
		
		return result;

	}

	/**
	 * Called by subclasses that initialize collections
	 */
	public final void loadCollection(final SessionImplementor session,
									 final Serializable id,
									 final Type type)
			throws HibernateException {

		if ( log.isDebugEnabled() ) {
			log.debug( 
					"batch loading collection: "+ 
					MessageHelper.collectionInfoString( getCollectionPersister(), id, getFactory() )
			);
		}

		Serializable[] ids = new Serializable[]{id};
		try {
			doQueryAndInitializeNonLazyCollections( 
					session,
					new QueryParameters( new Type[]{type}, ids, ids ),
					true 
			);
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
			        factory.getSQLExceptionConverter(),
			        sqle,
			        "could not initialize a collection: " + 
			        MessageHelper.collectionInfoString( getCollectionPersister(), id, getFactory() ),
			        getSQLString()
			);
		}
	
		log.debug("done loading collection");

	}

	/**
	 * Called by wrappers that batch initialize collections
	 */
	public final void loadCollectionBatch(final SessionImplementor session,
										  final Serializable[] ids,
										  final Type type)
			throws HibernateException {

		if ( log.isDebugEnabled() ) {
			log.debug( 
					"batch loading collection: "+ 
					MessageHelper.collectionInfoString( getCollectionPersister(), ids, getFactory() )
			);
		}

		Type[] idTypes = new Type[ids.length];
		Arrays.fill( idTypes, type );
		try {
			doQueryAndInitializeNonLazyCollections( 
					session,
					new QueryParameters( idTypes, ids, ids ),
					true 
			);
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
			        factory.getSQLExceptionConverter(),
			        sqle,
			        "could not initialize a collection batch: " + 
			        MessageHelper.collectionInfoString( getCollectionPersister(), ids, getFactory() ),
			        getSQLString()
			);
		}
		
		log.debug("done batch load");

	}

	/**
	 * Called by subclasses that batch initialize collections
	 */
	protected final void loadCollectionSubselect(final SessionImplementor session,
												 final Serializable[] ids,
												 final Object[] parameterValues,
												 final Type[] parameterTypes,
												 final Map namedParameters,
												 final Type type)
			throws HibernateException {

		Type[] idTypes = new Type[ids.length];
		Arrays.fill( idTypes, type );
		try {
			doQueryAndInitializeNonLazyCollections( session,
					new QueryParameters( parameterTypes, parameterValues, namedParameters, ids ),
					true );
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
			        factory.getSQLExceptionConverter(),
			        sqle,
			        "could not load collection by subselect: " + 
			        MessageHelper.collectionInfoString( getCollectionPersister(), ids, getFactory() ),
			        getSQLString()
			);
		}
	}

	/**
	 * Return the query results, using the query cache, called
	 * by subclasses that implement cacheable queries
	 */
	protected List list(final SessionImplementor session,
						final QueryParameters queryParameters,
						final Set querySpaces,
						final Type[] resultTypes)
			throws HibernateException {

		final boolean cacheable = factory.getSettings().isQueryCacheEnabled() && 
			queryParameters.isCacheable();

		if ( cacheable ) {

			final boolean queryStatisticsEnabled = getQueryIdentifier() != null &&
				factory.getStatistics().isStatisticsEnabled();

			QueryCache queryCache = factory.getQueryCache( queryParameters.getCacheRegion() );
			QueryKey key = new QueryKey( getSQLString(), queryParameters, session.getEntityMode() );
			List result = null;

			if ( /*!queryParameters.isForceCacheRefresh() &&*/ session.getCacheMode().isGetEnabled() ) {
				result = queryCache.get( key, resultTypes, querySpaces, session );

				if (queryStatisticsEnabled) {
					if (result==null) {
						factory.getStatisticsImplementor().queryCacheMiss( getQueryIdentifier(), queryCache.getRegionName() );
					}
					else {
						factory.getStatisticsImplementor().queryCacheHit( getQueryIdentifier(), queryCache.getRegionName() );
					}
				}
			}


			if ( result == null ) {
				result = doList( session, queryParameters );

				if ( cacheable && session.getCacheMode().isPutEnabled()  ) {
					queryCache.put( key, resultTypes, result, session );

					if ( queryStatisticsEnabled ) {
						factory.getStatisticsImplementor().queryCachePut( getQueryIdentifier(), queryCache.getRegionName() );
					}
				}
			}

			return getResultList( result );
		}
		else {
			return getResultList( doList( session, queryParameters ) );
		}
	}

	/**
	 * Actually execute a query, ignoring the query cache
	 */
	protected List doList(final SessionImplementor session, final QueryParameters queryParameters)
			throws HibernateException {

		final boolean stats = getQueryIdentifier() != null &&
				getFactory().getStatistics().isStatisticsEnabled();
		long startTime = 0;
		if ( stats ) startTime = System.currentTimeMillis();

		List result;
		try {
			result = doQueryAndInitializeNonLazyCollections( session, queryParameters, true );
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
			        factory.getSQLExceptionConverter(),
			        sqle,
			        "could not execute query",
			        getSQLString()
			);
		}

		if ( stats ) {
			getFactory().getStatisticsImplementor().queryExecuted( getQueryIdentifier(),
					result.size(),
					System.currentTimeMillis() - startTime );
		}

		return result;
	}

	/**
	 * Return the query results, as an instance of <tt>ScrollableResults</tt>
	 */
	protected ScrollableResults scroll(final QueryParameters queryParameters,
									   final Type[] returnTypes,
									   final Class holderClass,
									   final SessionImplementor session)
			throws HibernateException {

		if ( getCollectionPersister() != null ) {
			throw new HibernateException( "Cannot scroll queries which initialize collections" );
		}

		final boolean stats = getQueryIdentifier() != null &&
				getFactory().getStatistics().isStatisticsEnabled();
		long startTime = 0;
		if ( stats ) startTime = System.currentTimeMillis();

		try {

			PreparedStatement st = prepareQueryStatement( queryParameters, true, session );
			ResultSet rs = getResultSet( st, queryParameters.getRowSelection(), session );

			if ( stats ) {
				getFactory().getStatisticsImplementor().queryExecuted( getQueryIdentifier(),
						0,
						System.currentTimeMillis() - startTime );
			}

			ScrollableResults result = new ScrollableResultsImpl( rs,
					st,
					session,
					this,
					queryParameters,
					returnTypes,
					holderClass );

			return result;

		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
			        factory.getSQLExceptionConverter(),
			        sqle,
			        "could not execute query using scroll",
			        getSQLString()
			);
		}

	}

	/**
	 * Calculate and cache select-clause suffixes. Must be
	 * called by subclasses after instantiation.
	 */
	protected void postInstantiate() {}

	/**
	 * Get the result set descriptor
	 */
	protected abstract EntityAliases[] getEntityAliases();

	/**
	 * Identifies the query for statistics reporting, if null,
	 * no statistics will be reported
	 */
	protected String getQueryIdentifier() {
		return null;
	}

	public final SessionFactoryImplementor getFactory() {
		return factory;
	}

	public String toString() {
		return getClass().getName() + '(' + getSQLString() + ')';
	}

}
