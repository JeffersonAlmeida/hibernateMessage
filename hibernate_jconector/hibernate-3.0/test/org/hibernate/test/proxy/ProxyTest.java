//$Id: ProxyTest.java,v 1.6 2005/02/21 14:41:03 oneovthafew Exp $
package org.hibernate.test.proxy;

import java.math.BigDecimal;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.test.TestCase;
import org.hibernate.util.SerializationHelper;

/**
 * @author Gavin King
 */
public class ProxyTest extends TestCase {
	
	public ProxyTest(String str) {
		super(str);
	}

	public void testProxySerialization() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setDescription("a data point");
		dp.setX( new BigDecimal(1.0) );
		dp.setY( new BigDecimal(2.0) );
		s.persist(dp);
		s.flush();
		s.clear();

		dp = (DataPoint) s.load( DataPoint.class, new Long( dp.getId() ) );
		assertFalse( Hibernate.isInitialized(dp) );
		dp.getId();
		assertFalse( Hibernate.isInitialized(dp) );
		dp.getDescription();
		assertTrue( Hibernate.isInitialized(dp) );
		Object none = s.load( DataPoint.class, new Long(666));
		assertFalse( Hibernate.isInitialized(none) );
		
		t.commit();
		s.disconnect();
		
		Object[] holder = new Object[] { s, dp, none };
		
		holder = (Object[]) SerializationHelper.clone(holder);
		Session sclone = (Session) holder[0];
		dp = (DataPoint) holder[1];
		none = holder[2];
		
		//close the original:
		s.close();
		
		sclone.reconnect();
		t = sclone.beginTransaction();
		
		DataPoint sdp = (DataPoint) sclone.load( DataPoint.class, new Long( dp.getId() ) );
		assertSame(dp, sdp);
		assertFalse(sdp instanceof HibernateProxy);
		Object snone = sclone.load( DataPoint.class, new Long(666) );
		assertSame(none, snone);
		assertTrue(snone instanceof HibernateProxy);
		
		sclone.delete(dp);
		
		t.commit();
		sclone.close();
		
	}
	
	public void testProxy() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		DataPoint dp = new DataPoint();
		dp.setDescription("a data point");
		dp.setX( new BigDecimal(1.0) );
		dp.setY( new BigDecimal(2.0) );
		s.persist(dp);
		s.flush();
		s.clear();

		dp = (DataPoint) s.load( DataPoint.class, new Long(dp.getId() ));
		assertFalse( Hibernate.isInitialized(dp) );
		DataPoint dp2 = (DataPoint) s.get( DataPoint.class, new Long(dp.getId()) );
		assertSame(dp, dp2);
		assertTrue( Hibernate.isInitialized(dp) );
		s.clear();

		dp = (DataPoint) s.load( DataPoint.class, new Long( dp.getId() ) );
		assertFalse( Hibernate.isInitialized(dp) );
		dp2 = (DataPoint) s.load( DataPoint.class, new Long (dp.getId() ), LockMode.READ );
		assertSame(dp, dp2);
		assertFalse( Hibernate.isInitialized(dp) );
		s.clear();

		dp = (DataPoint) s.load( DataPoint.class, new Long (dp.getId() ));
		assertFalse( Hibernate.isInitialized(dp) );
		dp2 = (DataPoint) s.get( DataPoint.class, new Long ( dp.getId() ) , LockMode.READ );
		assertSame(dp, dp2);
		assertTrue( Hibernate.isInitialized(dp) );
		s.clear();

		dp = (DataPoint) s.load( DataPoint.class, new Long  ( dp.getId() ) );
		assertFalse( Hibernate.isInitialized(dp) );
		dp2 = (DataPoint) s.createQuery("from DataPoint").uniqueResult();
		assertSame(dp, dp2);
		assertTrue( Hibernate.isInitialized(dp) );
		t.commit();
		s.close();
	}

	
	protected String[] getMappings() {
		return new String[] { "proxy/DataPoint.hbm.xml" };
	}

	public static Test suite() {
		return new TestSuite(ProxyTest.class);
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

}

