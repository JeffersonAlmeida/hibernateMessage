//$Id: Transaction.java,v 1.1 2005/03/29 03:06:25 oneovthafew Exp $
package org.hibernate.test.cut;

/**
 * @author Gavin King
 */
public class Transaction {

	private Long id;
	private String description;
	private MonetoryAmount value;
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public MonetoryAmount getValue() {
		return value;
	}
	
	public void setValue(MonetoryAmount value) {
		this.value = value;
	}

}
