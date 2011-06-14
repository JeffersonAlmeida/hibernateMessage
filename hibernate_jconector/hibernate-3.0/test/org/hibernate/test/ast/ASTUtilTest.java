// $Id: ASTUtilTest.java,v 1.4 2005/02/12 20:27:50 pgmjsd Exp $
package org.hibernate.test.ast;

import antlr.ASTFactory;
import antlr.collections.AST;
import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.hibernate.hql.ast.ASTUtil;

/**
 * Unit test for ASTUtil.
 */
public class ASTUtilTest extends TestCase {
	private ASTFactory factory;

	/**
	 * Standard JUnit test case constructor.
	 *
	 * @param name The name of the test case.
	 */
	public ASTUtilTest(String name) {
		super( name );
	}

	protected void setUp() throws Exception {
		super.setUp();
		factory = new ASTFactory();
	}

	public void testCreate() throws Exception {
		AST n = ASTUtil.create( factory, 1, "one");
		assertNull( n.getFirstChild() );
		assertEquals("one",n.getText());
		assertEquals(1,n.getType());
	}
	/**
	 * Test adding a tree of children.
	 */
	public void testCreateTree() throws Exception {
		AST[] tree = new AST[4];
		AST grandparent = tree[0] = ASTUtil.create(factory, 1, "grandparent");
		AST parent = tree[1] = ASTUtil.create(factory,2,"parent");
		AST child = tree[2] = ASTUtil.create(factory,3,"child");
		AST baby = tree[3] = ASTUtil.create(factory,4,"baby");
		AST t = ASTUtil.createTree( factory, tree);
		assertSame(t,grandparent);
		assertSame(parent,t.getFirstChild());
		assertSame(child,t.getFirstChild().getFirstChild());
		assertSame(baby,t.getFirstChild().getFirstChild().getFirstChild());
	}

	public void testFindPreviousSibling() throws Exception {
		AST child1 = ASTUtil.create(factory,2, "child1");
		AST child2 = ASTUtil.create(factory,3, "child2");
		AST n = factory.make( new AST[] {
			ASTUtil.create(factory, 1, "parent"),
			child1,
			child2,
		});
		assertSame(child1,ASTUtil.findPreviousSibling( n,child2));
		Exception e = null;
		try {
			ASTUtil.findPreviousSibling(child1,null);
		}
		catch (Exception x) {
			e = x;
		}
		assertNotNull(e);
	}

	public static Test suite() {
		return new TestSuite( ASTUtilTest.class );
	}

}
