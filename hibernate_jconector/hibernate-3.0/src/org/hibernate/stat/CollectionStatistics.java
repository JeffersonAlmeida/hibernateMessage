//$Id: CollectionStatistics.java,v 1.7 2005/03/23 20:33:27 oneovthafew Exp $
package org.hibernate.stat;

import java.io.Serializable;

/**
 * Collection related statistics
 * 
 * @author Gavin King
 */
public class CollectionStatistics implements Serializable {
	long loadCount;
	long fetchCount;
	long updateCount;
	long removeCount;
	long recreateCount;
	
	public long getLoadCount() {
		return loadCount;
	}
	public long getFetchCount() {
		return fetchCount;
	}
	public long getRecreateCount() {
		return recreateCount;
	}
	public long getRemoveCount() {
		return removeCount;
	}
	public long getUpdateCount() {
		return updateCount;
	}

	public String toString() {
		return new StringBuffer()
		    .append("CollectionStatistics")
			.append("[loadCount=").append(this.loadCount)
			.append(",fetchCount=").append(this.fetchCount)
			.append(",recreateCount=").append(this.recreateCount)
			.append(",removeCount=").append(this.removeCount)
			.append(",updateCount=").append(this.updateCount)
			.append(']')
			.toString();
	}
}