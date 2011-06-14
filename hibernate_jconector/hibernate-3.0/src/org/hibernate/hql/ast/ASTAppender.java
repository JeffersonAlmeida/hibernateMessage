// $Id: ASTAppender.java,v 1.11 2005/02/26 11:12:06 oneovthafew Exp $
package org.hibernate.hql.ast;

import antlr.ASTFactory;
import antlr.collections.AST;

/**
 * Appends child nodes to a parent efficiently.
 *
 * @author josh Jul 24, 2004 8:28:23 AM
 */
public class ASTAppender {
	private AST parent;
	private AST last;
	private ASTFactory factory;

	public ASTAppender(ASTFactory factory, AST parent) {
		this( parent );
		this.factory = factory;
	}

	public ASTAppender(AST parent) {
		this.parent = parent;
		this.last = ASTUtil.getLastChild( parent );
	}

	public AST append(int type, String text, boolean appendIfEmpty) {
		if ( text != null && ( appendIfEmpty || text.length() > 0 ) ) {
			return append( factory.create( type, text ) );
		}
		else {
			return null;
		}
	}

	public AST append(AST child) {
		if ( last == null ) {
			parent.setFirstChild( child );
		}
		else {
			last.setNextSibling( child );
		}
		last = child;
		return last;
	}
}
