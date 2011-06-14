//$Id: Abstract.java,v 1.1 2004/09/26 05:18:24 oneovthafew Exp $
package org.hibernate.test.legacy;

import java.util.Set;

public abstract class Abstract extends Foo implements AbstractProxy {
	
	private java.sql.Time time;
	private Set abstracts;
	
	public java.sql.Time getTime() {
		return time;
	}
	
	public void setTime(java.sql.Time time) {
		this.time = time;
	}
	
	public Set getAbstracts() {
		return abstracts;
	}
	
	public void setAbstracts(Set abstracts) {
		this.abstracts = abstracts;
	}
	
}






