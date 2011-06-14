//$Id: Employer.java,v 1.2 2005/02/12 07:27:30 steveebersole Exp $
package org.hibernate.test.ops;

import java.util.Collection;
import java.io.Serializable;


/**
 * Employer in a employer-Employee relationship
 * 
 * @author Emmanuel Bernard
 */

public class Employer implements Serializable {
	private Integer id;
	private Collection employees;
	

	public Collection getEmployees() {
		return employees;
	}
	
	
	public Integer getId() {
		return id;
	}
	
	public void setEmployees(Collection set) {
		employees = set;
	}

	public void setId(Integer integer) {
		id = integer;
	}
}
