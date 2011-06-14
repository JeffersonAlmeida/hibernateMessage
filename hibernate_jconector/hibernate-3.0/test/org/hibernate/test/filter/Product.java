// $Id: Product.java,v 1.2 2004/08/10 16:37:37 steveebersole Exp $
package org.hibernate.test.filter;

import java.util.Set;
import java.util.Date;

/**
 * @author Steve Ebersole
 */
public class Product {
	private Long id;
	private String name;
	private int stockNumber;  // int for ease of hashCode() impl
	private Date effectiveStartDate;
	private Date effectiveEndDate;
	private Set orderLineItems;

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

	public Set getOrderLineItems() {
		return orderLineItems;
	}

	public void setOrderLineItems(Set orderLineItems) {
		this.orderLineItems = orderLineItems;
	}

	public int getStockNumber() {
		return stockNumber;
	}

	public void setStockNumber(int stockNumber) {
		this.stockNumber = stockNumber;
	}

	public int hashCode() {
		return stockNumber;
	}

	public boolean equals(Object obj) {
		return ( (Product) obj ).stockNumber == this.stockNumber;
	}

	public Date getEffectiveStartDate() {
		return effectiveStartDate;
	}

	public void setEffectiveStartDate(Date effectiveStartDate) {
		this.effectiveStartDate = effectiveStartDate;
	}

	public Date getEffectiveEndDate() {
		return effectiveEndDate;
	}

	public void setEffectiveEndDate(Date effectiveEndDate) {
		this.effectiveEndDate = effectiveEndDate;
	}
}
