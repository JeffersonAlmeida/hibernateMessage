//$Id: StandardSQLFunction.java,v 1.2 2005/02/23 03:36:54 oneovthafew Exp $
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.engine.Mapping;
import org.hibernate.type.Type;

/**
 * Provides a standard implementation that supports the majority of the HQL
 * functions that are translated to SQL. The Dialect and its sub-classes use
 * this class to provide details required for processing of the associated
 * function.
 *
 * @author David Channon
 */
public class StandardSQLFunction implements SQLFunction {
	private Type returnType = null;
	private String name;
	
	public StandardSQLFunction(String name) {
		this.name = name;
	}
	
	public StandardSQLFunction(String name, Type typeValue) {
		returnType = typeValue;
		this.name = name;
	}
	
	public Type getReturnType(Type columnType, Mapping mapping) {
		if (returnType == null) return columnType;
		return returnType;
	}
	
	public boolean hasArguments() {
		return true;
	}
	
	public boolean hasParenthesesIfNoArguments() {
		return true;
	}
	
	public String render(List args) {
		StringBuffer buf = new StringBuffer();
		buf.append(name)
			.append('(');
		for ( int i=0; i<args.size(); i++ ) {
			buf.append( args.get(i) );
			if ( i<args.size()-1 ) buf.append(", ");
		}
		return buf.append(')').toString();
	}
	
	public String toString() {
		return name;
	}
}
