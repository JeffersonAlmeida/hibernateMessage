//$Id: UserVersionType.java,v 1.2 2005/02/12 07:19:49 steveebersole Exp $
package org.hibernate.usertype;

import java.util.Comparator;

/**
 * A user type that may be used for a version property
 * 
 * @author Gavin King
 */
public interface UserVersionType extends UserType, Comparator {
	/**
	 * Generate an initial version.
	 * @return an instance of the type
	 */
	public Object seed();
	/**
	 * Increment the version.
	 * @param current the current version
	 * @return an instance of the type
	 */
	public Object next(Object current);

}
