// $Id: Instantiator.java,v 1.4 2005/02/19 07:06:06 oneovthafew Exp $
package org.hibernate.tuple;


import java.io.Serializable;

/**
 * Contract for implementors responsible for instantiating entity instances.
 *
 * @author Steve Ebersole
 */
public interface Instantiator extends Serializable {

	/**
	 * Perform the requested entity instantiation.
	 *
	 * @param id The id of the entity to be instantiated.
	 * @return An appropriately instantiated entity.
	 */
	public Object instantiate(Serializable id);

	/**
	 * Perform the requested instantiation (sans any id).
	 *
	 * @return The instantiated data structure. 
	 */
	public Object instantiate();

	/**
	 * Performs check to see if the given object is an instance of the entity
	 * which this Instantiator instantiates.
	 *
	 * @param object The object to be checked.
	 * @return True is the object does respresent an instance of the underlying
	 * entity.
	 */
	public boolean isInstance(Object object);
}
