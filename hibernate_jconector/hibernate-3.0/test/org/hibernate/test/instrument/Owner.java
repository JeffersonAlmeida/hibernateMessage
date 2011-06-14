//$Id: Owner.java,v 1.1 2004/08/12 01:49:29 oneovthafew Exp $
package org.hibernate.test.instrument;

/**
 * @author Gavin King
 */
public class Owner {
	private Long id;
	private String name;
	/**
	 * @return Returns the id.
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(Long id) {
		this.id = id;
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
}
