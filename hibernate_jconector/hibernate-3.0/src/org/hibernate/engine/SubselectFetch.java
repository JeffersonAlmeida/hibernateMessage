//$Id: SubselectFetch.java,v 1.4 2005/03/21 20:15:54 oneovthafew Exp $
package org.hibernate.engine;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.util.StringHelper;

/**
 * @author Gavin King
 */
public class SubselectFetch {
	private final Set resultingEntityKeys = new HashSet();
	private final String queryString;
	private final String alias;
	private final Loadable loadable;
	private final QueryParameters queryParameters;
	
	public SubselectFetch(
		//final String queryString, 
		final String alias, 
		final Loadable loadable,
		final QueryParameters queryParameters, 
		final EntityKey[] resultingEntityKeys
	) {
		this.resultingEntityKeys.addAll( Arrays.asList(resultingEntityKeys) );
		this.queryParameters = queryParameters;
		this.loadable = loadable;
		this.alias = alias;
		
		//TODO: ugly here:
		final String queryString = queryParameters.getFilteredSQL();
		int fromIndex = queryString.indexOf(" from ");
		int orderByIndex = queryString.lastIndexOf("order by");
		this.queryString = orderByIndex>0 ? 
			queryString.substring(fromIndex, orderByIndex) : 
			queryString.substring(fromIndex);
			
}

	public QueryParameters getQueryParameters() {
		return queryParameters;
	}
	
	/**
	 * Get the Set of EntityKeys
	 */
	public Set getResult() {
		return resultingEntityKeys;
	}
	
	public String toSubselectString(String ukname) {
		
		String[] joinColumns = ukname==null ?
			StringHelper.qualify( alias, loadable.getIdentifierColumnNames() ) :
			( (PropertyMapping) loadable ).toColumns(alias, ukname);
		
		return new StringBuffer()
			.append("select ")
			.append( StringHelper.join(", ", joinColumns) )
			.append(queryString)
			.toString();
	}

}
