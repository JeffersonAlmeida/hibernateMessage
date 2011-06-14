//$Id: HSQLDialect.java,v 1.23 2005/02/25 04:26:54 oneovthafew Exp $
package org.hibernate.dialect;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.exception.ErrorCodeConverter;
import org.hibernate.exception.SQLExceptionConverter;
import org.hibernate.exception.ViolatedConstraintNameExtracter;

import java.sql.Types;

/**
 * An SQL dialect compatible with HSQLDB (Hypersonic SQL).
 *
 * @author Christoph Sturm, Phillip Baird
 */
public class HSQLDialect extends Dialect {

	public HSQLDialect() {
		super();
		registerColumnType( Types.BIGINT, "bigint" );
		registerColumnType( Types.BINARY, "binary" );
		registerColumnType( Types.BIT, "bit" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.DECIMAL, "decimal" );
		registerColumnType( Types.DOUBLE, "double" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.INTEGER, "integer" );
		registerColumnType( Types.LONGVARBINARY, "longvarbinary" );
		registerColumnType( Types.LONGVARCHAR, "longvarchar" );
		registerColumnType( Types.SMALLINT, "smallint" );
		registerColumnType( Types.TINYINT, "tinyint" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.VARBINARY, "varbinary($l)" );
		registerColumnType( Types.NUMERIC, "numeric" );
		//HSQL has no Blob/Clob support .... but just put these here for now!
		registerColumnType( Types.BLOB, "longvarbinary" );
		registerColumnType( Types.CLOB, "longvarchar" );

		registerFunction( "ascii", new StandardSQLFunction( "ascii", Hibernate.INTEGER ) );
		registerFunction( "char", new StandardSQLFunction( "char", Hibernate.CHARACTER ) );
		registerFunction( "length", new StandardSQLFunction( "length", Hibernate.LONG ) );
		registerFunction( "lower", new StandardSQLFunction("lower") );
		registerFunction( "upper", new StandardSQLFunction("upper") );
		registerFunction( "lcase", new StandardSQLFunction("lcase") );
		registerFunction( "ucase", new StandardSQLFunction("ucase") );
		registerFunction( "soundex", new StandardSQLFunction( "soundex", Hibernate.STRING ) );
		registerFunction( "ltrim", new StandardSQLFunction("ltrim") );
		registerFunction( "rtrim", new StandardSQLFunction("rtrim") );
		registerFunction( "reverse", new StandardSQLFunction("reverse") );
		registerFunction( "space", new StandardSQLFunction( "space", Hibernate.STRING ) );
		registerFunction( "rawtohex", new StandardSQLFunction("rawtohex") );
		registerFunction( "hextoraw", new StandardSQLFunction("hextoraw") );

		registerFunction( "user", new NoArgSQLFunction( "user", Hibernate.STRING ) );
		registerFunction( "database", new NoArgSQLFunction( "database", Hibernate.STRING ) );

		registerFunction( "current_date", new NoArgSQLFunction( "current_date", Hibernate.DATE, false ) );
		registerFunction( "curdate", new NoArgSQLFunction( "curdate", Hibernate.DATE ) );
		registerFunction( "current_timestamp", new NoArgSQLFunction( "current_timestamp", Hibernate.TIMESTAMP, false ) );
		registerFunction( "now", new NoArgSQLFunction( "now", Hibernate.TIMESTAMP ) );
		registerFunction( "current_time", new NoArgSQLFunction( "current_time", Hibernate.TIME, false ) );
		registerFunction( "curtime", new NoArgSQLFunction( "curtime", Hibernate.TIME ) );
		registerFunction( "day", new StandardSQLFunction( "day", Hibernate.INTEGER ) );
		registerFunction( "dayofweek", new StandardSQLFunction( "dayofweek", Hibernate.INTEGER ) );
		registerFunction( "dayofyear", new StandardSQLFunction( "dayofyear", Hibernate.INTEGER ) );
		registerFunction( "dayofmonth", new StandardSQLFunction( "dayofmonth", Hibernate.INTEGER ) );
		registerFunction( "month", new StandardSQLFunction( "month", Hibernate.INTEGER ) );
		registerFunction( "year", new StandardSQLFunction( "year", Hibernate.INTEGER ) );
		registerFunction( "week", new StandardSQLFunction( "week", Hibernate.INTEGER ) );
		registerFunction( "quater", new StandardSQLFunction( "quater", Hibernate.INTEGER ) );
		registerFunction( "hour", new StandardSQLFunction( "hour", Hibernate.INTEGER ) );
		registerFunction( "minute", new StandardSQLFunction( "minute", Hibernate.INTEGER ) );
		registerFunction( "second", new StandardSQLFunction( "second", Hibernate.INTEGER ) );
		registerFunction( "dayname", new StandardSQLFunction( "dayname", Hibernate.STRING ) );
		registerFunction( "monthname", new StandardSQLFunction( "monthname", Hibernate.STRING ) );

		registerFunction( "abs", new StandardSQLFunction("abs") );
		registerFunction( "sign", new StandardSQLFunction( "sign", Hibernate.INTEGER ) );

		registerFunction( "acos", new StandardSQLFunction( "acos", Hibernate.DOUBLE ) );
		registerFunction( "asin", new StandardSQLFunction( "asin", Hibernate.DOUBLE ) );
		registerFunction( "atan", new StandardSQLFunction( "atan", Hibernate.DOUBLE ) );
		registerFunction( "cos", new StandardSQLFunction( "cos", Hibernate.DOUBLE ) );
		registerFunction( "cot", new StandardSQLFunction( "cot", Hibernate.DOUBLE ) );
		registerFunction( "exp", new StandardSQLFunction( "exp", Hibernate.DOUBLE ) );
		registerFunction( "log", new StandardSQLFunction( "log", Hibernate.DOUBLE ) );
		registerFunction( "log10", new StandardSQLFunction( "log10", Hibernate.DOUBLE ) );
		registerFunction( "sin", new StandardSQLFunction( "sin", Hibernate.DOUBLE ) );
		registerFunction( "sqrt", new StandardSQLFunction( "sqrt", Hibernate.DOUBLE ) );
		registerFunction( "tan", new StandardSQLFunction( "tan", Hibernate.DOUBLE ) );
		registerFunction( "pi", new NoArgSQLFunction( "pi", Hibernate.DOUBLE ) );
		registerFunction( "rand", new StandardSQLFunction( "rand", Hibernate.FLOAT ) );

		registerFunction( "radians", new StandardSQLFunction( "radians", Hibernate.DOUBLE ) );
		registerFunction( "degrees", new StandardSQLFunction( "degrees", Hibernate.DOUBLE ) );
		registerFunction( "roundmagic", new StandardSQLFunction("roundmagic") );

		registerFunction( "ceiling", new StandardSQLFunction("ceiling") );
		registerFunction( "floor", new StandardSQLFunction("floor") );

		// Multi-param dialect functions...
		registerFunction( "mod", new StandardSQLFunction( "mod", Hibernate.INTEGER ) );

		// function templates
		registerFunction( "concat", new SQLFunctionTemplate( Hibernate.STRING, "concat(?1 || ?2)" ) );

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
	}

