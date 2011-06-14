//$Id: TypeParameterTest.java,v 1.3 2005/02/12 07:27:31 steveebersole Exp $
package org.hibernate.test.typeparameters;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.hibernate.classic.Session;
import org.hibernate.Transaction;
import org.hibernate.test.TestCase;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Test for parameterizable types.
 * 
 * @author Michael Gloegl
 */
public class TypeParameterTest extends TestCase {

	public TypeParameterTest(String name) {
		super(name);
	}

	public void setUp() throws Exception {
		super.setUp();
	}

	public void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * @see org.hibernate.test.TestCase#getMappings()
	 */
	protected String[] getMappings() {
		return new String[] {
				"typeparameters/Typedef.hbm.xml", 
				"typeparameters/Widget.hbm.xml"
		};
	}

	public void testSave() throws Exception {
		deleteData();

		Session s = openSession();

		Transaction t = s.beginTransaction();

		Widget obj = new Widget();
		obj.setValueThree(5);

		Integer id = (Integer) s.save(obj);

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();

		Connection connection = s.connection();
		PreparedStatement statement = connection.prepareStatement("SELECT * FROM STRANGE_TYPED_OBJECT WHERE ID=?");
		statement.setInt(1, id.intValue());
		ResultSet resultSet = statement.executeQuery();

		TestCase.assertTrue("A row should have been returned", resultSet.next());
		TestCase.assertTrue("Default value should have been mapped to null", resultSet.getObject("VALUE_ONE") == null);
		TestCase.assertTrue("Default value should have been mapped to null", resultSet.getObject("VALUE_TWO") == null);
		TestCase.assertEquals("Non-Default value should not be changed", resultSet.getInt("VALUE_THREE"), 5);
		TestCase.assertTrue("Default value should have been mapped to null", resultSet.getObject("VALUE_FOUR") == null);

		t.commit();
		s.close();
	}

	public void testLoading() throws Exception {
		initData();

		Session s = openSession();
		Transaction t = s.beginTransaction();

		Widget obj = (Widget) s.createQuery("from Widget o where o.string = :string").setString("string", "all-normal").uniqueResult();
		TestCase.assertEquals("Non-Default value incorrectly loaded", obj.getValueOne(), 7);
		TestCase.assertEquals("Non-Default value incorrectly loaded", obj.getValueTwo(), 8);
		TestCase.assertEquals("Non-Default value incorrectly loaded", obj.getValueThree(), 9);
		TestCase.assertEquals("Non-Default value incorrectly loaded", obj.getValueFour(), 10);

		obj = (Widget) s.createQuery("from Widget o where o.string = :string").setString("string", "all-default").uniqueResult();
		TestCase.assertEquals("Default value incorrectly loaded", obj.getValueOne(), 1);
		TestCase.assertEquals("Default value incorrectly loaded", obj.getValueTwo(), 2);
		TestCase.assertEquals("Default value incorrectly loaded", obj.getValueThree(), -1);
		TestCase.assertEquals("Default value incorrectly loaded", obj.getValueFour(), -5);

		t.commit();
		s.close();
	}

	private void initData() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Widget obj = new Widget();
		obj.setValueOne(7);
		obj.setValueTwo(8);
		obj.setValueThree(9);
		obj.setValueFour(10);
		obj.setString("all-normal");
		s.save(obj);

		obj = new Widget();
		obj.setValueOne(1);
		obj.setValueTwo(2);
		obj.setValueThree(-1);
		obj.setValueFour(-5);
		obj.setString("all-default");
		s.save(obj);

		t.commit();
		s.close();
	}

	private void deleteData() throws Exception {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.delete("from Widget");
		t.commit();
		s.close();
	}

	public static Test suite() {
		return new TestSuite(TypeParameterTest.class);
	}

	public static void main(String[] args) throws Exception {
		TestRunner.run(suite());
	}
}