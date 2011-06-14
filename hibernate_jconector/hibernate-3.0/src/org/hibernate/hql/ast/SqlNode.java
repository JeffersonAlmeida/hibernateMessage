// $Id: SqlNode.java,v 1.10 2005/02/12 07:19:21 steveebersole Exp $
package org.hibernate.hql.ast;

import org.hibernate.type.Type;

/**
 * A base AST node for the intermediate tree.
 * User: josh
 * Date: Dec 6, 2003
 * Time: 10:29:14 AM
 */
class SqlNode extends antlr.CommonAST {
	/**
	 * The original text for the node, mostly for debugging.
	 */
	private String originalText;
	/**
	 * The data type of this node.  Null for 'no type'.
	 */
	private Type dataType;

	public void setText(String s) {
		super.setText( s );
		if ( s != null && s.length() > 0 && originalText == null ) {
			originalText = s;
		}
	}

	String getOriginalText() {
		return originalText;
	}

	public Type getDataType() {
		return dataType;
	}

	void setDataType(Type dataType) {
		this.dataType = dataType;
	}

}
