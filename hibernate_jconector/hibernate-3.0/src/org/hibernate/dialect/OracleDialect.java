//$Id: OracleDialect.java,v 1.11 2005/03/25 03:57:15 oneovthafew Exp $
package org.hibernate.dialect;

import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.DecodeCaseFragment;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.OracleJoinFragment;

import java.sql.Types;

/**
 * An SQL dialect for Oracle, compatible with Oracle 8.
 * @author Gavin King
 */
public class OracleDialect extends Oracle9Dialect {

	public OracleDialect() {
		super();
		// Oracle8 and previous define only a "DATE" type which
		//      is used to represent all aspects of date/time
		registerColumnType( Types.TIMESTAMP, "date" );
	}

	public JoinFragment createOuterJoinFragment() {
		return new OracleJoinFragment();
	}
	public CaseFragment createCaseFragment() {
		return new DecodeCaseFragment();
	}

	public String getLimitString(String sql, boolean hasOffset) {

		sql = sql.trim();
		boolean isForUpdate = false;
		if ( sql.toLowerCase().endsWith(" for update") ) {
			sql = sql.substring( 0, sql.length()-11 );
			isForUpdate = true;
		}
		
		StringBuffer pagingSelect = new StringBuffer( sql.length()+100 );
		if (hasOffset) {
			pagingSelect.append("select * from ( select row_.*, rownum rownum_ from ( ");
		}
		else {
			pagingSelect.append("select * from ( ");
		}
		pagingSelect.append(sql);
		if (hasOffset) {
			pagingSelect.append(" ) row_ ) where rownum_ <= ? and rownum_ > ?");
		}
		else {
			pagingSelect.append(" ) where rownum <= ?");
		}

		if (isForUpdate) pagingSelect.append(" for update");
		
		return pagingSelect.toString();
	}

	public String getSelectClauseNullString(int sqlType) {
		String literal;
		switch(sqlType) {
			case Types.VARCHAR:
				literal = "'x'";
				break;
			case Types.CHAR:
				literal = "'x'";
				break;
			case Types.DATE:
				literal = "'2000-1-1'";
				break;
			case Types.TIMESTAMP:
				literal = "'2000-1-1 00:00:00'";
			case Types.TIME:
				literal = "'00:00:00'";
			default:
				literal = "0";
		}
		return "nullif(" + literal + ',' + literal + ')';
	}
	
}
