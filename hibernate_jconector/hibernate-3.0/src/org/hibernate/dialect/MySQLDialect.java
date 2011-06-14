//$Id: MySQLDialect.java,v 1.26 2005/03/17 06:03:16 oneovthafew Exp $
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.Hibernate;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.exception.ViolatedConstraintNameExtracter;
import org.hibernate.exception.ErrorCodeConverter;
import org.hibernate.exception.SQLExceptionConverter;
import org.hibernate.cfg.Environment;
import org.hibernate.util.StringHelper;

/**
 * An SQL dialect for MySQL.
 * @author Gavin King
 */
public class MySQLDialect extends Dialect {

	public MySQLDialect() {
		super();
		registerColumnType( Types.BIT, "bit" );
		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "tinyint" );
		registerColumnType( Types.INTEGER, "integer" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, "longtext" );
		registerColumnType( Types.VARCHAR, 16777215, "mediumtext" );
		registerColumnType( Types.VARCHAR, 65535, "text" );
		registerColumnType( Types.VARCHAR, 255, "varchar($l)" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double precision" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "datetime" );
		registerColumnType( Types.VARBINARY, "longblob" );
		registerColumnType( Types.VARBINARY, 16777215, "mediumblob" );
		registerColumnType( Types.VARBINARY, 65535, "blob" );
		registerColumnType( Types.VARBINARY, 255, "tinyblob" );
		registerColumnType( Types.NUMERIC, "numeric($p,$s)" );
		registerColumnType( Types.BLOB, "longblob" );
		registerColumnType( Types.BLOB, 16777215, "mediumblob" );
		registerColumnType( Types.BLOB, 65535, "blob" );
		registerColumnType( Types.CLOB, "longtext" );
		registerColumnType( Types.CLOB, 16777215, "mediumtext" );
		registerColumnType( Types.CLOB, 65535, "text" );

		registerFunction("ascii", new StandardSQLFunction("ascii", Hibernate.INTEGER) );
		registerFunction("bin", new StandardSQLFunction("bin", Hibernate.STRING) );
		registerFunction("char_length", new StandardSQLFunction("char_length", Hibernate.LONG) );
		registerFunction("character_length", new StandardSQLFunction("character_length", Hibernate.LONG) );
		registerFunction("lcase", new StandardSQLFunction("lcase") );
		registerFunction("lower", new StandardSQLFunction("lower") );
		registerFunction("length", new StandardSQLFunction("length", Hibernate.LONG) );
		registerFunction("ltrim", new StandardSQLFunction("ltrim") );
		registerFunction("ord", new StandardSQLFunction("ord", Hibernate.INTEGER) );
		registerFunction("quote", new StandardSQLFunction("quote") );
		registerFunction("reverse", new StandardSQLFunction("reverse") );
		registerFunction("rtrim", new StandardSQLFunction("rtrim") );
		registerFunction("soundex", new StandardSQLFunction("soundex") );
		registerFunction("space", new StandardSQLFunction("space", Hibernate.STRING) );
		registerFunction("ucase", new StandardSQLFunction("ucase") );
		registerFunction("upper", new StandardSQLFunction("upper") );
		registerFunction("unhex", new StandardSQLFunction("unhex", Hibernate.STRING) );

		registerFunction("abs", new StandardSQLFunction("abs") );
		registerFunction("sign", new StandardSQLFunction("sign", Hibernate.INTEGER) );

		registerFunction("acos", new StandardSQLFunction("acos", Hibernate.DOUBLE) );
		registerFunction("asin", new StandardSQLFunction("asin", Hibernate.DOUBLE) );
		registerFunction("atan", new StandardSQLFunction("atan", Hibernate.DOUBLE) );
		registerFunction("cos", new StandardSQLFunction("cos", Hibernate.DOUBLE) );
		registerFunction("cot", new StandardSQLFunction("cot", Hibernate.DOUBLE) );
		registerFunction("crc32", new StandardSQLFunction("crc32", Hibernate.LONG) );
		registerFunction("exp", new StandardSQLFunction("exp", Hibernate.DOUBLE) );
		registerFunction("ln", new StandardSQLFunction("ln", Hibernate.DOUBLE) );
		registerFunction("log", new StandardSQLFunction("log", Hibernate.DOUBLE) );
		registerFunction("log2", new StandardSQLFunction("log2", Hibernate.DOUBLE) );
		registerFunction("log10", new StandardSQLFunction("log10", Hibernate.DOUBLE) );
		registerFunction("pi", new NoArgSQLFunction("pi", Hibernate.DOUBLE) );
		registerFunction("rand", new NoArgSQLFunction("rand", Hibernate.DOUBLE) );
		registerFunction("sin", new StandardSQLFunction("sin", Hibernate.DOUBLE) );
		registerFunction("sqrt", new StandardSQLFunction("sqrt", Hibernate.DOUBLE) );
		registerFunction("tan", new StandardSQLFunction("tan", Hibernate.DOUBLE) );

		registerFunction("radians", new StandardSQLFunction("radians", Hibernate.DOUBLE) );
		registerFunction("degrees", new StandardSQLFunction("degrees", Hibernate.DOUBLE) );

		registerFunction("ceiling", new StandardSQLFunction("ceiling", Hibernate.INTEGER) );
		registerFunction("ceil", new StandardSQLFunction("ceil", Hibernate.INTEGER) );
		registerFunction("floor", new StandardSQLFunction("floor", Hibernate.INTEGER) );
		registerFunction("round", new StandardSQLFunction("round", Hibernate.INTEGER) );

		registerFunction("curdate", new NoArgSQLFunction("curdate", Hibernate.DATE) );
		registerFunction("curtime", new NoArgSQLFunction("curtime", Hibernate.TIME) );
		registerFunction("current_date", new NoArgSQLFunction("current_date", Hibernate.DATE, false) );
		registerFunction("current_time", new NoArgSQLFunction("current_time", Hibernate.TIME, false) );
		registerFunction("current_timestamp", new NoArgSQLFunction("current_timestamp", Hibernate.TIMESTAMP, false) );
		registerFunction("date", new StandardSQLFunction("date", Hibernate.DATE) );
		registerFunction("day", new StandardSQLFunction("day", Hibernate.INTEGER) );
		registerFunction("dayofmonth", new StandardSQLFunction("dayofmonth", Hibernate.INTEGER) );
		registerFunction("dayname", new StandardSQLFunction("dayname", Hibernate.STRING) );
		registerFunction("dayofweek", new StandardSQLFunction("dayofweek", Hibernate.INTEGER) );
		registerFunction("dayofyear", new StandardSQLFunction("dayofyear", Hibernate.INTEGER) );
		registerFunction("from_days", new StandardSQLFunction("from_days", Hibernate.DATE) );
		registerFunction("from_unixtime", new StandardSQLFunction("from_unixtime", Hibernate.TIMESTAMP) );
		registerFunction("hour", new StandardSQLFunction("hour", Hibernate.INTEGER) );
		registerFunction("last_day", new StandardSQLFunction("last_day", Hibernate.DATE) );
		registerFunction("localtime", new NoArgSQLFunction("localtime", Hibernate.TIMESTAMP) );
		registerFunction("localtimestamp", new NoArgSQLFunction("localtimestamp", Hibernate.TIMESTAMP) );
		registerFunction("microseconds", new StandardSQLFunction("microseconds", Hibernate.INTEGER) );
		registerFunction("minute", new StandardSQLFunction("minute", Hibernate.INTEGER) );
		registerFunction("month", new StandardSQLFunction("month", Hibernate.INTEGER) );
		registerFunction("monthname", new StandardSQLFunction("monthname", Hibernate.STRING) );
		registerFunction("now", new NoArgSQLFunction("now", Hibernate.TIMESTAMP) );
		registerFunction("quarter", new StandardSQLFunction("quarter", Hibernate.INTEGER) );
		registerFunction("second", new StandardSQLFunction("second", Hibernate.INTEGER) );
		registerFunction("sec_to_time", new StandardSQLFunction("sec_to_time", Hibernate.TIME) );
		registerFunction("sysdate", new NoArgSQLFunction("sysdate", Hibernate.TIMESTAMP) );
		registerFunction("time", new StandardSQLFunction("time", Hibernate.TIME) );
		registerFunction("timestamp", new StandardSQLFunction("timestamp", Hibernate.TIMESTAMP) );
		registerFunction("time_to_sec", new StandardSQLFunction("time_to_sec", Hibernate.INTEGER) );
		registerFunction("to_days", new StandardSQLFunction("to_days", Hibernate.LONG) );
		registerFunction("unix_timestamp", new StandardSQLFunction("unix_timestamp", Hibernate.LONG) );
		registerFunction("utc_date", new NoArgSQLFunction("utc_date", Hibernate.STRING) );
		registerFunction("utc_time", new NoArgSQLFunction("utc_time", Hibernate.STRING) );
		registerFunction("utc_timestamp", new NoArgSQLFunction("utc_timestamp", Hibernate.STRING) );
		registerFunction("week", new StandardSQLFunction("week", Hibernate.INTEGER) );
		registerFunction("weekday", new StandardSQLFunction("weekday", Hibernate.INTEGER) );
		registerFunction("weekofyear", new StandardSQLFunction("weekofyear", Hibernate.INTEGER) );
		registerFunction("year", new StandardSQLFunction("year", Hibernate.INTEGER) );
		registerFunction("yearweek", new StandardSQLFunction("yearweek", Hibernate.INTEGER) );

		registerFunction("hex", new StandardSQLFunction("hex", Hibernate.STRING) );
		registerFunction("oct", new StandardSQLFunction("oct", Hibernate.STRING) );

		registerFunction("octet_length", new StandardSQLFunction("octet_length", Hibernate.LONG) );
		registerFunction("bit_length", new StandardSQLFunction("bit_length", Hibernate.LONG) );

		registerFunction("bit_count", new StandardSQLFunction("bit_count", Hibernate.LONG) );
		registerFunction("encrypt", new StandardSQLFunction("encrypt", Hibernate.STRING) );
		registerFunction("md5", new StandardSQLFunction("md5", Hibernate.STRING) );
		registerFunction("sha1", new StandardSQLFunction("sha1", Hibernate.STRING) );
		registerFunction("sha", new StandardSQLFunction("sha", Hibernate.STRING) );

		registerFunction( "concat", new SQLFunctionTemplate( Hibernate.STRING, "concat(?1, ?2)" ) );

		getDefaultProperties().setProperty(Environment.MAX_FETCH_DEPTH, "2");
		getDefaultProperties().setProperty(Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE);
	}

