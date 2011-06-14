// $Id: UnionSubclassFilterTest.java,v 1.2 2005/03/17 22:04:13 oneovthafew Exp $
package org.hibernate.test.subclassfilter;

import org.hibernate.test.TestCase;
import org.hibernate.classic.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashSet;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Implementation of DiscrimSubclassFilterTest.
 *
 * @author Steve Ebersole
 */
public class UnionSubclassFilterTest extends TestCase {

	public UnionSubclassFilterTest(String name) {
		super( name );
	}

	protected final String[] getMappings() {
		return new String[] { "subclassfilter/union-subclass.hbm.xml" };
	}

	public void testFiltersWithJoinedSubclass() {
		Session s = openSession();
		s.enableFilter( "region" ).setParameter( "userRegion", "US" );
		Transaction t = s.beginTransaction();

		prepareTestData( s );
		s.clear();

		List results;
		Iterator itr;

		results = s.createQuery( "from Person" ).list();
		assertEquals( "Incorrect qry result count", 4, results.size() );
		s.clear();

		results = s.createQuery( "from Employee" ).list();
		assertEquals( "Incorrect qry result count", 2, results.size() );
		s.clear();

		results = new ArrayList( new HashSet( s.createQuery( "from Person as p left join fetch p.minions" ).list() ) );
		assertEquals( "Incorrect qry result count", 4, results.size() );
		itr = results.iterator();
		while ( itr.hasNext() ) {
			// find john
			final Person p = ( Person ) itr.next();
			if ( p.getName().equals( "John Doe" ) ) {
				Employee john = ( Employee ) p;
				assertEquals( "Incorrect fecthed minions count", 1, john.getMinions().size() );
				break;
			}
		}
		s.clear();

		results = new ArrayList( new HashSet( s.createQuery( "from Employee as p left join fetch p.minions" ).list() ) );
		assertEquals( "Incorrect qry result count", 2, results.size() );
		itr = results.iterator();
		while ( itr.hasNext() ) {
			// find john
			final Person p = ( Person ) itr.next();
			if ( p.getName().equals( "John Doe" ) ) {
				Employee john = ( Employee ) p;
				assertEquals( "Incorrect fecthed minions count", 1, john.getMinions().size() );
				break;
			}
		}

		t.commit();
		s.close();
	}

	private void prepareTestData(Session s) {
		Employee john = new Employee("John Doe");
		john.setCompany( "JBoss" );
		john.setDepartment( "hr" );
		john.setTitle( "hr guru" );
		john.setRegion( "US" );

		Employee polli = new Employee("Polli Wog");
		polli.setCompany( "JBoss" );
		polli.setDepartment( "hr" );
		polli.setTitle( "hr novice" );
		polli.setRegion( "US" );
		polli.setManager( john );
		john.getMinions().add( polli );

		Employee suzie = new Employee( "Suzie Q" );
		suzie.setCompany( "JBoss" );
		suzie.setDepartment( "hr" );
		suzie.setTitle( "hr novice" );
		suzie.setRegion( "EMEA" );
		suzie.setManager( john );
		john.getMinions().add( suzie );

		Customer cust = new Customer( "John Q Public" );
		cust.setCompany( "Acme" );
		cust.setRegion( "US" );
		cust.setContactOwner( john );

		Person ups = new Person( "UPS guy" );
		ups.setCompany( "UPS" );
		ups.setRegion( "US" );

		s.save( john );
		s.save( cust );
		s.save( ups );

		s.flush();
	}

	public static Test suite() {
		return new TestSuite(UnionSubclassFilterTest.class);
	}
}
