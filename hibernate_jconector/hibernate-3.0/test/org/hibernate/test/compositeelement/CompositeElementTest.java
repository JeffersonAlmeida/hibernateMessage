//$Id: CompositeElementTest.java,v 1.1 2004/09/02 02:30:28 oneovthafew Exp $
package org.hibernate.test.compositeelement;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.TestCase;

/**
 * @author Gavin King
 */
public class CompositeElementTest extends TestCase {
	
	public CompositeElementTest(String str) {
		super(str);
	}
	
	public void testHandSQL() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Child c = new Child("Child One");
		Parent p = new Parent("Parent");
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
		s.createQuery("select distinct p from Parent p join p.children c where c.name like 'Child%'").uniqueResult();
		s.clear();
		s.createQuery("select new Child(c.name) from Parent p left outer join p.children c where c.name like 'Child%'").uniqueResult();
		s.clear();
		//s.createQuery("select c from Parent p left outer join p.children c where c.name like 'Child%'").uniqueResult(); //we really need to be able to do this!
		s.clear();
		p = (Parent) s.createQuery("from Parent p left join fetch p.children").uniqueResult();
		t.commit();
		s.close();
	}

	
	protected String[] getMappings() {
		return new String[] { "compositeelement/Parent.hbm.xml" };
	}

	public static Test suite() {
		return new TestSuite(CompositeElementTest.class);
	}

}

