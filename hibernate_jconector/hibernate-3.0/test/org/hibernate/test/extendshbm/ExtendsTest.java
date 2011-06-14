//$Id: ExtendsTest.java,v 1.6 2005/03/30 11:59:48 maxcsaucdk Exp $
package org.hibernate.test.extendshbm;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.TestCase;

/**
 * @author Gavin King
 */
public class ExtendsTest extends TestCase {
	
	public ExtendsTest(String str) {
		super(str);
	}
	
    public void testAllInOne() {
        assertNotNull(getCfg().getClassMapping("org.hibernate.test.extendshbm.Customer"));
        assertNotNull(getCfg().getClassMapping("org.hibernate.test.extendshbm.Person"));
        assertNotNull(getCfg().getClassMapping("org.hibernate.test.extendshbm.Employee"));    
    }
    
	public void testOutOfOrder() {
        Configuration cfg = new Configuration();
        
        try {
            cfg.addResource(getBaseForMappings() + "extendshbm/Customer.hbm.xml");
            assertNull("cannot be in the configuration yet!", cfg.getClassMapping("org.hibernate.test.extendshbm.Customer"));
            cfg.addResource(getBaseForMappings() + "extendshbm/Person.hbm.xml");
            cfg.addResource(getBaseForMappings() + "extendshbm/Employee.hbm.xml");
            
            cfg.buildSessionFactory();
        
            assertNotNull(cfg.getClassMapping("org.hibernate.test.extendshbm.Customer"));
            assertNotNull(cfg.getClassMapping("org.hibernate.test.extendshbm.Person"));
            assertNotNull(cfg.getClassMapping("org.hibernate.test.extendshbm.Employee"));
            
        } catch(HibernateException e) {
            fail("should not fail with exception! " + e);
        }
        
	}

	public void testNwaitingForSuper() {
        Configuration cfg = new Configuration();
        
        try {
            cfg.addResource(getBaseForMappings() + "extendshbm/Customer.hbm.xml");
            assertNull("cannot be in the configuration yet!", cfg.getClassMapping("org.hibernate.test.extendshbm.Customer"));
            cfg.addResource(getBaseForMappings() + "extendshbm/Employee.hbm.xml");
			assertNull("cannot be in the configuration yet!", cfg.getClassMapping("org.hibernate.test.extendshbm.Employee"));
			cfg.addResource(getBaseForMappings() + "extendshbm/Person.hbm.xml");
            
            cfg.buildMappings();
        
			assertNotNull(cfg.getClassMapping("org.hibernate.test.extendshbm.Person"));
            assertNotNull(cfg.getClassMapping("org.hibernate.test.extendshbm.Employee"));
			assertNotNull(cfg.getClassMapping("org.hibernate.test.extendshbm.Customer"));
            
            
        } catch(HibernateException e) {
			e.printStackTrace();
			fail("should not fail with exception! " + e);
			
        }
        
	}
	
    public void testMissingSuper() {
        Configuration cfg = new Configuration();
        
        try {
            cfg.addResource(getBaseForMappings() + "extendshbm/Customer.hbm.xml");
            assertNull("cannot be in the configuration yet!", cfg.getClassMapping("org.hibernate.test.extendshbm.Customer"));
            cfg.addResource(getBaseForMappings() + "extendshbm/Employee.hbm.xml");
            
            cfg.buildSessionFactory();
        
            fail("Should not be able to build sessionfactory without a Person");
        } catch(HibernateException e) {
            
        }
        
    }
	
    public void testAllSeparateInOne() {
        Configuration cfg = new Configuration();
        
        try {
            cfg.addResource(getBaseForMappings() + "extendshbm/allseparateinone.hbm.xml");
            
            cfg.buildSessionFactory();
        
            assertNotNull(cfg.getClassMapping("org.hibernate.test.extendshbm.Customer"));
            assertNotNull(cfg.getClassMapping("org.hibernate.test.extendshbm.Person"));
            assertNotNull(cfg.getClassMapping("org.hibernate.test.extendshbm.Employee"));
            
        } catch(HibernateException e) {
            fail("should not fail with exception! " + e);
        }
    	
    }
    
	public void testJoinedSubclass() {		
        Configuration cfg = new Configuration();
		
		try {
            cfg.addResource(getBaseForMappings() + "extendshbm/entitynames.hbm.xml");
            
            cfg.buildMappings();
        
            assertNotNull(cfg.getClassMapping("EntityHasName"));
            assertNotNull(cfg.getClassMapping("EntityCompany"));            
            
        } catch(HibernateException e) {
			e.printStackTrace();
            fail("should not fail with exception! " + e);
			
        }
	}
   
	protected String[] getMappings() {
		return new String[] { "extendshbm/allinone.hbm.xml" };
	}

	public static Test suite() {
		return new TestSuite(ExtendsTest.class);
	}

}

