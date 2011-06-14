// $Id: OrderByClause.java,v 1.1 2005/02/27 04:23:50 steveebersole Exp $
package org.hibernate.hql.ast;

import org.hibernate.hql.antlr.HqlSqlTokenTypes;
import antlr.collections.AST;

/**
 * Implementation of OrderByClause.
 *
 * @author Steve Ebersole
 */
public class OrderByClause extends HqlSqlWalkerNode implements HqlSqlTokenTypes {

	public void addOrderFragment(String orderByFragment) {
		AST fragment = ASTUtil.create( getASTFactory(), SQL_TOKEN, orderByFragment );
		if ( getFirstChild() == null ) {
            setFirstChild( fragment );
		}
		else {
			addChild( fragment );
		}
	}

}