	public String getAddColumnString() {
		return "add column";
	}
	
	public boolean qualifyIndexName() {
		return false;
	}

	public boolean supportsIdentityColumns() {
		return true;
	}
	
	public String getIdentitySelectString() {
		return "select last_insert_id()";
	}

	public String getIdentityColumnString() {
		return "not null auto_increment"; //starts with 1, implicitly
	}

	public String getAddForeignKeyConstraintString(
			String constraintName, 
			String[] foreignKey, 
			String referencedTable, 
			String[] primaryKey
	) {
		String cols = StringHelper.join(", ", foreignKey);
		return new StringBuffer(30)
			.append(" add index ")
			.append(constraintName)
			.append(" (")
			.append(cols)
			.append("), add constraint ")
			.append(constraintName)
			.append(" foreign key (")
			.append(cols)
			.append(") references ")
			.append(referencedTable)
			.append(" (")
			.append( StringHelper.join(", ", primaryKey) )
			.append(')')
			.toString();
	}

	public boolean supportsLimit() {
		return true;
	}
	
	public String getDropForeignKeyString() {
		return " drop foreign key ";
	}

	public String getLimitString(String sql, boolean hasOffset) {
		return new StringBuffer( sql.length()+20 )
			.append(sql)
			.append( hasOffset ? " limit ?, ?" : " limit ?")
			.toString();
	}
	
