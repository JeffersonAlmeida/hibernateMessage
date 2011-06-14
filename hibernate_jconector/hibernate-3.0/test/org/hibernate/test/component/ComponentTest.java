//$Id: ComponentTest.java,v 1.6 2005/03/21 18:29:50 oneovthafew Exp $
package org.hibernate.test.component;

import java.util.Date;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Property;
import org.hibernate.dialect.Oracle9Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.test.TestCase;

/**
 * @author Gavin King
 */
public class ComponentTest extends TestCase {
	
	public ComponentTest(String str) {
		super(str);
	}
	
	public void testComponent() {
		
		if ( getDialect() instanceof PostgreSQLDialect ) return; //postgres got no year() function
		if ( getDialect() instanceof Oracle9Dialect ) return; //oracle got no year() function
		
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User u = new User( "gavin", "secret", new Person("Gavin King", new Date(), "Karbarook Ave") );
		s.persist(u);
		s.flush();
		u.getPerson().changeAddress("Phipps Place");
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		u = (User) s.get(User.class, "gavin");
		assertEquals( u.getPerson().getAddress(), "Phipps Place" );
		assertEquals( u.getPerson().getPreviousAddress(), "Karbarook Ave" );
		assertEquals( u.getPerson().getYob(), u.getPerson().getDob().getYear()+1900 );
		u.setPassword("$ecret");
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		u = (User) s.get(User.class, "gavin");
		assertEquals( u.getPerson().getAddress(), "Phipps Place" );
		assertEquals( u.getPerson().getPreviousAddress(), "Karbarook Ave" );
		assertEquals( u.getPassword(), "$ecret" );
		s.delete(u);
		t.commit();
		s.close();
	}
	
	public void testComponentFormulaQuery() {
		
		if ( getDialect() instanceof PostgreSQLDialect ) return; //postgres got no year() function
		if ( getDialect() instanceof Oracle9Dialect ) return; //oracle got no year() function
		
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.createQuery("from User u where u.person.yob = 1999").list();
		s.createCriteria(User.class)
			.add( Property.forName("person.yob").between( new Integer(1999), new Integer(2002) ) )
			.list();
		t.commit();
		s.close();
	}

	
	protected String[] getMappings() {
		return new String[] { "component/User.hbm.xml" };
	}

	public static Test suite() {
		return new TestSuite(ComponentTest.class);
	}

}

