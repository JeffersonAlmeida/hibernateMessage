//$Id: JoinSequence.java,v 1.15 2005/02/23 03:14:11 oneovthafew Exp $
package org.hibernate.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.QueryJoinFragment;
import org.hibernate.type.AssociationType;
import org.hibernate.util.CollectionHelper;

/**
 * @author Gavin King
 */
public class JoinSequence {

	private final SessionFactoryImplementor factory;
	private final List joins = new ArrayList();
	private boolean useThetaStyle = false;
	private final StringBuffer conditions = new StringBuffer();
	private String rootAlias;
	private Joinable rootJoinable;
	private Selector selector;
	private JoinSequence next;
	private boolean isFromPart = false;

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append( "JoinSequence{" );
		if ( rootJoinable != null ) {
			buf.append( rootJoinable )
					.append( '[' )
					.append( rootAlias )
					.append( ']' );
		}
		for ( int i = 0; i < joins.size(); i++ ) {
			buf.append( "->" ).append( joins.get( i ) );
		}
		return buf.append( '}' ).toString();
	}

	final class Join {

		private final AssociationType associationType;
		private final Joinable joinable;
		private final int joinType;
		private final String alias;
		private final String[] lhsColumns;

		Join(AssociationType associationType, String alias, int joinType, String[] lhsColumns)
				throws MappingException {
			this.associationType = associationType;
			this.joinable = associationType.getAssociatedJoinable( factory );
			this.alias = alias;
			this.joinType = joinType;
			this.lhsColumns = lhsColumns;
		}

		String getAlias() {
			return alias;
		}

		AssociationType getAssociationType() {
			return associationType;
		}

		Joinable getJoinable() {
			return joinable;
		}

		int getJoinType() {
			return joinType;
		}

		String[] getLHSColumns() {
			return lhsColumns;
		}

		public String toString() {
			return joinable.toString() + '[' + alias + ']';
		}
	}

	public JoinSequence(SessionFactoryImplementor factory) {
		this.factory = factory;
	}

	public JoinSequence getFromPart() {
		JoinSequence fromPart = new JoinSequence( factory );
		fromPart.joins.addAll( this.joins );
		fromPart.useThetaStyle = this.useThetaStyle;
		fromPart.rootAlias = this.rootAlias;
		fromPart.rootJoinable = this.rootJoinable;
		fromPart.selector = this.selector;
		fromPart.next = this.next == null ? null : this.next.getFromPart();
		fromPart.isFromPart = true;
		return fromPart;
	}

	public JoinSequence copy() {
		JoinSequence copy = new JoinSequence( factory );
		copy.joins.addAll( this.joins );
		copy.useThetaStyle = this.useThetaStyle;
		copy.rootAlias = this.rootAlias;
		copy.rootJoinable = this.rootJoinable;
		copy.selector = this.selector;
		copy.next = this.next == null ? null : this.next.copy();
		copy.isFromPart = this.isFromPart;
		copy.conditions.append( this.conditions.toString() );
		return copy;
	}

	public JoinSequence addJoin(AssociationType associationType, String alias, int joinType, String[] referencingKey)
			throws MappingException {
		joins.add( new Join( associationType, alias, joinType, referencingKey ) );
		return this;
	}

	public JoinFragment toJoinFragment() throws MappingException {
		return toJoinFragment( CollectionHelper.EMPTY_MAP, true );
	}

	public JoinFragment toJoinFragment(Map enabledFilters, boolean includeExtraJoins) throws MappingException {
		QueryJoinFragment joinFragment = new QueryJoinFragment( factory.getDialect(), useThetaStyle );
		if ( rootJoinable != null ) {
			joinFragment.addCrossJoin( rootJoinable.getTableName(), rootAlias );
			String filterCondition = rootJoinable.filterFragment( rootAlias, enabledFilters );
			// JoinProcessor needs to know if the where clause fragment came from a dynamic filter or not so it
			// can put the where clause fragment in the right place in the SQL AST.   'hasFilterCondition' keeps track
			// of that fact.
			joinFragment.setHasFilterCondition( joinFragment.addCondition( filterCondition ) );
			if (includeExtraJoins) { //TODO: not quite sure about the full implications of this!
				addExtraJoins( joinFragment, rootAlias, rootJoinable, true );
			}
		}
		for ( int i = 0; i < joins.size(); i++ ) {
			Join join = ( Join ) joins.get( i );
			joinFragment.addJoin( join.getJoinable().getTableName(),
					join.getAlias(),
					join.getLHSColumns(),
					JoinHelper.getRHSColumnNames( join.getAssociationType(), factory ),
					join.joinType,
					join.getAssociationType().getOnCondition( join.getAlias(), factory, enabledFilters ) );
			if (includeExtraJoins) { //TODO: not quite sure about the full implications of this!
				addExtraJoins( joinFragment, join.getAlias(), join.getJoinable(), join.joinType == JoinFragment.INNER_JOIN );
			}
		}
		if ( next != null ) {
			joinFragment.addFragment( next.toJoinFragment( enabledFilters, includeExtraJoins ) );
		}
		joinFragment.addCondition( conditions.toString() );
		if ( isFromPart ) joinFragment.clearWherePart();
		return joinFragment;
	}

	private boolean isIncluded(String alias) {
		return selector != null && selector.includeSubclasses( alias );
	}

	private void addExtraJoins(JoinFragment joinFragment, String alias, Joinable joinable, boolean innerJoin) {
		boolean include = isIncluded( alias );
		joinFragment.addJoins( joinable.fromJoinFragment( alias, innerJoin, include ),
				joinable.whereJoinFragment( alias, innerJoin, include ) );
	}

	public JoinSequence addCondition(String condition) {
		if ( condition.trim().length() != 0 ) {
			if ( !condition.startsWith( " and " ) ) conditions.append( " and " );
			conditions.append( condition );
		}
		return this;
	}

	public JoinSequence addCondition(String alias, String[] columns, String condition) {
		for ( int i = 0; i < columns.length; i++ ) {
			conditions.append( " and " )
					.append( alias )
					.append( '.' )
					.append( columns[i] )
					.append( condition );
		}
		return this;
	}

	public JoinSequence setRoot(Joinable joinable, String alias) {
		this.rootAlias = alias;
		this.rootJoinable = joinable;
		return this;
	}

	public JoinSequence setNext(JoinSequence next) {
		this.next = next;
		return this;
	}

	public JoinSequence setSelector(Selector s) {
		this.selector = s;
		return this;
	}

	public JoinSequence setUseThetaStyle(boolean useThetaStyle) {
		this.useThetaStyle = useThetaStyle;
		return this;
	}

	public boolean isThetaStyle() {
		return useThetaStyle;
	}

	public int getJoinCount() {
		return joins.size();
	}
	
	public static interface Selector {
		public boolean includeSubclasses(String alias);
	}
}
