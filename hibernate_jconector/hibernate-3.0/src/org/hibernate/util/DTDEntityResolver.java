//$Id: DTDEntityResolver.java,v 1.4 2005/02/12 07:19:50 steveebersole Exp $
//Contributed by Markus Meissner
package org.hibernate.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public class DTDEntityResolver implements EntityResolver, Serializable {

	private static final Log log = LogFactory.getLog(DTDEntityResolver.class);

	private static final String URL = "http://hibernate.sourceforge.net/";
	private transient ClassLoader resourceLoader;

	/**
	 * Default constructor using DTDEntityResolver classloader for
	 * resource loading.
	 */
	public DTDEntityResolver() {
		//backward compatibility
		resourceLoader = this.getClass().getClassLoader();
	}

	/**
	 * Set the class loader used to load resouces
	 *
	 * @param resourceLoader class loader to use
	 */
	public DTDEntityResolver(ClassLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public InputSource resolveEntity (String publicId, String systemId) {
		if ( systemId!=null && systemId.startsWith(URL) ) {
			log.debug("trying to locate " + systemId + " in classpath under org/hibernate/");
			// Search for DTD
			String path = "org/hibernate/" + systemId.substring( URL.length() );
			InputStream dtdStream = resourceLoader==null ? 
					getClass().getResourceAsStream(path) :
					resourceLoader.getResourceAsStream(path);
			if (dtdStream==null) {
				log.debug(systemId + " not found in classpath");
				return null;
			}
			else {
				log.debug("found " + systemId + " in classpath");
				InputSource source = new InputSource(dtdStream);
				source.setPublicId(publicId);
				source.setSystemId(systemId);
				return source;
			}
		}
		else {
			// use the default behaviour
			return null;
		}
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		/** to allow serialization of configuration */
		ois.defaultReadObject();
		this.resourceLoader = this.getClass().getClassLoader();
	}
}







