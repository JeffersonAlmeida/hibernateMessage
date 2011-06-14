// $Id: AggregateNode.java,v 1.14 2005/02/12 07:19:19 steveebersole Exp $
package org.hibernate.hql.ast;

import antlr.SemanticException;
import org.hibernate.type.Type;

/**
 * Represents an aggregate function i.e. min, max, sum, avg.
 *
 * @author josh Sep 21, 2004 9:22:02 PM
 */
class AggregateNode extends AbstractSelectExpression implements SelectExpression {

	public AggregateNode() {
	}

	public Type getDataType() {
		// Get the function return value type, based on the type of the first argument.
		return getSessionFactoryHelper().findFunctionReturnType( getText(), getFirstChild() );
	}

	public void setScalarColumnText(int i) throws SemanticException {
		ColumnHelper.generateSingleScalarColumn( this, i );
	}
}
