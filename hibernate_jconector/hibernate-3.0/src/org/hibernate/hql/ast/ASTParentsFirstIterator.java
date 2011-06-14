// $Id: ASTParentsFirstIterator.java,v 1.1 2005/02/25 12:24:07 pgmjsd Exp $
package org.hibernate.hql.ast;

import antlr.collections.AST;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Depth first iteration of an ANTLR AST.
 *
 * @author josh Sep 25, 2004 7:44:39 AM
 */
public class ASTParentsFirstIterator implements Iterator {
	private AST next, current, tree;
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

	public ASTParentsFirstIterator(AST tree) {
		this.tree = next = tree;
	}

	public AST nextNode() {
		current = next;
		if ( next != null ) {
			AST child = next.getFirstChild();
			if ( child == null ) {
				AST sibling = next.getNextSibling();
				if ( sibling == null ) {
					AST parent = pop();
					while ( parent != null && parent.getNextSibling() == null )
						parent = pop();
					next = ( parent != null ) ? parent.getNextSibling() : null;
				}
				else {
					next = sibling;
				}
			}
			else {
				if ( next != tree ) {
					push( next );
				}
				next = child;
			}
		}
		return current;
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
