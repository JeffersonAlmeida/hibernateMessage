// $Id: ASTIteratorTest.java,v 1.8 2005/02/26 22:25:09 pgmjsd Exp $
package org.hibernate.test.ast;

import antlr.ASTFactory;
import antlr.collections.AST;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.hibernate.hql.antlr.HqlTokenTypes;
import org.hibernate.hql.ast.ASTIterator;
import org.hibernate.hql.ast.ASTParentsFirstIterator;
import org.hibernate.hql.ast.ASTPrinter;
import org.hibernate.hql.ast.ASTUtil;
import org.hibernate.hql.ast.HqlParser;

import java.io.PrintWriter;

/**
 * Test ASTIterator.
 */
public class ASTIteratorTest extends TestCase {
	private ASTFactory factory;

	/**
	 * Standard JUnit test case constructor.
	 *
	 * @param name The name of the test case.
	 */
	public ASTIteratorTest(String name) {
		super( name );
	}

	public static Test suite() {
		return new TestSuite( ASTIteratorTest.class );
	}

	protected void setUp() throws Exception {
		super.setUp();
		factory = new ASTFactory();
	}

	/**
	 * Test a simple tree, make sure the iterator encounters every node.
	 */
	public void testSimpleTree() throws Exception {
		String input = "select foo from foo in class org.hibernate.test.Foo, fee in class org.hibernate.test.Fee where foo.dependent = fee order by foo.string desc, foo.component.count asc, fee.id";
		HqlParser parser = HqlParser.getInstance( input );
		parser.statement();
		AST ast = parser.getAST();
		ASTPrinter printer = new ASTPrinter( HqlTokenTypes.class );
		printer.showAst( ast, new PrintWriter( System.out ) );
		ASTIterator iterator = new ASTIterator( ast );
		int count = 0;
		while ( iterator.hasNext() ) {
			assertTrue( iterator.next() instanceof AST );
			count++;
		}
		assertEquals( 43, count );

		UnsupportedOperationException uoe = null;
		try {
			iterator.remove();
		}
		catch ( UnsupportedOperationException e ) {
			uoe = e;
		}
		assertNotNull( uoe );
	}

	public void testParentsFirstIterator() throws Exception {
		AST[] tree = new AST[4];
		AST grandparent = tree[0] = ASTUtil.create( factory, 1, "grandparent" );
		AST parent = tree[1] = ASTUtil.create( factory, 2, "parent" );
		AST child = tree[2] = ASTUtil.create( factory, 3, "child" );
		AST baby = tree[3] = ASTUtil.create( factory, 4, "baby" );
		AST t = ASTUtil.createTree( factory, tree );
		AST brother = ASTUtil.create( factory, 10, "brother" );
		child.setNextSibling( brother );
		AST sister = ASTUtil.create( factory, 11, "sister" );
		brother.setNextSibling( sister );
		AST uncle = factory.make( new AST[]{
			factory.create( 20, "uncle" ),
			factory.create( 21, "cousin1" ),
			factory.create( 22, "cousin2" ),
			factory.create( 23, "cousin3" )} );
		parent.setNextSibling( uncle );
		System.out.println( t.toStringTree() );

		System.out.println( "--- ASTParentsFirstIterator ---" );
		ASTParentsFirstIterator iter = new ASTParentsFirstIterator( t );
		int count = 0;
		while ( iter.hasNext() ) {
			AST n = iter.nextNode();
			count++;
			System.out.println( n );
		}
		assertEquals( 10, count );

		System.out.println( "--- ASTIterator ---" );
		ASTIterator iter2 = new ASTIterator( t );
		int count2 = 0;
		while ( iter2.hasNext() ) {
			AST n = iter2.nextNode();
			count2++;
			System.out.println( n );
		}
		assertEquals( 10, count2 );

		System.out.println( "--- ASTParentsFirstIterator (parent) ---" );
		ASTParentsFirstIterator iter3 = new ASTParentsFirstIterator( parent );
		int count3 = 0;
		while ( iter3.hasNext() ) {
			AST n = iter3.nextNode();
			count3++;
			System.out.println( n );
		}
		assertEquals( 5, count3 );
	}
}
