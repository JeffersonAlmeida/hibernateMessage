// $Id: QueryLoader.java,v 1.4 2005/03/17 05:50:12 oneovthafew Exp $
package org.hibernate.loader.hql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.QueryException;
import org.hibernate.ScrollableResults;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.TypedValue;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.hql.QueryTranslator;
import org.hibernate.hql.ast.FromElement;
import org.hibernate.hql.ast.SelectClause;
import org.hibernate.impl.IteratorImpl;
import org.hibernate.loader.BasicLoader;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.ForUpdateFragment;
import org.hibernate.type.AssociationType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;

import java.lang.reflect.Constructor;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A delegate that implements the Loader part of QueryTranslator.
 * <p/>
 * User: josh
 * Date: Jan 3, 2004
 * Time: 8:29:58 PM
 */
public class QueryLoader extends BasicLoader {

	private static final Log log = LogFactory.getLog( QueryLoader.class );
	/**
	 * The query translator that is delegating to this object.
	 */
	private QueryTranslator queryTranslator;

	private Queryable[] entityPersisters;
	private String[] entityAliases;
	private String[] sqlAliases;
	private String[] sqlAliasSuffixes;
	private boolean[] includeInSelect;

	private boolean hasScalars;
	private String[][] scalarColumnNames;
	//private Type[] sqlResultTypes;
	private Type[] queryReturnTypes;
	
	private final Map sqlAliasByEntityAlias = new HashMap(8);

	private AssociationType[] ownerAssociationTypes;
	private int[] owners;

	private int collectionOwner = -1;
	private QueryableCollection collectionPersister;

	private int selectLength;
	private Constructor holderConstructor;
	
	private LockMode[] defaultLockModes;

	/**
	 * Creates a new Loader implementation.
	 *
	 * @param queryTranslator The query translator that is the delegator.
	 * @param factory         The factory from which this loader is being created.
	 */
	public QueryLoader(final QueryTranslator queryTranslator,
					   final SessionFactoryImplementor factory,
					   final SelectClause selectClause) {
		super( factory );
		this.queryTranslator = queryTranslator;
		initialize( selectClause );
		postInstantiate();
	}

	private void initialize(SelectClause selectClause) {

		List fromElementList = selectClause.getFromElementsForLoad();

		hasScalars = selectClause.isScalarSelect();
		scalarColumnNames = selectClause.getColumnNames();
		//sqlResultTypes = selectClause.getSqlResultTypes();
		queryReturnTypes = selectClause.getQueryReturnTypes();

		holderConstructor = selectClause.getConstructor();

		// make sure we grab the first collection persister to fully mimic old parser
		FromElement collectionFromElement = selectClause.getCollectionFromElement();
		if (collectionFromElement != null) {
			collectionPersister = collectionFromElement.getQueryableCollection();
			collectionOwner = fromElementList.indexOf( collectionFromElement.getOrigin() );
		}

		int size = fromElementList.size();
		entityPersisters = new Queryable[size];
		entityAliases = new String[size];
		sqlAliases = new String[size];
		sqlAliasSuffixes = new String[size];
		includeInSelect = new boolean[size];
		owners = new int[size];
		ownerAssociationTypes = new AssociationType[size];

		for ( int i = 0; i < size; i++ ) {
			final FromElement element = ( FromElement ) fromElementList.get( i );
			entityPersisters[i] = ( Queryable ) element.getEntityPersister();

			if ( entityPersisters[i] == null ) {
				throw new IllegalStateException( "No entity persister for " + element.toString() );
			}

			sqlAliases[i] = element.getTableAlias();
			entityAliases[i] = element.getClassAlias();
			sqlAliasByEntityAlias.put( entityAliases[i], sqlAliases[i] );
			sqlAliasSuffixes[i] = ( size == 1 ) ? "" : Integer.toString( i ) + "_";
			includeInSelect[i] = !element.isFetch();
			if ( includeInSelect[i] ) selectLength++;
			
			owners[i] = -1; //by default
			if ( element.isFetch() ) {
				if ( element.isCollectionJoin() || element.getQueryableCollection() != null ) {
					// This is now handled earlier in this method.
				}
				else if ( element.getDataType().isEntityType() ) {
					EntityType entityType = ( EntityType ) element.getDataType();
					if ( entityType.isOneToOne() ) {
						owners[i] = fromElementList.indexOf( element.getOrigin() );
					}
					ownerAssociationTypes[i] = entityType;
				}
			}
		}
		
		//NONE, because its the requested lock mode, not the actual! 
		defaultLockModes = ArrayHelper.fillArray(LockMode.NONE, size);
		
	}

