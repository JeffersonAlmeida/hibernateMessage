//$Id: User.java,v 1.4 2005/02/12 07:27:21 steveebersole Exp $
package org.hibernate.test.collection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gavin King
 */
public class User {
	private String userName;
	private List permissions = new ArrayList();
	private List emailAddresses = new ArrayList();
	private Map sessionData = new HashMap();

	User() {}
	public User(String name) {
		userName = name;
	}
	public List getPermissions() {
		return permissions;
	}
	public void setPermissions(List permissions) {
		this.permissions = permissions;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public List getEmailAddresses() {
		return emailAddresses;
	}
	public void setEmailAddresses(List emailAddresses) {
		this.emailAddresses = emailAddresses;
	}
	public Map getSessionData() {
		return sessionData;
	}
	public void setSessionData(Map sessionData) {
		this.sessionData = sessionData;
	}
}
