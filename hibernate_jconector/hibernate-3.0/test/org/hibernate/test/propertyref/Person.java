//$Id: Person.java,v 1.2 2004/08/21 08:43:20 oneovthafew Exp $
package org.hibernate.test.propertyref;

import java.util.HashSet;
import java.util.Set;

/**
 * @author gavin
 */
public class Person {
	private Long id;
	private String name;
	private Address address;
	private String userId;
	private Set accounts = new HashSet();
	/**
	 * @return Returns the userId.
	 */
	public String getUserId() {
		return userId;
	}
	/**
	 * @param userId The userId to set.
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}
	/**
	 * @return Returns the address.
	 */
	public Address getAddress() {
		return address;
	}
	/**
	 * @param address The address to set.
	 */
	public void setAddress(Address address) {
		this.address = address;
	}
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
	/**
	 * @return Returns the accounts.
	 */
	public Set getAccounts() {
		return accounts;
	}
	/**
	 * @param accounts The accounts to set.
	 */
	public void setAccounts(Set accounts) {
		this.accounts = accounts;
	}
}
