// $Id: LobTest.java,v 1.3 2005/02/12 07:27:27 steveebersole Exp $
package org.hibernate.test.lob;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.TestCase;

/**
 * Implementation of DynamicFilterTest.
 * 
 * @author Steve
 */
public class LobTest extends TestCase {

	public LobTest(String testName) {
		super(testName);
	}

	public void testNewSerializableType() {
		Session session = openSession();
		Transaction txn = session.beginTransaction();

		User user = new User();
		user.setEmail("nobody@nowhere.com");
		user.setName(new Name());
		user.getName().setFirstName("John");
		user.getName().setInitial(new Character('Q'));
		user.getName().setLastName("Public");
		user.setPassword("password");
		user.setHandle("myHandle");

		String payloadText = "Initial payload";
        user.setSerialData( new SerializableData(payloadText) );

		session.save(user);
		txn.commit();

		session.close();
		user = null;

		session = openSession();
        user = (User) session.createQuery("select u from User as u where u.handle = :myHandle")
                .setString("myHandle", "myHandle")
                .uniqueResult();

		SerializableData serialData = (SerializableData) user.getSerialData();
		assertTrue(payloadText.equals(serialData.getPayload()));
		session.close();
	}

    /**
     * Define the mappings needed for these tests.
     *
     * @return Mappings for these tests.
     */
	protected String[] getMappings() {
		return new String[] {
			"lob/User.hbm.xml"
		};
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

	public static Test suite() {
		return new TestSuite(LobTest.class);
	}

}
