// $Id: Filter.java,v 1.3 2005/01/04 23:13:49 steveebersole Exp $
package org.hibernate;

import java.util.Collection;

/**
 * Type definition of Filter.  Filter defines the user's view into enabled dynamic filters,
 * allowing them to set filter parameter values.
 *
 * @author Steve Ebersole
 */
public interface Filter {

	/**
	 * Set the named parameter's value for this filter.
	 *
	 * @param name The parameter's name.
	 * @param value The value to be applied.
	 * @return This FilterImpl instance (for method chaining).
	 */
	public Filter setParameter(String name, Object value);

	/**
	 * Set the named parameter's value list for this filter.  Used
	 * in conjunction with IN-style filter criteria.
	 *
	 * @param name The parameter's name.
	 * @param values The values to be expanded into an SQL IN list.
	 * @return This FilterImpl instance (for method chaining).
	 */
	public Filter setParameterList(String name, Collection values);

	/**
	 * Set the named parameter's value list for this filter.  Used
	 * in conjunction with IN-style filter criteria.
	 *
	 * @param name The parameter's name.
	 * @param values The values to be expanded into an SQL IN list.
	 * @return This FilterImpl instance (for method chaining).
	 */
	public Filter setParameterList(String name, Object[] values);

	/**
	 * Get the name of this filter.
	 *
	 * @return This filter's name.
	 */
	public String getName();

	/**
	 * Perform validation of the filter state.  This is used to verify the
	 * state of the filter after its enablement and before its use.
	 *
	 * @throws HibernateException If the state is not currently valid.
	 */
	public void validate() throws HibernateException;
}
