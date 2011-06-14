// $Id: FromReferenceNode.java,v 1.38 2005/03/27 00:22:14 pgmjsd Exp $
package org.hibernate.hql.ast;

import antlr.SemanticException;
import antlr.collections.AST;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Represents a reference to a FROM element, for example a class alias in a WHERE clause.
 *
 * @author josh Jul 21, 2004 7:02:04 AM
 */
abstract class FromReferenceNode extends AbstractSelectExpression implements
		ResolvableNode, DisplayableNode, InitializeableNode, PathNode {
	private static final Log log = LogFactory.getLog( FromReferenceNode.class );
	private FromElement fromElement;
	private boolean resolved = false;
	public static final int ROOT_LEVEL = 0;

	public FromElement getFromElement() {
		return fromElement;
	}

	public void setFromElement(FromElement fromElement) {
		this.fromElement = fromElement;
	}

	/**
	 * Resolves the left hand side of the DOT.
	 *
	 * @throws SemanticException
	 */
	void resolveFirstChild() throws SemanticException {
	}

	public String getPath() {
		return getOriginalText();
	}

	boolean isResolved() {
		return resolved;
	}

	void setResolved() {
		this.resolved = true;
		if ( log.isDebugEnabled() ) {
			log.debug( "Resolved :  " + this.getPath() + " -> " + this.getText() );
		}
	}

	public String getDisplayText() {
		StringBuffer buf = new StringBuffer();
		buf.append( "{" ).append( ( fromElement == null ) ? "no fromElement" : fromElement.getDisplayText() );
		buf.append( "}" );
		return buf.toString();
	}

	public void recursiveResolve(int level, boolean impliedAtRoot, String classAlias) throws SemanticException {
		AST lhs = getFirstChild();
		int nextLevel = level + 1;
		if ( lhs != null ) {
			FromReferenceNode n = ( FromReferenceNode ) lhs;
			n.recursiveResolve( nextLevel, impliedAtRoot, null );
		}
		resolveFirstChild();
		boolean impliedJoin = true;
		if ( level == ROOT_LEVEL && !impliedAtRoot ) {
			impliedJoin = false;
		}
		resolve( true, impliedJoin, classAlias, this );
	}

	public boolean isReturnableEntity() throws SemanticException {
		return fromElement.isEntity() && getDataType().isAssociationType();
	}

	public void resolveInFunctionCall(boolean generateJoin, boolean implicitJoin) throws SemanticException {
		resolve( generateJoin, implicitJoin );
	}

	public void resolve(boolean generateJoin, boolean implicitJoin) throws SemanticException {
		resolve( generateJoin, implicitJoin, null );
	}

	public void resolve(boolean generateJoin, boolean implicitJoin, String classAlias) throws SemanticException {
		resolve( generateJoin, implicitJoin, classAlias, null );
	}

	void prepareForDot(String propertyName) throws SemanticException {
	}

	/**
	 * Sub-classes can override this method if they produce implied joins (e.g. DotNode).
	 *
	 * @return an implied join created by this from reference.
	 */
	FromElement getImpliedJoin() {
		return null;
	}

}
