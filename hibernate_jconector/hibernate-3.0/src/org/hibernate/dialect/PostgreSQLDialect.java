//$Id: PostgreSQLDialect.java,v 1.14 2005/02/13 11:49:57 oneovthafew Exp $
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.Hibernate;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.id.SequenceGenerator;

/**
 * An SQL dialect for Postgres
 * @author Gavin King
 */
public class PostgreSQLDialect extends Dialect {

	public PostgreSQLDialect() {
		super();
		registerColumnType( Types.BIT, "bool" );
		registerColumnType( Types.BIGINT, "int8" );
		registerColumnType( Types.SMALLINT, "int2" );
		registerColumnType( Types.TINYINT, "int2" );
		registerColumnType( Types.INTEGER, "int4" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.FLOAT, "float4" );
		registerColumnType( Types.DOUBLE, "float8" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "time" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARBINARY, "bytea" );
		registerColumnType( Types.CLOB, "text" );
		registerColumnType( Types.BLOB, "oid" );
		registerColumnType( Types.NUMERIC, "numeric" );

		registerFunction( "abs", new StandardSQLFunction("abs") );
		registerFunction( "sign", new StandardSQLFunction("sign", Hibernate.INTEGER) );

		registerFunction( "acos", new StandardSQLFunction("acos", Hibernate.DOUBLE) );
		registerFunction( "asin", new StandardSQLFunction("asin", Hibernate.DOUBLE) );
		registerFunction( "atan", new StandardSQLFunction("atan", Hibernate.DOUBLE) );
		registerFunction( "cos", new StandardSQLFunction("cos", Hibernate.DOUBLE) );
		registerFunction( "cot", new StandardSQLFunction("cot", Hibernate.DOUBLE) );
		registerFunction( "exp", new StandardSQLFunction("exp", Hibernate.DOUBLE) );
		registerFunction( "ln", new StandardSQLFunction("ln", Hibernate.DOUBLE) );
		registerFunction( "log", new StandardSQLFunction("log", Hibernate.DOUBLE) );
		registerFunction( "sin", new StandardSQLFunction("sin", Hibernate.DOUBLE) );
		registerFunction( "sqrt", new StandardSQLFunction("sqrt", Hibernate.DOUBLE) );
		registerFunction( "cbrt", new StandardSQLFunction("cbrt", Hibernate.DOUBLE) );
		registerFunction( "tan", new StandardSQLFunction("tan", Hibernate.DOUBLE) );
		registerFunction( "radians", new StandardSQLFunction("radians", Hibernate.DOUBLE) );
		registerFunction( "degrees", new StandardSQLFunction("degrees", Hibernate.DOUBLE) );

		registerFunction( "stddev", new StandardSQLFunction("stddev", Hibernate.DOUBLE) );
		registerFunction( "variance", new StandardSQLFunction("variance", Hibernate.DOUBLE) );

		registerFunction( "random", new NoArgSQLFunction("random", Hibernate.DOUBLE) );

		registerFunction( "round", new StandardSQLFunction("round") );
		registerFunction( "trunc", new StandardSQLFunction("trunc") );
		registerFunction( "ceil", new StandardSQLFunction("ceil") );
		registerFunction( "floor", new StandardSQLFunction("floor") );

		registerFunction( "chr", new StandardSQLFunction("chr", Hibernate.CHARACTER) );
		registerFunction( "lower", new StandardSQLFunction("lower") );
		registerFunction( "upper", new StandardSQLFunction("upper") );
		registerFunction( "initcap", new StandardSQLFunction("initcap") );
		registerFunction( "to_ascii", new StandardSQLFunction("to_ascii") );
		registerFunction( "quote_ident", new StandardSQLFunction("quote_ident", Hibernate.STRING) );
		registerFunction( "quote_literal", new StandardSQLFunction("quote_literal", Hibernate.STRING) );
		registerFunction( "md5", new StandardSQLFunction("md5") );
		registerFunction( "ascii", new StandardSQLFunction("ascii", Hibernate.INTEGER) );
		registerFunction( "length", new StandardSQLFunction("length", Hibernate.LONG) );
		registerFunction( "char_length", new StandardSQLFunction("char_length", Hibernate.LONG) );
		registerFunction( "bit_length", new StandardSQLFunction("bit_length", Hibernate.LONG) );
		registerFunction( "octet_length", new StandardSQLFunction("octet_length", Hibernate.LONG) );

		registerFunction( "current_date", new NoArgSQLFunction("current_date", Hibernate.DATE, false) );
		registerFunction( "current_time", new NoArgSQLFunction("current_time", Hibernate.TIME, false) );
		registerFunction( "current_timestamp", new NoArgSQLFunction("current_timestamp", Hibernate.TIMESTAMP, false) );
		registerFunction( "localtime", new NoArgSQLFunction("localtime", Hibernate.TIME, false) );
		registerFunction( "localtimestamp", new NoArgSQLFunction("localtimestamp", Hibernate.TIMESTAMP, false) );
		registerFunction( "now", new NoArgSQLFunction("now", Hibernate.TIMESTAMP) );
		registerFunction( "timeofday", new NoArgSQLFunction("timeofday", Hibernate.STRING) );
		registerFunction( "age", new StandardSQLFunction("age") );

		registerFunction( "current_user", new NoArgSQLFunction("current_user", Hibernate.STRING, false) );
		registerFunction( "session_user", new NoArgSQLFunction("session_user", Hibernate.STRING, false) );
		registerFunction( "user", new NoArgSQLFunction("user", Hibernate.STRING, false) );
		registerFunction( "current_database", new NoArgSQLFunction("current_database", Hibernate.STRING, true) );
		registerFunction( "current_schema", new NoArgSQLFunction("current_schema", Hibernate.STRING, true) );

		registerFunction( "concat", new SQLFunctionTemplate( Hibernate.STRING, "(?1 || ?2)" ) );

		getDefaultProperties().setProperty(Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE);
	}

