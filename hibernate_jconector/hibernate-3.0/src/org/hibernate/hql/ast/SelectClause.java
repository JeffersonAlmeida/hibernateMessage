// $Id: SelectClause.java,v 1.39 2005/03/23 18:18:47 steveebersole Exp $
package org.hibernate.hql.ast;

import antlr.SemanticException;
import antlr.collections.AST;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.hql.antlr.SqlTokenTypes;
import org.hibernate.type.Type;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents the list of expressions in a SELECT clause.
 *
 * @author josh Sep 21, 2004 7:53:55 AM
 */
public class SelectClause extends SelectExpressionList {
	private static final Log log = LogFactory.getLog( SelectClause.class );

	private boolean prepared = false;
	private boolean scalarSelect;

	private List fromElementsForLoad = new ArrayList();
	//private Type[] sqlResultTypes;
	private Type[] queryReturnTypes;
	private String[][] columnNames;
	private ConstructorNode constructorNode;
	private FromElement collectionFromElement;

	/**
	 * Does this SelectClause represent a scalar query
	 *
	 * @return True if this is a scalara select clause; false otherwise.
	 */
	public boolean isScalarSelect() {
		return scalarSelect;
	}

	/**
	 * FromElements which need to be accounted for in the load phase (either for return or for fetch).
	 *
	 * @return List of appropriate FromElements.
	 */
	public List getFromElementsForLoad() {
		return fromElementsForLoad;
	}

	/*
	 * The types represented in the SQL result set.
	 *
	 * @return The types represented in the SQL result set.
	 */
	/*public Type[] getSqlResultTypes() {
		return sqlResultTypes;
	}*/

	/**
	 * The types actually being returned from this query at the "object level".
	 *
	 * @return The query return types.
	 */
	public Type[] getQueryReturnTypes() {
		return queryReturnTypes;
	}

	/**
	 * The column alias names being used in the generated SQL.
	 *
	 * @return The SQL column aliases.
	 */
	public String[][] getColumnNames() {
		return columnNames;
	}

	/**
	 * The constructor to use for dynamic instantiation queries.
	 *
	 * @return The appropriate Constructor reference, or null if not a
	 *         dynamic instantiation query.
	 */
	public Constructor getConstructor() {
		return constructorNode == null ? null : constructorNode.getConstructor();
	}

	/**
	 * Prepares an explicitly defined select clause.
	 *
	 * @param fromClause The from clause linked to this select clause.
	 * @throws SemanticException
	 */
	void initializeExplicitSelectClause(FromClause fromClause) throws SemanticException {
		if ( prepared ) {
			throw new IllegalStateException( "SelectClause was already prepared!" );
		}

		//explicit = true;	// This is an explict Select.
		//ArrayList sqlResultTypeList = new ArrayList();
		ArrayList queryReturnTypeList = new ArrayList();

		// First, collect all of the select expressions.
		// NOTE: This must be done *before* invoking setScalarColumnText() because setScalarColumnText()
		// changes the AST!!!
		SelectExpression[] selectExpressions = collectSelectExpressions();

		for ( int i = 0; i < selectExpressions.length; i++ ) {
			SelectExpression expr = selectExpressions[i];

			if ( expr.isConstructor() ) {
				constructorNode = ( ConstructorNode ) expr;
				List constructorArgumentTypeList = constructorNode.getConstructorArgumentTypeList();
				//sqlResultTypeList.addAll( constructorArgumentTypeList );
				queryReturnTypeList.addAll( constructorArgumentTypeList );
				scalarSelect = true;
			}
			else {
				Type type = expr.getDataType();
				if ( type == null ) {
					throw new IllegalStateException( "No data type for node: " + expr.getClass().getName() + " "
							+ new ASTPrinter( SqlTokenTypes.class ).showAsString( ( AST ) expr, "" ) );
				}
				//sqlResultTypeList.add( type );

				// If the data type is not an association type, it could not have been in the FROM clause.
				if ( expr.isScalar() ) {
					scalarSelect = true;
				}

				if ( isReturnableEntity( expr ) ) {
					fromElementsForLoad.add( expr.getFromElement() );
				}

				// Always add the type to the return type list.
				queryReturnTypeList.add( type );
			}
		}

		if ( !getWalker().isShallowQuery() ) {
			// add the fetched entities
			List fromElements = fromClause.getProjectionList();
	
			ASTAppender appender = new ASTAppender( getASTFactory(), this );	// Get ready to start adding nodes.
			int size = fromElements.size();
	
			Iterator iterator = fromElements.iterator();
			for ( int k = 0; iterator.hasNext(); k++ ) {
				FromElement fromElement = ( FromElement ) iterator.next();
	
				if ( fromElement.isFetch() ) {
					Type type = fromElement.getSelectType();
					setCollectionFromElement( fromElement );
					if ( type != null ) {
						boolean collectionOfElements = fromElement.isCollectionOfValuesOrComponents();
						if ( !collectionOfElements ) {
							// Add the type to the list of returned sqlResultTypes.
							fromElement.setIncludeSubclasses( true );
							fromElementsForLoad.add( fromElement );
							//sqlResultTypeList.add( type );
							// Generate the select expression.
							String text = fromElement.renderIdentifierSelect( size, k );
							SelectExpressionImpl generatedExpr = ( SelectExpressionImpl ) appender.append( SqlTokenTypes.SELECT_EXPR, text, false );
							if ( generatedExpr != null ) {
								generatedExpr.setFromElement( fromElement );
							}
						}
					}
				}
			}
	
			// generate id select fragment and then property select fragment for
			// each expression, just like generateSelectFragments().
			renderNonScalarSelects( collectSelectExpressions(), fromClause );
		}

		if ( scalarSelect || getWalker().isShallowQuery() ) {
			// If there are any scalars (non-entities) selected, render the select column aliases.
			renderScalarSelects( selectExpressions, fromClause );
		}

		finishInitialization( /*sqlResultTypeList,*/ queryReturnTypeList );
	}

