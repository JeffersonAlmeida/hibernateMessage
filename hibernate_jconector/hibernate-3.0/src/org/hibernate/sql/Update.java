//$Id: Update.java,v 1.4 2005/02/12 07:19:46 steveebersole Exp $
package org.hibernate.sql;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.type.LiteralType;
import org.hibernate.util.StringHelper;

import org.apache.commons.collections.SequencedHashMap;

/**
 * An SQL <tt>UPDATE</tt> statement
 *
 * @author Gavin King
 */
public class Update {

	private String tableName;
	private String[] primaryKeyColumnNames;
	private String versionColumnName;
	private String where;

	private String comment;
	public Update setComment(String comment) {
		this.comment = comment;
		return this;
	}

	private Map columns = new SequencedHashMap();
	private Map whereColumns = new SequencedHashMap();

	public Update addColumns(String[] columnNames) {
		for ( int i=0; i<columnNames.length; i++ ) {
			addColumn( columnNames[i] );
		}
		return this;
	}

	public Update addColumns(String[] columnNames, boolean[] updateable) {
		for ( int i=0; i<columnNames.length; i++ ) {
			if ( updateable[i] ) addColumn( columnNames[i] );
		}
		return this;
	}

	public Update addColumns(String[] columnNames, String value) {
		for ( int i=0; i<columnNames.length; i++ ) {
			addColumn( columnNames[i], value );
		}
		return this;
	}

	public Update addColumn(String columnName) {
		return addColumn(columnName, "?");
	}

	public Update addColumn(String columnName, String value) {
		columns.put(columnName, value);
		return this;
	}

	public Update addColumn(String columnName, Object value, LiteralType type) throws Exception {
		return addColumn( columnName, type.objectToSQLString(value) );
	}

	public Update addWhereColumns(String[] columnNames) {
		for ( int i=0; i<columnNames.length; i++ ) {
			addWhereColumn( columnNames[i] );
		}
		return this;
	}

	public Update addWhereColumns(String[] columnNames, String value) {
		for ( int i=0; i<columnNames.length; i++ ) {
			addWhereColumn( columnNames[i], value );
		}
		return this;
	}

	public Update addWhereColumn(String columnName) {
		return addWhereColumn(columnName, "=?");
	}

	public Update addWhereColumn(String columnName, String value) {
		whereColumns.put(columnName, value);
		return this;
	}

	public Update setTableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	public Update setWhere(String where) {
		this.where=where;
		return this;
	}

	public String toStatementString() {
		StringBuffer buf = new StringBuffer( columns.size()*15 + tableName.length() + 10 );
		if (comment!=null) buf.append("/*").append(comment).append("*/");
		buf.append("update ")
			.append(tableName)
			.append(" set ");
		Iterator iter = columns.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry e = (Map.Entry) iter.next();
			buf.append( e.getKey() )
				.append('=')
				.append( e.getValue() );
			if ( iter.hasNext() ) buf.append(", ");
		}
		buf.append(" where ")
			.append( StringHelper.join("=? and ", primaryKeyColumnNames) )
			.append("=?");
		if (where!=null) {
			buf.append(" and ")
				.append(where);
		}
		iter = whereColumns.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry e = (Map.Entry) iter.next();
			buf.append(" and ")
				.append( e.getKey() )
				.append( e.getValue() );
		}
		if (versionColumnName!=null) {
			buf.append(" and ")
				.append(versionColumnName)
				.append("=?");
		}
		return buf.toString();
	}

	public Update setPrimaryKeyColumnNames(String[] primaryKeyColumnNames) {
		this.primaryKeyColumnNames = primaryKeyColumnNames;
		return this;
	}

	public Update setVersionColumnName(String versionColumnName) {
		this.versionColumnName = versionColumnName;
		return this;
	}

}
