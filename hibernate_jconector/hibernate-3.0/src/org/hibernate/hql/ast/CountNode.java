// $Id: CountNode.java,v 1.10 2005/01/03 14:43:07 pgmjsd Exp $
package org.hibernate.hql.ast;

import antlr.SemanticException;
import org.hibernate.type.Type;

/**
 * Represents a COUNT expression in a select.
 *
 * @author josh Sep 21, 2004 9:23:40 PM
 */
class CountNode extends AbstractSelectExpression implements SelectExpression {
	public Type getDataType() {
		return getSessionFactoryHelper().findFunctionReturnType( getText(), null );
	}

	public void setScalarColumnText(int i) throws SemanticException {
		ColumnHelper.generateSingleScalarColumn( this, i );
	}

}
