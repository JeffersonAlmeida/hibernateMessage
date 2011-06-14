//$Id: BatchTest.java,v 1.3 2004/08/18 09:19:34 oneovthafew Exp $
package org.hibernate.test.batch;

import java.math.BigDecimal;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.TestCase;

/**
 * This is how to do batch processing in Hibernate.
 * Remember to enable JDBC batch updates, or this 
 * test will take a Very Long Time!
 * 
 * @author Gavin King
 */
public class BatchTest extends TestCase {
	
	public BatchTest(String str) {
		super(str);
	}
	
	public void testBatchInsertUpdate() {
		
		//remember to set hibernate.jdbc.batch_size=20
		
		long start = System.currentTimeMillis();
		
		final boolean flushInBatches = true;
		final int N = 50000; //26 secs with batch flush, 26 without
		//final int N = 100000; //53 secs with batch flush, OOME without
		//final int N = 250000; //137 secs with batch flush, OOME without
		
		Session s = openSession();
		Transaction t = s.beginTransaction();		
		for ( int i=0; i<N; i++ ) {
			DataPoint dp = new DataPoint();
			dp.setX( new BigDecimal(i * 0.1d) );
			dp.setY( new BigDecimal( Math.cos( dp.getX().doubleValue() ) ) );
			s.save(dp);
			if ( flushInBatches && i % 20 == 0 ) {
				s.flush();
				s.clear();
			}
		}
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		int i = 0;
		ScrollableResults sr = s.createQuery("from DataPoint dp order by dp.x asc").scroll(ScrollMode.FORWARD_ONLY);
		while ( sr.next() ) {
			DataPoint dp = (DataPoint) sr.get(0);
			dp.setDescription("done!");
			if ( flushInBatches && ++i % 20 == 0 ) {
				s.flush();
				s.clear();
			}
		}
		t.commit();
		s.close();
		System.out.println( System.currentTimeMillis() - start );
	}

	
	protected String[] getMappings() {
		return new String[] { "batch/DataPoint.hbm.xml" };
	}

	public static Test suite() {
		return new TestSuite(BatchTest.class);
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

}

