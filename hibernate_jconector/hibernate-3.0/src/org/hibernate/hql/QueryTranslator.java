//$Id: QueryTranslator.java,v 1.18 2005/02/27 02:54:12 steveebersole Exp $
package org.hibernate.hql;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.Type;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines the constract of an HQL->SQL translator.
 *
 * @author josh Mar 14, 2004 11:14:21 AM
 */
public interface QueryTranslator {
	// Error message constants.
	
	String ERROR_CANNOT_FETCH_WITH_ITERATE = "fetch may not be used with scroll() or iterate()";
	String ERROR_NAMED_PARAMETER_DOES_NOT_APPEAR = "Named parameter does not appear in Query: ";
    String ERROR_CANNOT_DETERMINE_TYPE = "Could not determine type of: ";
	String ERROR_CANNOT_FORMAT_LITERAL =  "Could not format constant value to SQL literal: ";

	/**
	 * Compile a "normal" query. This method may be called multiple
	 * times. Subsequent invocations are no-ops.
	 *
	 * @param replacements Defined query substitutions.
	 * @param shallow      Does this represent a shallow (scalar or entity-id) select?
	 * @throws QueryException   There was a problem parsing the query string.
	 * @throws MappingException There was a problem querying defined mappings.
	 */
	void compile(Map replacements, boolean shallow) throws QueryException, MappingException;

	/**
	 * Perform a list operation given the underlying query definition.
	 *
	 * @param session         The session owning this query.
	 * @param queryParameters The query bind parameters.
	 * @return The query list results.
	 * @throws HibernateException
	 */
	List list(SessionImplementor session, QueryParameters queryParameters)
			throws HibernateException;

	/**
	 * Perform an iterate operation given the underlying query defintion.
	 *
	 * @param queryParameters The query bind parameters.
	 * @param session         The session owning this query.
	 * @return An iterator over the query results.
	 * @throws HibernateException
	 */
	Iterator iterate(QueryParameters queryParameters, SessionImplementor session)
			throws HibernateException;

	/**
	 * Perform a scroll operation given the underlying query defintion.
	 *
	 * @param queryParameters The query bind parameters.
	 * @param session         The session owning this query.
	 * @return The ScrollableResults wrapper around the query results.
	 * @throws HibernateException
	 */
	ScrollableResults scroll(QueryParameters queryParameters, SessionImplementor session)
			throws HibernateException;

	/**
	 * Perform a bulk update/delete operation given the underlying query defintion.
	 *
	 * @param queryParameters The query bind parameters.
	 * @param session         The session owning this query.
	 * @return The number of entities updated or deleted.
	 * @throws HibernateException
	 */
	int executeUpdate(QueryParameters queryParameters, SessionImplementor session)
			throws HibernateException;

	/**
	 * Returns the set of query spaces (table names) that the query referrs to.
	 *
	 * @return A set of query spaces (table names).
	 */
	Set getQuerySpaces();

	/**
	 * Returns the SQL string generated by the translator.
	 *
	 * @return the SQL string generated by the translator.
	 */
	String getSQLString();

	/**
	 * Returns the HQL string processed by the translator.
	 *
	 * @return the HQL string processed by the translator.
	 */
	String getQueryString();

	/**
	 * Returns the filters enabled for this query translator.
	 *
	 * @return Filters enabled for this query execution.
	 */
	Map getEnabledFilters();

	/**
	 * Returns an array of Types represented in the query result.
	 *
	 * @return Query return types.
	 */
	Type[] getReturnTypes();

	/**
	 * Returns the column names in the generated SQL.
	 *
	 * @return the column names in the generated SQL.
	 */
	String[][] getColumnNames();

	/**
	 * Returns the locations of the specified named parameter in the SQL.
	 * @param name The name of the named parameter.
	 * @return the locations of the specified named parameter in the SQL.
	 * @throws QueryException if something goes wrong.
	 */
	int[] getNamedParameterLocs(String name) throws QueryException;
}
