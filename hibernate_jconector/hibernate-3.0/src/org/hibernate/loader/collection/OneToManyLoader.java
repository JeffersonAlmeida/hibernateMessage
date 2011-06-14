//$Id: OneToManyLoader.java,v 1.2 2005/03/21 17:29:54 oneovthafew Exp $
package org.hibernate.loader.collection;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.Select;
import org.hibernate.type.AssociationType;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.StringHelper;

/**
 * Loads one-to-many associations<br>
 * <br>
 * The collection persister must implement <tt>QueryableCOllection<tt>. For
 * other collections, create a customized subclass of <tt>Loader</tt>.
 *
 * @see CollectionLoader
 * @author Gavin King
 */
public class OneToManyLoader extends OuterJoinLoader implements CollectionInitializer {

	private static final Log log = LogFactory.getLog(OneToManyLoader.class);

	private final QueryableCollection collectionPersister;
	private final Type keyType;

	protected boolean isDuplicateAssociation(
		final Set visitedAssociationKeys, 
		final String foreignKeyTable, 
		final String[] foreignKeyColumns
	) {
		//disable a join back to this same association
		final boolean isSameJoin = collectionPersister.getTableName().equals(foreignKeyTable) &&
			Arrays.equals( foreignKeyColumns, collectionPersister.getKeyColumnNames() );
		return isSameJoin || 
			super.isDuplicateAssociation(visitedAssociationKeys, foreignKeyTable, foreignKeyColumns);
	}

	public OneToManyLoader(
			QueryableCollection collPersister, 
			SessionFactoryImplementor session, 
			Map enabledFilters)
	throws MappingException {
		this(collPersister, 1, session, enabledFilters);
	}

	public OneToManyLoader(
			QueryableCollection collPersister, 
			int batchSize, 
			SessionFactoryImplementor factory, 
			Map enabledFilters)
	throws MappingException {
		this(collPersister, batchSize, null, factory, enabledFilters);
	}

	public OneToManyLoader(
			QueryableCollection collPersister, 
			int batchSize, 
			String subquery, 
			SessionFactoryImplementor factory, 
			Map enabledFilters)
	throws MappingException {

		super(factory, enabledFilters);

		this.collectionPersister = collPersister;
		this.keyType = collPersister.getKeyType();

		final OuterJoinLoadable persister = (OuterJoinLoadable) collPersister.getElementPersister();
		final String alias = generateRootAlias( collPersister.getRole() );

		final List associations = walkEntityTree(persister, alias);
		initPersisters(persister, associations);

		initStatementString(collPersister, persister, alias, associations, batchSize, subquery);

		postInstantiate();

		log.debug( "Static select for one-to-many " + collPersister.getRole() + ": " + getSQLString() );
	}

	protected CollectionPersister getCollectionPersister() {
		return collectionPersister;
	}

	public void initialize(Serializable id, SessionImplementor session)
	throws HibernateException {
		loadCollection(session, id, keyType);
	}

	private void initPersisters(
		final OuterJoinLoadable persister,
		final List associations)
	throws MappingException {

		final int joins = associations.size();
		lockModeArray = ArrayHelper.fillArray(LockMode.NONE, joins+1);

		persisters = new Loadable[joins+1];
		owners = new int[joins+1];
		ownerAssociationType = new AssociationType[joins+1];
		for ( int i=0; i<joins; i++ ) {
			OuterJoinableAssociation oj = (OuterJoinableAssociation) associations.get(i);
			persisters[i] = (Loadable) oj.getJoinable();
			//cast is safe b/c one-to-many can't outerjoin to another collection!
			owners[i] = oj.getOwner(associations);
			ownerAssociationType[i] = oj.getJoinableType();
		}
		persisters[joins] = persister;
		owners[joins] = -1;

		if ( ArrayHelper.isAllNegative(owners) ) owners = null;
	}

	private void initStatementString(
		final QueryableCollection collPersister,
		final OuterJoinLoadable persister,
		final String alias,
		final List associations,
		final int batchSize,
		final String subquery)
	throws MappingException {

		final int joins=associations.size();
		suffixes = generateSuffixes(joins+1);

		StringBuffer whereString = whereString(
				alias, collPersister.getKeyColumnNames(), batchSize, subquery
		);
		String filter = collPersister.filterFragment( alias, getEnabledFilters() );
		whereString.insert( 0, StringHelper.moveAndToBeginning(filter) );

		JoinFragment ojf = mergeOuterJoins(associations);
		Select select = new Select( getDialect() )
			.setSelectClause(
				collPersister.selectFragment(alias, suffixes[joins], true) +
				selectString(associations)
			)
			.setFromClause(
				persister.fromTableFragment(alias) +
				persister.fromJoinFragment(alias, true, true)
			)
			.setWhereClause( whereString.toString() )
			.setOuterJoins(
				ojf.toFromFragmentString(),
				ojf.toWhereFragmentString() +
				persister.whereJoinFragment(alias, true, true)
			);

		if ( collPersister.hasOrdering() ) {
			select.setOrderByClause( collPersister.getSQLOrderByString(alias) );
		}

		if ( getFactory().getSettings().isCommentsEnabled() ) {
			select.setComment( "load one-to-many " + collPersister.getRole() );
		}

		sql = select.toStatementString();
	}

	protected Type getKeyType() {
		return keyType;
	}

	public String toString() {
		return getClass().getName() + '(' + collectionPersister.getRole() + ')';
	}


}
