//$Id: QueryKey.java,v 1.3 2005/02/16 12:50:10 oneovthafew Exp $
package org.hibernate.cache;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.RowSelection;
import org.hibernate.type.Type;
import org.hibernate.util.EqualsHelper;

/**
 * A key that identifies a particular query with bound parameter values
 * @author Gavin King
 */
public class QueryKey implements Serializable {
	private final String sqlQueryString;
	private final Type[] types;
	private final Object[] values;
	private final Integer firstRow;
	private final Integer maxRows;
	private final Map namedParameters;
	private EntityMode entityMode;
	
	public QueryKey(String queryString, QueryParameters queryParameters, EntityMode entityMode) {
		this.sqlQueryString = queryString;
		this.types = queryParameters.getPositionalParameterTypes();
		this.values = queryParameters.getPositionalParameterValues();
		RowSelection selection = queryParameters.getRowSelection();
		if (selection!=null) {
			firstRow = selection.getFirstRow();
			maxRows = selection.getMaxRows();
		}
		else {
			firstRow = null;
			maxRows = null;
		}
		this.namedParameters = queryParameters.getNamedParameters();
		this.entityMode = entityMode;
	}
	
	public boolean equals(Object other) {
		QueryKey that = (QueryKey) other;
		if ( !sqlQueryString.equals(that.sqlQueryString) ) return false;
		if ( !EqualsHelper.equals(firstRow, that.firstRow) || !EqualsHelper.equals(maxRows, that.maxRows) ) return false;
		if (types==null) {
			if (that.types!=null) return false;
		}
		else {
			if (that.types==null) return false;
			if ( types.length!=that.types.length ) return false;
			for ( int i=0; i<types.length; i++ ) {
				//if ( !types[i].equals( that.types[i] ) ) return false;
				//if ( !EqualsHelper.equals( values[i], that.values[i] ) ) return false;
				if ( types[i].getReturnedClass() != that.types[i].getReturnedClass() ) return false;
				if ( !types[i].isEqual( values[i], that.values[i], entityMode ) ) return false;
			}
		}
		if ( !EqualsHelper.equals(namedParameters, that.namedParameters) ) return false;
		return true;
	}
	
	public int hashCode() {
		int result = 13;
		result = 37 * result + ( firstRow==null ? 0 : firstRow.hashCode() );
		result = 37 * result + ( maxRows==null ? 0 : maxRows.hashCode() );
		result = 37 * result + ( namedParameters==null ? 0 : namedParameters.hashCode() );
		/*for ( int i=0; i<types.length; i++ ) {
			result = 37 * result + ( types[i]==null ? 0 : types[i].hashCode() );
		}
		for ( int i=0; i<values.length; i++ ) {
			result = 37 * result + ( values[i]==null ? 0 : values[i].hashCode() );
		}*/
		for ( int i=0; i<values.length; i++ ) {
			result = 37 * result + ( values[i]==null ? 0 : types[i].getHashCode( values[i], entityMode ) );
		}
		result = 37 * result + sqlQueryString.hashCode();
		return result;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer()
			.append("sql: ")
			.append(sqlQueryString);
		if (values!=null) {
			buf.append("; parameters: ");
			for (int i=0; i<values.length; i++) {
				buf.append( values[i] )
					.append(", ");
			}
		}
		if (namedParameters!=null) {
			buf.append("; named parameters: ")
				.append(namedParameters);
		}
		if (firstRow!=null) buf.append("; first row: ").append(firstRow);
		if (maxRows!=null) buf.append("; max rows: ").append(maxRows);
		return buf.toString();
	}
	
}
