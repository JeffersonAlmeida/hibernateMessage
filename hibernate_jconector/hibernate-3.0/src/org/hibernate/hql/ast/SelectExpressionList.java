// $Id: SelectExpressionList.java,v 1.6 2005/01/03 14:43:07 pgmjsd Exp $
package org.hibernate.hql.ast;

import antlr.collections.AST;
import org.hibernate.hql.antlr.SqlTokenTypes;

import java.util.ArrayList;

/**
 * Common behavior - a node that contains a list of select expressions.
 *
 * @author josh Nov 6, 2004 8:51:00 AM
 */
abstract class SelectExpressionList extends HqlSqlWalkerNode {
	/**
	 * Returns an array of SelectExpressions gathered from the children of the given parent AST node.
	 *
	 * @return an array of SelectExpressions gathered from the children of the given parent AST node.
	 */
	SelectExpression[] collectSelectExpressions() {
		// Get the first child to be considered.  Sub-classes may do this differently in order to skip nodes that
		// are not select expressions (e.g. DISTINCT).
		AST firstChild = getFirstSelectExpression();
		AST parent = this;
		ArrayList list = new ArrayList( parent.getNumberOfChildren() );
		for ( AST n = firstChild; n != null; n = n.getNextSibling() ) {
			if ( n instanceof SelectExpression ) {
				list.add( n );
			}
			else {
				throw new IllegalStateException( "Unexpected AST: " + n.getClass().getName() + " " + new ASTPrinter( SqlTokenTypes.class ).showAsString( n, "" ) );
			}
		}
		return ( SelectExpression[] ) list.toArray( new SelectExpression[list.size()] );
	}

	/**
	 * Returns the first select expression node that should be considered when building the array of select
	 * expressions.
	 *
	 * @return the first select expression node that should be considered when building the array of select
	 *         expressions
	 */
	protected abstract AST getFirstSelectExpression();

}
