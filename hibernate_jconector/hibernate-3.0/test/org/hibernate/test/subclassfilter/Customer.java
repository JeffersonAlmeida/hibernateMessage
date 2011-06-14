// $Id: Customer.java,v 1.1 2005/02/24 20:08:04 steveebersole Exp $
package org.hibernate.test.subclassfilter;

/**
 * Implementation of Customer.
 *
 * @author Steve Ebersole
 */
public class Customer extends Person {
	private Employee contactOwner;

	public Customer() {
	}

	public Customer(String name) {
		super( name );
	}

	public Employee getContactOwner() {
		return contactOwner;
	}

	public void setContactOwner(Employee contactOwner) {
		this.contactOwner = contactOwner;
	}
}
