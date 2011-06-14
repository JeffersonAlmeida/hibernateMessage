// $Id: ASTParserLoadingTest.java,v 1.14 2005/02/25 04:46:37 oneovthafew Exp $
package org.hibernate.test.hql;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Environment;
import org.hibernate.test.TestCase;
import org.hibernate.test.cid.Customer;
import org.hibernate.test.cid.LineItem;
import org.hibernate.test.cid.Order;
import org.hibernate.test.cid.Product;

/**
 * Tests the integration of the new AST parser into the loading of query results using
 * the Hibernate persisters and loaders.
 *
 * @author Steve
 */
public class ASTParserLoadingTest extends TestCase {

	public ASTParserLoadingTest(String name) {
		super( name );
	}

	private List createdAnimalIds = new ArrayList();

	protected String[] getMappings() {
		// Make sure we are using the new AST parser translator...
		System.setProperty( Environment.QUERY_TRANSLATOR, "org.hibernate.hql.ast.ASTQueryTranslatorFactory" );
		return new String[]{
			"hql/Animal.hbm.xml",
			"batchfetch/ProductLine.hbm.xml",
			"cid/Customer.hbm.xml",
			"cid/Order.hbm.xml",
			"cid/LineItem.hbm.xml",
			"cid/Product.hbm.xml"
		};
	}

