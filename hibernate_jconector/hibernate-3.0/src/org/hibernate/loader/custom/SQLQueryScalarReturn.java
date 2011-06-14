package org.hibernate.loader.custom;

import org.hibernate.type.Type;

/**
 * @author gloegl
 */
public class SQLQueryScalarReturn {

	private Type type;
	private String columnAlias;
	
	public SQLQueryScalarReturn(String alias, Type type) {
		this.type = type;
		this.columnAlias = alias;
	}
	
	public String getColumnAlias() {
		return columnAlias;
	}
	
	public Type getType() {
		return type;
	}

}
