//$Id: SybaseDialect.java,v 1.13 2005/03/21 09:54:26 maxcsaucdk Exp $
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;

/**
 * An SQL dialect compatible with Sybase and MS SQL Server.
 * @author Gavin King
 */

public class SybaseDialect extends Dialect {
	public SybaseDialect() {
		super();
		registerColumnType( Types.BIT, "tinyint" ); //Sybase BIT type does not support null values
		registerColumnType( Types.BIGINT, "numeric(19,0)" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "tinyint" );
		registerColumnType( Types.INTEGER, "int" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double precision" );
		registerColumnType( Types.DATE, "datetime" );
		registerColumnType( Types.TIME, "datetime" );
		registerColumnType( Types.TIMESTAMP, "datetime" );
		registerColumnType( Types.VARBINARY, "varbinary($l)" );
		registerColumnType( Types.NUMERIC, "numeric($p,$s)" );
		registerColumnType( Types.BLOB, "image" );
		registerColumnType( Types.CLOB, "text" );

		registerFunction( "ascii", new StandardSQLFunction("ascii", Hibernate.INTEGER) );
		registerFunction( "char", new StandardSQLFunction("char", Hibernate.CHARACTER) );
		registerFunction( "len", new StandardSQLFunction("len", Hibernate.LONG) );
		registerFunction( "lower", new StandardSQLFunction("lower") );
		registerFunction( "upper", new StandardSQLFunction("upper") );
		registerFunction( "str", new StandardSQLFunction("str", Hibernate.STRING) );
		registerFunction( "ltrim", new StandardSQLFunction("ltrim") );
		registerFunction( "rtrim", new StandardSQLFunction("rtrim") );
		registerFunction( "reverse", new StandardSQLFunction("reverse") );
		registerFunction( "space", new StandardSQLFunction("space", Hibernate.STRING) );

		registerFunction( "user", new NoArgSQLFunction("user", Hibernate.STRING) );

		registerFunction( "current_timestamp", new NoArgSQLFunction("getdate", Hibernate.TIMESTAMP, false) );
		registerFunction( "current_time", new NoArgSQLFunction("getdate", Hibernate.TIME, false) );
		registerFunction( "current_date", new NoArgSQLFunction("getdate", Hibernate.DATE, false) );
		registerFunction( "getdate", new NoArgSQLFunction("getdate", Hibernate.TIMESTAMP) );
		registerFunction( "getutcdate", new NoArgSQLFunction("getutcdate", Hibernate.TIMESTAMP) );
		registerFunction( "day", new StandardSQLFunction("day", Hibernate.INTEGER) );
		registerFunction( "month", new StandardSQLFunction("month", Hibernate.INTEGER) );
		registerFunction( "year", new StandardSQLFunction("year", Hibernate.INTEGER) );
		registerFunction( "datename", new StandardSQLFunction("datename", Hibernate.STRING) );

		registerFunction( "abs", new StandardSQLFunction("abs") );
		registerFunction( "sign", new StandardSQLFunction("sign", Hibernate.INTEGER) );

		registerFunction( "acos", new StandardSQLFunction("acos", Hibernate.DOUBLE) );
		registerFunction( "asin", new StandardSQLFunction("asin", Hibernate.DOUBLE) );
		registerFunction( "atan", new StandardSQLFunction("atan", Hibernate.DOUBLE) );
		registerFunction( "cos", new StandardSQLFunction("cos", Hibernate.DOUBLE) );
		registerFunction( "cot", new StandardSQLFunction("cot", Hibernate.DOUBLE) );
		registerFunction( "exp", new StandardSQLFunction("exp", Hibernate.DOUBLE) );
		registerFunction( "log", new StandardSQLFunction( "log", Hibernate.DOUBLE) );
		registerFunction( "log10", new StandardSQLFunction("log10", Hibernate.DOUBLE) );
		registerFunction( "sin", new StandardSQLFunction("sin", Hibernate.DOUBLE) );
		registerFunction( "sqrt", new StandardSQLFunction("sqrt", Hibernate.DOUBLE) );
		registerFunction( "tan", new StandardSQLFunction("tan", Hibernate.DOUBLE) );
		registerFunction( "pi", new NoArgSQLFunction("pi", Hibernate.DOUBLE) );
		registerFunction( "square", new StandardSQLFunction("square") );
		registerFunction( "rand", new StandardSQLFunction("rand", Hibernate.FLOAT) );

		registerFunction("radians", new StandardSQLFunction("radians", Hibernate.DOUBLE) );
		registerFunction("degrees", new StandardSQLFunction("degrees", Hibernate.DOUBLE) );

		registerFunction( "round", new StandardSQLFunction("round") );
		registerFunction( "ceiling", new StandardSQLFunction("ceiling") );
		registerFunction( "floor", new StandardSQLFunction("floor") );

		registerFunction( "concat", new SQLFunctionTemplate( Hibernate.STRING, "concat(?1, ?2)" ) );		

		getDefaultProperties().setProperty(Environment.STATEMENT_BATCH_SIZE, NO_BATCH);
	}

	public String getAddColumnString() {
		return "add";
	}
	public String getNullColumnString() {
		return " null";
	}
	public boolean qualifyIndexName() {
		return false;
	}

	public String getForUpdateString() {
		return "";
	}

	public boolean supportsIdentityColumns() {
		return true;
	}
	public String getIdentitySelectString() {
		return "select @@identity";
	}
	public String getIdentityColumnString() {
		return "identity not null"; //starts with 1, implicitly
	}

	public boolean supportsInsertSelectIdentity() {
		return true;
	}

	public String appendIdentitySelectToInsert(String insertSQL) {
		return insertSQL + "\nselect @@identity";
	}

	public String appendLockHint(LockMode mode, String tableName) {
		if ( mode.greaterThan(LockMode.READ) ) {
			return tableName + " holdlock";
		}
		else {
			return tableName;
		}
	}
	
	public int registerResultSetOutParameter(CallableStatement statement,int col) throws SQLException {
		return col; // sql server just returns automatically.
	}
	
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
//		 This will work with SQL Server/Sybase 
		boolean isResultSet = ps.execute(); 
//		 This assumes you will want to ignore any update counts 
		while (!isResultSet && ps.getUpdateCount() != -1) { 
		    isResultSet = ps.getMoreResults(); 
		} 
		ResultSet rs = ps.getResultSet(); 
//		 You may still have other ResultSets or update counts left to process here 
//		 but you can't do it now or the ResultSet you just got will be closed 
		return rs;
	}

}
