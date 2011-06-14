//$Id: LiteralType.java,v 1.1 2004/06/03 16:31:30 steveebersole Exp $
package org.hibernate.type;

/**
 * A type that may appear as an SQL literal
 * @author Gavin King
 */
public interface LiteralType {
	/**
	 * String representation of the value, suitable for embedding in
	 * an SQL statement.
	 * @param value
	 * @return String
	 * @throws Exception
	 */
	public String objectToSQLString(Object value) throws Exception;

}






