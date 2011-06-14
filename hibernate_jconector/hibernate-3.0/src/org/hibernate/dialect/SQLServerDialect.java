//$Id: SQLServerDialect.java,v 1.12 2005/03/23 19:45:26 maxcsaucdk Exp $
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.LockMode;
import org.hibernate.Hibernate;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;

/**
 * A dialect for Microsoft SQL Server 2000
 * @author Gavin King
 */
public class SQLServerDialect extends SybaseDialect {

	public SQLServerDialect() {		
		registerFunction( "concat", new SQLFunctionTemplate( Hibernate.STRING, "(?1 + ?2)" ) );
		registerColumnType( Types.VARBINARY, "image" );
		registerColumnType( Types.VARBINARY, 8000, "varbinary($l)" );
	}

	public String getNoColumnsInsertString() {
		return "default values";
	}

	static int getAfterSelectInsertPoint(String sql) {
		final int selectDistinctIndex = sql.indexOf( "select distinct" );
		if ( selectDistinctIndex >= 0 ) {
			return selectDistinctIndex + 15;
		}
		else {
			return sql.indexOf( "select" ) + 6;
		}
	}

	public String getLimitString(String querySelect, int offset, int limit) {
		if (offset>0) throw new UnsupportedOperationException("sql server has no offset");
		return new StringBuffer( querySelect.length()+8 )
			.append(querySelect)
			.insert( getAfterSelectInsertPoint(querySelect), " top " + limit )
			.toString();
	}

	/**
	 * Use <tt>insert table(...) values(...) select SCOPE_IDENTITY()</tt>
	 *
	 * @author <a href="mailto:jkristian@docent.com">John Kristian</a>
	 */
	public String appendIdentitySelectToInsert(String insertSQL) {
		return insertSQL + " select scope_identity()";
	}

	public boolean supportsLimit() {
		return true;
	}

	public boolean useMaxForLimit() {
		return true;
	}

	public boolean supportsLimitOffset() {
		return false;
	}

	public boolean supportsVariableLimit() {
		return false;
	}

	public char closeQuote() {
		return ']';
	}

	public char openQuote() {
		return '[';
	}

	public String appendLockHint(LockMode mode, String tableName) {
		if ( mode.greaterThan(LockMode.READ) ) {
			return tableName + " with (updlock, rowlock)";
		}
		else {
			return tableName;
		}
	}
	
	public String getSelectGUIDString() {
		return "select newid()";
	}	
	
}
