//$Id: MultipleHiLoPerTableGeneratorTest.java,v 1.4 2005/02/21 14:40:59 oneovthafew Exp $
package org.hibernate.test.id;

import org.hibernate.test.TestCase;
import org.hibernate.Session;
import org.hibernate.Transaction;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Emmanuel Bernard
 */
public class MultipleHiLoPerTableGeneratorTest extends TestCase {
	public MultipleHiLoPerTableGeneratorTest(String x) {
		super(x);
	}

	protected String[] getMappings() {
		return new String[]{
			"id/Car.hbm.xml",
			"id/Plane.hbm.xml",
			"id/Radio.hbm.xml"
		};
	}

	public void testDistinctId() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		int testLength = 3;
		Car[] cars = new Car[testLength];
		Plane[] planes = new Plane[testLength];
		for (int i = 0; i < testLength ; i++) {
			cars[i] = new Car();
			cars[i].setColor("Color" + i);
			planes[i] = new Plane();
			planes[i].setNbrOfSeats(i);
			s.persist(cars[i]);
			s.persist(planes[i]);
		}
		tx.commit();
		s.close();
		for (int i = 0; i < testLength ; i++) {
			assertEquals(i+1, cars[i].getId().intValue());
			assertEquals(i+1, planes[i].getId().intValue());
		}
	}

	public void testAllParams() throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Radio radio = new Radio();
		radio.setFrequency("32 MHz");
		s.persist(radio);
		assertEquals( new Integer(1), radio.getId() );
		radio = new Radio();
		radio.setFrequency("32 MHz");
		s.persist(radio);
		assertEquals( new Integer(2), radio.getId() );
		tx.commit();
		s.close();
	}

	public static Test suite() {
		return new TestSuite(MultipleHiLoPerTableGeneratorTest.class);
	}
}
