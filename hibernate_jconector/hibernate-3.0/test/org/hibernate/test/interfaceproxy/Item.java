//$Id: Item.java,v 1.1 2004/08/22 01:20:07 oneovthafew Exp $
package org.hibernate.test.interfaceproxy;

/**
 * @author Gavin King
 */
public interface Item {
	/**
	 * @return Returns the id.
	 */
	public Long getId();

	/**
	 * @return Returns the name.
	 */
	public String getName();

	/**
	 * @param name The name to set.
	 */
	public void setName(String name);
}