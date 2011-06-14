//$Id: CriteriaSpecification.java,v 1.2 2005/02/12 07:19:13 steveebersole Exp $
package org.hibernate.criterion;

import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.hibernate.transform.DistinctRootEntityResultTransformer;
import org.hibernate.transform.PassThroughResultTransformer;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.transform.RootEntityResultTransformer;

/**
 * @author Gavin King
 */
public interface CriteriaSpecification {

	/**
	 * The alias that refers to the "root" entity of the criteria query.
	 */
	public static final String ROOT_ALIAS = "this";
	
	/**
	 * Each row of results is a <tt>Map</tt> from alias to entity instance
	 */
	public static final ResultTransformer ALIAS_TO_ENTITY_MAP = new AliasToEntityMapResultTransformer();
	/**
	 * Each row of results is an instance of the root entity
	 */
	public static final ResultTransformer ROOT_ENTITY = new RootEntityResultTransformer();
	/**
	 * Each row of results is a distinct instance of the root entity
	 */
	public static final ResultTransformer DISTINCT_ROOT_ENTITY = new DistinctRootEntityResultTransformer();
	/**
	 * This result transformer is selected implicitly by calling <tt>setProjection()</tt>
	 */
	public static final ResultTransformer PROJECTION = new PassThroughResultTransformer();
	
}
