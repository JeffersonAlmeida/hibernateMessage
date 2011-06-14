//$Id: OuterJoinLoader.java,v 1.40 2005/02/17 04:41:54 oneovthafew Exp $
package org.hibernate.loader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.FetchMode;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.JoinHelper;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.sql.ConditionFragment;
import org.hibernate.sql.DisjunctionFragment;
import org.hibernate.sql.InFragment;
import org.hibernate.sql.JoinFragment;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.EntityType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.StringHelper;

/**
 * Implements logic for walking a tree of associated classes.
 *
 * Generates an SQL select string containing all properties of those classes.
 * Tables are joined using an ANSI-style left outer join.
 *
 * @author Gavin King, Jon Lipsky
 */
public abstract class OuterJoinLoader extends BasicLoader {

	protected Loadable[] persisters;
	protected LockMode[] lockModeArray;
	protected int[] owners;
	protected AssociationType[] ownerAssociationType;
	protected String sql;
	protected String[] suffixes;

    private Map enabledFilters;
    
    protected final Dialect getDialect() {
    	return getFactory().getDialect();
    }

	public OuterJoinLoader(SessionFactoryImplementor factory, Map enabledFilters) {
		super(factory);
		this.enabledFilters = enabledFilters;
	}

	/**
	 * Override on subclasses to enable or suppress joining 
	 * of certain association types
	 */
	protected boolean isJoinedFetchEnabled(AssociationType type, FetchMode config) {
		return type.isEntityType() && isJoinedFetchEnabledInMapping(config, type) ;
	}
	
	/**
	 * Uniquely identifier a foreign key, so that we don't
	 * join it more than once, and create circularities
	 */
	private static final class AssociationKey {
		private String[] columns;
		private String table;
		private AssociationKey(String[] columns, String table) {
			this.columns = columns;
			this.table = table;
		}
		public boolean equals(Object other) {
			AssociationKey that = (AssociationKey) other;
			return that.table.equals(table) && Arrays.equals(columns, that.columns);
		}
		public int hashCode() {
			return table.hashCode(); //TODO: inefficient
		}
	}
	
	/**
	 * Used to detect circularities in the joined graph
	 */
	protected boolean isDuplicateAssociation(
		final Set visitedAssociationKeys, 
		final String lhsTable,
		final String[] lhsColumnNames,
		final AssociationType type
	) {
		final String foreignKeyTable;
		final String[] foreignKeyColumns;
		if ( type.getForeignKeyDirection()==ForeignKeyDirection.FOREIGN_KEY_FROM_PARENT ) {
			foreignKeyTable = lhsTable;
			foreignKeyColumns = lhsColumnNames;
		}
		else {
			foreignKeyTable = type.getAssociatedJoinable( getFactory() ).getTableName();
			foreignKeyColumns = JoinHelper.getRHSColumnNames( type, getFactory() );
		}
		return isDuplicateAssociation(visitedAssociationKeys, foreignKeyTable, foreignKeyColumns);
	}
	
	/**
	 * Should we join this association?
	 */
	protected boolean isJoinable(
		final int joinType,
		final Set visitedAssociationKeys, 
		final String lhsTable,
		final String[] lhsColumnNames,
		final AssociationType type,
		final int depth
	) {
		if (joinType<0) return false;
		
		if (joinType==JoinFragment.INNER_JOIN) return true;
		
		Integer maxFetchDepth = getFactory().getSettings().getMaximumFetchDepth();
		final boolean tooDeep = maxFetchDepth!=null && 
			depth >= maxFetchDepth.intValue();
		
		return !tooDeep && !isDuplicateAssociation(
			visitedAssociationKeys, 
			lhsTable, 
			lhsColumnNames, 
			type
		);	
	}
	
	/**
	 * Used to detect circularities in the joined graph
	 */
	protected boolean isDuplicateAssociation(
		final Set visitedAssociationKeys, 
		final String foreignKeyTable, 
		final String[] foreignKeyColumns
	) {
		return !visitedAssociationKeys.add( 
			new AssociationKey(foreignKeyColumns, foreignKeyTable) 
		);
	}
	
	protected boolean isTooDeep(int currentDepth) {
		Integer maxFetchDepth = getFactory().getSettings().getMaximumFetchDepth();
		return maxFetchDepth!=null && currentDepth >= maxFetchDepth.intValue();
	}
	
