//$Id: CriteriaLoader.java,v 1.3 2005/03/21 17:29:56 oneovthafew Exp $
package org.hibernate.loader.criteria;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.impl.CriteriaImpl;
import org.hibernate.loader.AbstractEntityLoader;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.ForUpdateFragment;
import org.hibernate.sql.JoinFragment;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;

/**
 * A <tt>Loader</tt> for <tt>Criteria</tt> queries. Note that criteria queries are
 * more like multi-object <tt>load()</tt>s than like HQL queries.
 *
 * @author Gavin King
 */
public class CriteriaLoader extends AbstractEntityLoader {

	//TODO: this class depends directly upon CriteriaImpl, 
	//      in the impl package ... add a CriteriaImplementor 
	//      interface

	//NOTE: unlike all other Loaders, this one is NOT
	//      multithreaded, or cacheable!!

	private final CriteriaQueryTranslator translator;
	private final Set querySpaces;
	private final Type[] resultTypes;
	//the user visible aliases, which are unknown to the superclass,
	//these are not the actual "physical" SQL aliases
	private final String[] userAliases;
	private final List userAliasList = new ArrayList();

	public CriteriaLoader(
			final OuterJoinLoadable persister, 
			final SessionFactoryImplementor factory, 
			final CriteriaImpl criteria, 
			final String rootEntityName,
			final Map enabledFilters)
	throws HibernateException {
		super(persister, factory, enabledFilters);

		translator = new CriteriaQueryTranslator(
				factory, 
				criteria, 
				rootEntityName, 
				CriteriaQueryTranslator.ROOT_SQL_ALIAS
		);

		querySpaces = translator.getQuerySpaces();

		if ( translator.hasProjection() ) {
			resultTypes = translator.getProjectedTypes();
			
			initProjection( 
					translator.getSelect(), 
					translator.getWhereCondition(), 
					translator.getOrderBy(),
					translator.getGroupBy(),
					LockMode.NONE 
			);
		}
		else {
			resultTypes = new Type[] { Hibernate.entity( persister.getEntityName() ) };

			initAll( translator.getWhereCondition(), translator.getOrderBy(), LockMode.NONE );
		}
		
		userAliasList.add( criteria.getAlias() ); //root entity comes *last*
		userAliases = ArrayHelper.toStringArray(userAliasList);

		postInstantiate();

	}
	
	public ScrollableResults scroll(SessionImplementor session, ScrollMode scrollMode) 
	throws HibernateException {
		QueryParameters qp = translator.getQueryParameters();
		qp.setScrollMode(scrollMode);
		return scroll(qp, resultTypes, null, session);
	}

	public List list(SessionImplementor session) 
	throws HibernateException {
		return list( session, translator.getQueryParameters(), querySpaces, resultTypes );
	}

	protected Object getResultColumnOrRow(Object[] row, ResultSet rs, SessionImplementor session)
	throws SQLException, HibernateException {
		final Object[] result;
		final String[] aliases;
		if ( translator.hasProjection() ) {
			Type[] types = translator.getProjectedTypes();
			result = new Object[types.length];
			String[] columnAliases = translator.getProjectedColumnAliases();
			for ( int i=0; i<result.length; i++ ) {
				result[i] = types[i].nullSafeGet(rs, columnAliases[i], session, null);
			}
			aliases = translator.getProjectedAliases();
		}
		else {
			result = row;
			aliases = userAliases;
		}
		return translator.getRootCriteria().getResultTransformer().transformTuple(result, aliases);
	}

	protected int getJoinType(
			AssociationType type, 
			FetchMode config, 
			String path, 
			Set visitedAssociations,
			String lhsTable,
			String[] lhsColumns,
			boolean nullable,
			int currentDepth)
	throws MappingException {

		if ( translator.isJoin(path) ) {
			return JoinFragment.INNER_JOIN;
		}
		else {
			FetchMode fm = translator.getRootCriteria()
				.getFetchMode(path);
			if ( isDefaultFetchMode(fm) ) {
				return super.getJoinType(
						type, 
						config, 
						path, 
						visitedAssociations, 
						lhsTable, 
						lhsColumns, 
						nullable,
						currentDepth
				);
			}
			else {
				if ( fm==FetchMode.JOIN ) {
					return getJoinType(nullable, currentDepth);
				}
				else {
					return -1;
				}
			}
		}
	}
	
	private static boolean isDefaultFetchMode(FetchMode fm) {
		return fm==null || fm==FetchMode.DEFAULT;
	}

	/**
	 * Use the discriminator, to narrow the select to instances
	 * of the queried subclass, also applying any filters.
	 */
	protected String getWhereFragment() throws MappingException {
		return super.getWhereFragment() +
			( (Queryable) getPersister() ).filterFragment( getAlias(), getEnabledFilters() );
	}
	
	protected String generateTableAlias(int n, String path, Joinable joinable) {
		if ( joinable.consumesAlias() ) {
			final Criteria subcriteria = translator.getCriteria(path);
			String sqlAlias = subcriteria==null ? null : translator.getSQLAlias(subcriteria);
			if (sqlAlias!=null) {
				userAliasList.add( subcriteria.getAlias() ); //alias may be null
				return sqlAlias; //EARLY EXIT
			}
			else {
				userAliasList.add(null);
			}
		}
		return super.generateTableAlias( n + translator.getSQLAliasCount(), path, joinable );
	}

	protected String generateRootAlias(String tableName) {
		return CriteriaQueryTranslator.ROOT_SQL_ALIAS;
	}

	public Set getQuerySpaces() {
		return querySpaces;
	}

	/*protected void addToPropertySpaces(Serializable space) {
		querySpaces.add(space);
	}*/

	protected String applyLocks(String sqlSelectString, Map lockModes, Dialect dialect) 
	throws QueryException {
		
		if ( lockModes==null || lockModes.size()==0 ) {
			return sqlSelectString;
		}
		else {
			Map keyColumnNames = null;
			Loadable[] persisters = getEntityPersisters();
			String[] entityAliases = getAliases();
			if ( dialect.forUpdateOfColumns() ) {
				keyColumnNames = new HashMap();
				for ( int i=0; i<entityAliases.length; i++ ) {
					keyColumnNames.put( entityAliases[i], persisters[i].getIdentifierColumnNames() );
				}
			}
			return sqlSelectString + 
				new ForUpdateFragment(dialect, lockModes, keyColumnNames).toFragmentString();
		}
	}

	protected LockMode[] getLockModes(Map lockModes) {
		final String[] entityAliases = getAliases();
		if (entityAliases==null) return null;
		final int size = entityAliases.length;
		LockMode[] lockModesArray = new LockMode[size];
		for ( int i=0; i<size; i++ ) {
			LockMode lockMode = (LockMode) lockModes.get( entityAliases[i] );
			lockModesArray[i] = lockMode==null ? LockMode.NONE : lockMode;
		}
		return lockModesArray;
	}

	protected boolean isSubselectLoadingEnabled() {
		return hasSubselectLoadableCollections();
	}
	
	protected List getResultList(List results) {
		return translator.getRootCriteria().getResultTransformer().transformList(results);
	}

	public String getComment() {
		return "criteria query";
	}

}
