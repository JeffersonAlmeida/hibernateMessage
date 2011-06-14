//$Id: Human.java,v 1.7 2005/03/30 15:41:42 steveebersole Exp $
package org.hibernate.test.hql;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Human extends Mammal {
	private Name name;
	private String nickName;
	private Collection friends;
	private Collection pets;
	private Map family;
	private double height;
	private Set nickNames;
	private Map addresses;

	public Collection getFriends() {
		return friends;
	}

	public void setFriends(Collection friends) {
		this.friends = friends;
	}

	public Collection getPets() {
		return pets;
	}

	public void setPets(Collection pets) {
		this.pets = pets;
	}

	public Name getName() {
		return name;
	}

	public void setName(Name name) {
		this.name = name;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}
	
	public double getHeight() {
		return height;
	}
	public void setHeight(double height) {
		this.height = height;
	}

	public Map getFamily() {
		return family;
	}
	

	public void setFamily(Map family) {
		this.family = family;
	}

	public Set getNickNames() {
		return nickNames;
	}

	public void setNickNames(Set nickNames) {
		this.nickNames = nickNames;
	}

	public Map getAddresses() {
		return addresses;
	}

	public void setAddresses(Map addresses) {
		this.addresses = addresses;
	}
}