	public String getAddColumnString() {
		return "add column";
	}

	public boolean supportsIdentityColumns() {
		return true;
	}

	public String getIdentityColumnString() {
		return "generated by default as identity (start with 1)"; //not null is implicit
	}

	public String getIdentitySelectString() {
		return "call identity()";
	}

	public String getIdentityInsertString() {
		return "null";
	}

	public String getForUpdateString() {
		return "";
	}

	/**
	 * Not supported in 1.7.1 (1.7.2 only)
	 */
	public boolean supportsUnique() {
		return false;
	}

	public boolean supportsLimit() {
		return true;
	}

	public String getLimitString(String sql, boolean hasOffset) {
		return new StringBuffer( sql.length() + 10 )
				.append( sql )
				.insert( sql.indexOf( "select" ) + 6, hasOffset ? " limit ? ?" : " top ?" )
				.toString();
	}

	/*public CaseFragment createCaseFragment() {
		return new HSQLCaseFragment();
	}*/

	public boolean bindLimitParametersFirst() {
		return true;
	}

	public boolean supportsIfExistsAfterTableName() {
		return true;
	}

	public boolean supportsColumnCheck() {
		return false;
	}

	public String[] getCreateSequenceStrings(String sequenceName) {
		return new String[]{
			"create table dual_" + sequenceName + " (zero integer)",
			"insert into dual_" + sequenceName + " values (0)",
			"create sequence " + sequenceName + " start with 1"
		};
	}

	public String[] getDropSequenceStrings(String sequenceName) {
		return new String[]{
			"drop table dual_" + sequenceName + " if exists",
			"drop sequence " + sequenceName
		};
	}

	public String getSequenceNextValString(String sequenceName) {
		return "select next value for " + sequenceName + " from dual_" + sequenceName;
	}

	public boolean supportsSequences() {
		return true;
	}

	public String getQuerySequencesString() {
		return "select sequence_name from system_sequences";
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
		private int[] sqlGrammarCodes = new int[]{-22, -28};
		private int[] integrityViolationCodes = new int[]{-9, -177, -104};

		public ExceptionConverter(ViolatedConstraintNameExtracter extracter) {
			super( extracter );
		}

		protected int[] getSQLGrammarErrorCodes() {
			return sqlGrammarCodes;
		}

		protected int[] getIntegrityViolationErrorCodes() {
			return integrityViolationCodes;
		}
	}
}
