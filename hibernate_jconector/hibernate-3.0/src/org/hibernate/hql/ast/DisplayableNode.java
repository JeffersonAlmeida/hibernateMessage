// $Id: DisplayableNode.java,v 1.1 2004/06/03 16:30:08 steveebersole Exp $
package org.hibernate.hql.ast;

/**
 * Implementors will return additional display text, which will be used
 * by the ASTPrinter to display information (besides the node type and node
 * text).
 */
public interface DisplayableNode {
	/**
	 * Returns additional display text for the AST node.
	 *
	 * @return String - The additional display text.
	 */
	String getDisplayText();
}
