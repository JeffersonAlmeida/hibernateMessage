// $Id: PathNode.java,v 1.1 2004/11/07 16:06:35 pgmjsd Exp $
package org.hibernate.hql.ast;

/**
 * An AST node with a path property.  This path property will be the fully qualified name.
 *
 * @author josh Nov 7, 2004 10:56:49 AM
 */
public interface PathNode {
	/**
	 * Returns the full path name represented by the node.
	 *
	 * @return the full path name represented by the node.
	 */
	String getPath();
}
