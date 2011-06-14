//$Id: LazyOneToOneTest.java,v 1.3 2005/02/21 14:41:00 oneovthafew Exp $
package org.hibernate.test.lazyonetoone;

import java.util.Date;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.test.TestCase;

/**
 * @author Gavin King
 */
public class LazyOneToOneTest extends TestCase {
	
	public LazyOneToOneTest(String str) {
		super(str);
	}

	public void testLazy() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Person p = new Person("Gavin");
		Employee e = new Employee(p);
		new Employment(e, "JBoss");
		Employment old = new Employment(e, "IFA");
		old.setEndDate( new Date() );
		s.persist(p);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		p = (Person) s.get(Person.class, "Gavin");
		assertFalse( Hibernate.isPropertyInitialized(p, "employee") );
		assertSame( p.getEmployee().getPerson(), p );
		assertTrue( Hibernate.isInitialized( p.getEmployee().getEmployments() ) );
		assertEquals( p.getEmployee().getEmployments().size(), 1 );
		s.delete(old);
		s.delete(p);
		t.commit();
		s.close();
	}
	
	protected void configure(Configuration cfg) {
		cfg.setProperty(Environment.MAX_FETCH_DEPTH, "2");
	}
	protected String[] getMappings() {
		return new String[] { "lazyonetoone/Person.hbm.xml" };
	}

	public static Test suite() {
		return new TestSuite(LazyOneToOneTest.class);
	}

}