	// -- Loader implementation --

	public Loadable[] getEntityPersisters() {
		return entityPersisters;
	}

	public String[] getAliases() {
		return sqlAliases;
	}

	public String[] getSqlAliasSuffixes() {
		return sqlAliasSuffixes;
	}

	public String[] getSuffixes() {
		return getSqlAliasSuffixes();
	}

	protected String getQueryIdentifier() {
		return queryTranslator.getQueryString();
	}

	/**
	 * The SQL query string to be called.
	 */
	protected String getSQLString() {
		return queryTranslator.getSQLString();
	}

	/**
	 * An (optional) persister for a collection to be initialized; only collection loaders
	 * return a non-null value
	 */
	protected CollectionPersister getCollectionPersister() {
		return collectionPersister;
	}

	protected int getCollectionOwner() {
		return collectionOwner;
	}

	/**
	 * An array of indexes of the entity that owns a one-to-one association
	 * to the entity at the given index (-1 if there is no "owner")
	 */
	protected int[] getOwners() {
		return owners;
	}

	protected AssociationType[] getOwnerAssociationTypes() {
		return ownerAssociationTypes;
	}

	// -- Loader overrides --

	protected boolean isSubselectLoadingEnabled() {
		return hasSubselectLoadableCollections();
	}
	
	protected int bindNamedParameters(final PreparedStatement ps,
									  final Map namedParams,
									  final int start,
									  final SessionImplementor session)
			throws SQLException, HibernateException {

		if ( namedParams != null ) {
			// assumes that types are all of span 1
			Iterator iter = namedParams.entrySet().iterator();
			int result = 0;
			while ( iter.hasNext() ) {
				Map.Entry e = ( Map.Entry ) iter.next();
				String name = ( String ) e.getKey();
				TypedValue typedval = ( TypedValue ) e.getValue();
				int[] locs = getNamedParameterLocs( name );
				for ( int i = 0; i < locs.length; i++ ) {
					if ( log.isDebugEnabled() ) {
						log.debug( "bindNamedParameters() " +
								typedval.getValue() + " -> " + name +
								" [" + ( locs[i] + start ) + "]" );
					}
					typedval.getType().nullSafeSet( ps, typedval.getValue(), locs[i] + start, session );
				}
				result += locs.length;
			}
			return result;
		}
		else {
			return 0;
		}
	}

	/**
	 * @param lockModes a collection of lock modes specified dynamically via the Query interface
	 */
	protected LockMode[] getLockModes(Map lockModes) {
		
		if ( lockModes==null || lockModes.size()==0 ) {
			return defaultLockModes;
		}
		else {
			// unfortunately this stuff can't be cached because
			// it is per-invocation, not constant for the
			// QueryTranslator instance
	
			LockMode[] lockModeArray = new LockMode[entityAliases.length];
			for ( int i = 0; i < entityAliases.length; i++ ) {
				LockMode lockMode = (LockMode) lockModes.get( entityAliases[i] );
				if ( lockMode == null ) {
					//NONE, because its the requested lock mode, not the actual! 
					lockMode = LockMode.NONE;
				}
				lockModeArray[i] = lockMode;
			}
			return lockModeArray;
		}
	}

	protected String applyLocks(String sql, Map lockModes, Dialect dialect)
			throws QueryException {

		if ( lockModes == null || lockModes.size() == 0 ) {
			return sql;
		}
		else {
			// can't cache this stuff either (per-invocation)
			
			//we are given a map of user alias -> lock mode
			//create a new map of sql alias -> lock mode
			final Map aliasedLockModes = new HashMap();
			final Iterator iter = lockModes.entrySet().iterator();
			while ( iter.hasNext() ) {
				Map.Entry me = ( Map.Entry ) iter.next();
				final String userAlias = ( String ) me.getKey();
				final String sqlAlias = (String) sqlAliasByEntityAlias.get( userAlias );
				aliasedLockModes.put( sqlAlias, me.getValue() );
			}
			
			//if necessary, create a map of sql alias -> key columns
			Map keyColumnNames = null;
			if ( dialect.forUpdateOfColumns() ) {
				final Loadable[] persisters = getEntityPersisters();
				keyColumnNames = new HashMap();
				for ( int i = 0; i < sqlAliases.length; i++ ) {
					keyColumnNames.put( sqlAliases[i], persisters[i].getIdentifierColumnNames() );
				}
			}
			
			return sql + new ForUpdateFragment( dialect, aliasedLockModes, keyColumnNames ).toFragmentString();

		}
	}

