// $Id: JoinProcessor.java,v 1.46 2005/03/25 04:04:11 oneovthafew Exp $
package org.hibernate.hql.ast;

import antlr.ASTFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.AssertionFailure;
import org.hibernate.engine.JoinSequence;
import org.hibernate.hql.antlr.SqlTokenTypes;
import org.hibernate.sql.JoinFragment;
import org.hibernate.util.StringHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;

/**
 * Performs the post-processing of the join information gathered during semantic analysis.
 * The join generating classes are complex, this encapsulates some of the JoinSequence-related
 * code.
 *
 * @author josh Jul 22, 2004 7:33:42 AM
 */
class JoinProcessor implements SqlTokenTypes {
	private static final Log log = LogFactory.getLog( JoinProcessor.class );
	private QueryTranslatorImpl queryTranslatorImpl;
	private SyntheticAndFactory andFactory;

	public JoinProcessor(ASTFactory astFactory, QueryTranslatorImpl queryTranslatorImpl) {
		this.andFactory = new SyntheticAndFactory( astFactory );
		this.queryTranslatorImpl = queryTranslatorImpl;
	}

	/**
	 * Translates an AST join type into a JoinFragment.XXX join type.
	 *
	 * @param astJoinType The AST join type (from HqlSqlTokenTypes or SqlTokenTypes)
	 * @return a JoinFragment.XXX join type.
	 * @see JoinFragment
	 * @see SqlTokenTypes
	 */
	public static int toHibernateJoinType(int astJoinType) {
		switch ( astJoinType ) {
			case LEFT_OUTER:
				return JoinFragment.LEFT_OUTER_JOIN;
			case INNER:
				return JoinFragment.INNER_JOIN;
			case RIGHT_OUTER:
				return JoinFragment.RIGHT_OUTER_JOIN;
			default:
				throw new AssertionFailure( "undefined join type " + astJoinType );
		}
	}

	void processJoins(QueryNode query) {
		final FromClause fromClause = query.getFromClause();

		// TODO : found it easiest to simply reorder the FromElements here into ascending order
		// in terms of injecting them into the resulting sql ast in orders relative to those
		// expected by the old parser; this is definitely another of those "only needed
		// for regression purposes".  The SyntheticAndFactory, then, simply injects them as it
		// encounters them.
		ArrayList orderedFromElements = new ArrayList();
		ListIterator liter = fromClause.getFromElements().listIterator( fromClause.getFromElements().size() );
		while ( liter.hasPrevious() ) {
			orderedFromElements.add( liter.previous() );
		}

		// Iterate through the alias,JoinSequence pairs and generate SQL token nodes.
		Iterator iter = orderedFromElements.iterator();
		while ( iter.hasNext() ) {
			final FromElement fromElement = ( FromElement ) iter.next();
			JoinSequence join = fromElement.getJoinSequence();
			join.setSelector( new JoinSequence.Selector() {
				public boolean includeSubclasses(String alias) {
					boolean shallowQuery = queryTranslatorImpl.isShallowQuery();
					boolean containsTableAlias = fromClause.containsTableAlias( alias );
					boolean includeSubclasses = fromElement.isIncludeSubclasses();
					boolean subQuery = fromClause.isSubQuery();
					boolean include = includeSubclasses && containsTableAlias && !subQuery && !shallowQuery;
					return include;
				}
			} );
			addJoinNodes( query, join, fromElement );
		} // while

	}

	private void addJoinNodes(QueryNode query, JoinSequence join, FromElement fromElement) {
		// Generate FROM and WHERE fragments for the from element.
		JoinFragment joinFragment = join.toJoinFragment( 
				query.isDML() ? 
					Collections.EMPTY_MAP : 
					queryTranslatorImpl.getEnabledFilters(),
				fromElement.useFromFragment() );

		String frag = joinFragment.toFromFragmentString();
		String whereFrag = joinFragment.toWhereFragmentString();

		// If the from element represents a JOIN_FRAGMENT and it is
		// a theta-style join, convert its type from JOIN_FRAGMENT
		// to FROM_FRAGMENT
		if ( fromElement.getType() == JOIN_FRAGMENT &&
				( join.isThetaStyle() || StringHelper.isNotEmpty( whereFrag ) ) ) {
			fromElement.setType( FROM_FRAGMENT );
		}

		// If there is a FROM fragment and the FROM element is an explicit, then add the from part.
		if ( fromElement.useFromFragment() /*&& StringHelper.isNotEmpty( frag )*/ ) {
			String fromFragment = processFromFragment( frag, join );
			if ( log.isDebugEnabled() ) log.debug( "Using FROM fragment [" + fromFragment + "]" );
			fromElement.setText( fromFragment.trim() ); // Set the text of the fromElement.
		}
		andFactory.addWhereFragment( joinFragment, whereFrag, query, fromElement );
	}

	private String processFromFragment(String frag, JoinSequence join) {
		// *** BEGIN FROM FRAGMENT VOODOO ***
		String fromFragment = frag.trim();
		// The FROM fragment will probably begin with ', '.  Remove this if it is present.
		if ( fromFragment.startsWith( ", " ) ) {
			fromFragment = fromFragment.substring( 2 );
		}
		// If there is more than one join, reverse the order of the tables in the FROM fragment.
		if ( join.getJoinCount() > 1 && fromFragment.indexOf( ',' ) >= 0 ) {
			String[] froms = StringHelper.split( ",", fromFragment );
			StringBuffer buf = new StringBuffer();
			for ( int i = froms.length - 1; i >= 0; i-- ) {
				buf.append( froms[i] );
				if ( i > 0 ) {
					buf.append( ", " );
				}
			}
			fromFragment = buf.toString();
		}
		// *** END OF FROM FRAGMENT VOODOO ***
		return fromFragment;
	}

}
