//$Id: AliasToEntityMapResultTransformer.java,v 1.3 2004/08/10 05:06:14 oneovthafew Exp $
package org.hibernate.transform;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gavin King
 */
public class AliasToEntityMapResultTransformer implements ResultTransformer {

	public Object transformTuple(Object[] tuple, String[] aliases) {
		Map result = new HashMap();
		for ( int i=0; i<tuple.length; i++ ) {
			String alias = aliases[i];
			if ( alias!=null ) {
				result.put( alias, tuple[i] );
			}
		}
		return result;
	}

	public List transformList(List collection) {
		return collection;
	}

}
