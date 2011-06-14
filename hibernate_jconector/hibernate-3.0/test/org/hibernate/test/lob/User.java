// $Id: User.java,v 1.4 2005/02/12 07:27:27 steveebersole Exp $
package org.hibernate.test.lob;

import java.io.Serializable;

/**
 * Implementation of User.
 *
 * @author Steve
 */
public class User implements Serializable {
	private Long id;
	private String handle;
	private String password;
	private Name name;
	private String email;
	private Serializable serialData;

	public String getHandle() {
		return handle;
	}

	public void setHandle(String handle) {
		this.handle = handle;
	}

	public Long getId() {
		return id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	public Name getName() {
		return name;
	}

	public void setName(Name name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public boolean equals(Object other) {
		if (other==null) return false;
		if ( !(other instanceof User) ) return false;
		return ( (User) other ).getHandle().equals(handle);
	}

	public int hashCode() {
		return handle.hashCode();
	}


   public Serializable getSerialData()
   {
      return serialData;
   }

   public void setSerialData(Serializable serialData)
   {
      this.serialData = serialData;
   }
}
