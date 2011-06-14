// $Id: Name.java,v 1.1 2004/11/04 21:59:22 steveebersole Exp $
package org.hibernate.test.lob;

import java.io.Serializable;

/**
 * Implementation of Name.
 *
 * @author Steve
 */
public class Name implements Serializable {

	private String firstName;
	private String lastName;
	private Character initial;

	public Name() {}

	public Name(String first, Character middle, String last) {
		firstName = first;
		initial = middle;
		lastName = last;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public Character getInitial() {
		return initial;
	}

	public void setInitial(Character initial) {
		this.initial = initial;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer()
			.append(firstName)
			.append(' ');
		if (initial!=null) buf.append(initial)
			.append(' ');
		return buf.append(lastName)
			.toString();
	}
}
