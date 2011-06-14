package org.hibernate.dialect;

import org.hibernate.Hibernate;
import org.hibernate.dialect.function.NoArgSQLFunction;

/**
 * Dialect for SQLServer7
 * 
 * @author max
 *
 */
public class SQLServer7Dialect extends SQLServerDialect {

	public SQLServer7Dialect() {
		registerFunction( "current_timestamp", new NoArgSQLFunction("getdate", Hibernate.TIMESTAMP, true) );
		registerFunction( "current_time", new NoArgSQLFunction("getdate", Hibernate.TIME, true) );
		registerFunction( "current_date", new NoArgSQLFunction("getdate", Hibernate.DATE, true) );		
	}
}