	public void testAggregation() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Human h = new Human();
		h.setBodyWeight( (float) 74.0 );
		h.setHeight(120.5);
		h.setDescription("Me");
		h.setName( new Name("Gavin", 'A', "King") );
		h.setNickName("Oney");
		s.persist(h);
		Float sum = (Float) s.createQuery("select sum(h.bodyWeight) from Human h").uniqueResult();
		Double avg = (Double) s.createQuery("select avg(h.height) from Human h").uniqueResult();
		assertEquals(sum.floatValue(), 74.0, 0.01);
		assertEquals(avg.doubleValue(), 120.5, 0.01);
		s.delete(h);
		t.commit();
		s.close();
	}
	
	public void testImplicitPolymorphism() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Product product = new Product();
		product.setDescription( "My Product" );
		product.setNumberAvailable( 10 );
		product.setPrice( new BigDecimal( 123 ) );
		product.setProductId( "4321" );
		s.save( product );

		List list = s.createQuery("from java.lang.Comparable").list();
		assertEquals( list.size(), 0 );
		
		list = s.createQuery("from java.lang.Object").list();
		assertEquals( list.size(), 1 );
		
		s.delete(product);
		
		list = s.createQuery("from java.lang.Object").list();
		assertEquals( list.size(), 0 );
		
		t.commit();
		s.close();
	}

	public void testOneToManyFilter() throws Throwable {
		Session session = openSession();
		Transaction txn = session.beginTransaction();

		Product product = new Product();
		product.setDescription( "My Product" );
		product.setNumberAvailable( 10 );
		product.setPrice( new BigDecimal( 123 ) );
		product.setProductId( "4321" );
		session.save( product );

		Customer customer = new Customer();
		customer.setCustomerId( "123456789" );
		customer.setName( "My customer" );
		customer.setAddress( "somewhere" );
		session.save( customer );

		Order order = customer.generateNewOrder( new BigDecimal( 1234 ) );
		session.save( order );

		LineItem li = order.generateLineItem( product, 5 );
		session.save( li );

		session.flush();

		assertEquals( session.createFilter( customer.getOrders(), "" ).list().size(), 1 );

		assertEquals( session.createFilter( order.getLineItems(), "" ).list().size(), 1 );
		assertEquals( session.createFilter( order.getLineItems(), "where this.quantity > :quantity" ).setInteger( "quantity", 5 ).list().size(), 0 );
		
		session.delete(li);
		session.delete(order);
		session.delete(product);
		session.delete(customer);
		txn.commit();
		session.close();
	}

	public void testManyToManyFilter() throws Throwable {
		Session session = openSession();
		Transaction txn = session.beginTransaction();

		Human human = new Human();
		human.setName( new Name() );
		human.getName().setFirst( "Steve" );
		human.getName().setInitial( 'L' );
		human.getName().setLast( "Ebersole" );
		session.save( human );

		Human friend = new Human();
		friend.setName( new Name() );
		friend.getName().setFirst( "John" );
		friend.getName().setInitial( 'Q' );
		friend.getName().setLast( "Doe" );
		friend.setBodyWeight( 11.0f );
		session.save( friend );

		human.setFriends( new ArrayList() );
		friend.setFriends( new ArrayList() );
		human.getFriends().add( friend );
		friend.getFriends().add( human );

		session.flush();

		assertEquals( session.createFilter( human.getFriends(), "" ).list().size(), 1 );
		assertEquals( session.createFilter( human.getFriends(), "where this.bodyWeight > ?" ).setFloat( 0, 10f ).list().size(), 1 );
		assertEquals( session.createFilter( human.getFriends(), "where this.bodyWeight < ?" ).setFloat( 0, 10f ).list().size(), 0 );
		
		session.delete(human);
		session.delete(friend);
		
		txn.commit();
		session.close();
	}

	private void createTestBaseData() {
		Session session = openSession();
		Transaction txn = session.beginTransaction();

		Mammal m1 = new Mammal();
		m1.setBodyWeight( 11f );
		m1.setDescription( "Mammal #1" );

		session.save( m1 );

		Mammal m2 = new Mammal();
		m2.setBodyWeight( 9f );
		m2.setDescription( "Mammal #2" );
		m2.setMother( m1 );

		session.save( m2 );

		txn.commit();
		session.close();

		createdAnimalIds.add( m1.getId() );
		createdAnimalIds.add( m2.getId() );
	}

	private void destroyTestBaseData() {
		Session session = openSession();
		Transaction txn = session.beginTransaction();

		for ( int i = 0; i < createdAnimalIds.size(); i++ ) {
			Animal animal = ( Animal ) session.load( Animal.class, ( Long ) createdAnimalIds.get( i ) );
			session.delete( animal );
		}

		txn.commit();
		session.close();
	}

	public void testFromOnly() throws Exception {

		createTestBaseData();

		Session session = openSession();

		List results = session.createQuery( "from Animal" ).list();
		assertEquals( "Incorrect result size", 2, results.size() );
		assertTrue( "Incorrect result return type", results.get( 0 ) instanceof Animal );

		session.close();

		destroyTestBaseData();
	}

	public void testSimpleSelect() throws Exception {

		createTestBaseData();

		Session session = openSession();

		List results = session.createQuery( "select a from Animal as a" ).list();
		assertEquals( "Incorrect result size", 2, results.size() );
		assertTrue( "Incorrect result return type", results.get( 0 ) instanceof Animal );

		session.close();

		destroyTestBaseData();
	}

	public void testEntityPropertySelect() throws Exception {

		createTestBaseData();

		Session session = openSession();

		List results = session.createQuery( "select a.mother from Animal as a" ).list();
//		assertEquals("Incorrect result size", 2, results.size());
		assertTrue( "Incorrect result return type", results.get( 0 ) instanceof Animal );

		session.close();

		destroyTestBaseData();
	}

	public void testWhere() throws Exception {

		createTestBaseData();

		Session session = openSession();
		List results = null;

		results = session.createQuery( "from Animal an where an.bodyWeight > 10" ).list();
		assertEquals( "Incorrect result size", 1, results.size() );

		results = session.createQuery( "from Animal an where not an.bodyWeight > 10" ).list();
		assertEquals( "Incorrect result size", 1, results.size() );

		results = session.createQuery( "from Animal an where an.bodyWeight between 0 and 10" ).list();
		assertEquals( "Incorrect result size", 1, results.size() );

		results = session.createQuery( "from Animal an where an.bodyWeight not between 0 and 10" ).list();
		assertEquals( "Incorrect result size", 1, results.size() );

		results = session.createQuery( "from Animal an where sqrt(an.bodyWeight)/2 > 10" ).list();
		assertEquals( "Incorrect result size", 0, results.size() );

		results = session.createQuery( "from Animal an where (an.bodyWeight > 10 and an.bodyWeight < 100) or an.bodyWeight is null" ).list();
		assertEquals( "Incorrect result size", 1, results.size() );

		session.close();

		destroyTestBaseData();
	}

	public void testEntityFetching() throws Exception {

		createTestBaseData();

		Session session = openSession();

		List results = session.createQuery( "from Animal an join fetch an.mother" ).list();
		assertEquals( "Incorrect result size", 1, results.size() );
		assertTrue( "Incorrect result return type", results.get( 0 ) instanceof Animal );
		Animal mother = ( ( Animal ) results.get( 0 ) ).getMother();
		assertTrue( "fetch uninitialized", mother != null && Hibernate.isInitialized( mother ) );

		results = session.createQuery( "select an from Animal an join fetch an.mother" ).list();
		assertEquals( "Incorrect result size", 1, results.size() );
		assertTrue( "Incorrect result return type", results.get( 0 ) instanceof Animal );
		mother = ( ( Animal ) results.get( 0 ) ).getMother();
		assertTrue( "fetch uninitialized", mother != null && Hibernate.isInitialized( mother ) );

		session.close();

		destroyTestBaseData();
	}

	public void testCollectionFetching() throws Exception {

		createTestBaseData();

		Session session = openSession();
		List results = session.createQuery( "from Animal an join fetch an.offspring" ).list();
		assertEquals( "Incorrect result size", 1, results.size() );
		assertTrue( "Incorrect result return type", results.get( 0 ) instanceof Animal );
		Collection os = ( ( Animal ) results.get( 0 ) ).getOffspring();
		assertTrue( "fetch uninitialized", os != null && Hibernate.isInitialized( os ) && os.size() == 1 );

		results = session.createQuery( "select an from Animal an join fetch an.offspring" ).list();
		assertEquals( "Incorrect result size", 1, results.size() );
		assertTrue( "Incorrect result return type", results.get( 0 ) instanceof Animal );
		os = ( ( Animal ) results.get( 0 ) ).getOffspring();
		assertTrue( "fetch uninitialized", os != null && Hibernate.isInitialized( os ) && os.size() == 1 );

		session.close();

		destroyTestBaseData();
	}

	public void testProjectionQueries() throws Exception {

		createTestBaseData();

		Session session = openSession();

		List results = session.createQuery( "select an.mother.id, max(an.bodyWeight) from Animal an group by an.mother.id" ).list();
		// mysql returns nulls in this group by
		assertEquals( "Incorrect result size", 2, results.size() );
		assertTrue( "Incorrect return type", results.get( 0 ) instanceof Object[] );
		assertEquals( "Incorrect return dimensions", 2, ( ( Object[] ) results.get( 0 ) ).length );

		session.close();

		destroyTestBaseData();

	}
	
	public void testStandardFunctions() throws Exception {
		Session session = openSession();
		Product p = new Product();
		p.setDescription("a product");
		p.setPrice( new BigDecimal(1.0) );
		p.setProductId("abc123");
		session.persist(p);
		Object[] result = (Object[]) session
			.createQuery("select current_time(), current_date(), current_timestamp() from Product")
			.uniqueResult();
		assertTrue( result[0] instanceof Time );
		assertTrue( result[1] instanceof Date );
		assertTrue( result[2] instanceof Timestamp );
		assertNotNull( result[0] );
		assertNotNull( result[1] );
		assertNotNull( result[2] );
		session.delete(p);
		session.close();
		
	}

	public void testDynamicInstantiationQueries() throws Exception {

		createTestBaseData();

		Session session = openSession();

		List results = session.createQuery( "select new Animal(an.description, an.bodyWeight) from Animal an" ).list();
		assertEquals( "Incorrect result size", 2, results.size() );
		assertTrue( "Incorrect return type", results.get( 0 ) instanceof Animal );

		session.close();

		destroyTestBaseData();
	}

	public static Test suite() {
		TestSuite suite = new TestSuite( ASTParserLoadingTest.class );
		return suite;
	}

}
