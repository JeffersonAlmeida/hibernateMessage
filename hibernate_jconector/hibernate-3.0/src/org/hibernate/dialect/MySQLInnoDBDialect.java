//$Id: MySQLInnoDBDialect.java,v 1.3 2005/02/12 07:19:15 steveebersole Exp $
package org.hibernate.dialect;

/**
 * @author Gavin King
 */
public class MySQLInnoDBDialect extends MySQLDialect {

	public String getTableTypeString() {
		return " type=InnoDB";
	}

	public boolean hasSelfReferentialForeignKeyBug() {
		return true;
	}
	
}
