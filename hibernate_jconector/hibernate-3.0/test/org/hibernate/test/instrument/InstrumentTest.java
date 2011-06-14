//$Id: InstrumentTest.java,v 1.9 2005/03/30 17:12:54 epbernard Exp $
package org.hibernate.test.instrument;

import java.util.HashSet;

import junit.framework.Test;
import junit.framework.TestSuite;
import net.sf.cglib.transform.impl.InterceptFieldEnabled;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.intercept.FieldInterceptor;
import org.hibernate.test.TestCase;

/**
 * @author Gavin King
 */
public class InstrumentTest extends TestCase {
	
	public InstrumentTest(String str) {
		super(str);
	}

	public void testLazy() throws Exception {
		Session s = openSession();
		Owner o = new Owner();
		Document doc = new Document();
		Folder fol = new Folder();
		o.setName("gavin");
		doc.setName("Hibernate in Action");
		doc.setSummary("blah");
		doc.updateText("blah blah");
		fol.setName("books");
		doc.setOwner(o);
		doc.setFolder(fol);
		fol.getDocuments().add(doc);
		s.save(o);
		s.save(fol);
		s.flush();
		s.connection().commit();
		s.close();
		
		getSessions().evict(Document.class);
		
		s = openSession();
		doc = (Document) s.createQuery("from Document").uniqueResult();
		doc.getName();
		assertEquals( doc.getText(), "blah blah" );
		s.connection().commit();
		s.close();

		s = openSession();
		doc = (Document) s.createQuery("from Document").uniqueResult();
		doc.getName();
		assertFalse(Hibernate.isPropertyInitialized(doc, "text"));
		assertFalse(Hibernate.isPropertyInitialized(doc, "summary"));
		assertEquals( doc.getText(), "blah blah" );
		assertTrue(Hibernate.isPropertyInitialized(doc, "text"));
		assertTrue(Hibernate.isPropertyInitialized(doc, "summary"));
		s.connection().commit();
		s.close();

		s = openSession();
		doc = (Document) s.createQuery("from Document").uniqueResult();
		doc.setName("HiA");
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		doc = (Document) s.createQuery("from Document").uniqueResult();
		assertEquals( doc.getName(), "HiA" );
		assertEquals( doc.getText(), "blah blah" );
		s.connection().commit();
		s.close();

		s = openSession();
		doc = (Document) s.createQuery("from Document").uniqueResult();
		doc.getText();
		doc.setName("HiA second edition");
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		doc = (Document) s.createQuery("from Document").uniqueResult();
		assertTrue(Hibernate.isPropertyInitialized(doc, "weirdProperty"));
		assertTrue(Hibernate.isPropertyInitialized(doc, "name"));
		assertFalse(Hibernate.isPropertyInitialized(doc, "text"));
		assertFalse(Hibernate.isPropertyInitialized(doc, "upperCaseName"));
		assertFalse(Hibernate.isPropertyInitialized(doc, "owner"));
		assertEquals( doc.getName(), "HiA second edition" );
		assertEquals( doc.getText(), "blah blah" );
		assertEquals( doc.getUpperCaseName(), "HIA SECOND EDITION" );
		assertTrue(Hibernate.isPropertyInitialized(doc, "text"));
		assertTrue(Hibernate.isPropertyInitialized(doc, "weirdProperty"));
		assertTrue(Hibernate.isPropertyInitialized(doc, "upperCaseName"));
		s.connection().commit();
		s.close();

		s = openSession();
		doc = (Document) s.createQuery("from Document").uniqueResult();
		s.connection().commit();
		s.close();
		
		assertFalse(Hibernate.isPropertyInitialized(doc, "text"));

		s = openSession();
		s.lock(doc, LockMode.NONE);
		assertFalse(Hibernate.isPropertyInitialized(doc, "text"));
		assertEquals( doc.getText(), "blah blah" );
		assertTrue(Hibernate.isPropertyInitialized(doc, "text"));
		s.connection().commit();
		s.close();

		s = openSession();
		doc = (Document) s.createQuery("from Document").uniqueResult();
		s.connection().commit();
		s.close();
		
		doc.setName("HiA2");
		
		assertFalse(Hibernate.isPropertyInitialized(doc, "text"));

		s = openSession();
		s.saveOrUpdate(doc);
		s.flush();
		assertFalse(Hibernate.isPropertyInitialized(doc, "text"));
		assertEquals( doc.getText(), "blah blah" );
		assertTrue(Hibernate.isPropertyInitialized(doc, "text"));
		doc.updateText("blah blah blah blah");
		s.flush();
		s.connection().commit();
		s.close();

		s = openSession();
		doc = (Document) s.createQuery("from Document").uniqueResult();
		assertEquals( doc.getName(), "HiA2" );
		assertEquals( doc.getText(), "blah blah blah blah" );
		s.connection().commit();
		s.close();

		s = openSession();
		doc = (Document) s.load( Document.class, doc.getId() );
		doc.getName();
		assertFalse(Hibernate.isPropertyInitialized(doc, "text"));
		assertFalse(Hibernate.isPropertyInitialized(doc, "summary"));
		s.connection().commit();
		s.close();

		s = openSession();
		doc = (Document) s.createQuery("from Document").uniqueResult();
		s.delete(doc);
		s.flush();
		s.connection().commit();
		s.close();

	}
	
	protected String[] getMappings() {
		return new String[] { "instrument/Documents.hbm.xml" };
	}
	
	public void testSetFieldInterceptor() {
		Document doc = new Document();
		( (InterceptFieldEnabled) doc ).setInterceptFieldCallback( 
				new FieldInterceptor(null, "Document", new HashSet()) 
		);
		doc.getId();
	}

	public static Test suite() {
		return new TestSuite(InstrumentTest.class);
	}

}

