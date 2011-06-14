//$Id: Email.java,v 1.2 2005/02/12 07:27:21 steveebersole Exp $
package org.hibernate.test.collection;

/**
 * @author Gavin King
 */
public class Email {
	private String address;
	Email() {}
	public String getAddress() {
		return address;
	}
	public void setAddress(String type) {
		this.address = type;
	}
	public Email(String type) {
		this.address = type;
	}
	public boolean equals(Object that) {
		if ( !(that instanceof Email) ) return false;
		Email p = (Email) that;
		return this.address.equals(p.address);
	}
	public int hashCode() {
		return address.hashCode();
	}
}
