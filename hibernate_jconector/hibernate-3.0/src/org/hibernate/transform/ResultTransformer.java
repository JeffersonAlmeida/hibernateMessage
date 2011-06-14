//$Id: ResultTransformer.java,v 1.3 2005/02/12 07:19:47 steveebersole Exp $
package org.hibernate.transform;

import java.io.Serializable;
import java.util.List;

/**
 * Implementors define a strategy for transforming criteria query
 * results into the actual application-visible query result list.
 * @see org.hibernate.Criteria#setResultTransformer(ResultTransformer)
 * @author Gavin King
 */
public interface ResultTransformer extends Serializable {
	public Object transformTuple(Object[] tuple, String[] aliases);
	public List transformList(List collection);
}
