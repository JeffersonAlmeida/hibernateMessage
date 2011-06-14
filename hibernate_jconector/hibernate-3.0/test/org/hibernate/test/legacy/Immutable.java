//$Id: Immutable.java,v 1.1 2004/09/26 05:18:25 oneovthafew Exp $
package org.hibernate.test.legacy;

public class Immutable {
	private String foo;
	private String bar;
	private String id;
	
	public String getFoo() {
		return foo;
	}
	
	public void setFoo(String foo) {
		this.foo = foo;
	}
	
	public String getBar() {
		return bar;
	}
	
	public void setBar(String bar) {
		this.bar = bar;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
}






