//$Id: MySQLMyISAMDialect.java,v 1.4 2005/02/12 07:19:15 steveebersole Exp $
package org.hibernate.dialect;

/**
 * @author Gavin King
 */
public class MySQLMyISAMDialect extends MySQLDialect {

	public String getTableTypeString() {
		return " type=MyISAM";
	}

	public boolean dropConstraints() {
		return false;
	}

}
