//$Id: SubselectTest.java,v 1.2 2004/08/16 06:05:30 oneovthafew Exp $
package org.hibernate.test.subselect;

import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.TestCase;

/**
 * @author Gavin King
 */
public class SubselectTest extends TestCase {
	
	public SubselectTest(String str) {
		super(str);
	}
	
	public void testEntitySubselect() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Human gavin = new Human();
		gavin.name = "gavin";
		gavin.sex = 'M';
		gavin.address = "Melbourne, Australia";
		Alien x23y4 = new Alien();
		x23y4.identity = "x23y4$$hu%3";
		x23y4.planet = "Mars";
		x23y4.species = "martian";
		s.save(gavin);
		s.save(x23y4);
		s.flush();
		List beings = s.createQuery("from Being").list();
		for ( Iterator iter = beings.iterator(); iter.hasNext(); ) {
			Being b = (Being) iter.next();
			assertNotNull( b.location );
			assertNotNull( b.identity );
			assertNotNull( b.species );
		}
		s.clear();
		getSessions().evict(Being.class);
		Being gav = (Being) s.get(Being.class, gavin.id);
		assertEquals( gav.location, gavin.address );
		assertEquals( gav.species, "human" );
		assertEquals( gav.identity, gavin.name );
		s.clear();
		//test the <synchronized> tag:
		gavin = (Human) s.get(Human.class, gavin.id);
		gavin.address = "Atlanta, GA";
		gav = (Being) s.createQuery("from Being b where b.location like '%GA%'").uniqueResult();
		assertEquals( gav.location, gavin.address );
		s.delete(gavin);
		s.delete(x23y4);
		assertTrue( s.createQuery("from Being").list().isEmpty() );
		t.commit();
		s.close();
	}

	
	protected String[] getMappings() {
		return new String[] { "subselect/Beings.hbm.xml" };
	}

	public static Test suite() {
		return new TestSuite(SubselectTest.class);
	}

}