	/*
	 * Temporary, until MySQL fix Connector/J bug
	 */
	/*public String getLimitString(String sql, int offset, int limit) {
		StringBuffer buf = new StringBuffer( sql.length()+20 )
			.append(sql);
		if (offset>0) {
			buf.append(" limit ")
				.append(offset)
				.append(", ")
				.append(limit);
		}
		else {
			buf.append(" limit ")
				.append(limit);
		}
		return buf.toString();
	}*/

	/*
	 * Temporary, until MySQL fix Connector/J bug
	 */
	/*public boolean supportsVariableLimit() {
		return false;
	}*/

	public char closeQuote() {
		return '`';
	}

	public char openQuote() {
		return '`';
	}

	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}

	public char getSchemaSeparator() {
	  return '_';
	}

	public String getSelectGUIDString() {
		return "select uuid()";
	}

	public boolean supportsCascadeDelete() {
		return false;
	}

	/**
	 * Build an instance of the SQLExceptionConverter preferred by this dialect for
	 * converting SQLExceptions into Hibernate's JDBCException hierarchy.  The default
	 * Dialect implementation simply returns a converter based on X/Open SQLState codes.
	 * <p/>
	 * It is strongly recommended that specific Dialect implementations override this
	 * method, since interpretation of a SQL error is much more accurate when based on
	 * the ErrorCode rather than the SQLState.  Unfortunately, the ErrorCode is a vendor-
	 * specific approach.
	 *
	 * @return The Dialect's preferred SQLExceptionConverter.
	 */
	public SQLExceptionConverter buildSQLExceptionConverter() {
		return new ExceptionConverter( getViolatedConstraintNameExtracter() );
	}

	private static class ExceptionConverter extends ErrorCodeConverter {
		private int[] sqlGrammarCodes = new int[] { 1054, 1064, 1146 };
		private int[] integrityViolationCodes = new int[] { 1062, 1216 };
		private int[] connectionCodes = new int[] { 1049 };
		private int[] lockAcquisitionErrorCodes = new int[] { 1099, 1100, 1150, 1165, 1192, 1205, 1206, 1207, 1213, 1223 };

		public ExceptionConverter(ViolatedConstraintNameExtracter extracter) {
			super(extracter);
		}

		protected int[] getSQLGrammarErrorCodes() {
			return sqlGrammarCodes;
		}

		protected int[] getIntegrityViolationErrorCodes() {
			return integrityViolationCodes;
		}

		protected int[] getConnectionErrorCodes() {
			return connectionCodes;
		}

		protected int[] getLockAcquisitionErrorCodes() {
			return lockAcquisitionErrorCodes;
		}
	}
}
