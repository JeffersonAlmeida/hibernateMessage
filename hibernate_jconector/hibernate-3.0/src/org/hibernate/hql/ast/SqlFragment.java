// $Id: SqlFragment.java,v 1.3 2004/12/19 02:11:19 pgmjsd Exp $
package org.hibernate.hql.ast;

import org.hibernate.sql.JoinFragment;

/**
 * Represents an SQL fragment in the AST.
 *
 * @author josh Dec 5, 2004 9:01:52 AM
 */
class SqlFragment extends antlr.CommonAST {
	private JoinFragment joinFragment;
	private FromElement fromElement;

	public void setJoinFragment(JoinFragment joinFragment) {
		this.joinFragment = joinFragment;
	}

	public boolean hasFilterCondition() {
		return joinFragment.hasFilterCondition();
	}

	public void setFromElement(FromElement fromElement) {
		this.fromElement = fromElement;
	}

	public FromElement getFromElement() {
		return fromElement;
	}
}
