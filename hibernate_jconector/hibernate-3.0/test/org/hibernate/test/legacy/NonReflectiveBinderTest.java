//$Id: NonReflectiveBinderTest.java,v 1.3 2004/09/26 05:28:50 oneovthafew Exp $
package org.hibernate.test.legacy;

import java.util.Iterator;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.hibernate.MappingException;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;


public class NonReflectiveBinderTest extends TestCase {

	Configuration cfg;
	private Class lastTestClass;

	public static Test suite() {
		return new TestSuite(NonReflectiveBinderTest.class);
	}

	Configuration getCfg() {
		return cfg;
	}

	public String[] getMappings() {
		return new String[] { "legacy/Wicked.hbm.xml" };
	}

	void buildConfiguration(String[] files) throws MappingException {

			try {
				setCfg( new Configuration() );

				for (int i=0; i<files.length; i++) {
					if ( !files[i].startsWith("net/") ) files[i] = "org/hibernate/test/" + files[i];
					getCfg().addResource( files[i], TestCase.class.getClassLoader() );
				}
			} catch (MappingException e) {
				throw e;
			}


	}

	protected void setUp() throws Exception {
		if ( getCfg()==null || lastTestClass!=getClass() ) {
			buildConfiguration( getMappings() );
			lastTestClass = getClass();
		}
	}

	/**
	 * @param configuration
	 */
	private void setCfg(Configuration configuration) {
		cfg = configuration;
	}

	public void testMetaInheritance() {
		Configuration cfg = getCfg();
		cfg.buildMappings();
		PersistentClass cm = cfg.getClassMapping("org.hibernate.test.legacy.Wicked");
		Map m = cm.getMetaAttributes();
		assertNotNull(m);
		assertNotNull(cm.getMetaAttribute("global"));
		assertNull(cm.getMetaAttribute("globalnoinherit"));
		
		MetaAttribute metaAttribute = cm.getMetaAttribute("implements");
		assertNotNull(metaAttribute);
		assertEquals("implements", metaAttribute.getName());
		assertTrue(metaAttribute.isMultiValued());
		assertEquals(3, metaAttribute.getValues().size());
		assertEquals("java.lang.Observer",metaAttribute.getValues().get(0));
		assertEquals("java.lang.Observer",metaAttribute.getValues().get(1));
		assertEquals("org.foo.BogusVisitor",metaAttribute.getValues().get(2));
		
		/*Property property = cm.getIdentifierProperty();
		property.getMetaAttribute(null);*/
		
		Iterator propertyIterator = cm.getPropertyIterator();
		while (propertyIterator.hasNext()) {
			Property element = (Property) propertyIterator.next();
			System.out.println(element);
			Map ma = element.getMetaAttributes();
			assertNotNull(ma);
			assertNotNull(element.getMetaAttribute("global"));
			MetaAttribute metaAttribute2 = element.getMetaAttribute("implements");
			assertNotNull(metaAttribute2);
			assertNull(element.getMetaAttribute("globalnoinherit"));					
		}
		
		Property element = cm.getProperty("component");
		Map ma = element.getMetaAttributes();
		assertNotNull(ma);
		assertNotNull(element.getMetaAttribute("global"));
		assertNotNull(element.getMetaAttribute("componentonly"));
		assertNotNull(element.getMetaAttribute("allcomponent"));
		assertNotNull(element.getMetaAttribute("implements"));
		assertNull(element.getMetaAttribute("globalnoinherit"));					
	
		
	}

}
