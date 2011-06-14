//$Id: Child.java,v 1.1 2004/09/02 02:30:28 oneovthafew Exp $
package org.hibernate.test.compositeelement;

/**
 * @author gavin
 */
public class Child {
	private String name;
	private Parent parent;
	Child() {}
	public Child(String name) {
		this.name = name;
	}
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return Returns the parent.
	 */
	public Parent getParent() {
		return parent;
	}
	/**
	 * @param parent The parent to set.
	 */
	public void setParent(Parent parent) {
		this.parent = parent;
	}
}
