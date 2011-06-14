//$Id: CollectionLoader.java,v 1.3 2005/03/21 17:29:53 oneovthafew Exp $
package org.hibernate.loader.collection;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.loader.OuterJoinLoader;
import org.hibernate.loader.OuterJoinableAssociation;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.Select;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.StringHelper;

/**
 * Loads a collection of values or a many-to-many association.
 * <br>
 * The collection persister must implement <tt>QueryableCOllection<tt>. For
 * other collections, create a customized subclass of <tt>Loader</tt>.
 *
 * @see OneToManyLoader
 * @author Gavin King
 */
public class CollectionLoader extends OuterJoinLoader implements CollectionInitializer {

	private static final Log log = LogFactory.getLog(CollectionLoader.class);

	private final QueryableCollection collectionPersister;
	private final Type keyType;
	private String[] aliases;

	public CollectionLoader(QueryableCollection persister, SessionFactoryImplementor session, Map enabledFilters)
	throws MappingException {
		this(persister, 1, session, enabledFilters);
	}

	public CollectionLoader(QueryableCollection persister, int batchSize, SessionFactoryImplementor factory, Map enabledFilters)
	throws MappingException {
		this(persister, batchSize, null, factory, enabledFilters);
	}
	
	public CollectionLoader(QueryableCollection persister, int batchSize, String subquery, SessionFactoryImplementor factory, Map enabledFilters)
	throws MappingException {

		super(factory, enabledFilters);

		this.keyType = persister.getKeyType();
		this.collectionPersister = persister;

		String alias = generateRootAlias( persister.getRole() );

		final List associations = walkCollectionTree(persister, alias);
		initStatementString(persister, alias, associations, batchSize, subquery);
		initPersisters(associations);

		postInstantiate();

		log.debug( "Static select for collection " + persister.getRole() + ": " + getSQLString() );
	}

	private void initPersisters(List associations)
	throws MappingException {

		final int joins = associations.size();
		lockModeArray = ArrayHelper.fillArray(LockMode.NONE, joins);

		persisters = new Loadable[joins];
		owners = new int[joins];
		aliases = new String[joins];
		ownerAssociationType = new AssociationType[joins];
		for ( int i=0; i<joins; i++ ) {
			OuterJoinableAssociation oj = (OuterJoinableAssociation) associations.get(i);
			persisters[i] = (Loadable) oj.getJoinable();
			//cast is safe b/c one-to-many can't outerjoin to another collection!
			owners[i] = oj.getOwner(associations);
			ownerAssociationType[i] = oj.getJoinableType();
			aliases[i] = oj.getRHSAlias();
		}

		if ( ArrayHelper.isAllNegative(owners) ) owners = null;


	}

	protected CollectionPersister getCollectionPersister() {
		return collectionPersister;
	}

	public void initialize(Serializable id, SessionImplementor session)
	throws HibernateException {
		loadCollection(session, id, keyType);
	}

	private void initStatementString(
		final QueryableCollection persister,
		final String alias,
		final List associations,
		final int batchSize,
		final String subquery)
	throws MappingException {

		final int joins=associations.size();
		suffixes = generateSuffixes(joins);

		StringBuffer whereString = whereString(alias, persister.getKeyColumnNames(), batchSize, subquery);
		String filter = persister.filterFragment( alias, getEnabledFilters() );
		whereString.insert( 0, StringHelper.moveAndToBeginning( filter ) );

		JoinFragment ojf = mergeOuterJoins(associations);
		Select select = new Select( getDialect() )
			.setSelectClause(
				persister.selectFragment(alias) +
				selectString(associations)
			)
			.setFromClause( persister.getTableName(), alias )
			.setWhereClause( whereString.toString()	)
			.setOuterJoins(
				ojf.toFromFragmentString(),
				ojf.toWhereFragmentString()
			);

		if ( persister.hasOrdering() ) {
			select.setOrderByClause( persister.getSQLOrderByString(alias) );
		}

		if ( getFactory().getSettings().isCommentsEnabled() ) {
			select.setComment( "load collection " + persister.getRole() );
		}

		sql = select.toStatementString();
	}

	/**
	 * We can use an inner join for first many-to-many association
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

		int joinType = super.getJoinType(
				type, 
				config, 
				path, 
				visitedAssociations, 
				lhsTable, 
				lhsColumns, 
				nullable, 
				currentDepth
		);
		//we can use an inner join for the many-to-many
		if ( joinType==JoinFragment.LEFT_OUTER_JOIN && "".equals(path) ) {
			joinType=JoinFragment.INNER_JOIN;
		}
		return joinType;
	}
	
	protected Type getKeyType() {
		return keyType;
	}

	public String toString() {
		return getClass().getName() + '(' + collectionPersister.getRole() + ')';
	}

	protected String[] getAliases() {
		return aliases;
	}

	protected boolean isSubselectLoadingEnabled() {
		return hasSubselectLoadableCollections();
	}

}
