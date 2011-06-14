//$Id: User.java,v 1.2 2005/02/12 07:27:21 steveebersole Exp $
package org.hibernate.test.component;

import java.util.Date;

/**
 * @author Gavin King
 */
public class User {
	private String userName;
	private String password;
	private Person person;
	private Date lastModified;
	User() {}
	public User(String id, String pw, Person person) {
		this.userName = id;
		this.password = pw;
		this.person = person;
	}
	public Date getLastModified() {
		return lastModified;
	}
	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public Person getPerson() {
		return person;
	}
	public void setPerson(Person person) {
		this.person = person;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
}
