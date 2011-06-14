//$Id: ColumnMetadata.java,v 1.2 2004/08/13 08:05:47 oneovthafew Exp $
package org.hibernate.tool.hbm2ddl;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JDBC column metadata
 * @author Christoph Sturm
 */
public class ColumnMetadata {
	private final String name;
	private final String typeName;
	private final int columnSize;
	private final int decimalDigits;
	private final String isNullable;

	ColumnMetadata(ResultSet rs) throws SQLException {
		name = rs.getString("COLUMN_NAME");
		typeName = rs.getString("TYPE_NAME");
		columnSize = rs.getInt("COLUMN_SIZE");
		decimalDigits = rs.getInt("DECIMAL_DIGITS");
		isNullable = rs.getString("IS_NULLABLE");
	}

	public String getName() {
		return name;
	}

	public String getTypeName() {
		return typeName;
	}

	public int getColumnSize() {
		return columnSize;
	}

	public int getDecimalDigits() {
		return decimalDigits;
	}

	public String getNullable() {
		return isNullable;
	}

	public String toString() {
		return "ColumnMetadata(" + name + ')';
	}

}






