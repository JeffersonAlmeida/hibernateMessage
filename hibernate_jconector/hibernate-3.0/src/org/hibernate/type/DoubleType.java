//$Id: DoubleType.java,v 1.2 2004/09/11 11:29:23 oneovthafew Exp $
package org.hibernate.type;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * <tt>double</tt>: A type that maps an SQL DOUBLE to a Java Double.
 * @author Gavin King
 */
public class DoubleType extends PrimitiveType {

	public Serializable getDefaultValue() {
		return new Double(0.0);
	}
	
	public Object get(ResultSet rs, String name) throws SQLException {
		return new Double( rs.getDouble(name) );
	}

	public Class getPrimitiveClass() {
		return double.class;
	}

	public Class getReturnedClass() {
		return Double.class;
	}

	public void set(PreparedStatement st, Object value, int index)
		throws SQLException {

		st.setDouble( index, ( (Double) value ).doubleValue() );
	}

	public int sqlType() {
		return Types.DOUBLE;
	}
	public String getName() { return "double"; }

	public String objectToSQLString(Object value) throws Exception {
		return value.toString();
	}

	public Object fromStringValue(String xml) {
		return new Double(xml);
	}

}