	public String getAddColumnString() {
		return "add column";
	}
	public String getSequenceNextValString(String sequenceName) {
		return "select nextval ('" + sequenceName +"')";
	}
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName; //starts with 1, implicitly
	}
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}

	public String getCascadeConstraintsString() {
		return "";//" cascade";
	}
	public boolean dropConstraints() {
		return true;
	}

	public boolean supportsSequences() {
		return true;
	}

	public String getQuerySequencesString() {
		return "select relname from pg_class where relkind='S'";
	}

	public boolean supportsLimit() {
		return true;
	}

	public String getLimitString(String sql, boolean hasOffset) {
		return new StringBuffer( sql.length()+20 )
			.append(sql)
			.append(hasOffset ? " limit ? offset ?" : " limit ?")
			.toString();
	}

	public boolean bindLimitParametersInReverseOrder() {
		return true;
	}

	public boolean supportsIdentityColumns() {
		return true;
	}

	public String getForUpdateString(String aliases) {
		return getForUpdateString() + " of " + aliases;
	}

	public String getIdentitySelectString(String table, String column, int type) {
		return new StringBuffer().append("select currval('")
			.append(table)
			.append('_')
			.append(column)
			.append("_seq')")
			.toString();
	}

	public String getIdentityColumnString(int type) {
		return type==Types.BIGINT ?
			"bigserial not null" :
			"serial not null";
	}

	public boolean hasDataTypeInIdentityColumn() {
		return false;
	}

	public String getNoColumnsInsertString() {
		return "default values";
	}

	public Class getNativeIdentifierGeneratorClass() {
		return SequenceGenerator.class;
	}

	public boolean supportsOuterJoinForUpdate() {
		return false;
	}
	
	public boolean useInputStreamToInsertBlob() {
		return false;
	}

}
