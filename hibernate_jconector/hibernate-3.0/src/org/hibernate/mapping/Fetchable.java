//$Id: Fetchable.java,v 1.2 2004/09/21 11:04:57 oneovthafew Exp $
package org.hibernate.mapping;

import org.hibernate.FetchMode;

/**
 * Any mapping with an outer-join attribute
 * @author Gavin King
 */
public interface Fetchable {
	public FetchMode getFetchMode();
	public void setFetchMode(FetchMode joinedFetch);
}
