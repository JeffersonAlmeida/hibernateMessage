// $Id: BulkManipulationTest.java,v 1.7 2005/03/29 03:33:48 oneovthafew Exp $
package org.hibernate.test.hql;

import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.hibernate.test.TestCase;

import java.util.List;


/**
 * Tests parsing of bulk UPDATE/DELETE statements through the new AST parser.
 *
 * @author Steve Ebersole
 */
public class BulkManipulationTest extends TestCase {

	public BulkManipulationTest(String name) {
		super( name );
	}

	protected String[] getMappings() {
		return new String[] {
			"hql/Animal.hbm.xml",
			"cid/Customer.hbm.xml",
			"cid/Order.hbm.xml",
			"cid/LineItem.hbm.xml",
			"cid/Product.hbm.xml"
		};
	}

	public void testSimpleUpdateOnAnimal() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		// create some test data...
		TestData data = new TestData();
		data.prepare( s );

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		try {
			int count = s.createQuery( "update Animal set description = description where description = :desc" )
					.setString( "desc", data.frog.getDescription() )
					.executeUpdate();

			assertEquals( "Incorrect entity-updated count", 1, count );

			count = s.createQuery( "update Animal set description = :newDesc where description = :desc" )
					.setString( "desc", data.polliwog.getDescription() )
					.setString( "newDesc", "Tadpole" )
					.executeUpdate();

			assertEquals( "Incorrect entity-updated count", 1, count );

			Animal tadpole = ( Animal ) s.load( Animal.class, data.polliwog.getId() );
			assertEquals( "Update did not take effect", "Tadpole", tadpole.getDescription() );
		}
		finally {
			data.cleanup( s );
		}
		t.commit();
		s.close();
	}

	public void testSimpleDeleteOnAnimal() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		TestData data = new TestData();
		data.prepare( s );

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		try {
			int count = s.createQuery( "delete from Animal where id = :id" )
					.setLong( "id", data.polliwog.getId().longValue() )
					.executeUpdate();
			assertEquals( "Incorrect delete count", 1, count );

			count = s.createQuery( "delete Animal" ).executeUpdate();
			assertEquals( "Incorrect delete count", 3, count );

			List list = s.createQuery( "select a from Animal as a" ).list();
			assertTrue( "table not empty", list.isEmpty() );
		}
		finally {
			data.cleanup( s );
		}
		t.commit();
		s.close();
	}

	public void testUpdateOnDiscriminatorSubclass() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		TestData data = new TestData();
		data.prepare( s );

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		try {
            int count = s.createQuery( "update PettingZoo set name = name" ).executeUpdate();
			assertEquals( "Incorrect discrim subclass update count", 1, count );

			t.rollback();
			t = s.beginTransaction();

			count = s.createQuery( "update PettingZoo set name = name where id = :id" )
			        .setLong( "id", data.pettingZoo.getId().longValue() )
			        .executeUpdate();
			assertEquals( "Incorrect discrim subclass update count", 1, count );

			t.rollback();
			t = s.beginTransaction();

			count = s.createQuery( "update Zoo set name = name" ).executeUpdate();
			assertEquals( "Incorrect discrim subclass update count", 2, count );

			t.rollback();
			t = s.beginTransaction();

			count = s.createQuery( "update Zoo set name = name where id = :id" )
					.setLong( "id", data.zoo.getId().longValue() )
					.executeUpdate();
			assertEquals( "Incorrect discrim subclass update count", 1, count );
		}
		finally {
			data.cleanup( s );
		}
		t.commit();
		s.close();

	}

	// TODO : another failing scenario to consider is update/delete against classes using <join/>

	// tests which currently should fail, but for which we need to add support
	public void testDeleteOnMammalFails() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		try {
			s.createQuery( "delete Mammal" ).executeUpdate();
			fail( "delete against subclass did not error" );
		}
		catch( Throwable ignore ) {
			// should be OK;
			// TODO : do we need a specific exception type for this?  Currently QueryException
		}

		t.rollback();
		s.close();
	}

	public void testUpdateOnMammalFails() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		try {
			s.createQuery( "update Mammal set description = description" ).executeUpdate();
			fail( "uopdate against subclass did not error" );
		}
		catch( Throwable ignore ) {
			// should be OK;
			// TODO : do we need a specific exception type for this?  Currently QueryException
		}

		t.rollback();
		s.close();
	}

	private static class TestData {

		private Animal polliwog;
		private Animal catepillar;
		private Animal frog;
		private Animal butterfly;

		private Zoo zoo;
		private Zoo pettingZoo;

		private void prepare(Session s) {
			polliwog = new Animal();
			polliwog.setBodyWeight( 12 );
			polliwog.setDescription( "Polliwog" );

			catepillar = new Animal();
			catepillar.setBodyWeight( 10 );
			catepillar.setDescription( "Catepillar" );

			frog = new Animal();
			frog.setBodyWeight( 34 );
			frog.setDescription( "Frog" );

			polliwog.setFather( frog );
			frog.addOffspring( polliwog );

			butterfly = new Animal();
			butterfly.setBodyWeight( 9 );
			butterfly.setDescription( "Butterfly" );

			catepillar.setMother( butterfly );
			butterfly.addOffspring( catepillar );

			s.save( frog );
			s.save( polliwog );
			s.save( butterfly );
			s.save( catepillar );

			zoo = new Zoo();
			zoo.setName( "Zoo" );
			pettingZoo = new PettingZoo();
			pettingZoo.setName( "Petting Zoo" );

			s.save( zoo );
			s.save( pettingZoo );
		}

		private void cleanup(Session s) {
			List list = s.createQuery( "from Animal" ).list();
			for ( int i=0; i<list.size(); i++ ) {
				s.delete( list.get(i) );
			}
			s.delete( zoo );
			s.delete( pettingZoo );
		}
	}
}
