/*
 * Created on 28-Jan-2005
 *
 */
package org.hibernate.property;

import org.hibernate.PropertyAccessException;
import org.hibernate.PropertyNotFoundException;

/**
 * @author max
 *
 */
public class ChainedPropertyAccessor implements PropertyAccessor {

	final PropertyAccessor[] chain;
	
	public ChainedPropertyAccessor(PropertyAccessor[] chain) {
		this.chain = chain;
	}
	
	public Getter getGetter(Class theClass, String propertyName)
			throws PropertyNotFoundException {
		Getter result = null;
		for (int i = 0; i < chain.length; i++) {
			PropertyAccessor candidate = chain[i];
			try {
				result = candidate.getGetter(theClass, propertyName);
				return result;
			} catch (PropertyNotFoundException pnfe) {
				// ignore
			}
		}
		throw new PropertyNotFoundException("Could not find Getter ");
	}

	public Setter getSetter(Class theClass, String propertyName)
			throws PropertyNotFoundException {
		Setter result = null;
		for (int i = 0; i < chain.length; i++) {
			PropertyAccessor candidate = chain[i];
			try {
				result = candidate.getSetter(theClass, propertyName);
				return result;
			} catch (PropertyNotFoundException pnfe) {
				//
			}
		}
		throw new PropertyAccessException(null, "could not find a property " , false, theClass, propertyName);
	}

}
