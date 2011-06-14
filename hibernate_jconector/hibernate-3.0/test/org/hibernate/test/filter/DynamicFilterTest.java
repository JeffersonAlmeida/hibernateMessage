// $Id: DynamicFilterTest.java,v 1.20 2005/02/17 08:03:36 oneovthafew Exp $
package org.hibernate.test.filter;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.EntityMode;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.Hibernate;
import org.hibernate.cache.CacheKey;
import org.hibernate.cache.entry.CollectionCacheEntry;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.criterion.Expression;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.test.TestCase;

/**
 * Implementation of DynamicFilterTest.
 *
 * @author Steve
 */
public class DynamicFilterTest extends TestCase {

	private Log log = LogFactory.getLog(DynamicFilterTest.class);


	public DynamicFilterTest(String testName) {
		super(testName);
	}

	public void testSecondLevelCachedCollectionsFiltering() {
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();

		// Force a collection into the second level cache, with its non-filtered elements
		Salesperson sp = ( Salesperson ) session.load( Salesperson.class, testData.steveId );
		Hibernate.initialize( sp.getOrders() );
		CollectionPersister persister = ( ( SessionFactoryImpl ) getSessions() )
		        .getCollectionPersister( Salesperson.class.getName() + ".orders" );
		assertTrue( "No cache for collection", persister.hasCache() );
		CollectionCacheEntry cachedData = ( CollectionCacheEntry ) persister.getCache().getCache()
		        .get( new CacheKey( testData.steveId, persister.getKeyType(), persister.getRole(), EntityMode.POJO ) );
		assertNotNull( "collection was not in cache", cachedData );

		session.close();

		session = openSession();
		session.enableFilter( "fulfilledOrders" ).setParameter( "asOfDate", testData.lastMonth.getTime() );
		sp = ( Salesperson ) session.createQuery("from Salesperson as s where s.id = :id")
		        .setLong( "id", testData.steveId.longValue() )
		        .uniqueResult();
		assertEquals( "Filtered-collection not bypassing 2L-cache", 1, sp.getOrders().size() );

		CollectionCacheEntry cachedData2 = ( CollectionCacheEntry ) persister.getCache().getCache()
		        .get( new CacheKey( testData.steveId, persister.getKeyType(), persister.getRole(), EntityMode.POJO ) );
		assertNotNull( "collection no longer in cache!", cachedData2 );
		assertSame( "Different cache values!", cachedData, cachedData2 );

		session.close();

		session = openSession();
		session.enableFilter( "fulfilledOrders" ).setParameter( "asOfDate", testData.lastMonth.getTime() );
		sp = ( Salesperson ) session.load( Salesperson.class, testData.steveId );
		assertEquals( "Filtered-collection not bypassing 2L-cache", 1, sp.getOrders().size() );

		session.close();

		// Finally, make sure that the original cached version did not get over-written
		session = openSession();
		sp = ( Salesperson ) session.load( Salesperson.class, testData.steveId );
		assertEquals( "Actual cached version got over-written", 2, sp.getOrders().size() );

		session.close();
		testData.release();
	}

	public void testCombinedClassAndCollectionFiltersEnabled() {
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter( "regionlist" ).setParameterList( "regions", new String[] { "LA", "APAC" } );
		session.enableFilter( "fulfilledOrders" ).setParameter( "asOfDate", testData.lastMonth.getTime() );

		// test retreival through hql with the collection as non-eager
		List salespersons = session.createQuery("select s from Salesperson as s").list();
		assertEquals("Incorrect salesperson count", 1, salespersons.size());
		Salesperson sp = ( Salesperson ) salespersons.get(0);
		assertEquals("Incorrect order count", 1, sp.getOrders().size());

		session.clear();

		// test retreival through hql with the collection join fetched
		salespersons = session.createQuery("select s from Salesperson as s left join fetch s.orders").list();
		assertEquals("Incorrect salesperson count", 1, salespersons.size());
		sp = ( Salesperson ) salespersons.get(0);
		assertEquals("Incorrect order count", 1, sp.getOrders().size());

		session.close();
		testData.release();
	}

	public void testHqlFilters() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// HQL test
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info("Starting HQL filter tests");
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter("region").setParameter("region", "APAC");

		session.enableFilter("effectiveDate")
				.setParameter("asOfDate", testData.lastMonth.getTime());

		log.info("HQL against Salesperson...");
		List results = session.createQuery("select s from Salesperson as s left join fetch s.orders").list();
		assertTrue("Incorrect filtered HQL result count [" + results.size() + "]", results.size() == 1);
		Salesperson result = (Salesperson) results.get(0);
		assertTrue("Incorrect collectionfilter count", result.getOrders().size() == 1);

		log.info("HQL against Product...");
		results = session.createQuery("from Product as p where p.stockNumber = ?").setInteger(0, 124).list();
		assertTrue(results.size() == 1);

