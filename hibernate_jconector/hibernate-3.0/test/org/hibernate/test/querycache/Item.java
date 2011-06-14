//$Id: Item.java,v 1.1 2004/12/22 22:33:51 oneovthafew Exp $
package org.hibernate.test.querycache;


/**
 * @author Gavin King
 */
public class Item {
	private long id;
	private String name;
	private String description;
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
