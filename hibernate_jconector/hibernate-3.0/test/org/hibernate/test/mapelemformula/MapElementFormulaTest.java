//$Id: MapElementFormulaTest.java,v 1.4 2005/02/21 14:41:01 oneovthafew Exp $
package org.hibernate.test.mapelemformula;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.TestCase;

/**
 * @author Gavin King
 */
public class MapElementFormulaTest extends TestCase {
	
	public MapElementFormulaTest(String str) {
		super(str);
	}
	
	public void testManyToManyFormula() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User gavin = new User("gavin", "secret");
		User turin = new User("turin", "tiger");
		Group g = new Group("users");
		g.getUsers().put("Gavin", gavin);
		g.getUsers().put("Turin", turin);
		s.persist(g);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		g = (Group) s.get(Group.class, "users");
		assertEquals( g.getUsers().size(), 2 );
		g.getUsers().remove("Turin");
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		g = (Group) s.get(Group.class, "users");
		assertEquals( g.getUsers().size(), 1 );
		s.delete(g);
		s.delete( g.getUsers().get("Gavin") );
		s.delete( s.get(User.class, "turin") );
		t.commit();
		s.close();
	}
	
	protected String[] getMappings() {
		return new String[] { "mapelemformula/UserGroup.hbm.xml" };
	}

	public static Test suite() {
		return new TestSuite(MapElementFormulaTest.class);
	}

}