	private void finishInitialization(/*ArrayList sqlResultTypeList,*/ ArrayList queryReturnTypeList) {
		//sqlResultTypes = ( Type[] ) sqlResultTypeList.toArray( new Type[sqlResultTypeList.size()] );
		queryReturnTypes = ( Type[] ) queryReturnTypeList.toArray( new Type[queryReturnTypeList.size()] );
		initializeColumnNames();
		prepared = true;
	}

	private void initializeColumnNames() {
		// Generate an 2d array of column names, the first dimension is parallel with the
		// return types array.  The second dimension is the list of column names for each
		// type.

		// todo: we should really just collect these from the various SelectExpressions, rather than regenerating here
		columnNames = getSessionFactoryHelper().generateColumnNames( queryReturnTypes );
	}

	/**
	 * Prepares a derived (i.e., not explicitly defined in the query) select clause.
	 *
	 * @param fromClause The from clause to which this select clause is linked.
	 */
	void initializeDerivedSelectClause(FromClause fromClause) throws SemanticException {
		if ( prepared ) {
			throw new IllegalStateException( "SelectClause was already prepared!" );
		}
		List fromElements = fromClause.getProjectionList();

		ASTAppender appender = new ASTAppender( getASTFactory(), this );	// Get ready to start adding nodes.
		int size = fromElements.size();
		ArrayList sqlResultTypeList = new ArrayList( size );
		ArrayList queryReturnTypeList = new ArrayList( size );

		Iterator iterator = fromElements.iterator();
		for ( int k = 0; iterator.hasNext(); k++ ) {
			FromElement fromElement = ( FromElement ) iterator.next();
			Type type = fromElement.getSelectType();

			setCollectionFromElement( fromElement );

			if ( type != null ) {
				boolean collectionOfElements = fromElement.isCollectionOfValuesOrComponents();
				if ( !collectionOfElements ) {
					if ( !fromElement.isFetch() ) {
						// Add the type to the list of returned sqlResultTypes.
						queryReturnTypeList.add( type );
					}
					fromElementsForLoad.add( fromElement );
					sqlResultTypeList.add( type );
					// Generate the select expression.
					String text = fromElement.renderIdentifierSelect( size, k );
					SelectExpressionImpl generatedExpr = ( SelectExpressionImpl ) appender.append( SqlTokenTypes.SELECT_EXPR, text, false );
					if ( generatedExpr != null ) {
						generatedExpr.setFromElement( fromElement );
					}
				}
			}
		}

		// Get all the select expressions (that we just generated) and render the select.
		SelectExpression[] selectExpressions = collectSelectExpressions();

		if ( getWalker().isShallowQuery() ) {
			renderScalarSelects( selectExpressions, fromClause );
		}
		else {
			renderNonScalarSelects( selectExpressions, fromClause );
		}
		finishInitialization( /*sqlResultTypeList,*/ queryReturnTypeList );
	}

