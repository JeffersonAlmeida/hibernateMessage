//$Id: Child.java,v 1.2 2005/02/12 07:27:31 steveebersole Exp $
package org.hibernate.test.unidir;


/**
 * @author Gavin King
 */
public class Child {
	private String name;
	private int age;
	Child() {}
	public Child(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getAge() {
		return age;
	}
	public void setAge(int age) {
		this.age = age;
	}
}
