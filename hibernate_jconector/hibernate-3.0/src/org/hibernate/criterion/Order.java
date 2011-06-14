//$Id: Order.java,v 1.8 2005/02/12 07:19:14 steveebersole Exp $
package org.hibernate.criterion;

import java.io.Serializable;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;

/**
 * Represents an order imposed upon a <tt>Criteria</tt> result set
 * @author Gavin King
 */
public class Order implements Serializable {

	private boolean ascending;
	private String propertyName;
	
	public String toString() {
		return propertyName + ' ' + (ascending?"asc":"desc");
	}

	/**
	 * Constructor for Order.
	 */
	protected Order(String propertyName, boolean ascending) {
		this.propertyName = propertyName;
		this.ascending = ascending;
	}

	/**
	 * Render the SQL fragment
	 *
	 */
	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		String[] columns = criteriaQuery.getColumnsUsingProjection(criteria, propertyName);
		StringBuffer fragment = new StringBuffer();
		for ( int i=0; i<columns.length; i++ ) {
			fragment.append( columns[0] ).append( ascending ? " asc" : " desc" );
			if ( i<columns.length-1 ) fragment.append(", ");
		}
		return fragment.toString();
	}

	/**
	 * Ascending order
	 *
	 * @param propertyName
	 * @return Order
	 */
	public static Order asc(String propertyName) {
		return new Order(propertyName, true);
	}

	/**
	 * Descending order
	 *
	 * @param propertyName
	 * @return Order
	 */
	public static Order desc(String propertyName) {
		return new Order(propertyName, false);
	}

}
