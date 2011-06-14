// $Id: MultiRepresentationTest.java,v 1.7 2005/03/06 16:31:23 oneovthafew Exp $
package org.hibernate.test.entity;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.dom4j.Element;
import org.dom4j.DocumentFactory;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.impl.SessionImpl;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.classic.Session;
import org.hibernate.test.TestCase;

/**
 * Implementation of MultiRepresentationTest.
 *
 * @author Steve Ebersole
 */
public class MultiRepresentationTest extends TestCase {

	Long stockId;
	Long valId;

	public MultiRepresentationTest(String name) {
		super(name);
	}

	public void testPojoRetreival() {
		Session session = openSession();
		Transaction txn = session.beginTransaction();

		prepareTestData( session );

		Stock stock = ( Stock ) session.get( Stock.class, new Long(1) );
		assertEquals( "Something wrong!", new Long(1), stock.getId() );

		txn.rollback();
		session.close();
	}

	public void testDom4jRetreival() {
		Session session = openSession();
		Transaction txn = session.beginTransaction();
		org.hibernate.Session dom4j = session.getSession( EntityMode.DOM4J );

		prepareTestData( session );

		Object rtn = dom4j.get( Stock.class.getName(), stockId );
		Element element = ( Element ) rtn;

		assertEquals( "Something wrong!", stockId, Long.valueOf( element.attributeValue("id") ) );

		System.out.println("**** XML: ****************************************************");
		prettyPrint( element );
		System.out.println("**************************************************************");

		Element currVal = element.element( "currentValuation");

		System.out.println("**** XML: ****************************************************");
		prettyPrint( currVal );
		System.out.println("**************************************************************");


		txn.rollback();
		session.close();
	}

	public void testDom4jSave() {
		Session pojos = openSession();
		Transaction txn = pojos.beginTransaction();

		prepareTestData( pojos );

		org.hibernate.Session dom4j = pojos.getSession( EntityMode.DOM4J );

		Element stock = DocumentFactory.getInstance().createElement( "stock" );
		stock.addElement( "tradeSymbol" ).setText( "IBM" );

		Element val = stock.addElement( "currentValuation" ).addElement( "valuation" );
		val.appendContent( stock );
		val.addElement( "valuationDate" ).setText( new java.util.Date().toString() );
		val.addElement( "value" ).setText( "121.00" );

		dom4j.save( Stock.class.getName(), stock );
		dom4j.flush();

		txn.rollback();

		pojos.close();

		assertTrue( !pojos.isOpen() );
		assertTrue( !dom4j.isOpen() );

		prettyPrint( stock );
	}

	public void testDom4jHQL() {
		Session session = openSession();
		Transaction txn = session.beginTransaction();
		org.hibernate.Session dom4j = session.getSession( EntityMode.DOM4J );

		prepareTestData( session );

		List result = dom4j.createQuery( "from Stock" ).list();

		assertEquals( "Incorrect result size", 1, result.size() );
		Element element = ( Element ) result.get( 0 );
		assertEquals( "Something wrong!", stockId, Long.valueOf( element.attributeValue("id") ) );

		System.out.println("**** XML: ****************************************************");
		prettyPrint( element );
		System.out.println("**************************************************************");

		txn.rollback();
		session.close();
	}

	private void prepareTestData(Session session) throws HibernateException {
		try {
			IdentifierGenerator stockIdGen = ( (SessionFactoryImplementor) session.getSessionFactory() )
			        .getIdentifierGenerator( Stock.class.getName() );
			IdentifierGenerator valIdGen = ( (SessionFactoryImplementor) session.getSessionFactory() )
			        .getIdentifierGenerator( Valuation.class.getName() );

			stockId = ( Long ) stockIdGen.generate( (SessionImpl) session, null );
			valId = ( Long ) valIdGen.generate( (SessionImpl) session, null );

			Connection conn = session.connection();
			PreparedStatement ps = conn.prepareStatement( "INSERT INTO STOCK VALUES (?,?,?)");
			ps.setLong( 1, stockId.longValue() );
			ps.setString( 2, "JBOSS" );
			ps.setNull(3, Types.BIGINT);
			ps.executeUpdate();
			ps.close();

			ps = conn.prepareStatement( "INSERT INTO STOCK_VAL VALUES (?,?,?,?)" );
			ps.setLong( 1, valId.longValue() );
			ps.setLong( 2, stockId.longValue() );
			ps.setDate( 3, new Date( new java.util.Date().getTime() ) );
			ps.setDouble( 4, 200.0 );
			ps.executeUpdate();
			ps.close();

			ps = conn.prepareStatement( "UPDATE STOCK SET CURR_VAL_ID = ? WHERE STOCK_ID = ?" );
			ps.setLong( 1, valId.longValue() );
			ps.setLong( 2, stockId.longValue() );
			ps.executeUpdate();
			ps.close();
		}
		catch( SQLException e ) {
			System.err.println( "Error : " + e );
			e.printStackTrace();
			throw new HibernateException("Unable to generate test data",e);
		}
	}

	private void prettyPrint(Element element) {
		//System.out.println( element.asXML() );
		try {
			OutputFormat format = OutputFormat.createPrettyPrint();
			new XMLWriter( System.out, format ).write( element );
			System.out.println();
		}
		catch( Throwable t ) {
			System.err.println("Unable to pretty print element : " + t);
		}
	}

	public static Test suite() {
		return new TestSuite( MultiRepresentationTest.class );
	}

	protected String[] getMappings() {
		return new String[] {"entity/Stock.hbm.xml", "entity/Valuation.hbm.xml"};
	}
}
