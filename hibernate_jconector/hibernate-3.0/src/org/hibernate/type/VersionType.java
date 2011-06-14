//$Id: VersionType.java,v 1.3 2005/02/16 12:50:19 oneovthafew Exp $
package org.hibernate.type;

import java.util.Comparator;

/**
 * A <tt>Type</tt> that may be used to version data.
 * @author Gavin King
 */
public interface VersionType extends Type {
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
	/**
	 * Get a comparator for the version numbers
	 */
	public Comparator getComparator();
	
	public boolean isEqual(Object x, Object y);
}






