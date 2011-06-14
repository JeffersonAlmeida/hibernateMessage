//$Id: Oracle9Dialect.java,v 1.26 2005/03/24 19:36:52 oneovthafew Exp $
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.exception.ErrorCodeConverter;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.exception.SQLExceptionConverter;
import org.hibernate.exception.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.ViolatedConstraintNameExtracter;
import org.hibernate.util.ReflectHelper;

/**
 * An SQL dialect for Oracle 9 (uses ANSI-style syntax where possible).
 * @author Gavin King, David Channon
 */
public class Oracle9Dialect extends Dialect {

	public Oracle9Dialect() {
		super();
		registerColumnType( Types.BIT, "number(1,0)" );
		registerColumnType( Types.BIGINT, "number(19,0)" );
		registerColumnType( Types.SMALLINT, "number(5,0)" );
		registerColumnType( Types.TINYINT, "number(3,0)" );
		registerColumnType( Types.INTEGER, "number(10,0)" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, 4000, "varchar2($l)" );
		registerColumnType( Types.VARCHAR, "long" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double precision" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "date" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARBINARY, 2000, "raw($l)" );
		registerColumnType( Types.VARBINARY, "long raw" );
		registerColumnType( Types.NUMERIC, "number($p,$s)" );
		registerColumnType( Types.BLOB, "blob" );
		registerColumnType( Types.CLOB, "clob" );

		getDefaultProperties().setProperty(Environment.USE_STREAMS_FOR_BINARY, "true");
		getDefaultProperties().setProperty(Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE);

		registerFunction( "abs", new StandardSQLFunction("abs") );
		registerFunction( "sign", new StandardSQLFunction("sign", Hibernate.INTEGER) );

		registerFunction( "acos", new StandardSQLFunction("acos", Hibernate.DOUBLE) );
		registerFunction( "asin", new StandardSQLFunction("asin", Hibernate.DOUBLE) );
		registerFunction( "atan", new StandardSQLFunction("atan", Hibernate.DOUBLE) );
		registerFunction( "cos", new StandardSQLFunction("cos", Hibernate.DOUBLE) );
		registerFunction( "cosh", new StandardSQLFunction("cosh", Hibernate.DOUBLE) );
		registerFunction( "exp", new StandardSQLFunction("exp", Hibernate.DOUBLE) );
		registerFunction( "ln", new StandardSQLFunction("ln", Hibernate.DOUBLE) );
		registerFunction( "sin", new StandardSQLFunction("sin", Hibernate.DOUBLE) );
		registerFunction( "sinh", new StandardSQLFunction("sinh", Hibernate.DOUBLE) );
		registerFunction( "stddev", new StandardSQLFunction("stddev", Hibernate.DOUBLE) );
		registerFunction( "sqrt", new StandardSQLFunction("sqrt", Hibernate.DOUBLE) );
		registerFunction( "tan", new StandardSQLFunction("tan", Hibernate.DOUBLE) );
		registerFunction( "tanh", new StandardSQLFunction("tanh", Hibernate.DOUBLE) );
		registerFunction( "variance", new StandardSQLFunction("variance", Hibernate.DOUBLE) );

		registerFunction( "round", new StandardSQLFunction("round") );
		registerFunction( "trunc", new StandardSQLFunction("trunc") );
		registerFunction( "ceil", new StandardSQLFunction("ceil") );
		registerFunction( "floor", new StandardSQLFunction("floor") );

		registerFunction( "chr", new StandardSQLFunction("chr", Hibernate.CHARACTER) );
		registerFunction( "initcap", new StandardSQLFunction("initcap") );
		registerFunction( "lower", new StandardSQLFunction("lower") );
		registerFunction( "ltrim", new StandardSQLFunction("ltrim") );
		registerFunction( "rtrim", new StandardSQLFunction("rtrim") );
		registerFunction( "soundex", new StandardSQLFunction("soundex") );
		registerFunction( "upper", new StandardSQLFunction("upper") );
		registerFunction( "ascii", new StandardSQLFunction("ascii", Hibernate.INTEGER) );
		registerFunction( "length", new StandardSQLFunction("length", Hibernate.LONG) );

		registerFunction( "to_char", new StandardSQLFunction("to_char", Hibernate.STRING) );
		registerFunction( "to_date", new StandardSQLFunction("to_date", Hibernate.TIMESTAMP) );

		registerFunction( "current_date", new NoArgSQLFunction("current_date", Hibernate.DATE, false) );
		registerFunction( "current_time", new NoArgSQLFunction("current_timestamp", Hibernate.TIME, false) );
		registerFunction( "current_timestamp", new NoArgSQLFunction("current_timestamp", Hibernate.TIMESTAMP, false) );
		
		registerFunction( "lastday", new StandardSQLFunction("lastday", Hibernate.DATE) );
		registerFunction( "sysdate", new NoArgSQLFunction("sysdate", Hibernate.DATE, false) );
		registerFunction( "systimestamp", new NoArgSQLFunction("systimestamp", Hibernate.TIMESTAMP, false) );
		registerFunction( "uid", new NoArgSQLFunction("uid", Hibernate.INTEGER, false) );
		registerFunction( "user", new NoArgSQLFunction("user", Hibernate.STRING, false) );

		registerFunction( "rowid", new NoArgSQLFunction("rowid", Hibernate.LONG, false) );
		registerFunction( "rownum", new NoArgSQLFunction("rownum", Hibernate.LONG, false) );

		// Multi-param string dialect functions...
		registerFunction( "concat", new StandardSQLFunction("concat", Hibernate.STRING) );
		registerFunction( "instr", new StandardSQLFunction("instr", Hibernate.STRING) );
		registerFunction( "instrb", new StandardSQLFunction("instrb", Hibernate.STRING) );
		registerFunction( "lpad", new StandardSQLFunction("lpad", Hibernate.STRING) );
		registerFunction( "replace", new StandardSQLFunction("replace", Hibernate.STRING) );
		registerFunction( "rpad", new StandardSQLFunction("rpad", Hibernate.STRING) );
		registerFunction( "substr", new StandardSQLFunction("substr", Hibernate.STRING) );
		registerFunction( "substrb", new StandardSQLFunction("substrb", Hibernate.STRING) );
		registerFunction( "translate", new StandardSQLFunction("translate", Hibernate.STRING) );

		// Multi-param numeric dialect functions...
		registerFunction( "atan2", new StandardSQLFunction("atan2", Hibernate.FLOAT) );
		registerFunction( "log", new StandardSQLFunction("log", Hibernate.INTEGER) );
		registerFunction( "mod", new StandardSQLFunction("mod", Hibernate.INTEGER) );
		registerFunction( "nvl", new StandardSQLFunction("nvl") );
		registerFunction( "power", new StandardSQLFunction("power", Hibernate.FLOAT) );

		// Multi-param date dialect functions...
		registerFunction( "add_months", new StandardSQLFunction("add_months", Hibernate.DATE) );
		registerFunction( "months_between", new StandardSQLFunction("months_between", Hibernate.FLOAT) );
		registerFunction( "next_day", new StandardSQLFunction("next_day", Hibernate.DATE) );
	}

	public String getAddColumnString() {
		return "add";
	}

	public String getSequenceNextValString(String sequenceName) {
		return "select " + sequenceName + ".nextval from dual";
	}
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName; //starts with 1, implicitly
	}
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}

	public String getCascadeConstraintsString() {
		return " cascade constraints";
	}

	public boolean dropConstraints() {
		return false;
	}

	public String getForUpdateNowaitString() {
		return " for update nowait";
	}

	public boolean supportsSequences() {
		return true;
	}

	public boolean supportsLimit() {
		return true;
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
			pagingSelect.append(" ) row_ where rownum <= ?) where rownum_ > ?");
		}
		else {
			pagingSelect.append(" ) where rownum <= ?");
		}

		if (isForUpdate) pagingSelect.append(" for update");
		
		return pagingSelect.toString();
	}

	public String getForUpdateString(String aliases) {
		return getForUpdateString() + " of " + aliases;
	}

	public String getForUpdateNowaitString(String aliases) {
		return getForUpdateNowaitString() + " of " + aliases;
	}

	public boolean bindLimitParametersInReverseOrder() {
		return true;
	}

	public boolean useMaxForLimit() {
		return true;
	}
	
	public boolean forUpdateOfColumns() {
		return true;
	}

	public String getQuerySequencesString() {
		return "select sequence_name from user_sequences";
	}

	public String getSelectGUIDString() {
		return "select rawtohex(sys_guid()) from dual";
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

	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
        return EXTRACTER;
	}

	private static class ExceptionConverter extends ErrorCodeConverter {
		private int[] sqlGrammarCodes = new int[] { 900, 903, 904, 917, 936, 942, 17006 };
		private int[] integrityViolationCodes = new int[] { 1, 1400, 2291 };
		private int[] lockAcquisitionCodes = new int[] { 54, 55 };

		public ExceptionConverter(ViolatedConstraintNameExtracter extracter) {
			super(extracter);
		}

		protected int[] getSQLGrammarErrorCodes() {
			return sqlGrammarCodes;
		}

		protected int[] getIntegrityViolationErrorCodes() {
			return integrityViolationCodes;
		}

		protected int[] getLockAcquisitionErrorCodes() {
			return lockAcquisitionCodes;
		}
	}

	private static ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {

		/**
		 * Extract the name of the violated constraint from the given SQLException.
		 *
		 * @param sqle The exception that was the result of the constraint violation.
		 * @return The extracted constraint name.
		 */
		public String extractConstraintName(SQLException sqle) {
			int errorCode = JDBCExceptionHelper.extractErrorCode(sqle);
			if ( errorCode == 1 || errorCode == 2291 ) {
				return extractUsingTemplate( "constraint (", ") violated", sqle.getMessage() );
			}
			else if ( errorCode == 1400 ) {
				// simple nullability constraint
				return null;
			}
			else {
				return null;
			}
		}

	};

	// not final-static to avoid possible classcast exceptions if using different oracle drivers.
	int oracletypes_cursor_value = 0; 
	public int registerResultSetOutParameter(java.sql.CallableStatement statement,int col) throws SQLException {
		if(oracletypes_cursor_value==0) {
			try {
				Class types = ReflectHelper.classForName("oracle.jdbc.driver.OracleTypes");
				oracletypes_cursor_value = types.getField("CURSOR").getInt(types.newInstance());
			} catch (Exception se) {
				throw new HibernateException("Problem while trying to load or access OracleTypes.CURSOR value",se);
			} 
		}
		//	register the type of the out param - an Oracle specific type
		statement.registerOutParameter(col, oracletypes_cursor_value);
		col++;
		return col;
	}
	
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		ps.execute();
		ResultSet rs = (ResultSet) ps.getObject(1);			
		return rs;		
	}
}
