//$Id: TernaryTest.java,v 1.4 2005/02/21 14:41:03 oneovthafew Exp $
package org.hibernate.test.ternary;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.TestCase;

/**
 * @author Gavin King
 */
public class TernaryTest extends TestCase {
	
	public TernaryTest(String str) {
		super(str);
	}
	
	public void testTernary() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Employee bob = new Employee("Bob");
		Employee tom = new Employee("Tom");
		Employee jim = new Employee("Jim");
		Employee tim = new Employee("Tim");
		Site melb = new Site("Melbourne");
		Site geel = new Site("Geelong");
		s.persist(bob);
		s.persist(tom);
		s.persist(jim);
		s.persist(tim);
		s.persist(melb);
		s.persist(geel);
		bob.getManagerBySite().put(melb, tom);
		bob.getManagerBySite().put(geel, jim);
		tim.getManagerBySite().put(melb, tom);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		tom = (Employee) s.get(Employee.class, "Tom");
		assertFalse( Hibernate.isInitialized(tom.getUnderlings()) );
		assertEquals( tom.getUnderlings().size(), 2 );
		bob = (Employee) s.get(Employee.class, "Bob");
		assertFalse( Hibernate.isInitialized(bob.getManagerBySite()) );
		assertTrue( tom.getUnderlings().contains(bob) );
		melb = (Site) s.get(Site.class, "Melbourne");
		assertSame( bob.getManagerBySite().get(melb), tom );
		assertTrue( melb.getEmployees().contains(bob) );
		assertTrue( melb.getManagers().contains(tom) );
		t.commit();
		s.close();		

		s = openSession();
		t = s.beginTransaction();
		List l = s.createQuery("from Employee e join e.managerBySite m where m.name='Bob'").list();
		assertEquals( l.size(), 0 );
		l = s.createQuery("from Employee e join e.managerBySite m where m.name='Tom'").list();
		assertEquals( l.size(), 2 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		l = s.createQuery("from Employee e left join fetch e.managerBySite").list();
		assertEquals( l.size(), 5 );
		Set set = new HashSet(l);
		assertEquals( set.size(), 4 );
		Iterator iter = set.iterator();
		int total=0;
		while ( iter.hasNext() ) {
			Map map = ( (Employee) iter.next() ).getManagerBySite();
			assertTrue( Hibernate.isInitialized(map) );
			total += map.size();
		}
		assertTrue(total==3);
		l = s.createQuery("from Employee e left join e.managerBySite m left join m.managerBySite m2").list();
		t.commit();
		s.close();
	}
	
	protected String[] getMappings() {
		return new String[] { "ternary/Ternary.hbm.xml" };
	}

	public static Test suite() {
		return new TestSuite(TernaryTest.class);
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

}

