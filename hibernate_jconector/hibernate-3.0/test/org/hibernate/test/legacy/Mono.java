//$Id: Mono.java,v 1.1 2004/09/26 05:18:25 oneovthafew Exp $
package org.hibernate.test.legacy;

import java.util.Set;


public class Mono extends Top {
	
	private Set strings;

	/**
	 * Constructor for Mono.
	 * @param c
	 */
	public Mono(int c) {
		super(c);
	}

	/**
	 * Constructor for Mono.
	 */
	public Mono() {
		super();
	}

	/**
	 * Returns the strings.
	 * @return Set
	 */
	public Set getStrings() {
		return strings;
	}

	/**
	 * Sets the strings.
	 * @param strings The strings to set
	 */
	public void setStrings(Set strings) {
		this.strings = strings;
	}

}
