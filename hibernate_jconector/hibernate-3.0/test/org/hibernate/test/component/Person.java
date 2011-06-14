//$Id: Person.java,v 1.2 2005/02/12 07:27:21 steveebersole Exp $
package org.hibernate.test.component;

import java.util.Date;

/**
 * @author Gavin King
 */
public class Person {
	private String name;
	private Date dob;
	private String address;
	private String previousAddress;
	private int yob;
	Person() {}
	public Person(String name, Date dob, String address) {
		this.name = name;
		this.dob = dob;
		this.address = address;
	}
	public int getYob() {
		return yob;
	}
	public void setYob(int age) {
		this.yob = age;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public Date getDob() {
		return dob;
	}
	public void setDob(Date dob) {
		this.dob = dob;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPreviousAddress() {
		return previousAddress;
	}
	public void setPreviousAddress(String previousAddress) {
		this.previousAddress = previousAddress;
	}
	public void changeAddress(String add) {
		setPreviousAddress( getAddress() );
		setAddress(add);
	}
}
