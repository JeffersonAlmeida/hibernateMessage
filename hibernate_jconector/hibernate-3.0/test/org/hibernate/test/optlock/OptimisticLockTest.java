//$Id: OptimisticLockTest.java,v 1.1 2004/08/21 08:43:19 oneovthafew Exp $
package org.hibernate.test.optlock;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.TestCase;

/**
 * @author Gavin King
 */
public class OptimisticLockTest extends TestCase {
	
	public OptimisticLockTest(String str) {
		super(str);
	}
	
	public void testOptimisticLockDirty() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Document doc = new Document();
		doc.setTitle("Hibernate in Action");
		doc.setAuthor("Bauer et al");
		doc.setSummary("Very boring book about persistence");
		doc.setText("blah blah yada yada yada");
		s.save(doc);
		s.flush();
		doc.setSummary("A modern classic");
		s.flush();
		s.delete(doc);
		t.commit();
		s.close();
	}

	
	protected String[] getMappings() {
		return new String[] { "optlock/Document.hbm.xml" };
	}

	public static Test suite() {
		return new TestSuite(OptimisticLockTest.class);
	}

}

