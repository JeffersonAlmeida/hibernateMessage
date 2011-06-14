//$Id: CustomLoader.java,v 1.2 2005/03/03 13:26:32 oneovthafew Exp $
package org.hibernate.loader.custom;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.QueryException;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.TypedValue;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.Loader;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;

/**
 * Extension point for loaders which use a SQL
 * result set with "unexpected" column aliases.
 * 
 * @author Gavin King
 */
public class CustomLoader extends Loader {
	
	private final Type[] resultTypes;	
	private final Loadable[] persisters;
	private final CollectionPersister collectionPersister;
	private final CustomQuery customQuery;

	public CustomLoader(
			CustomQuery customQuery,
			SessionFactoryImplementor factory
	) {
		super(factory);
		this.customQuery = customQuery;
		
		collectionPersister = customQuery.getCollectionRole()==null ?
				null :
				factory.getCollectionPersister( customQuery.getCollectionRole() );
		
		String[] entityNames = customQuery.getEntityNames();
		persisters = new Loadable[entityNames.length];
		for ( int i=0; i<entityNames.length; i++ ) {
			persisters[i] = (Loadable) factory.getEntityPersister( entityNames[i] );
		}
		
		Type[] scalarTypes = customQuery.getScalarTypes();
		
		resultTypes = new Type[ entityNames.length + ( scalarTypes==null ? 0 : scalarTypes.length ) ];
		for ( int i=0; i<scalarTypes.length; i++ ) {
			resultTypes[i] = scalarTypes[i];
		}
		for ( int i=0; i<entityNames.length; i++ ) {
			resultTypes[i + scalarTypes.length] = Hibernate.entity( entityNames[i] );
		}
	}

	protected String getSQLString() {
		return customQuery.getSQL();
	}

	protected Loadable[] getEntityPersisters() {
		return persisters;
	}

	protected LockMode[] getLockModes(Map lockModesMap) {
		return customQuery.getLockModes();
	}

	protected CollectionPersister getCollectionPersister() {
		return collectionPersister;
	}

	protected int getCollectionOwner() {
		return customQuery.getCollectionOwner();
	}
	
	protected int[] getOwners() {
		return customQuery.getEntityOwners();
	}
	
	public Set getQuerySpaces() {
		return customQuery.getQuerySpaces();
	}
	
	protected String getQueryIdentifier() {
		return customQuery.getSQL();
	}

	public List list(SessionImplementor session, QueryParameters queryParameters)
	throws HibernateException {
		return list(session, queryParameters, customQuery.getQuerySpaces(), resultTypes);
	}

	public ScrollableResults scroll(
			final QueryParameters queryParameters,
			final SessionImplementor session) 
	throws HibernateException {
		return scroll(queryParameters, resultTypes, null, session);
	}
	
	protected Object getResultColumnOrRow(Object[] row, ResultSet rs, SessionImplementor session) 
	throws SQLException, HibernateException {
		
		Type[] scalarTypes = customQuery.getScalarTypes();
		String[] scalarColumnAliases = customQuery.getScalarColumnAliases();
		
		final Object[] resultRow;
		if ( scalarTypes!=null && scalarTypes.length>0 ) {
			//all scalar results appear first
			resultRow = new Object[scalarTypes.length + row.length];
			for ( int i=0; i<scalarTypes.length; i++ ) {
				resultRow[i] = scalarTypes[i].nullSafeGet(rs, scalarColumnAliases[i], session, null);
			}
			//then entity results
			System.arraycopy(row, 0, resultRow, scalarTypes.length, row.length);
		}
		else {
			resultRow = row;
		}
		
		return resultRow.length == 1 ? resultRow[0] : resultRow;		
	}

	/*protected Object getResultColumnOrRow(Object[] row, ResultSet rs, SessionImplementor session) 
	throws SQLException, HibernateException {
		if (persisters.length == 1) {
			return row[row.length - 1];
		}
		else {
			return row;
		}
	}*/

	protected EntityAliases[] getEntityAliases() {
		return customQuery.getEntityAliases();
	}

	protected int bindNamedParameters(
			PreparedStatement ps, 
			Map namedParams, 
			int start, 
			SessionImplementor session)
	throws SQLException, HibernateException {
		if (namedParams != null) {
			// assumes that types are all of span 1
			Iterator iter = namedParams.entrySet().iterator();
			int result = 0;
			while ( iter.hasNext() ) {
				Map.Entry e = (Map.Entry) iter.next();
				String name = (String) e.getKey();
				TypedValue typedval = (TypedValue) e.getValue();
				int[] locs = getNamedParameterLocs(name);
				for (int i = 0; i < locs.length; i++) {
					typedval.getType().nullSafeSet(
						ps, typedval.getValue(), locs[i] + start, session
					);
				}
				result += locs.length;
			}
			return result;
		}
		else {
			return 0;
		}
	}
	
	protected int[] getNamedParameterLocs(String name) throws QueryException {
		Object loc = customQuery.getNamedParameterBindPoints().get(name);
		if (loc == null) {
			QueryException qe = new QueryException(
					"Named parameter does not appear in Query: " + 
					name, 
					customQuery.getSQL()
			);
			throw qe;
		}
		if (loc instanceof Integer) {
			return new int[] { ( (Integer) loc ).intValue() };
		}
		else {
			return ArrayHelper.toIntArray( (List) loc );
		}
	}

}
