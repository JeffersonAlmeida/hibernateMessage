// $Id: Dom4jTest.java,v 1.3 2005/03/06 16:31:25 oneovthafew Exp $
package org.hibernate.test.dom4j;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Example;
import org.hibernate.test.TestCase;

/**
 * @author Gavin King
 */
public class Dom4jTest extends TestCase {

	public Dom4jTest(String str) {
		super( str );
	}

	public void testDom4j() throws Exception {
		Element acct = DocumentFactory.getInstance().createElement( "account" );
		acct.addAttribute( "id", "abc123" );
		acct.addElement( "balance" ).setText( "123.45" );
		Element cust = acct.addElement( "customer" );
		cust.addAttribute( "id", "xyz123" );
		Element foo1 = cust.addElement( "stuff" ).addElement( "foo" );
		foo1.setText( "foo" );
		foo1.addAttribute("bar", "x");
		Element foo2 = cust.element( "stuff" ).addElement( "foo" );
		foo2.setText( "bar" );
		foo2.addAttribute("bar", "y");
		cust.addElement( "amount" ).setText( "45" );
		cust.setText( "An example customer" );
		Element name = cust.addElement( "name" );
		name.addElement( "first" ).setText( "Gavin" );
		name.addElement( "last" ).setText( "King" );

		Element loc = DocumentFactory.getInstance().createElement( "location" );
		loc.addElement( "address" ).setText( "Karbarook Avenue" );

		print( acct );

		Session s = openSession();
		Session dom4jSession = s.getSession( EntityMode.DOM4J );
		Transaction t = s.beginTransaction();
		dom4jSession.persist( "Location", loc );
		cust.addElement( "location" ).addAttribute( "id", loc.attributeValue( "id" ) );
		dom4jSession.persist( "Account", acct );
		t.commit();
		s.close();

		print( loc );

		s = openSession();
		dom4jSession = s.getSession( EntityMode.DOM4J );
		t = s.beginTransaction();
		cust = (Element) dom4jSession.get( "Customer", "xyz123" );
		print( cust );
		acct = (Element) dom4jSession.get( "Account", "abc123" );
		print( acct );
		assertSame( acct.element( "customer" ), cust );
		cust.element( "name" ).element( "first" ).setText( "Gavin A" );
		Element foo3 = cust.element("stuff").addElement("foo");
		foo3.setText("baz");
		foo3.addAttribute("bar", "z");
		cust.element("amount").setText("3");
		cust.addElement("amount").setText("56");
		t.commit();
		s.close();

		System.out.println();

		acct.element( "balance" ).setText( "3456.12" );
		cust.addElement( "address" ).setText( "Karbarook Ave" );

		assertSame( acct.element( "customer" ), cust );

		cust.setText( "Still the same example!" );

		s = openSession();
		dom4jSession = s.getSession( EntityMode.DOM4J );
		t = s.beginTransaction();
		dom4jSession.saveOrUpdate( "Account", acct );
		t.commit();
		s.close();

		s = openSession();
		dom4jSession = s.getSession( EntityMode.DOM4J );
		t = s.beginTransaction();
		cust = (Element) dom4jSession.get( "Customer", "xyz123" );
		print( cust );
		acct = (Element) dom4jSession.get( "Account", "abc123" );
		print( acct );
		assertSame( acct.element( "customer" ), cust );
		t.commit();
		s.close();

		System.out.println();

		s = openSession();
		dom4jSession = s.getSession( EntityMode.DOM4J );
		t = s.beginTransaction();
		cust = (Element) dom4jSession
			.createCriteria( "Customer" )
			.add( Example.create( cust ) )
			.uniqueResult();
		print( cust );
		t.commit();
		s.close();

		System.out.println();

		s = openSession();
		dom4jSession = s.getSession( EntityMode.DOM4J );
		t = s.beginTransaction();
		acct = (Element) dom4jSession
			.createQuery( "from Account a left join fetch a.customer" )
			.uniqueResult();
		print( acct );
		t.commit();
		s.close();

		System.out.println();

		s = openSession();
		dom4jSession = s.getSession( EntityMode.DOM4J );
		t = s.beginTransaction();
		acct = (Element) dom4jSession.createQuery( "from Account" ).uniqueResult();
		print( acct );
		t.commit();
		s.close();

		System.out.println();

		s = openSession();
		dom4jSession = s.getSession( EntityMode.DOM4J );
		t = s.beginTransaction();
		cust = (Element) dom4jSession
			.createQuery( "from Customer c left join fetch c.stuff" )
			.uniqueResult();
		print( cust );
		t.commit();
		s.close();

		System.out.println();

		s = openSession();
		dom4jSession = s.getSession( EntityMode.DOM4J );
		t = s.beginTransaction();
		cust = (Element) dom4jSession
			.createQuery( "from Customer c left join fetch c.morestuff" )
			.uniqueResult();
		print( cust );
		t.commit();
		s.close();

		System.out.println();

		s = openSession();
		dom4jSession = s.getSession( EntityMode.DOM4J );
		t = s.beginTransaction();
		cust = (Element) dom4jSession
			.createQuery( "from Customer c left join fetch c.morestuff" )
			.uniqueResult();
		cust = (Element) dom4jSession
			.createQuery( "from Customer c left join fetch c.stuff" )
			.uniqueResult();
		print( cust );
		t.commit();
		s.close();

		System.out.println();

		s = openSession();
		dom4jSession = s.getSession( EntityMode.DOM4J );
		t = s.beginTransaction();
		cust = (Element) dom4jSession
			.createQuery( "from Customer c left join fetch c.accounts" )
			.uniqueResult();
		Element a1 = cust.element( "accounts" ).addElement( "account" );
		a1.addElement( "balance" ).setText( "12.67" );
		a1.addAttribute( "id", "lkj345" );
		a1.addAttribute("acnum", "0");
		Element a2 = cust.element( "accounts" ).addElement( "account" );
		a2.addElement( "balance" ).setText( "10000.00" );
		a2.addAttribute( "id", "hsh987" );
		a2.addAttribute("acnum", "1");
		// dom4jSession.create( "Account", cust.element("accounts").element("account") );
		print( cust );
		t.commit();
		s.close();

		System.out.println();

		s = openSession();
		dom4jSession = s.getSession( EntityMode.DOM4J );
		t = s.beginTransaction();
		cust = (Element) dom4jSession
			.createQuery( "from Customer c left join fetch c.accounts" )
			.uniqueResult();
		print( cust );
		t.commit();
		s.close();
	}

	protected String[] getMappings() {
		return new String[] { "dom4j/Account.hbm.xml" };
	}

	public static Test suite() {
		return new TestSuite( Dom4jTest.class );
	}

	public static void print(Element elt) throws Exception {
		OutputFormat outformat = OutputFormat.createPrettyPrint();
		// outformat.setEncoding(aEncodingScheme);
		XMLWriter writer = new XMLWriter( System.out, outformat );
		writer.write( elt );
		writer.flush();
		// System.out.println( elt.asXML() );
	}

	protected void configure(Configuration cfg) {
		//cfg.setProperty(Environment.USE_SECOND_LEVEL_CACHE, "false");
	}
}
