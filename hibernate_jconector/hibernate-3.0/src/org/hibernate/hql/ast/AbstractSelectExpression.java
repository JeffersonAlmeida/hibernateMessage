// $Id: AbstractSelectExpression.java,v 1.7 2005/02/12 07:19:19 steveebersole Exp $
package org.hibernate.hql.ast;

import antlr.SemanticException;
import org.hibernate.type.Type;

/**
 * Partial implementation of SelectExpression for all the nodes that aren't constructors.
 *
 * @author josh Nov 11, 2004 7:09:11 AM
 */
abstract class AbstractSelectExpression extends HqlSqlWalkerNode implements SelectExpression {

	public boolean isConstructor() {
		return false;
	}

	public boolean isReturnableEntity() throws SemanticException {
		return false;
	}

	public FromElement getFromElement() {
		return null;
	}

	public boolean isScalar() throws SemanticException {
		// Default implementation:
		// If this node has a data type, and that data type is not an association, then this is scalar.
		Type type = getDataType();
		return type != null && !type.isAssociationType();	// Moved here from SelectClause [jsd]
	}
}
