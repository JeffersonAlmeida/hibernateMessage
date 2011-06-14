//$Id: OneToManyTest.java,v 1.3 2004/09/23 08:26:21 oneovthafew Exp $
package org.hibernate.test.onetomany;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.TestCase;

/**
 * @author Gavin King
 */
public class OneToManyTest extends TestCase {
	
	public OneToManyTest(String str) {
		super(str);
	}
	
	public void testOneToManyLinkTable() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Child c = new Child();
		c.setName("Child One");
		Parent p = new Parent();
		p.setName("Parent");
		p.getChildren().add(c);
		c.setParent(p);
		s.save(p);
		s.flush();
		
		p.getChildren().remove(c);
		c.setParent(null);
		s.flush();
		
		p.getChildren().add(c);
		c.setParent(p);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		c.setParent(null);
		s.update(c);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c.setParent(p);
		s.update(c);
		t.commit();
		s.close();

		
		s = openSession();
		t = s.beginTransaction();
		c = (Child) s.createQuery("from Child").uniqueResult();
		s.createQuery("from Child c left join fetch c.parent").list();
		s.createQuery("from Child c inner join fetch c.parent").list();
		s.clear();
		p = (Parent) s.createQuery("from Parent p left join fetch p.children").uniqueResult();
		t.commit();
		s.close();
	}

	
	protected String[] getMappings() {
		return new String[] { "onetomany/Parent.hbm.xml" };
	}

	public static Test suite() {
		return new TestSuite(OneToManyTest.class);
	}

}

