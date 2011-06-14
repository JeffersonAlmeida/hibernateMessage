// $Id: FilterImpl.java,v 1.4 2005/01/04 23:12:33 steveebersole Exp $
package org.hibernate.impl;

import org.hibernate.engine.FilterDefinition;
import org.hibernate.type.Type;
import org.hibernate.Filter;
import org.hibernate.HibernateException;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import java.util.Arrays;
import java.io.Serializable;

/**
 * Implementation of FilterImpl.  FilterImpl implements the user's
 * view into enabled dynamic filters, allowing them to set filter parameter values.
 *
 * @author Steve Ebersole
 */
public class FilterImpl implements Filter, Serializable {
	public static final String MARKER = "$FILTER_PLACEHOLDER$";

	private FilterDefinition configuration;
	private Map parameters = new HashMap();

	/**
	 * Constructs a new FilterImpl.
	 *
	 * @param configuration The filter's global configuration.
	 */
	public FilterImpl(FilterDefinition configuration) {
		this.configuration = configuration;
	}

	/**
	 * Set the named parameter's value for this filter.
	 *
	 * @param name The parameter's name.
	 * @param value The value to be applied.
	 * @return This FilterImpl instance (for method chaining).
	 * @throws IllegalArgumentException Indicates that either the parameter was undefined or that the type
	 * of the passed value did not match the configured type.
	 */
	public Filter setParameter(String name, Object value) throws IllegalArgumentException {
		// Make sure this is a defined parameter and check the incoming value type
		// TODO: what should be the actual exception type here?
		Type type = configuration.getParameterType( name );
		if ( type == null ) {
			throw new IllegalArgumentException( "Undefined filter parameter [" + name + "]" );
		}
		if ( value != null && !type.getReturnedClass().isAssignableFrom( value.getClass() ) ) {
			throw new IllegalArgumentException( "Incorrect type for parameter [" + name + "]" );
		}
		parameters.put( name, value );
		return this;
	}

	/**
	 * Set the named parameter's value list for this filter.  Used
	 * in conjunction with IN-style filter criteria.
	 *
	 * @param name   The parameter's name.
	 * @param values The values to be expanded into an SQL IN list.
	 * @return This FilterImpl instance (for method chaining).
	 */
	public Filter setParameterList(String name, Collection values) throws IllegalArgumentException  {
		// Make sure this is a defined parameter and check the incoming value type
		// TODO: what should be the actual exception types here?
		if ( values == null ) {
			throw new IllegalArgumentException( "Collection must be not null!" );
		}
		Type type = configuration.getParameterType( name );
		if ( type == null ) {
			throw new IllegalArgumentException( "Undefined filter parameter [" + name + "]" );
		}
		if ( values != null && values.size() > 0 ) {
			Class elementClass = values.iterator().next().getClass();
			if ( !type.getReturnedClass().isAssignableFrom( elementClass ) ) {
				throw new IllegalArgumentException("Incorrect type for parameter [" + name + "]");
			}
		}
		parameters.put( name, values );
		return this;
	}

	/**
	 * Set the named parameter's value list for this filter.  Used
	 * in conjunction with IN-style filter criteria.
	 *
	 * @param name The parameter's name.
	 * @param values The values to be expanded into an SQL IN list.
	 * @return This FilterImpl instance (for method chaining).
	 */
	public Filter setParameterList(String name, Object[] values) throws IllegalArgumentException {
		return setParameterList( name, Arrays.asList( values ) );
	}

	/**
	 * Get the value of the named parameter for the current filter.
	 *
	 * @param name The name of the parameter for which to return the value.
	 * @return The value of the named parameter.
	 */
	public Object getParameter(String name) {
		return parameters.get( name );
	}

	/**
	 * Get the name of this filter.
	 *
	 * @return This filter's name.
	 */
	public String getName() {
		return configuration.getFilterName();
	}

	/**
	 * Perform validation of the filter state.  This is used to verify the
	 * state of the filter after its enablement and before its use.
	 *
	 * @throws HibernateException If the state is not currently valid.
	 */
	public void validate() throws HibernateException {
		// for each of the defined parameters, make sure its value
		// has been set
		Iterator itr = configuration.getParameterNames().iterator();
		while ( itr.hasNext() ) {
			final String parameterName = (String) itr.next();
			if ( parameters.get(parameterName) == null ) {
				throw new IllegalArgumentException("Filter [" + getName() + "] defined parameter [" + parameterName + "] whose value was not set");
			}
		}
	}
}
