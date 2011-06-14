// $Id: ConstructorNode.java,v 1.22 2005/02/12 20:27:49 pgmjsd Exp $
package org.hibernate.hql.ast;

import antlr.SemanticException;
import antlr.collections.AST;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.type.Type;
import org.hibernate.util.ReflectHelper;
import org.hibernate.util.StringHelper;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a constructor (new) in a SELECT.
 *
 * @author josh Sep 24, 2004 6:46:08 PM
 */
public class ConstructorNode extends SelectExpressionList implements SelectExpression {

	private Constructor constructor;
	private Type[] constructorArgumentTypes;

	public void setScalarColumnText(int i) throws SemanticException {
		SelectExpression[] selectExpressions = collectSelectExpressions();
		// Invoke setScalarColumnText on each constructor argument.
		for ( int j = 0; j < selectExpressions.length; j++ ) {
			SelectExpression selectExpression = selectExpressions[j];
			selectExpression.setScalarColumnText( j );
		}
	}

	protected AST getFirstSelectExpression() {
		// Collect the select expressions, skip the first child because it is the class name.
		return getFirstChild().getNextSibling();
	}

	/**
	 * @deprecated (tell clover to ignore this method)
	 */
	public Type getDataType() {
/*
		// Return the type of the object created by the constructor.
		AST firstChild = getFirstChild();
		String text = firstChild.getText();
		if ( firstChild.getType() == SqlTokenTypes.DOT ) {
			DotNode dot = ( DotNode ) firstChild;
			text = dot.getPath();
		}
		return getSessionFactoryHelper().requireEntityType( text );
*/
		throw new UnsupportedOperationException( "getDataType() is not supported by ConstructorNode!" );
	}

	public void prepare() throws SemanticException {
		constructorArgumentTypes = resolveConstructorArgumentTypes();
		constructor = resolveConstructor();
	}

	private Type[] resolveConstructorArgumentTypes() throws SemanticException {
		SelectExpression[] argumentExpressions = collectSelectExpressions();
		if ( argumentExpressions == null ) {
			// return an empty Type array
			return new Type[]{};
		}

		Type[] types = new Type[argumentExpressions.length];
		for ( int x = 0; x < argumentExpressions.length; x++ ) {
			types[x] = argumentExpressions[x].getDataType();
		}
		return types;
	}

	private Constructor resolveConstructor() throws SemanticException {
		String path = ( ( PathNode ) getFirstChild() ).getPath();
		String importedClassName = getSessionFactoryHelper().getImportedClassName( path );
		String className = StringHelper.isEmpty( importedClassName ) ? path : importedClassName;
		if ( className == null ) {
			throw new SemanticException( "Unable to locate class [" + path + "]" );
		}
		try {
			Class holderClass = ReflectHelper.classForName( className );
			return ReflectHelper.getConstructor( holderClass, constructorArgumentTypes );
		}
		catch ( ClassNotFoundException e ) {
			throw new DetailedSemanticException( "Unable to locate class [" + className + "]", e );
		}
		catch ( PropertyNotFoundException e ) {
			// this is the exception returned by ReflectHelper.getConstructor() if it cannot
			// locate an appropriate constructor
			throw new DetailedSemanticException( "Unable to locate appropriate constructor on class [" + className + "]", e );
		}
	}

	public Constructor getConstructor() {
		return constructor;
	}

	public List getConstructorArgumentTypeList() {
		return Arrays.asList( constructorArgumentTypes );
	}

	public FromElement getFromElement() {
		return null;
	}

	public boolean isConstructor() {
		return true;
	}

	public boolean isReturnableEntity() throws SemanticException {
		return false;
	}

	public boolean isScalar() {
		// Constructors are always considered scalar results.
		return true;
	}
}
