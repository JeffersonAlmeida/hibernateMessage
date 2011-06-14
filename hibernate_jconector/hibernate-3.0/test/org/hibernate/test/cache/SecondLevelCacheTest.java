//$Id: SecondLevelCacheTest.java,v 1.5 2005/02/21 14:40:57 oneovthafew Exp $
package org.hibernate.test.cache;

import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cache.TreeCacheProvider;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.test.TestCase;

/**
 * @author Gavin King
 */
public class SecondLevelCacheTest extends TestCase {
	
	public SecondLevelCacheTest(String str) {
		super(str);
	}
	
	public void testQueryCacheInvalidation() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Item i = new Item();
		i.setName("widget");
		i.setDescription("A really top-quality, full-featured widget.");
		s.persist(i);
		t.commit();
		s.close();
		
		SecondLevelCacheStatistics slcs = s.getSessionFactory()
			.getStatistics()
			.getSecondLevelCacheStatistics( Item.class.getName() );
		
		assertEquals( slcs.getPutCount(), 1 );
		assertEquals( slcs.getElementCountInMemory(), 1 );
		assertEquals( slcs.getEntries().size(), 1 );
		
		s = openSession();
		t = s.beginTransaction();
		i = (Item) s.get( Item.class, i.getId() );
		
		assertEquals( slcs.getHitCount(), 1 );
		assertEquals( slcs.getMissCount(), 0 );
		
		i.setDescription("A bog standard item");
		
		t.commit();
		s.close();

		assertEquals( slcs.getPutCount(), 2 );
		
		Map map = (Map) slcs.getEntries().get( i.getId() );
		assertTrue( map.get("description").equals("A bog standard item") );
		assertTrue( map.get("name").equals("widget") );

	}

	
	protected String[] getMappings() {
		return new String[] { "cache/Item.hbm.xml" };
	}

	public static Test suite() {
		return new TestSuite(SecondLevelCacheTest.class);
	}

	protected void configure(Configuration cfg) {
		super.configure( cfg );    //todo: implement overriden method body
		cfg.setProperty( Environment.CACHE_REGION_PREFIX, "" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
		cfg.setProperty( Environment.USE_STRUCTURED_CACHE, "true" );
		cfg.setProperty( Environment.CACHE_PROVIDER, TreeCacheProvider.class.getName() );
	}

	public String getCacheConcurrencyStrategy() {
		return "transactional";
	}
}

