//$Id: Animal.java,v 1.4 2005/02/28 20:18:28 steveebersole Exp $
package org.hibernate.test.hql;

import java.util.Set;
import java.util.HashSet;

/**
 * @author Gavin King
 */
public class Animal {
	private Long id;
	private float bodyWeight;
	private Set offspring;
	private Animal mother;
	private Animal father;
	private String description;
	private Zoo zoo;

	public Animal() {
	}

	public Animal(String description, float bodyWeight) {
		this.description = description;
		this.bodyWeight = bodyWeight;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public float getBodyWeight() {
		return bodyWeight;
	}

	public void setBodyWeight(float bodyWeight) {
		this.bodyWeight = bodyWeight;
	}

	public Set getOffspring() {
		return offspring;
	}

	public void addOffspring(Animal offspring) {
		if ( this.offspring == null ) {
			this.offspring = new HashSet();
		}

		this.offspring.add( offspring );
	}

	public void setOffspring(Set offspring) {
		this.offspring = offspring;
	}

	public Animal getMother() {
		return mother;
	}

	public void setMother(Animal mother) {
		this.mother = mother;
	}

	public Animal getFather() {
		return father;
	}

	public void setFather(Animal father) {
		this.father = father;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Zoo getZoo() {
		return zoo;
	}

	public void setZoo(Zoo zoo) {
		this.zoo = zoo;
	}
}
