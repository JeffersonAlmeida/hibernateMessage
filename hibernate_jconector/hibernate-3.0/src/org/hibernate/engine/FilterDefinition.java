// $Id: FilterDefinition.java,v 1.2 2005/03/02 11:48:12 oneovthafew Exp $
package org.hibernate.engine;

import org.hibernate.type.Type;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * A FilterDefinition defines the global attributes of a dynamic filter.  This
 * information includes its name as well as its defined parameters (name and type).
 * 
 * @author Steve Ebersole
 */
public class FilterDefinition {
	private String filterName;
	private String defaultFilterCondition;
	private Map parameterTypes = new HashMap();

	/**
	 * Construct a new FilterDefinition instance.
	 *
	 * @param name The name of the filter for which this configuration is in effect.
	 */
	public FilterDefinition(String name) {
		this.filterName = name;
	}

	/**
	 * Get the name of the filter this configuration defines.
	 *
	 * @return The filter name for this configuration.
	 */
	public String getFilterName() {
		return filterName;
	}

	/**
	 * Get a set of the parameters defined by this configuration.
	 *
	 * @return The parameters named by this configuration.
	 */
	public Set getParameterNames() {
		return parameterTypes.keySet();
	}

	/**
	 * Retreive the type of the named parameter defined for this filter.
	 *
	 * @param parameterName The name of the filter parameter for which to return the type.
	 * @return The type of the named parameter.
	 */
    public Type getParameterType(String parameterName) {
	    return (Type) parameterTypes.get(parameterName);
    }

	/**
	 * Add a named parameter to the filter definition.
	 *
	 * @param name The name of the parameter.
	 * @param type The parameter's type.
	 * @return The filter definition instance (for method chaining).
	 */
	public FilterDefinition addParameterType(String name, Type type) {
		parameterTypes.put(name, type);
		return this;
	}

	public String getDefaultFilterCondition() {
		return defaultFilterCondition;
	}
	

	public void setDefaultFilterCondition(String defaultFilter) {
		this.defaultFilterCondition = defaultFilter;
	}
	
}
