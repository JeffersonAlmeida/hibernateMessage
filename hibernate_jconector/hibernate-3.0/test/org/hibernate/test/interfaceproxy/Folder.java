//$Id: Folder.java,v 1.1 2004/08/22 01:20:07 oneovthafew Exp $
package org.hibernate.test.interfaceproxy;

/**
 * @author Gavin King
 */
public interface Folder extends Item {
	/**
	 * @return Returns the parent.
	 */
	public Folder getParent();

	/**
	 * @param parent The parent to set.
	 */
	public void setParent(Folder parent);
}