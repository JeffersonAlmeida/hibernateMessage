// $Id: ASTIterator.java,v 1.5 2005/03/27 00:22:10 pgmjsd Exp $
package org.hibernate.hql.ast;

import antlr.collections.AST;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Depth first iteration of an ANTLR AST.
 *
 * @author josh Sep 25, 2004 7:44:39 AM
 */
public class ASTIterator implements Iterator {
	private AST next, current;
	private LinkedList parents = new LinkedList();

	public void remove() {
		throw new UnsupportedOperationException( "remove() is not supported" );
	}

	public boolean hasNext() {
		return next != null;
	}

	public Object next() {
		return nextNode();
	}

	public ASTIterator(AST tree) {
		next = tree;
		down();
	}

	public AST nextNode() {
		current = next;
		if ( next != null ) {
			AST nextSibling = next.getNextSibling();
			if ( nextSibling == null ) {
				next = pop();
			}
			else {
				next = nextSibling;
				down();
			}
		}
		return current;
	}

	private void down() {
		while ( next != null && next.getFirstChild() != null ) {
			push( next );
			next = next.getFirstChild();
		}
	}

	private void push(AST parent) {
		parents.addFirst( parent );
	}

	private AST pop() {
		if ( parents.size() == 0 ) {
			return null;
		}
		else {
			return ( AST ) parents.removeFirst();
		}
	}

}
