//$Id: AbstractEntityLoader.java,v 1.28 2005/03/17 05:50:09 oneovthafew Exp $
package org.hibernate.loader;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.FetchMode;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.loader.criteria.CriteriaLoader;
import org.hibernate.loader.entity.EntityLoader;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.Select;
import org.hibernate.type.AssociationType;
import org.hibernate.util.ArrayHelper;

/**
 * Abstract superclass for entity loaders that use outer joins
 *
 * @see CriteriaLoader
 * @see EntityLoader
 * @author Gavin King
 */
public abstract class AbstractEntityLoader extends OuterJoinLoader {

	private final OuterJoinLoadable persister;
	private CollectionPersister collectionPersister;
	private int collectionOwner;
	private String alias;
	private String[] aliases;
	
	protected String[] getAliases() {
		return aliases;
	}

	public AbstractEntityLoader(OuterJoinLoadable persister, SessionFactoryImplementor factory, Map enabledFilters) {
		super(factory, enabledFilters);
		this.persister = persister;
		alias = generateRootAlias( persister.getEntityName() );
	}

	/*protected final void addAllToPropertySpaces(Serializable[] spaces) {
		for ( int i=0; i<spaces.length; i++ ) {
			addToPropertySpaces( spaces[i] );
		}
	}

	protected void addToPropertySpaces(Serializable space) {
		throw new AssertionFailure("only criteria queries need to autoflush");
	}*/

	protected final void initAll(
		final String whereString,
		final String orderByString,
		final LockMode lockMode)
	throws MappingException {
		final List associations = walkEntityTree( persister, getAlias() );
		initPersisters(associations, lockMode);
		initStatementString(associations, whereString, orderByString, lockMode);
	}
	
	protected final void initProjection(
		final String projectionString,
		final String whereString,
		final String orderByString,
		final String groupByString,
		final LockMode lockMode)
	throws MappingException {
		final List associations = walkEntityTree( persister, getAlias() );
		persisters = new Loadable[0];		
		initStatementString(associations, projectionString, whereString, orderByString, groupByString, lockMode);
	}

	private void initPersisters(
		final List associations,
		final LockMode lockMode)
	throws MappingException {

		final int joins = countEntityPersisters(associations);

		collectionOwner = -1; //if no collection found
		persisters = new Loadable[joins+1];
		aliases = new String[joins+1];
		owners = new int[joins+1];
		ownerAssociationType = new AssociationType[joins+1];
		lockModeArray = ArrayHelper.fillArray(lockMode, joins+1);
		int i=0;
		Iterator iter = associations.iterator();
		while ( iter.hasNext() ) {
			final OuterJoinableAssociation oj = (OuterJoinableAssociation) iter.next();
			if ( !oj.isCollection() ) {
				persisters[i] = (Loadable) oj.getJoinable();
				aliases[i] = oj.getRHSAlias();
				owners[i] = oj.getOwner(associations);
				ownerAssociationType[i] = oj.getJoinableType();
				/*if ( oj.getJoinType()==JoinFragment.INNER_JOIN ) {
					addAllToPropertySpaces( persisters[i].getQuerySpaces() );
				}*/
				i++;
			}
			else {
				QueryableCollection collPersister = (QueryableCollection) oj.getJoinable();
				if ( oj.getJoinType()==JoinFragment.LEFT_OUTER_JOIN ) {
					collectionPersister = collPersister;
					collectionOwner = oj.getOwner(associations);
				}
				/*else {
					addAllToPropertySpaces( collPersister.getCollectionSpaces() );
				}*/

				if ( collPersister.isOneToMany() ) {
					persisters[i] = (Loadable) collPersister.getElementPersister();
					aliases[i] = oj.getRHSAlias();
					i++;
				}
			}
		}
		persisters[joins] = persister;
		owners[joins] = -1;
		aliases[joins] = alias;

		if ( ArrayHelper.isAllNegative(owners) ) owners = null;
	}
	
	private void initStatementString(
		final List associations,
		final String condition,
		final String orderBy,
		final LockMode lockMode)
	throws MappingException {
		initStatementString(associations, null, condition, orderBy, "", lockMode);
	}
	
	private void initStatementString(
			final List associations,
			final String projection,
			final String condition,
			final String orderBy,
			final String groupBy,
			final LockMode lockMode)
		throws MappingException {

		final int joins = countEntityPersisters(associations);

		suffixes = generateSuffixes(joins+1);

		JoinFragment ojf = mergeOuterJoins(associations);

		Select select = new Select( getDialect() )
			.setLockMode(lockMode)
			.setSelectClause(
					projection==null ? 
							persister.selectFragment( alias, suffixes[joins] ) + selectString(associations) : 
							projection
			)
			.setFromClause(
				persister.fromTableFragment(alias) +
				persister.fromJoinFragment(alias, true, true)
			)
			.setWhereClause(condition)
			.setOuterJoins(
				ojf.toFromFragmentString(),
				ojf.toWhereFragmentString() + getWhereFragment()
			)
			.setOrderByClause(orderBy)
			.setGroupByClause(groupBy);

		if ( getFactory().getSettings().isCommentsEnabled() ) {
			select.setComment( getComment() );
		}
		sql = select.toStatementString();
	}

	/**
	 * Don't bother with the discriminator, unless overridded by subclass
	 */
	protected String getWhereFragment() throws MappingException {
		return persister.whereJoinFragment(alias, true, true);
	}

	protected final Loadable getPersister() {
		return persister;
	}

	protected final String getAlias() {
		return alias;
	}

	protected final CollectionPersister getCollectionPersister() {
		return collectionPersister;
	}

	protected final int getCollectionOwner() {
		return collectionOwner;
	}
	
	/**
	 * The superclass deliberately excludes collections
	 */
	protected boolean isJoinedFetchEnabled(AssociationType type, FetchMode config) {
		return isJoinedFetchEnabledInMapping(config, type);
	}

	public String toString() {
		return getClass().getName() + '(' + getPersister().getEntityName() + ')';
	}

	public abstract String getComment();
}
