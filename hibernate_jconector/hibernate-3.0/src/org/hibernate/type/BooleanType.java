//$Id: BooleanType.java,v 1.3 2005/02/19 12:58:24 oneovthafew Exp $
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * <tt>boolean</tt>: A type that maps an SQL BIT to a Java Boolean.
 * @author Gavin King
 */
public class BooleanType extends PrimitiveType implements DiscriminatorType {

	private static final String TRUE = "1";
	private static final String FALSE = "0";

	public Serializable getDefaultValue() {
		throw new UnsupportedOperationException("not a valid id type");
	}
	
	public Object get(ResultSet rs, String name) throws SQLException {
		return rs.getBoolean(name) ? Boolean.TRUE : Boolean.FALSE;
	}

	public Class getPrimitiveClass() {
		return boolean.class;
	}

	public Class getReturnedClass() {
		return Boolean.class;
	}

	public void set(PreparedStatement st, Object value, int index)
	throws SQLException {
		st.setBoolean( index, ( (Boolean) value ).booleanValue() );
	}

	public int sqlType() {
		return Types.BIT;
	}

	public String getName() { return "boolean"; }

	public String objectToSQLString(Object value) throws Exception {
		return ( ( (Boolean) value ).booleanValue() ) ? TRUE : FALSE;
	}

	public Object stringToObject(String xml) throws Exception {
		return fromStringValue(xml);
	}

	public Object fromStringValue(String xml) {
		return Boolean.valueOf(xml);
	}

}





