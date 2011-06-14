//$Id: Delete.java,v 1.1 2004/06/03 16:30:12 steveebersole Exp $
package org.hibernate.sql;

import org.hibernate.util.StringHelper;

/**
 * An SQL <tt>DELETE</tt> statement
 *
 * @author Gavin King
 */
public class Delete {

	private String tableName;
	private String[] primaryKeyColumnNames;
	private String versionColumnName;
	private String where;

	private String comment;
	public Delete setComment(String comment) {
		this.comment = comment;
		return this;
	}

	public Delete setTableName(String tableName) {
		this.tableName = tableName;
		return this;
	}

	public String toStatementString() {
		StringBuffer buf = new StringBuffer( tableName.length() + 10 );
		if (comment!=null) buf.append("/*").append(comment).append("*/");
		buf.append("delete from ")
			.append(tableName)
			.append(" where ")
			.append( StringHelper.join("=? and ", primaryKeyColumnNames) )
			.append("=?");
		if (where!=null) {
			buf.append(" and ")
				.append(where);
		}
		if (versionColumnName!=null) {
			buf.append(" and ")
				.append(versionColumnName)
				.append("=?");
		}
		return buf.toString();
	}

	public Delete setWhere(String where) {
		this.where=where;
		return this;
	}

	public Delete setPrimaryKeyColumnNames(String[] primaryKeyColumnNames) {
		this.primaryKeyColumnNames = primaryKeyColumnNames;
		return this;
	}

	public Delete setVersionColumnName(String versionColumnName) {
		this.versionColumnName = versionColumnName;
		return this;
	}

}
