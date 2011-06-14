//$Id: Zoo.java,v 1.4 2005/03/01 01:21:51 steveebersole Exp $
package org.hibernate.test.hql;

import java.util.Map;

/**
 * @author Gavin King
 */
public class Zoo {
	private Long id;
	private String name;
	private Map mammals;
	public Map getAnimals() {
		return animals;
	}
	public void setAnimals(Map animals) {
		this.animals = animals;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map getMammals() {
		return mammals;
	}
	public void setMammals(Map mammals) {
		this.mammals = mammals;
	}
	private Map animals;
}