	/**
	 * Get the join type (inner, outer, etc) or -1 if the
	 * association should not be joined. Override on
	 * subclasses.
	 */
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
		
		if  ( !isJoinedFetchEnabled(type, config) ) return -1;
		
		if ( isTooDeep(currentDepth) ) return -1;
		
		final boolean dupe = isDuplicateAssociation(visitedAssociations, lhsTable,  lhsColumns, type);
		if (dupe) return -1;
		
		return getJoinType(nullable, currentDepth);
		
	}
	
	/**
	 * Use an inner join if it is a non-null association and this
	 * is the "first" join in a series
	 */
	protected int getJoinType(boolean nullable, int currentDepth) {
		//TODO: this is too conservative; if all preceding joins were 
		//      also inner joins, we could use an inner join here
		return !nullable && currentDepth==0 ? 
					JoinFragment.INNER_JOIN : 
					JoinFragment.LEFT_OUTER_JOIN;
	}

	/**
	 * For an entity class, return a list of associations to be fetched by outerjoin
	 */
	protected final List walkEntityTree(OuterJoinLoadable persister, String alias)
	throws MappingException {
		List associations = new ArrayList();
		walkEntityTree(persister, alias, associations, new HashSet(), "", 0);
		return associations;
	}

	/**
	 * For a collection role, return a list of associations to be fetched by outerjoin
	 */
	protected final List walkCollectionTree(QueryableCollection persister, String alias)
	throws MappingException {
		return walkCollectionTree(persister, alias, new ArrayList(), new HashSet(), "", 0);
		//TODO: when this is the entry point, we should use an INNER_JOIN for fetching the many-to-many elements!
	}

	/**
	 * For a collection role, return a list of associations to be fetched by outerjoin
	 */
	private final List walkCollectionTree(
		final QueryableCollection persister,
		final String alias,
		final List associations,
		final Set visitedAssociations,
		final String path,
		final int currentDepth)
	throws MappingException {

		if ( persister.isOneToMany() ) {
			walkEntityTree(
				(OuterJoinLoadable) persister.getElementPersister(),
				alias,
				associations,
				visitedAssociations,
				path,
				currentDepth
			);
		}
		else {
			Type type = persister.getElementType();
			if ( type.isAssociationType() ) {
				// a many-to-many
				AssociationType associationType = (AssociationType) type;
				String[] aliasedLhsColumns = persister.getElementColumnNames(alias);
				String[] lhsColumns = persister.getElementColumnNames();
				final int joinType = getJoinType(
					associationType,
					persister.getFetchMode(),
					path,
					visitedAssociations,
					persister.getTableName(),
					lhsColumns,
					false,
					currentDepth
				);
				addAssociationToJoinTreeIfNecessary(
					associationType,
					aliasedLhsColumns,
					alias,
					associations,
					visitedAssociations,
					path,
					currentDepth,
					joinType
				);
			}
			else if ( type.isComponentType() ) {
				walkCompositeElementTree(
					(AbstractComponentType) type,
					persister.getElementColumnNames(),
					persister,
					alias,
					associations,
					new HashSet(),
					path,
					currentDepth
				);
			}
		}

		return associations;
	}
	
	/**
	 * Walk the tree for a particular entity association
	 */
	private final void walkEntityAssociationTree(
		final AssociationType associationType,
		final OuterJoinLoadable persister,
		final int propertyNumber,
		final String alias,
		final List associations,
		final Set visitedAssociations,
		final String path,
		final boolean nullable,
		final int currentDepth)
	throws MappingException {

		String[] aliasedLhsColumns = JoinHelper.getAliasedLHSColumnNames(
			associationType, alias, propertyNumber, persister, getFactory()
		);

		String[] lhsColumns = JoinHelper.getLHSColumnNames(
			associationType, propertyNumber, persister, getFactory()
		);
		String lhsTable = JoinHelper.getLHSTableName(associationType, propertyNumber, persister);

		String subpath = subPath( path, persister.getSubclassPropertyName(propertyNumber) );
		int joinType = getJoinType(
			associationType,
			persister.getFetchMode(propertyNumber),
			subpath,
			visitedAssociations,
			lhsTable,
			lhsColumns,
			nullable,
			currentDepth
		);
		addAssociationToJoinTreeIfNecessary(
			associationType,
			aliasedLhsColumns,
			alias,
			associations,
			visitedAssociations,
			subpath,
			currentDepth,
			joinType
		);

	}

	/**
	 * For an entity class, add to a list of associations to be fetched 
	 * by outerjoin
	 */
	private final void walkEntityTree(
		final OuterJoinLoadable persister,
		final String alias,
		final List associations,
		final Set visitedAssociations,
		final String path,
		final int currentDepth) 
	throws MappingException {

		int n = persister.countSubclassProperties();
		for ( int i=0; i<n; i++ ) {
			Type type = persister.getSubclassPropertyType(i);
			if ( type.isAssociationType() ) {
				walkEntityAssociationTree(
					(AssociationType) type,
					persister,
					i,
					alias,
					associations,
					visitedAssociations,
					path,
					persister.isSubclassPropertyNullable(i),
					currentDepth
				);
			}
			else if ( type.isComponentType() ) {
				walkComponentTree(
					(AbstractComponentType) type,
					i,
					0,
					persister,
					alias,
					associations,
					visitedAssociations,
					subPath( path, persister.getSubclassPropertyName(i) ),
					currentDepth
				);
			}
		}
	}

	/**
	 * For a component, add to a list of associations to be fetched by outerjoin
	 */
	private void walkComponentTree(
		final AbstractComponentType componentType,
		final int propertyNumber,
		int begin,
		final OuterJoinLoadable persister,
		final String alias,
		final List associations,
		final Set visitedAssociations,
		final String path,
		final int currentDepth
	) throws MappingException {

		Type[] types = componentType.getSubtypes();
		String[] propertyNames = componentType.getPropertyNames();
		for ( int i=0; i <types.length; i++ ) {

			if ( types[i].isAssociationType() ) {
				AssociationType associationType = (AssociationType) types[i];

				String[] aliasedLhsColumns = JoinHelper.getAliasedLHSColumnNames(
					associationType, alias, propertyNumber, begin, persister, getFactory()
				);

				String[] lhsColumns = JoinHelper.getLHSColumnNames(
					associationType, propertyNumber, begin, persister, getFactory()
				);
				String lhsTable = JoinHelper.getLHSTableName(associationType, propertyNumber, persister);

				String subpath = subPath( path, propertyNames[i] );
				final boolean[] propertyNullability = componentType.getPropertyNullability();
				final int joinType = getJoinType(
					associationType,
					componentType.getFetchMode(i),
					subpath,
					visitedAssociations,
					lhsTable,
					lhsColumns,
					propertyNullability==null || propertyNullability[i],
					currentDepth
				);
				addAssociationToJoinTreeIfNecessary(			
					associationType,
					aliasedLhsColumns,
					alias,
					associations,
					visitedAssociations,
					subpath,
					currentDepth,
					joinType
				);

			}
			else if ( types[i].isComponentType() ) {
				String subpath = subPath( path, propertyNames[i] );
				walkComponentTree(
					(AbstractComponentType) types[i],
					propertyNumber,
					begin,
					persister,
					alias,
					associations,
					visitedAssociations,
					subpath,
					currentDepth
				);
			}
			
			begin+=types[i].getColumnSpan( getFactory() );
		}

	}

	/**
	 * For a composite element, add to a list of associations to be fetched by outerjoin
	 */
	private void walkCompositeElementTree(
		final AbstractComponentType compositeType,
		final String[] cols,
		final QueryableCollection persister,
		final String alias,
		final List associations,
		final Set visitedAssociations,
		final String path,
		final int currentDepth) 
	throws MappingException {

		Type[] types = compositeType.getSubtypes();
		String[] propertyNames = compositeType.getPropertyNames();
		int begin = 0;
		for ( int i=0; i <types.length; i++ ) {
			int length = types[i].getColumnSpan( getFactory() );
			String[] lhsColumns = ArrayHelper.slice(cols, begin, length);

			if ( types[i].isAssociationType() ) {
				AssociationType associationType = (AssociationType) types[i];

				// simple, because we can't have a one-to-one or a collection 
				// (or even a property-ref) in a composite-element:
				String[] aliasedLhsColumns = StringHelper.qualify(alias, lhsColumns);

				String subpath = subPath( path, propertyNames[i] );
				final boolean[] propertyNullability = compositeType.getPropertyNullability();
				final int joinType = getJoinType(
					associationType,
					compositeType.getFetchMode(i),
					subpath,
					visitedAssociations,
					persister.getTableName(),
					lhsColumns,
					propertyNullability==null || propertyNullability[i],
					currentDepth
				);
				addAssociationToJoinTreeIfNecessary(
					associationType,
					aliasedLhsColumns,
					alias,
					associations,
					visitedAssociations,
					subpath,
					currentDepth,
					joinType
				);
			}
			else if ( types[i].isComponentType() ) {
				String subpath = subPath( path, propertyNames[i] );
				walkCompositeElementTree(
					(AbstractComponentType) types[i],
					lhsColumns,
					persister,
					alias,
					associations,
					visitedAssociations,
					subpath,
					currentDepth
				);
			}
			begin+=length;
		}

	}

	/**
	 * Does the mapping, and Hibernate default semantics, specify that
	 * this association should be fetched by outer joining
	 */
	protected boolean isJoinedFetchEnabledInMapping(FetchMode config, AssociationType type) 
	throws MappingException {
		if ( !type.isEntityType() && !type.isCollectionType() ) {
			return false;
		}
		else {
			if (config==FetchMode.JOIN) return true;
			if (config==FetchMode.SELECT) return false;
			if ( type.isEntityType() ) {
				//TODO: look at the owning property and check that it 
				//      isn't lazy (by instrumentation)
				EntityType entityType =(EntityType) type;
				EntityPersister persister = getFactory().getEntityPersister( entityType.getAssociatedEntityName() );
				return !persister.hasProxy();
			}
			else {
				return false;
			}
		}
	}

	/**
	 * Add on association (one-to-one, many-to-one, or a collection) to a list 
	 * of associations to be fetched by outerjoin (if necessary)
	 */
	private void addAssociationToJoinTreeIfNecessary(
		final AssociationType type,
		final String[] aliasedLhsColumns,
		final String alias,
		final List associations,
		final Set visitedAssociations,
		final String path,
		int currentDepth,
		final int joinType)
	throws MappingException {
		
		if (joinType<0) return;
				
		// to avoid cartesian product problem, and since
		// Loader cannot handle multiple collection roles
		final boolean isCartesianProduct = joinType!=JoinFragment.INNER_JOIN && 
			type.getAssociatedJoinable( getFactory() ).isCollection() &&
			containsCollectionPersister(associations);

		if ( !isCartesianProduct ) {
		
			addAssociationToJoinTree(
					type, 
					aliasedLhsColumns, 
					alias, 
					associations, 
					visitedAssociations, 
					path,
					currentDepth,
					joinType
			);
			
		}

	}

	/**
	 * Add on association (one-to-one, many-to-one, or a collection) to a list 
	 * of associations to be fetched by outerjoin 
	 */
	private void addAssociationToJoinTree(
		final AssociationType type,
		final String[] aliasedLhsColumns,
		final String alias,
		final List associations,
		final Set visitedAssociations,
		final String path,
		final int currentDepth,
		final int joinType)
	throws MappingException {

		Joinable joinable = type.getAssociatedJoinable( getFactory() );

		String subalias = generateTableAlias(
			associations.size()+1, //before adding to collection!
			path, 
			joinable
		);

		OuterJoinableAssociation assoc = new OuterJoinableAssociation(
			type, 
			alias, 
			aliasedLhsColumns, 
			subalias, 
			joinType, 
			getFactory(), 
			enabledFilters
		);
		assoc.validateJoin(path);
		associations.add(assoc);

		int nextDepth = currentDepth+1;
		if ( !joinable.isCollection() ) {
			if (joinable instanceof OuterJoinLoadable) {
				walkEntityTree(
					(OuterJoinLoadable) joinable, 
					subalias, 
					associations, 
					visitedAssociations, 
					path, 
					nextDepth
				);
			}
		}
		else {
			if (joinable instanceof QueryableCollection) {
				walkCollectionTree(
					(QueryableCollection) joinable, 
					subalias, 
					associations, 
					visitedAssociations, 
					path, 
					nextDepth
				);
			}
		}

	}


	/**
	 * Generate a select list of columns containing all properties of the entity classes
	 */
	protected final String selectString(List associations)
	throws MappingException {

		if ( associations.size()==0 ) {
			return "";
		}
		else {
			StringBuffer buf = new StringBuffer( associations.size() * 100 )
				.append(", ");
			int aliasCount=0;
			for ( int i=0; i<associations.size(); i++ ) {
				OuterJoinableAssociation join = (OuterJoinableAssociation) associations.get(i);
				final Joinable joinable = join.getJoinable();
				final String selectFragment = joinable.selectFragment(
					join.getRHSAlias(),
					getSuffixes()[aliasCount],
					join.getJoinType()==JoinFragment.LEFT_OUTER_JOIN
				);
				buf.append(selectFragment);
				if ( joinable.consumesAlias() ) aliasCount++;
				if (
					i<associations.size()-1 &&
					selectFragment.trim().length()>0
				) {
					buf.append(", ");
				}
			}
			return buf.toString();
		}
	}

	protected String[] getSuffixes() {
		return suffixes;
	}

	protected String generateTableAlias(
		final int n,
		final String path,
		final Joinable joinable
	) {
		return StringHelper.generateAlias( joinable.getName(), n );
	}

	protected String generateRootAlias(final String description) {
		return StringHelper.generateAlias(description, 0);
	}

	/**
	 * Generate a sequence of <tt>LEFT OUTER JOIN</tt> clauses for the given associations.
	 */
	protected final JoinFragment mergeOuterJoins(List associations)
	throws MappingException {
		JoinFragment outerjoin = getDialect().createOuterJoinFragment();
		Iterator iter = associations.iterator();
		while ( iter.hasNext() ) {
			OuterJoinableAssociation oj = (OuterJoinableAssociation) iter.next();
			oj.addJoins(outerjoin);
		}
		return outerjoin;
	}

	/**
	 * Count the number of instances of Joinable which are actually
	 * also instances of Loadable, or are one-to-many associations
	 */
	protected static final int countEntityPersisters(List associations)
	throws MappingException {
		int result = 0;
		Iterator iter = associations.iterator();
		while ( iter.hasNext() ) {
			OuterJoinableAssociation oj = (OuterJoinableAssociation) iter.next();
			if ( oj.getJoinable().consumesAlias() ) result++;
		}
		return result;
	}
	
	/**
	 * Since Loader can handle only one collection role, we need
	 * to ignore any collections after the first one
	 */
	protected static boolean containsCollectionPersister(List associations)
	throws MappingException {
		Iterator iter = associations.iterator();
		while ( iter.hasNext() ) {
			OuterJoinableAssociation oj = (OuterJoinableAssociation) iter.next();
			if ( oj.getJoinable().isCollection() ) return true;
		}
		return false;
	}

	/**
	 * Extend the path by the given property name
	 */
	private static String subPath(String path, String property) {
		if ( path==null || path.length()==0) {
			return property;
		}
		else {
			return StringHelper.qualify(path, property);
		}
	}

	/**
	 * Render the where condition for a (batch) load by identifier / collection key
	 */
	protected StringBuffer whereString(String alias, String[] columnNames, int batchSize, String subquery) {
		if ( columnNames.length==1 ) {
			// if not a composite key, use "foo in (?, ?, ?)" for batching
			// if no batch, and not a composite key, use "foo = ?"
			InFragment in = new InFragment().setColumn( alias, columnNames[0] );
			for ( int i=0; i<batchSize; i++ ) in.addValue("?");
			return new StringBuffer( in.toFragmentString() );
		}
		else {
			//a composite key
			ConditionFragment byId = new ConditionFragment()
				.setTableAlias(alias)
				.setCondition( columnNames, "?" );
	
			StringBuffer whereString = new StringBuffer();
			if ( batchSize==1 ) {
				// if no batch, use "foo = ? and bar = ?"
				whereString.append( byId.toFragmentString() );
			}
			else {
				// if a composite key, use "( (foo = ? and bar = ?) or (foo = ? and bar = ?) )" for batching
				whereString.append('('); //TODO: unnecessary for databases with ANSI-style joins
				DisjunctionFragment df = new DisjunctionFragment();
				for ( int i=0; i<batchSize; i++ ) {
					df.addCondition(byId);
				}
				whereString.append( df.toFragmentString() );
				whereString.append(')'); //TODO: unnecessary for databases with ANSI-style joins
			}
			return whereString;
		}
	}

	protected final String getSQLString() {
		return sql;
	}

	protected final Loadable[] getEntityPersisters() {
		return persisters;
	}

	protected int[] getOwners() {
		return owners;
	}

	protected AssociationType[] getOwnerAssociationTypes() {
		return ownerAssociationType;
	}

	protected LockMode[] getLockModes(Map lockModes) {
		return lockModeArray;
	}
	
	public Map getEnabledFilters() {
		return enabledFilters;
	}
}
