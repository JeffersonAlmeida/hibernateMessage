// $Id: IdentNode.java,v 1.23 2005/03/23 18:18:47 steveebersole Exp $
package org.hibernate.hql.ast;

import antlr.SemanticException;
import antlr.collections.AST;
import org.hibernate.hql.antlr.SqlTokenTypes;
import org.hibernate.type.Type;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.util.StringHelper;

import java.util.List;

/**
 * Represents an identifier all by itself, which may be either a function name or a class alias depending on the
 * context.
 *
 * @author josh Aug 16, 2004 7:20:55 AM
 */
class IdentNode extends FromReferenceNode implements SelectExpression {

	private boolean nakedPropertyRef = false;

	public void resolveIndex(AST parent) throws SemanticException {
		throw new UnsupportedOperationException();
	}

	public void resolve(boolean generateJoin, boolean implicitJoin, String classAlias, AST parent) {
		if ( !isResolved() ) {
			if ( getWalker().getCurrentFromClause().isFromElementAlias( getText() ) ) {
				if ( resolveAsAlias() ) {
					setResolved();
					// We represent a from-clause alias
				}
			}
			else if ( parent != null && parent.getType() == SqlTokenTypes.DOT ) {
				DotNode dot = ( DotNode ) parent;
				if ( parent.getFirstChild() == this ) {
					if ( resolveAsNakedComponentPropertyRefLHS( dot ) ) {
						// we are the LHS of the DOT representing a naked comp-prop-ref
						setResolved();
					}
				}
				else {
					if ( resolveAsNakedComponentPropertyRefRHS( dot ) ) {
						// we are the RHS of the DOT representing a naked comp-prop-ref
						setResolved();
					}
				}
			}
			else {
				if ( resolveAsNakedPropertyRef() ) {
					// we represent a naked (simple) prop-ref
					setResolved();
				}
			}

			// if we are still not resolved, we might represent a constant.
			//      needed to add this here because the allowance of
			//      naked-prop-refs in the grammar collides with the
			//      definition of literals/constants ("nondeterminism").
			//      TODO: cleanup the grammar so that "processConstants" is always just handled from here
			if ( !isResolved() ) {
				// Avoid recursion from LiteralProcessor...
				if ( !getWalker().getCurrentFromClause().isFromElementAlias( getText() ) ) {
					try {
						getWalker().processConstant( this );
					}
					catch( Throwable ignore ) {
						// just ignore it for now, it'll get resolved later...
					}
				}
			}
		}
	}

	private boolean resolveAsAlias() {
		// This is not actually a constant, but a reference to FROM element.
		FromElement element = getWalker().getCurrentFromClause().getFromElement( getText() );
		if ( element != null ) {
			setFromElement( element );
			setText( element.getIdentityColumn() );
			setType( SqlTokenTypes.ALIAS_REF );
			return true;
		}
		return false;
	}

	private boolean resolveAsNakedPropertyRef() {
		FromElement fromElement = locateSingleFromElement();
		if ( fromElement == null ) {
			return false;
		}
		Queryable persister = fromElement.getQueryable();
		if ( persister == null ) {
			return false;
		}

		String property = getText();
		Type propertyType = null;
		try {
			propertyType = fromElement.getPropertyType( getText(), property );
		}
		catch( Throwable t ) {
			// assume this ident's text does *not* refer to a property on the given persister
			return false;
		}

		setFromElement( fromElement );

		if ( persister != null ) {
			String[] columns = getWalker().isSelectStatement()
					? persister.toColumns( fromElement.getTableAlias(), property )
					: persister.toColumns( property );
			setText( StringHelper.join( ", ", columns ) );
			setType( SqlTokenTypes.SQL_TOKEN );
		}

		// these pieces are needed for usage in select clause
		super.setDataType( propertyType );
		nakedPropertyRef = true;

		return true;
	}

	private boolean resolveAsNakedComponentPropertyRefLHS(DotNode parent) {
		FromElement fromElement = locateSingleFromElement();
		if ( fromElement == null ) {
			return false;
		}

		String propertyPath = getText() + "." + getNextSibling().getText();
		Type propertyType = null;  // used to inject the dot node
		try {
			// check to see if our "propPath" actually
			// represents a property on the persister
			propertyType = fromElement.getPropertyType( getText(), propertyPath );
		}
		catch( Throwable t ) {
			// assume we do *not* refer to a property on the given persister
			return false;
		}

		setFromElement( fromElement );
		parent.setPropertyPath( propertyPath );
		parent.setDataType( propertyType );

		return true;
	}

	private boolean resolveAsNakedComponentPropertyRefRHS(DotNode parent) {
		FromElement fromElement = locateSingleFromElement();
		if ( fromElement == null ) {
			return false;
		}

		Type propertyType = null;
		String propertyPath = parent.getLhs().getText() + "." + getText();
		try {
			// check to see if our "propPath" actually
			// represents a property on the persister
			propertyType = fromElement.getPropertyType( getText(), propertyPath );
		}
		catch( Throwable t ) {
			// assume we do *not* refer to a property on the given persister
			return false;
		}

		setFromElement( fromElement );

		// this piece is needed for usage in select clause
		super.setDataType( propertyType );
		nakedPropertyRef = true;

		return true;
	}

	private FromElement locateSingleFromElement() {
		List fromElements = getWalker().getCurrentFromClause().getFromElements();
		if ( fromElements == null || fromElements.size() != 1 ) {
			// TODO : should this be an error?
			return null;
		}
		FromElement element = ( FromElement ) fromElements.get(0);
		if ( element.getClassAlias() != null ) {
			// naked property-refs cannot be used with an aliased from element
			return null;
		}
		return element;
	}

	public Type getDataType() {
		Type type = super.getDataType();
		// If there's no type in the superclass, use the from element's type.
		return ( type == null && getFromElement() != null ) ? getFromElement().getDataType() : type;
	}

	public void setScalarColumnText(int i) throws SemanticException {
		if ( nakedPropertyRef ) {
			// do *not* over-write the column text, as that has already been
			// "rendered" during resolve
		}
		else {
			setText( getFromElement().renderScalarIdentifierSelect( i ) );
		}
	}

	public boolean isReturnableEntity() throws SemanticException {
		// TODO: Question... Are *all* ident nodes really returnable entities?
//		return true;
		// TODO : Answer... No! :)
		return !nakedPropertyRef;
	}

	public String getDisplayText() {
		StringBuffer buf = new StringBuffer();

		if ( getType() == SqlTokenTypes.ALIAS_REF ) {
			buf.append( "{alias=" ).append( getOriginalText() );
			if ( getFromElement() == null ) {
				buf.append( ", no from element" );
			}
			else {
				buf.append( ", className=" ).append( getFromElement().getClassName() );
				buf.append( ", tableAlias=" ).append( getFromElement().getTableAlias() );
			}
			buf.append( "}" );
		}
		else {
			buf.append( "{originalText=" + getOriginalText() ).append( "}" );
		}
		return buf.toString();
	}

}