		session.close();
		testData.release();
	}

	public void testCriteriaQueryFilters() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Criteria-query test
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info("Starting Criteria-query filter tests");
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter("region").setParameter("region", "APAC");

		session.enableFilter("fulfilledOrders")
				.setParameter("asOfDate", testData.lastMonth.getTime());

		session.enableFilter("effectiveDate")
				.setParameter("asOfDate", testData.lastMonth.getTime());

		log.info("Criteria query against Salesperson...");
		List salespersons = session.createCriteria(Salesperson.class)
				.setFetchMode("orders", FetchMode.JOIN)
				.list();
		assertEquals( "Incorrect salesperson count", 1, salespersons.size() );
		assertEquals( "Incorrect order count", 1, ( (Salesperson) salespersons.get(0) ).getOrders().size() );

		log.info("Criteria query against Product...");
		List products = session.createCriteria(Product.class)
		        .add( Expression.eq("stockNumber", new Integer(124) ) )
				.list();
		assertEquals( "Incorrect product count", 1, products.size() );

		session.close();
		testData.release();
	}

	public void testGetFilters() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Get() test
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info("Starting get() filter tests (eager assoc. fetching).");
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter("region").setParameter("region", "APAC");

		log.info("Performing get()...");
		Salesperson salesperson = (Salesperson) session.get(Salesperson.class, testData.steveId);
		assertNotNull(salesperson);
		assertEquals("Incorrect order count", 1, salesperson.getOrders().size());

		session.close();
		testData.release();
	}

	public void testOneToManyFilters() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// one-to-many loading tests
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info("Starting one-to-many collection loader filter tests.");
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter("seniorSalespersons")
		        .setParameter( "asOfDate", testData.lastMonth.getTime() );

		log.info("Performing load of Department...");
		Department department = (Department) session.load(Department.class, testData.deptId);
		Set salespersons = department.getSalespersons();
		assertEquals("Incorrect salesperson count", 1, salespersons.size());

		session.close();
		testData.release();
	}

	public void testManyToManyFilters() {
		// todo: add once the ability to filter on the association table is added
	}

	public void testInStyleFilterParameter() {
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// one-to-many loading tests
		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		log.info("Starting one-to-many collection loader filter tests.");
		TestData testData = new TestData();
		testData.prepare();

		Session session = openSession();
		session.enableFilter("regionlist")
		        .setParameterList( "regions", new String[] { "LA", "APAC" } );

		log.debug("Performing query of Salespersons");
		List salespersons = session.createQuery("from Salesperson").list();
		assertEquals("Incorrect salesperson count", 1, salespersons.size());

		session.close();
		testData.release();
	}

    /**
     * Define the mappings needed for these tests.
     *
     * @return Mappings for these tests.
     */
	protected String[] getMappings() {
		return new String[] {
			"filter/defs.hbm.xml",
			"filter/LineItem.hbm.xml",
			"filter/Order.hbm.xml",
			"filter/Product.hbm.xml",
			"filter/Salesperson.hbm.xml",
			"filter/Department.hbm.xml"
		};
	}


	public static Test suite() {
		return new TestSuite(DynamicFilterTest.class);
	}

	private class TestData {
		private Long steveId;
		private Long deptId;
		private Calendar lastMonth;
		private Calendar nextMonth;
		private Calendar sixMonthsAgo;
		private Calendar fourMonthsAgo;

		private List entitiesToCleanUp = new ArrayList();

		private void prepare() {
			Session session = openSession();
			Transaction transaction = session.beginTransaction();

			lastMonth = new GregorianCalendar();
			lastMonth.add(Calendar.MONTH, -1);

			nextMonth = new GregorianCalendar();
			nextMonth.add(Calendar.MONTH, 1);

			sixMonthsAgo = new GregorianCalendar();
			sixMonthsAgo.add(Calendar.MONTH, -6);

			fourMonthsAgo = new GregorianCalendar();
			fourMonthsAgo.add(Calendar.MONTH, -4);

			Department dept = new Department();
			dept.setName("Sales");

			session.save(dept);
			deptId = dept.getId();
			entitiesToCleanUp.add(dept);

			Salesperson steve = new Salesperson();
			steve.setName("steve");
			steve.setRegion("APAC");
			steve.setHireDate( sixMonthsAgo.getTime() );

			steve.setDepartment(dept);
			dept.getSalespersons().add(steve);

			Salesperson max = new Salesperson();
			max.setName("max");
			max.setRegion("EMEA");
			max.setHireDate( nextMonth.getTime() );

			max.setDepartment(dept);
			dept.getSalespersons().add(max);

			session.save(steve);
			session.save(max);
			entitiesToCleanUp.add(steve);
			entitiesToCleanUp.add(max);

			steveId = steve.getId();

			Product product1 = new Product();
			product1.setName("Acme Hair Gel");
			product1.setStockNumber(123);
			product1.setEffectiveStartDate( lastMonth.getTime() );
			product1.setEffectiveEndDate( nextMonth.getTime() );

			session.save(product1);
			entitiesToCleanUp.add(product1);

			Order order1 = new Order();
			order1.setBuyer("gavin");
			order1.setRegion("APAC");
			order1.setPlacementDate( sixMonthsAgo.getTime() );
			order1.setFulfillmentDate( fourMonthsAgo.getTime() );
			order1.setSalesperson(steve);
			order1.addLineItem(product1, 500);

			session.save(order1);
			entitiesToCleanUp.add(order1);

			Product product2 = new Product();
			product2.setName("Acme Super-Duper DTO Factory");
			product2.setStockNumber(124);
			product2.setEffectiveStartDate( sixMonthsAgo.getTime() );
			product2.setEffectiveEndDate( new Date() );

			session.save(product2);
			entitiesToCleanUp.add(product2);

			Order order2 = new Order();
			order2.setBuyer("christian");
			order2.setRegion("EMEA");
			order2.setPlacementDate( lastMonth.getTime() );
			order2.setSalesperson(steve);
			order2.addLineItem(product2, -1);

			session.save(order2);
			entitiesToCleanUp.add(order2);

			transaction.commit();
			session.close();
		}

		private void release() {
			Session session = openSession();
			Transaction transaction = session.beginTransaction();

			Iterator itr = entitiesToCleanUp.iterator();
			while( itr.hasNext() ) {
				session.delete( itr.next() );
			}

			transaction.commit();
			session.close();
		}
	}
}
