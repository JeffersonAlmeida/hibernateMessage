//$Id: SQLFunction.java,v 1.1 2005/02/13 11:49:57 oneovthafew Exp $
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.engine.Mapping;
import org.hibernate.type.Type;

/**
 * Provides support routines for the HQL functions as used
 * in the various SQL Dialects
 *
 * Provides an interface for supporting various HQL functions that are
 * translated to SQL. The Dialect and its sub-classes use this interface to
 * provide details required for processing of the function.
 *
 * @author David Channon
 */
public interface SQLFunction {
	public Type getReturnType(Type columnType, Mapping mapping) throws QueryException;
	public boolean hasArguments();
	public boolean hasParenthesesIfNoArguments();
	public String render(List args) throws QueryException;
}
