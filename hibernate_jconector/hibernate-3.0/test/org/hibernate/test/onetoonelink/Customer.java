//$Id: Customer.java,v 1.2 2005/02/12 07:27:30 steveebersole Exp $
package org.hibernate.test.onetoonelink;

/**
 * @author Gavin King
 */
public class Customer {
	private Long id;
	private Person person;
	public Person getPerson() {
		return person;
	}
	public void setPerson(Person person) {
		this.person = person;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
}
