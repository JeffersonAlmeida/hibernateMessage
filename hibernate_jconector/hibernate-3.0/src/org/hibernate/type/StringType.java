//$Id: StringType.java,v 1.2 2004/09/25 11:22:19 oneovthafew Exp $
package org.hibernate.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * <tt>string</tt>: A type that maps an SQL VARCHAR to a Java String.
 * @author Gavin King
 */
public class StringType extends ImmutableType implements DiscriminatorType {

	public Object get(ResultSet rs, String name) throws SQLException {
		return rs.getString(name);
	}

	public Class getReturnedClass() {
		return String.class;
	}

	public void set(PreparedStatement st, Object value, int index) throws SQLException {
		st.setString(index, (String) value);
	}

	public int sqlType() {
		return Types.VARCHAR;
	}

	public String getName() { return "string"; }

	public String objectToSQLString(Object value) throws Exception {
		return '\'' + (String) value + '\'';
	}

	public Object stringToObject(String xml) throws Exception {
		return xml;
	}

	public String toString(Object value) {
		return (String) value;
	}

	public Object fromStringValue(String xml) {
		return xml;
	}

}





