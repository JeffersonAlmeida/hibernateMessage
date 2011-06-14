//$Id: BackrefTest.java,v 1.5 2005/02/21 14:41:05 oneovthafew Exp $
package org.hibernate.test.unidir;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.TestCase;

/**
 * @author Gavin King
 */
public class BackrefTest extends TestCase {
	
	public BackrefTest(String str) {
		super(str);
	}
	
	public void testBackRef() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Parent p = new Parent("Marc");
		Parent p2 = new Parent("Nathalie");
		Child c = new Child("Elvira");
		Child c2 = new Child("Blase");
		p.getChildren().add(c);
		p.getChildren().add(c2);
		s.persist(p);
		s.persist(p2);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c = (Child) s.get(Child.class, "Elvira");
		c.setAge(2);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		p = (Parent) s.get(Parent.class, "Marc");
		c = (Child) s.get(Child.class, "Elvira");
		c.setAge(18);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		p = (Parent) s.get(Parent.class, "Marc");
		p2 = (Parent) s.get(Parent.class, "Nathalie");
		c = (Child) s.get(Child.class, "Elvira");
		assertEquals( p.getChildren().indexOf(c), 0 );
		p.getChildren().remove(c);
		p2.getChildren().add(c);
		t.commit();
		s.close();
	}
	
	protected String[] getMappings() {
		return new String[] { "unidir/ParentChild.hbm.xml" };
	}

	public static Test suite() {
		return new TestSuite(BackrefTest.class);
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

}

