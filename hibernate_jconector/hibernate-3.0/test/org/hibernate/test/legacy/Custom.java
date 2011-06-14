//$Id: Custom.java,v 1.1 2004/09/26 05:18:25 oneovthafew Exp $
package org.hibernate.test.legacy;


public class Custom implements Cloneable {
	String id;
	String name;
	
	public Object clone() {
		try {
			return super.clone();
		}
		catch (CloneNotSupportedException cnse) {
			throw new RuntimeException();
		}
	}
}






