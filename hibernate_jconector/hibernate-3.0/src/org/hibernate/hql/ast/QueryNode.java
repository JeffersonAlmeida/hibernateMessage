// $Id: QueryNode.java,v 1.17 2005/02/28 23:00:36 steveebersole Exp $
package org.hibernate.hql.ast;

import antlr.collections.AST;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.hql.antlr.SqlTokenTypes;

import java.util.Iterator;

/**
 * Defines a top-level AST node representing the notion of a query.
 *
 * @author Joshua Davis
 */
public class QueryNode extends SqlNode implements InitializeableNode, DisplayableNode {

	private static final Log log = LogFactory.getLog( QueryNode.class );

	private HqlSqlWalker walker;
	private AST where = null;
	private FromClause fromClause = null;
	private OrderByClause orderByClause;

	public void initialize(Object param) {
		walker = ( HqlSqlWalker ) param;
	}

	final HqlSqlWalker getWalker() {
		return walker;
	}

	public boolean isDML() {
		// TODO : really like to see this be based on something other than the AST node types, like maybe subclassing via the SqlASTFactory
		return getType() == SqlTokenTypes.UPDATE || getType() == SqlTokenTypes.DELETE;
	}

	public final AST getWhereClause() {
		if ( where == null ) {
			where = ASTUtil.findTypeInChildren( this, SqlTokenTypes.WHERE );
			// If there is no WHERE node, make one.
			if ( where == null ) {
				if ( log.isDebugEnabled() ) {
					log.debug( "getWhereClause() : Creating a new WHERE clause..." );
				}
				where = ASTUtil.create( walker.getASTFactory(), SqlTokenTypes.WHERE, "WHERE" );
				if ( getType() == SqlTokenTypes.UPDATE ) {
					// inject the WHERE after the SET
					AST set = ASTUtil.findTypeInChildren( this, SqlTokenTypes.SET );
					where.setNextSibling( set.getNextSibling() );
					set.setNextSibling( where );
				}
				else {
					// inject tje WHERE after the FROM
					AST from = ASTUtil.findTypeInChildren( this, SqlTokenTypes.FROM );
					where.setNextSibling( from.getNextSibling() );
					from.setNextSibling( where );
				}
			}
		}
		return where;
	}

	public final FromClause getFromClause() {
		if ( fromClause == null ) {
			fromClause = ( FromClause ) ASTUtil.findTypeInChildren( this, SqlTokenTypes.FROM );
		}
		return fromClause;
	}

	public final OrderByClause getOrderByClause() {
		if ( orderByClause == null ) {
			orderByClause = ( OrderByClause ) ASTUtil.findTypeInChildren( this, SqlTokenTypes.ORDER );

			// if there is no order by, making one
			if ( orderByClause == null ) {
				log.debug( "getOrderByClause() : Creating a new ORDER BY clause" );
			}
			orderByClause = ( OrderByClause ) ASTUtil.create( walker.getASTFactory(), SqlTokenTypes.ORDER, "ORDER" );

			// Find the WHERE; if there is no WHERE, find the FROM...
			AST prevSibling = ASTUtil.findTypeInChildren( this, SqlTokenTypes.WHERE );
			if ( prevSibling == null ) {
				prevSibling = ASTUtil.findTypeInChildren( this, SqlTokenTypes.FROM );
			}

			// Now, inject the newly built ORDER BY into the tree
			orderByClause.setNextSibling( prevSibling.getNextSibling() );
			prevSibling.setNextSibling( orderByClause );
		}
		return orderByClause;
	}

	/**
	 * Returns additional display text for the AST node.
	 *
	 * @return String - The additional display text.
	 */
	public String getDisplayText() {
		StringBuffer buf = new StringBuffer();
		if ( getWalker().getQuerySpaces().size() > 0 ) {
			buf.append( " querySpaces (" );
			for ( Iterator iterator = getWalker().getQuerySpaces().iterator(); iterator.hasNext(); ) {
				buf.append( iterator.next() );
				if ( iterator.hasNext() ) {
					buf.append( "," );
				}
			}
			buf.append( ")" );
		}
		return buf.toString();
	}
}
