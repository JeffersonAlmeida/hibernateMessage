//$Id: CMTTest.java,v 1.6 2005/03/06 16:31:14 oneovthafew Exp $
package org.hibernate.test.tm;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.test.TestCase;

/**
 * @author Gavin King
 */
public class CMTTest extends TestCase {
	
	public CMTTest(String str) {
		super(str);
	}
	
	public void testCMT() throws Exception {
		DummyTransactionManager.INSTANCE.begin();
		Session s = openSession();
		DummyTransactionManager.INSTANCE.getTransaction().commit();
		assertFalse( s.isOpen() );

		assertEquals( getSessions().getStatistics().getFlushCount(), 0 );

		DummyTransactionManager.INSTANCE.begin();
		s = openSession();
		Map item = new HashMap();
		item.put("name", "The Item");
		item.put("description", "The only item we have");
		s.getSession(EntityMode.MAP).persist("Item", item);
		DummyTransactionManager.INSTANCE.getTransaction().commit();
		assertFalse( s.isOpen() );

		DummyTransactionManager.INSTANCE.begin();
		s = openSession().getSession(EntityMode.MAP);
		item = (Map) s.createQuery("from Item").uniqueResult();
		assertNotNull(item);
		s.delete(item);
		DummyTransactionManager.INSTANCE.getTransaction().commit();
		assertFalse( s.isOpen() );
		
		assertEquals( getSessions().getStatistics().getSuccessfulTransactionCount(), 3 );
		assertEquals( getSessions().getStatistics().getEntityDeleteCount(), 1 );
		assertEquals( getSessions().getStatistics().getEntityInsertCount(), 1 );
		assertEquals( getSessions().getStatistics().getSessionOpenCount(), 5 );
		assertEquals( getSessions().getStatistics().getSessionCloseCount(), 5 );
		assertEquals( getSessions().getStatistics().getQueryExecutionCount(), 1 );
		assertEquals( getSessions().getStatistics().getFlushCount(), 2 );
	}
	
	protected String[] getMappings() {
		return new String[] { "tm/Item.hbm.xml" };
	}

	public static Test suite() {
		return new TestSuite(CMTTest.class);
	}

	protected void configure(Configuration cfg) {
		cfg.setProperty(Environment.CONNECTION_PROVIDER, DummyConnectionProvider.class.getName());
		cfg.setProperty(Environment.TRANSACTION_MANAGER_STRATEGY, DummyTransactionManagerLookup.class.getName());
		cfg.setProperty(Environment.AUTO_CLOSE_SESSION, "true");
		cfg.setProperty(Environment.FLUSH_BEFORE_COMPLETION, "true");
		cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
	}
}