	private void setCollectionFromElement(FromElement fromElement) {
		if ( fromElement.isFetch() ) {
			if ( fromElement.isCollectionJoin() || fromElement.getQueryableCollection() != null ) {
				if ( collectionFromElement == null ) {
					collectionFromElement = fromElement;
				}
				else {
					log.warn( "Collection from element was already set.  Trying to fetch more than one collection?" );
				}
			}
		}
	}

	protected AST getFirstSelectExpression() {
		AST n = getFirstChild();
		// Skip 'DISTINCT' and 'ALL', so we return the first expression node.
		while ( n != null && ( n.getType() == SqlTokenTypes.DISTINCT || n.getType() == SqlTokenTypes.ALL ) ) {
			n = n.getNextSibling();
		}
		return n;
	}

	private boolean isReturnableEntity(SelectExpression selectExpression) throws SemanticException {
		FromElement fromElement = selectExpression.getFromElement();
		boolean isFetchOrValueCollection = fromElement != null && 
				( fromElement.isFetch() || fromElement.isCollectionOfValuesOrComponents() ); 
		if ( isFetchOrValueCollection ) {
			return false;
		}
		else {
			return selectExpression.isReturnableEntity();
		}
	}

	private void renderScalarSelects(SelectExpression[] se, FromClause currentFromClause) throws SemanticException {
		if ( !currentFromClause.isSubQuery() ) {
			for ( int i = 0; i < se.length; i++ ) {
				SelectExpression expr = se[i];
				expr.setScalarColumnText( i );	// Create SQL_TOKEN nodes for the columns.
			}
		}
	}

	private void renderNonScalarSelects(SelectExpression[] selectExpressions, FromClause currentFromClause) throws SemanticException {
		ASTAppender appender = new ASTAppender( getASTFactory(), this );
		final int size = selectExpressions.length;
		int nonscalarSize = 0;
		for ( int i = 0; i < size; i++ ) {
			if ( !selectExpressions[i].isScalar() ) nonscalarSize++;
		}

		int j = 0;
		for ( int i = 0; i < size; i++ ) {
			if ( !selectExpressions[i].isScalar() ) {
				SelectExpression expr = selectExpressions[i];
				FromElement fromElement = expr.getFromElement();
				if ( fromElement != null ) {
					renderNonScalarIdentifiers( fromElement, nonscalarSize, j, expr, appender );
					j++;
				}
			}
		}

		if ( !currentFromClause.isSubQuery() ) {
			// Generate the property select tokens.
			int k = 0;
			for ( int i = 0; i < size; i++ ) {
				if ( !selectExpressions[i].isScalar() ) {
					FromElement fromElement = selectExpressions[i].getFromElement();
					if ( fromElement != null ) {
						renderNonScalarProperties( appender, fromElement, nonscalarSize, k );
						k++;
					}
				}
			}
		}
	}

	private void renderNonScalarIdentifiers(FromElement fromElement, int nonscalarSize, int j, SelectExpression expr, ASTAppender appender) {
		String text = fromElement.renderIdentifierSelect( nonscalarSize, j );
		if ( !fromElement.getFromClause().isSubQuery() ) {
			if ( !scalarSelect && !getWalker().isShallowQuery() ) {
				//TODO: is this a bit ugly?
				expr.setText( text );
			}
			else {
				appender.append( SqlTokenTypes.SQL_TOKEN, text, false );
			}
		}
	}

	private void renderNonScalarProperties(ASTAppender appender, FromElement fromElement, int nonscalarSize, int k) {
		appender.append( SqlTokenTypes.SQL_TOKEN, fromElement.renderPropertySelect( nonscalarSize, k ), false );
		String text = fromElement.renderCollectionSelectFragment();
		appender.append( SqlTokenTypes.SQL_TOKEN, text, false );
		// Look through the FromElement's children to find any collections of values that should be fetched...
		ASTIterator iter = new ASTIterator( fromElement );
		while ( iter.hasNext() ) {
			FromElement child = ( FromElement ) iter.next();
			if ( child.isCollectionOfValuesOrComponents() && child.isFetch() ) {
				text = child.renderCollectionSelectFragment();
				appender.append( SqlTokenTypes.SQL_TOKEN, text, false );
			}
		}
	}

	public FromElement getCollectionFromElement() {
		return collectionFromElement;
	}
}
