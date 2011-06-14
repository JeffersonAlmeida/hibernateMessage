//$Id: IdBagTest.java,v 1.2 2005/03/22 15:32:03 oneovthafew Exp $
package org.hibernate.test.idbag;

import java.sql.SQLException;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.TestCase;

/**
 * @author Gavin King
 */
public class IdBagTest extends TestCase {
	
	public IdBagTest(String str) {
		super(str);
	}

	public void testUpdateIdBag() throws HibernateException, SQLException {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User gavin = new User("gavin");
		Group admins = new Group("admins");
		Group plebs = new Group("plebs");
		Group moderators = new Group("moderators");
		Group banned = new Group("banned");
		gavin.getGroups().add(plebs);
		//gavin.getGroups().add(moderators);
		s.persist(gavin);
		s.persist(plebs);
		s.persist(admins);
		s.persist(moderators);
		s.persist(banned);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		gavin = (User) s.createCriteria(User.class).uniqueResult();
		admins = (Group) s.load(Group.class, "admins");
		plebs = (Group) s.load(Group.class, "plebs");
		banned = (Group) s.load(Group.class, "banned");
		gavin.getGroups().add(admins);
		gavin.getGroups().remove(plebs);
		//gavin.getGroups().add(banned);

		s.delete(plebs);
		s.delete(banned);
		s.delete(moderators);
		s.delete(admins);
		s.delete(gavin);
		
		t.commit();
		s.close();		
	}
	
	public void testJoin() throws HibernateException, SQLException {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		User gavin = new User("gavin");
		Group admins = new Group("admins");
		Group plebs = new Group("plebs");
		gavin.getGroups().add(plebs);
		gavin.getGroups().add(admins);
		s.persist(gavin);
		s.persist(plebs);
		s.persist(admins);
		
		List l = s.createQuery("from User u join u.groups g").list();
		assertEquals( l.size(), 2 );
		s.clear();
		
		gavin = (User) s.createQuery("from User u join fetch u.groups").uniqueResult();
		assertTrue( Hibernate.isInitialized( gavin.getGroups() ) );
		assertEquals( gavin.getGroups().size(), 2 );
		assertEquals( ( (Group) gavin.getGroups().get(0) ).getName(), "admins" );
		
		s.delete( gavin.getGroups().get(0) );
		s.delete( gavin.getGroups().get(1) );
		s.delete(gavin);
		
		t.commit();
		s.close();		
	}
	
	protected String[] getMappings() {
		return new String[] { "idbag/UserGroup.hbm.xml" };
	}

	public static Test suite() {
		return new TestSuite(IdBagTest.class);
	}

}

