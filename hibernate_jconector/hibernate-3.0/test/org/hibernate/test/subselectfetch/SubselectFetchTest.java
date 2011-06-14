//$Id: SubselectFetchTest.java,v 1.1 2005/03/17 05:56:00 oneovthafew Exp $
package org.hibernate.test.subselectfetch;

import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Property;
import org.hibernate.test.TestCase;

/**
 * @author Gavin King
 */
public class SubselectFetchTest extends TestCase {
	
	public SubselectFetchTest(String str) {
		super(str);
	}
	
	public void testManyToManyCriteriaJoin() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Parent p = new Parent("foo");
		p.getChildren().add( new Child("foo1") );
		p.getChildren().add( new Child("foo2") );
		Parent q = new Parent("bar");
		q.getChildren().add( new Child("bar1") );
		q.getChildren().add( new Child("bar2") );
		q.getMoreChildren().addAll( p.getChildren() );
		s.persist(p); 
		s.persist(q);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		
		List parents = s.createCriteria(Parent.class)
			.createCriteria("moreChildren")
			.createCriteria("friends")
			.addOrder( Order.desc("name") )
			.list();

		parents = s.createCriteria(Parent.class)
			.setFetchMode("moreChildren", FetchMode.JOIN)
			.setFetchMode("moreChildren.friends", FetchMode.JOIN)
			.addOrder( Order.desc("name") )
			.list();

		s.delete( parents.get(0) );
		s.delete( parents.get(1) );

		t.commit();
		s.close();
	}
	
	public void testSubselectFetch() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Parent p = new Parent("foo");
		p.getChildren().add( new Child("foo1") );
		p.getChildren().add( new Child("foo2") );
		Parent q = new Parent("bar");
		q.getChildren().add( new Child("bar1") );
		q.getChildren().add( new Child("bar2") );
		q.getMoreChildren().addAll( p.getChildren() );
		s.persist(p); 
		s.persist(q);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		
		List parents = s.createCriteria(Parent.class)
			.add( Property.forName("name").between("bar", "foo") )
			.addOrder( Order.desc("name") )
			.list();
		p = (Parent) parents.get(0);
		q = (Parent) parents.get(1);

		assertFalse( Hibernate.isInitialized( p.getChildren() ) );
		assertFalse( Hibernate.isInitialized( q.getChildren() ) );

		assertEquals( p.getChildren().size(), 2 );
				
		assertTrue( Hibernate.isInitialized( q.getChildren() ) );
		
		assertEquals( q.getChildren().size(), 2 );
		
		assertFalse( Hibernate.isInitialized( p.getMoreChildren() ) );
		assertFalse( Hibernate.isInitialized( q.getMoreChildren() ) );

		assertEquals( p.getMoreChildren().size(), 0 );
		
		assertTrue( Hibernate.isInitialized( q.getMoreChildren() ) );
	
		assertEquals( q.getMoreChildren().size(), 2 );
		
		Child c = (Child) p.getChildren().get(0);
		c.getFriends().size();

		s.delete(p);
		s.delete(q);		

		t.commit();
		s.close();
	}
	
	protected String[] getMappings() {
		return new String[] { "subselectfetch/ParentChild.hbm.xml" };
	}

	public static Test suite() {
		return new TestSuite(SubselectFetchTest.class);
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

}

