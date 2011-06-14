//$Id: PropertyRefTest.java,v 1.4 2005/02/18 02:53:22 oneovthafew Exp $
package org.hibernate.test.propertyref;

import java.util.List;

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
public class PropertyRefTest extends TestCase {
	
	public PropertyRefTest(String str) {
		super(str);
	}
	
	public void testOneToOnePropertyRef() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Person p = new Person();
		p.setName("Steve");
		p.setUserId("steve");
		Address a = new Address();
		a.setAddress("Texas");
		a.setCountry("USA");
		p.setAddress(a);
		a.setPerson(p);
		s.save(p);
		Person p2 = new Person();
		p2.setName("Max");
		p2.setUserId("max");
		s.save(p2);
		Account act = new Account();
		act.setType('c');
		act.setUser(p2);
		p2.getAccounts().add(act);
		s.save(act);
		s.flush();
		s.clear();
		
		p = (Person) s.get( Person.class, p.getId() ); //get address reference by outer join
		p2 = (Person) s.get( Person.class, p2.getId() ); //get null address reference by outer join
		assertNull( p2.getAddress() );
		assertNotNull( p.getAddress() );
		List l = s.createQuery("from Person").list(); //pull address references for cache
		assertEquals( l.size(), 2 );
		assertTrue( l.contains(p) && l.contains(p2) );
		s.clear();
		
		l = s.createQuery("from Person p order by p.name").list(); //get address references by sequential selects
		assertEquals( l.size(), 2 );
		assertNull( ( (Person) l.get(0) ).getAddress() );
		assertNotNull( ( (Person) l.get(1) ).getAddress() );
		s.clear();
		
		l = s.createQuery("from Person p left join fetch p.address a order by a.country").list(); //get em by outer join
		assertEquals( l.size(), 2 );
		if ( ( (Person) l.get(0) ).getName().equals("Max") ) {
			assertNull( ( (Person) l.get(0) ).getAddress() );
			assertNotNull( ( (Person) l.get(1) ).getAddress() );
		}
		else {
			assertNull( ( (Person) l.get(1) ).getAddress() );
			assertNotNull( ( (Person) l.get(0) ).getAddress() );
		}
		s.clear();
		
		l = s.createQuery("from Person p left join p.accounts a").list();
		for ( int i=0; i<2; i++ ) {
			Object[] row = (Object[]) l.get(i);
			Person px = (Person) row[0];
			assertFalse( Hibernate.isInitialized( px.getAccounts() ) );
			assertTrue( px.getAccounts().size()>0 || row[1]==null );
		}
		s.clear();

		l = s.createQuery("from Person p left join fetch p.accounts a order by p.name").list();
		Person p0 = (Person) l.get(0);
		assertTrue( Hibernate.isInitialized( p0.getAccounts() ) );
		assertEquals( p0.getAccounts().size(), 1 );
		assertSame( ( (Account) p0.getAccounts().iterator().next() ).getUser(), p0 );
		Person p1 = (Person) l.get(1);
		assertTrue( Hibernate.isInitialized( p1.getAccounts() ) );
		assertEquals( p1.getAccounts().size(), 0 );
		
		t.commit();
		s.close();
	}

	
	protected String[] getMappings() {
		return new String[] { "propertyref/Person.hbm.xml" };
	}

	public static Test suite() {
		return new TestSuite(PropertyRefTest.class);
	}

	protected void configure(Configuration cfg) {
		cfg.setProperty(Environment.DEFAULT_BATCH_FETCH_SIZE, "1");
	}
}

