//$Id: LocaleType.java,v 1.7 2005/02/19 12:58:24 oneovthafew Exp $
package org.hibernate.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.StringTokenizer;

import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;

/**
 * <tt>locale</tt>: A type that maps an SQL VARCHAR to a Java Locale.
 * @author Gavin King
 */
public class LocaleType extends ImmutableType implements LiteralType {

	public Object get(ResultSet rs, String name) throws HibernateException, SQLException {
		return fromStringValue( (String) Hibernate.STRING.get(rs, name) );
	}

	public void set(PreparedStatement st, Object value, int index) throws HibernateException, SQLException {
		Hibernate.STRING.set(st, value.toString(), index);
	}

	public Object fromStringValue(String string) {
		if (string == null) {
			return null;
		}
		else {
			StringTokenizer tokens = new StringTokenizer(string, "_");
			String language = tokens.hasMoreTokens() ? tokens.nextToken() : "";
			String country = tokens.hasMoreTokens() ? tokens.nextToken() : "";
			// Need to account for allowable '_' within the variant
			String variant = "";
			String sep = "";
			while ( tokens.hasMoreTokens() ) {
				variant += sep + tokens.nextToken();
				sep = "_";
			}
			return new Locale(language, country, variant);
		}
	}
	
	public int compare(Object x, Object y, EntityMode entityMode) {
		return x.toString().compareTo( y.toString() );
	}

	public int sqlType() {
		return Hibernate.STRING.sqlType();
	}

	public String toString(Object value) throws HibernateException {
		return value.toString();
	}

	public Class getReturnedClass() {
		return Locale.class;
	}

	public String getName() {
		return "locale";
	}

	public String objectToSQLString(Object value) throws Exception {
		return ( (LiteralType) Hibernate.STRING ).objectToSQLString( value.toString() );
	}

}






