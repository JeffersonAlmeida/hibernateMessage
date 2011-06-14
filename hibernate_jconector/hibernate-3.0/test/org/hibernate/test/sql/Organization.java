//$Id: Organization.java,v 1.5 2004/08/21 08:43:20 oneovthafew Exp $
package org.hibernate.test.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author Gavin King
 */
public class Organization {
	private long id;
	private String name;
	private Collection employments;
	private Collection currentEmployments;

	public Organization(String name) {
		this.name = name;
		employments = new HashSet();
		currentEmployments = new ArrayList();
	}

	public Organization() {}

	/**
	 * @return Returns the employments.
	 */
	public Collection getEmployments() {
		return employments;
	}
	/**
	 * @param employments The employments to set.
	 */
	public void setEmployments(Collection employments) {
		this.employments = employments;
	}
	/**
	 * @return Returns the id.
	 */
	public long getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(long id) {
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
	/**
	 * @return Returns the currentEmployments.
	 */
	public Collection getCurrentEmployments() {
		return currentEmployments;
	}
	/**
	 * @param currentEmployments The currentEmployments to set.
	 */
	public void setCurrentEmployments(Collection currentEmployments) {
		this.currentEmployments = currentEmployments;
	}

}
