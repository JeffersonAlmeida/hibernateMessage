//$Id: PojoInstantiator.java,v 1.3 2005/02/21 02:46:41 oneovthafew Exp $
package org.hibernate.tuple;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;

import net.sf.cglib.reflect.FastClass;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.InstantiationException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.util.ReflectHelper;

/**
 * Defines a POJO-based instantiator for use from the tuplizers.
 */
public class PojoInstantiator implements Instantiator, Serializable {

	private static final Log log = LogFactory.getLog(PojoInstantiator.class);

	private transient Constructor constructor;

	private final Class mappedClass;
	private final transient FastClass fastClass;
	private final boolean embeddedIdentifier;
	private final Class proxyInterface;

	public PojoInstantiator(Class mappedClass, Class proxyInterface, FastClass fastClass, boolean embedded) {
		this.mappedClass = mappedClass;
		this.proxyInterface = proxyInterface;
		this.fastClass = fastClass;
		this.embeddedIdentifier = embedded;
		try {
			constructor = ReflectHelper.getDefaultConstructor(mappedClass);
		}
		catch ( PropertyNotFoundException pnfe ) {
			log.info(
			        "no default (no-argument) constructor for class: " +
					mappedClass.getName() +
					" (class must be instantiated by Interceptor)"
			);
			constructor = null;
		}
	}

	private void readObject(java.io.ObjectInputStream stream)
	throws ClassNotFoundException, IOException {
		stream.defaultReadObject();
		constructor = ReflectHelper.getDefaultConstructor(mappedClass);
	}

	public Object instantiate() {
		if ( ReflectHelper.isAbstractClass(mappedClass) ) {
			throw new InstantiationException( "Cannot instantiate abstract class or interface: ", mappedClass );
		}
		else if ( fastClass != null ) {
			try {
				return fastClass.newInstance();
			}
			catch ( Throwable t ) {
				throw new InstantiationException( "Could not instantiate entity with CGLIB: ", mappedClass, t );
			}
		}
		else if ( constructor == null ) {
			throw new InstantiationException( "No default constructor for entity: ", mappedClass );
		}
		else {
			try {
				return constructor.newInstance( null );
			}
			catch ( Exception e ) {
				throw new InstantiationException( "Could not instantiate entity: ", mappedClass, e );
			}
		}
	}
	
	public Object instantiate(Serializable id) {
		if ( embeddedIdentifier && id != null && id.getClass().equals(mappedClass) ) {
			return id;
		}
		else {
			return instantiate();
		}
	}

	public boolean isInstance(Object object) {
		return mappedClass.isInstance(object) || 
				( proxyInterface!=null && proxyInterface.isInstance(object) ); //this one needed only for guessEntityMode()
	}
}