	protected boolean upgradeLocks() {
		return true;
	}

	protected Object getResultColumnOrRow(Object[] row, ResultSet rs, SessionImplementor session)
			throws SQLException, HibernateException {

		row = toResultRow( row );
		if ( hasScalars ) {
			String[][] scalarColumns = scalarColumnNames;
			int queryCols = queryReturnTypes.length;
			if ( holderConstructor == null && queryCols == 1 ) {
				return queryReturnTypes[0].nullSafeGet( rs, scalarColumns[0], session, null );
			}
			else {
				row = new Object[queryCols];
				for ( int i = 0; i < queryCols; i++ )
					row[i] = queryReturnTypes[i].nullSafeGet( rs, scalarColumns[i], session, null );
				return row;
			}
		}
		else if ( holderConstructor == null ) {
			return row.length == 1 ? row[0] : row;
		}
		else {
			return row;
		}

	}

	protected List getResultList(List results) throws QueryException {
		// meant to handle dynamic instantiation queries...
		if ( holderConstructor != null ) {
			for ( int i = 0; i < results.size(); i++ ) {
				Object[] row = ( Object[] ) results.get( i );
				try {
					results.set( i, holderConstructor.newInstance( row ) );
				}
				catch ( Exception e ) {
					throw new QueryException( 
						"could not instantiate: " + 
						holderConstructor.getDeclaringClass().getName(), 
						e );
				}
			}
		}
		return results;
	}

	// --- Query translator methods ---

	/**
	 * Delegats
	 *
	 * @param session
	 * @param queryParameters
	 * @return
	 * @throws HibernateException
	 */
	public List list(SessionImplementor session, QueryParameters queryParameters)
			throws HibernateException {
		return list( session, queryParameters, queryTranslator.getQuerySpaces(), queryReturnTypes );
	}

	/**
	 * Return the query results as an iterator
	 */
	public Iterator iterate(QueryParameters queryParameters, SessionImplementor session)
			throws HibernateException {

		final boolean stats = session.getFactory().getStatistics().isStatisticsEnabled();
		long startTime = 0;
		if ( stats ) startTime = System.currentTimeMillis();

		try {

			final PreparedStatement st = prepareQueryStatement( queryParameters, false, session );
			final ResultSet rs = getResultSet( st, queryParameters.getRowSelection(), session );
			final Iterator result = new IteratorImpl( rs,
					st,
					session,
					queryReturnTypes,
					queryTranslator.getColumnNames(),
					holderConstructor == null ? null : holderConstructor.getDeclaringClass() );

			if ( stats ) {
				session.getFactory().getStatisticsImplementor().queryExecuted( "HQL: " + queryTranslator.getQueryString(),
						0,
						System.currentTimeMillis() - startTime );
			}

			return result;

		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert( getFactory().getSQLExceptionConverter(),
					sqle,
					"could not execute query using iterate",
					getSQLString() );
		}

	}

	public ScrollableResults scroll(final QueryParameters queryParameters,
									final SessionImplementor session)
			throws HibernateException {
		return scroll( queryParameters, queryReturnTypes, null, session );
	}

	// -- Implementation private methods --

	private Object[] toResultRow(Object[] row) {
		if ( selectLength == row.length ) {
			return row;
		}
		else {
			Object[] result = new Object[selectLength];
			int j = 0;
			for ( int i = 0; i < row.length; i++ ) {
				if ( includeInSelect[i] ) result[j++] = row[i];
			}
			return result;
		}
	}

	/**
	 * Returns the locations of all occurrences of the named parameter.
	 */
	private int[] getNamedParameterLocs(String name) throws QueryException {
		return queryTranslator.getNamedParameterLocs( name );
	}
}
