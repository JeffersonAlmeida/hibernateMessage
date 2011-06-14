//$Id: OneToOneTest.java,v 1.3 2005/02/21 14:41:02 oneovthafew Exp $
package org.hibernate.test.onetoonelink;

import java.util.Date;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.TestCase;

/**
 * @author Gavin King
 */
public class OneToOneTest extends TestCase {
	
	public OneToOneTest(String str) {
		super(str);
	}
	
	public void testOneToOneViaAssociationTable() {
		Person p = new Person();
		p.setName("Gavin King");
		p.setDob( new Date() );
		Employee e = new Employee();
		p.setEmployee(e);
		e.setPerson(p);
		
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.persist(p);
		t.commit();
		s.close();
	
		s = openSession();
		t = s.beginTransaction();
		e = (Employee) s.createQuery("from Employee e where e.person.name like 'Gavin%'").uniqueResult();
		assertEquals( e.getPerson().getName(), "Gavin King" );
		assertFalse( Hibernate.isInitialized( e.getPerson() ) );
		assertNull( e.getPerson().getCustomer() );
		s.clear();

		e = (Employee) s.createQuery("from Employee e where e.person.dob = :date")
			.setDate("date", new Date() )
			.uniqueResult();
		assertEquals( e.getPerson().getName(), "Gavin King" );
		assertFalse( Hibernate.isInitialized( e.getPerson() ) );
		assertNull( e.getPerson().getCustomer() );
		s.clear();
		
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();

		e = (Employee) s.createQuery("from Employee e join fetch e.person p left join fetch p.customer").uniqueResult();
		assertTrue( Hibernate.isInitialized( e.getPerson() ) );
		assertNull( e.getPerson().getCustomer() );
		Customer c = new Customer();
		e.getPerson().setCustomer(c);
		c.setPerson( e.getPerson() );
		
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();

		e = (Employee) s.createQuery("from Employee e join fetch e.person p left join fetch p.customer").uniqueResult();
		assertTrue( Hibernate.isInitialized( e.getPerson() ) );
		assertTrue( Hibernate.isInitialized( e.getPerson().getCustomer() ) );
		assertNotNull( e.getPerson().getCustomer() );
		s.delete(e);
		t.commit();
		s.close();
		
	}
	
	protected String[] getMappings() {
		return new String[] { "onetoonelink/Person.hbm.xml" };
	}

	public static Test suite() {
		return new TestSuite(OneToOneTest.class);
	}

}

