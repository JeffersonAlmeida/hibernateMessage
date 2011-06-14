// $Id: SyntheticAndFactory.java,v 1.8 2005/03/07 12:31:22 pgmjsd Exp $
package org.hibernate.hql.ast;

import antlr.ASTFactory;
import antlr.collections.AST;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.hql.antlr.HqlSqlTokenTypes;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.sql.JoinFragment;
import org.hibernate.util.StringHelper;

/**
 * Creates synthetic and nodes based on the where fragment part of a JoinSequence.
 *
 * @author josh Dec 5, 2004 12:25:20 PM
 */
public class SyntheticAndFactory implements HqlSqlTokenTypes {
	private static final Log log = LogFactory.getLog( SyntheticAndFactory.class );

	private ASTFactory astFactory;
	private AST thetaJoins;
	private AST filters;

	public SyntheticAndFactory(ASTFactory astFactory) {
		this.astFactory = astFactory;
	}

	void addWhereFragment(JoinFragment joinFragment, String whereFragment, QueryNode query, FromElement fromElement) {

		if ( whereFragment != null ) {
			whereFragment = whereFragment.trim();
		}
		if ( StringHelper.isEmpty( whereFragment ) ) {
			return;
		}
		else if ( !fromElement.useWhereFragment() && !joinFragment.hasThetaJoins() ) {
			return;
		}

		// Forcefully remove leading ands from where fragments; the grammar will
		// handle adding them
		if ( whereFragment.startsWith( "and" ) ) {
			whereFragment = whereFragment.substring( 4 );
		}

		if ( log.isDebugEnabled() ) log.debug( "Using WHERE fragment [" + whereFragment + "]" );

		SqlFragment fragment = ( SqlFragment ) ASTUtil.create( astFactory, SQL_TOKEN, whereFragment );
		fragment.setJoinFragment( joinFragment );
		fragment.setFromElement( fromElement );

		// Filter conditions need to be inserted before the HQL where condition and the
		// theta join node.  This is because org.hibernate.loader.Loader binds the filter parameters first,
		// then it binds all the HQL query parameters, see org.hibernate.loader.Loader.processFilterParameters().
		if ( fragment.getFromElement().isFilter() || fragment.hasFilterCondition() ) {
			if ( filters == null ) {
				AST where = query.getWhereClause();		// Find or create the WHERE clause.
				filters = astFactory.create( FILTERS, "{filter conditions}" );
				ASTUtil.insertChild( where, filters );	// Put the FILTERS node before the HQL condition.
			}
			filters.addChild( fragment );
		}
		else {
			if ( thetaJoins == null ) {
				AST where = query.getWhereClause();		// Find or create the WHERE clause.
				thetaJoins = astFactory.create( THETA_JOINS, "{theta joins}" );
				if ( where.getFirstChild() == null ) {
					where.setFirstChild( thetaJoins );
				}
				else {
					where.addChild( thetaJoins );
				}
			}
			ASTUtil.insertChild( thetaJoins, fragment );
		}

	}

	public void addDiscriminatorWhereFragment(QueryNode query, SingleTableEntityPersister persister, String alias) {
		String whereFragment = persister.filterFragment( alias ).trim();
		if ( "".equals( whereFragment ) ) {
			return;
		}
		if ( whereFragment.startsWith( "and" ) ) {
			whereFragment = whereFragment.substring( 4 );
		}
		AST subtree = parseEqFragmentToSubtree( whereFragment );
		if ( query.getWhereClause().getFirstChild() == null ) {
			query.getWhereClause().setFirstChild( subtree );
		}
		else {
			AST and = astFactory.create( AND, "{and}" );
			AST currentFirstChild = query.getWhereClause().getFirstChild();
			and.setFirstChild( subtree );
			and.addChild( currentFirstChild );
			query.getWhereClause().setFirstChild( and );
		}
	}

	private AST parseEqFragmentToSubtree(String whereFragment) {
		int chunkPos = whereFragment.lastIndexOf( "=" );
		String check = whereFragment.substring( 0, chunkPos ).trim();
		String test = whereFragment.substring( chunkPos + 1 ).trim();
		AST eq = astFactory.create( EQ, "{discriminator}" );
		eq.setFirstChild( astFactory.create( IDENT, check ) );
		eq.addChild( astFactory.create( SQL_TOKEN, test ) );
		return eq;
	}
}
