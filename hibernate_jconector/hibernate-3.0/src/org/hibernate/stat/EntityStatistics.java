//$Id: EntityStatistics.java,v 1.7 2005/03/23 20:33:30 oneovthafew Exp $
package org.hibernate.stat;

import java.io.Serializable;

/**
 * Entity related statistics
 * 
 * @author Gavin King
 */
public class EntityStatistics implements Serializable {

	long loadCount;
	long updateCount;
	long insertCount;
	long deleteCount;
	long fetchCount;

	public long getDeleteCount() {
		return deleteCount;
	}
	public long getInsertCount() {
		return insertCount;
	}
	public long getLoadCount() {
		return loadCount;
	}
	public long getUpdateCount() {
		return updateCount;
	}
	public long getFetchCount() {
		return fetchCount;
	}

	public String toString() {
		return new StringBuffer()
		    .append("EntityStatistics")
			.append("[loadCount=").append(this.loadCount)
			.append(",updateCount=").append(this.updateCount)
			.append(",insertCount=").append(this.insertCount)
			.append(",deleteCount=").append(this.deleteCount)
			.append(",fetchCount=").append(this.fetchCount)
			.append(']')
			.toString();
	}
}
