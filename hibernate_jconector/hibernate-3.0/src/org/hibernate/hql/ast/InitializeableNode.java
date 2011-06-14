// $Id: InitializeableNode.java,v 1.1 2004/06/03 16:30:08 steveebersole Exp $

package org.hibernate.hql.ast;

/**
 * An interface for initializeable AST nodes.
 */
interface InitializeableNode {
	/**
	 * Initializes the node with the parameter.
	 *
	 * @param param the initialization parameter.
	 */
	void initialize(Object param);
}
