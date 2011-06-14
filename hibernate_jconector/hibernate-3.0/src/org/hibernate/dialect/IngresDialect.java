//$Id: IngresDialect.java,v 1.7 2005/02/13 11:49:57 oneovthafew Exp $
package org.hibernate.dialect;

import org.hibernate.Hibernate;
import org.hibernate.dialect.function.SQLFunctionTemplate;

import java.sql.Types;


/**
 * An Ingres SQL dialect
 * @author Ian Booth
 */
public class IngresDialect extends Dialect {

	public IngresDialect() {
		super();
		registerColumnType( Types.BIT, "byte" );
		registerColumnType( Types.BIGINT, "integer4" );
		registerColumnType( Types.SMALLINT, "integer2" );
		registerColumnType( Types.TINYINT, "integer1" );
		registerColumnType( Types.INTEGER, "integer4" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, "varchar($l)" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double precision" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "date" );
		registerColumnType( Types.TIMESTAMP, "date" );
		registerColumnType( Types.VARBINARY, "varbinary($l)" );
		registerColumnType( Types.NUMERIC, "numeric($p,$s)" );
		registerColumnType( Types.BLOB, "long varchar" );
		registerColumnType( Types.CLOB, "long varchar" );
		registerColumnType( Types.VARBINARY, "long varchar" );

		registerFunction( "concat", new SQLFunctionTemplate( Hibernate.STRING, "concat(?1 || ?2)" ) );
	}

	public String getAddColumnString() {
		return "add column";
	}

 	/**
	 * Do we need to drop constraints before dropping tables in this dialect?
	 * @return boolean
	 */
	public boolean dropConstraints() {
		return false;
	}

	public String getLowercaseFunction() {
		return "lowercase";
	}

}
