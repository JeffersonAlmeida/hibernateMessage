// $Id: SqlASTFactory.java,v 1.28 2005/02/27 04:23:50 steveebersole Exp $
package org.hibernate.hql.ast;

import antlr.ASTFactory;
import antlr.Token;
import antlr.collections.AST;
import org.hibernate.hql.antlr.HqlSqlTokenTypes;

import java.lang.reflect.Constructor;

/**
 * Custom AST factory the intermediate tree that causes ANTLR to create specialized
 * AST nodes, given the AST node type (from HqlSqlTokenTypes).   HqlSqlWalker registers
 * this factory with itself when it is initialized.
 * <br>User: josh
 * <br>Date: Nov 22, 2003
 * <br>Time: 3:34:28 PM
 */
public class SqlASTFactory extends ASTFactory implements HqlSqlTokenTypes {
	private HqlSqlWalker walker;

	/**
	 * Create factory with a specific mapping from token type
	 * to Java AST node type.  Your subclasses of ASTFactory
	 * can override and reuse the map stuff.
	 */
	public SqlASTFactory(HqlSqlWalker walker) {
		super();
		this.walker = walker;
	}

	/**
	 * Returns the class for a given token type (a.k.a. AST node type).
	 *
	 * @param tokenType The token type.
	 * @return Class - The AST node class to instantiate.
	 */
	public Class getASTNodeType(int tokenType) {
		switch ( tokenType ) {
			case SELECT:
			case QUERY:
				return QueryNode.class;
			case UPDATE:
			case DELETE:
				return QueryNode.class;
			case FROM:
				return FromClause.class;
			case FROM_FRAGMENT:
				return FromElement.class;
			case IMPLIED_FROM:
				return ImpliedFromElement.class;
			case DOT:
				return DotNode.class;
			case INDEX_OP:
				return IndexNode.class;
				// Alias references and identifiers use the same node class.
			case ALIAS_REF:
			case IDENT:
				return IdentNode.class;
			case SQL_TOKEN:
				return SqlFragment.class;
			case METHOD_CALL:
				return MethodNode.class;
			case ELEMENTS:
			case INDICES:
				return CollectionFunction.class;
			case SELECT_CLAUSE:
				return SelectClause.class;
			case SELECT_EXPR:
				return SelectExpressionImpl.class;
			case AGGREGATE:
				return AggregateNode.class;
			case COUNT:
				return CountNode.class;
			case CONSTRUCTOR:
				return ConstructorNode.class;
			case NUM_INT:
			case NUM_FLOAT:
			case NUM_LONG:
			case NUM_DOUBLE:
			case QUOTED_STRING:
			case TRUE:
			case FALSE:
				return LiteralNode.class;
			case ORDER:
				return OrderByClause.class;
			default:
				return SqlNode.class;
		} // switch
	}

	protected AST createUsingCtor(Token token, String className) {
		Class c;
		AST t;
		try {
			c = Class.forName( className );
			Class[] tokenArgType = new Class[]{antlr.Token.class};
			Constructor ctor = c.getConstructor( tokenArgType );
			if ( ctor != null ) {
				t = ( AST ) ctor.newInstance( new Object[]{token} ); // make a new one
				initializeSqlNode( t );
			}
			else {
				// just do the regular thing if you can't find the ctor
				// Your AST must have default ctor to use this.
				t = create( c );
			}
		}
		catch ( Exception e ) {
			throw new IllegalArgumentException( "Invalid class or can't make instance, " + className );
		}
		return t;
	}

	private void initializeSqlNode(AST t) {
		// Initialize SQL nodes here.
		if ( t instanceof InitializeableNode ) {
			InitializeableNode initializeableNode = ( InitializeableNode ) t;
			initializeableNode.initialize( walker );
		}
	}

	/**
	 * Actually instantiate the AST node.
	 *
	 * @param c The class to instantiate.
	 * @return The instantiated and initialized node.
	 */
	protected AST create(Class c) {
		AST t;
		try {
			t = ( AST ) c.newInstance(); // make a new one
			initializeSqlNode( t );
		}
		catch ( Exception e ) {
			error( "Can't create AST Node " + c.getName() );
			return null;
		}
		return t;
	}

}
