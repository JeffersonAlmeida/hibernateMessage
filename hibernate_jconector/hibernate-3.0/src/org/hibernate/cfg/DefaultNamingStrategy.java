//$Id: DefaultNamingStrategy.java,v 1.1 2004/06/03 16:30:05 steveebersole Exp $
package org.hibernate.cfg;

import java.io.Serializable;

import org.hibernate.util.StringHelper;

/**
 * The default <tt>NamingStrategy</tt>
 * @see ImprovedNamingStrategy a better alternative
 * @author Gavin King
 */
public class DefaultNamingStrategy implements NamingStrategy, Serializable {

	/**
	 * The singleton instance
	 */
	public static final NamingStrategy INSTANCE = new DefaultNamingStrategy();

	protected DefaultNamingStrategy() {}

	/**
	 * Return the unqualified class name
	 */
	public String classToTableName(String className) {
		return StringHelper.unqualify(className);
	}
	/**
	 * Return the unqualified property name
	 */
	public String propertyToColumnName(String propertyName) {
		return StringHelper.unqualify(propertyName);
	}
	/**
	 * Return the argument
	 */
	public String tableName(String tableName) {
		return tableName;
	}
	/**
	 * Return the argument
	 */
	public String columnName(String columnName) {
		return columnName;
	}
	/**
	 * Return the unqualified property name
	 */
	public String propertyToTableName(String className, String propertyName) {
		return StringHelper.unqualify(propertyName);
	}
}