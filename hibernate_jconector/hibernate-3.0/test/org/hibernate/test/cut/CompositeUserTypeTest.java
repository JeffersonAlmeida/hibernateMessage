//$Id: CompositeUserTypeTest.java,v 1.1 2005/03/29 03:06:25 oneovthafew Exp $
package org.hibernate.test.cut;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.Session;
import org.hibernate.test.TestCase;

/**
 * @author Gavin King
 */
public class CompositeUserTypeTest extends TestCase {
	
	public CompositeUserTypeTest(String str) {
		super(str);
	}
	
	public void testCompositeUserType() {
		Session s = openSession();
		org.hibernate.Transaction t = s.beginTransaction();
		
		Transaction tran = new Transaction();
		tran.setDescription("a small transaction");
		tran.setValue( new MonetoryAmount( new BigDecimal(1.5), Currency.getInstance("USD") ) );
		s.persist(tran);
		
		List result = s.createQuery("from Transaction tran where tran.value.amount > 1.0 and tran.value.currency = 'USD'").list();
		assertEquals( result.size(), 1 );
		tran.getValue().setCurrency( Currency.getInstance("AUD") );
		result = s.createQuery("from Transaction tran where tran.value.amount > 1.0 and tran.value.currency = 'AUD'").list();
		assertEquals( result.size(), 1 );
		
		s.delete(tran);
		t.commit();
		s.close();
	}

	
	protected String[] getMappings() {
		return new String[] { "cut/Transaction.hbm.xml" };
	}

	public static Test suite() {
		return new TestSuite(CompositeUserTypeTest.class);
	}

}

