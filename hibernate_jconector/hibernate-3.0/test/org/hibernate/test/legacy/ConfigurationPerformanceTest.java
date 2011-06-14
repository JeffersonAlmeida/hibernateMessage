/*
 * Created on 01-05-2004
 *
  */
package org.hibernate.test.legacy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * @author MAX
 *
 */
public class ConfigurationPerformanceTest extends TestCase /** purposely not extending hibernate test case to manage the configuration loading 100% on it's own */ {

	String[] files = new String[] {
			"legacy/ABC.hbm.xml",
			"legacy/ABCExtends.hbm.xml",
			"legacy/Baz.hbm.xml",
			"legacy/Blobber.hbm.xml",
			"legacy/Broken.hbm.xml",
			"legacy/Category.hbm.xml",
			"legacy/Circular.hbm.xml",
			"legacy/Commento.hbm.xml",
			"legacy/ComponentNotNullMaster.hbm.xml",
			"legacy/Componentizable.hbm.xml",
			"legacy/Container.hbm.xml",
			"legacy/Custom.hbm.xml",
			"legacy/CustomSQL.hbm.xml",
			"legacy/Eye.hbm.xml",
			"legacy/Fee.hbm.xml",
			"legacy/Fo.hbm.xml",
			"legacy/FooBar.hbm.xml",
			"legacy/Fum.hbm.xml",
			"legacy/Fumm.hbm.xml",
			"legacy/Glarch.hbm.xml",
			"legacy/Holder.hbm.xml",
			"legacy/IJ2.hbm.xml",
			"legacy/Immutable.hbm.xml",
			"legacy/Location.hbm.xml",
			"legacy/Many.hbm.xml",
			"legacy/Map.hbm.xml",
			"legacy/Marelo.hbm.xml",
			"legacy/MasterDetail.hbm.xml",
			"legacy/Middle.hbm.xml",
			"legacy/Multi.hbm.xml",
			"legacy/MultiExtends.hbm.xml",
			"legacy/Nameable.hbm.xml",
			"legacy/One.hbm.xml",
			"legacy/ParentChild.hbm.xml",
			"legacy/Qux.hbm.xml",
			"legacy/Simple.hbm.xml",
			"legacy/SingleSeveral.hbm.xml",
			"legacy/Stuff.hbm.xml",
			"legacy/UpDown.hbm.xml",
			"legacy/Vetoer.hbm.xml",
			"legacy/WZ.hbm.xml",
	};


	public void testLoadingConfiguration() throws HibernateException, Exception, Exception {
		long start = System.currentTimeMillis();

		Configuration cfg = new Configuration();
		System.err.println("Created configuration: " + (System.currentTimeMillis() - start) / 1000.0 + " sec.");

		if(!new File("hibernate.cfg.bin").exists()) {
		start = System.currentTimeMillis();
		/*for (int i=0; i<files.length; i++) {
			if ( !files[i].startsWith("net/") ) files[i] = "test/org/hibernate/test/" + files[i];
			cfg.addFile(files[i]);
			//cfg.addLazyFile(files[i]);
		}*/

		int cnt = 0;
		File[] diffFiles = new File("perftest").listFiles();
        if(diffFiles!=null) {
		for (int i = 0; i < diffFiles.length; i++) {
			File file = diffFiles[i];
			if(file.getName().endsWith("hbm.xml")) {
				//cfg.addFile(file);
				cfg.addCacheableFile(file.getAbsolutePath());
				cnt++;
			}
		}} else {
		    for (int i = 0; i < files.length; i++) {
                String file = files[i];
                String prefix = "./test/org/hibernate/test/";
                cfg.addCacheableFile(new File(prefix + file));
            }
        }

		System.err.println("Added " + (files.length + cnt) + " resources: " + (System.currentTimeMillis() - start) / 1000.0 + " sec.");
		
		ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream("hibernate.cfg.bin"));
		os.writeObject(cfg); // need to serialize Configuration *before* building sf since it would require non-mappings and cfg types to be serializable
		os.flush();
		os.close();

		} else {
			start = System.currentTimeMillis();
			ObjectInputStream is = new ObjectInputStream(new FileInputStream("hibernate.cfg.bin"));
			cfg = (Configuration) is.readObject();
			is.close();
			System.err.println("Loaded serializable configuration:" + (System.currentTimeMillis() - start) / 1000.0 + " sec.");
            new File("hibernate.cfg.bin").delete();
		}
		start = System.currentTimeMillis();
		SessionFactory factory = cfg.buildSessionFactory();
		System.err.println("Build session factory:" + (System.currentTimeMillis() - start) / 1000.0 + " sec.");



		//assertNotSame(factory, f2);
	}


	public static Test suite() {
		return new TestSuite(ConfigurationPerformanceTest.class);
	}

	public static void main(String[] args) throws Exception {
		TestRunner.run( suite() );
	}
}
