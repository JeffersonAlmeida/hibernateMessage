//$Id: IJTest.java,v 1.1 2004/09/26 05:18:25 oneovthafew Exp $
package org.hibernate.test.legacy;

import java.io.Serializable;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.LockMode;
import org.hibernate.classic.Session;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.test.TestCase;

/**
 * @author Gavin King
 */
public class IJTest extends TestCase {

	public void testFormulaDiscriminator() throws Exception {
		if ( ( getDialect() instanceof OracleDialect ) || ( getDialect() instanceof HSQLDialect ) ) return;
		Session s = getSessions().openSession();
		I i = new I();
		i.name = "i";
		i.type = 'a';
		J j = new J();
		j.name = "j";
		j.type = 'x';
		j.amount = 1.0f;
		Serializable iid = s.save(i);
		Serializable jid = s.save(j);
		s.flush();
		s.connection().commit();
		s.close();

		getSessions().evict(I.class);

		s = getSessions().openSession();
		j = (J) s.get(I.class, jid);
		i = (I) s.get(I.class, iid);
		assertTrue( i.getClass()==I.class );
		j.amount = 0.5f;
		s.lock(i, LockMode.UPGRADE);
		s.flush();
		s.connection().commit();
		s.close();

		s = getSessions().openSession();
		j = (J) s.get(I.class, jid, LockMode.UPGRADE);
		i = (I) s.get(I.class, iid, LockMode.UPGRADE);
		s.flush();
		s.connection().commit();
		s.close();

		s = getSessions().openSession();
		assertTrue( s.find("from I").size()==2 );
		assertTrue( s.find("from J").size()==1 );
		assertTrue( s.find("from I i where i.class = 0").size()==1 );
		assertTrue( s.find("from I i where i.class = 1").size()==1 );
		s.connection().commit();
		s.close();

		s = getSessions().openSession();
		j = (J) s.get(J.class, jid);
		i = (I) s.get(I.class, iid);
		s.delete(j);
		s.delete(i);
		s.flush();
		s.connection().commit();
		s.close();

	}

	protected String[] getMappings() {
		return new String[] { "legacy/IJ.hbm.xml" };
	}

	public IJTest(String x) {
		super(x);
	}

	public static Test suite() {
		return new TestSuite(IJTest.class);
	}
}
