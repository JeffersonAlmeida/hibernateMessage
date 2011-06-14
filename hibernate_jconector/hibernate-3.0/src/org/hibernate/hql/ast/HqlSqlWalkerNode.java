// $Id: HqlSqlWalkerNode.java,v 1.7 2005/02/12 07:19:20 steveebersole Exp $
package org.hibernate.hql.ast;

import antlr.ASTFactory;

/**
 * A semantic analysis node, that points back to the main analyzer.
 *
 * @author josh Sep 24, 2004 4:08:13 PM
 */
class HqlSqlWalkerNode extends SqlNode implements InitializeableNode {
	/**
	 * A pointer back to the phase 2 processor.
	 */
	private HqlSqlWalker walker;

	public void initialize(Object param) {
		walker = ( HqlSqlWalker ) param;
	}

	public HqlSqlWalker getWalker() {
		return walker;
	}

	public SessionFactoryHelper getSessionFactoryHelper() {
		return walker.getSessionFactoryHelper();
	}

	public ASTFactory getASTFactory() {
		return walker.getASTFactory();
	}

	public AliasGenerator getAliasGenerator() {
		return walker.getAliasGenerator();
	}
}
