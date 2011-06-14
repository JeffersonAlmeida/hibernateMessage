//$Id: QueryStatistics.java,v 1.9 2005/03/23 20:33:30 oneovthafew Exp $
package org.hibernate.stat;

import java.io.Serializable;

/**
 * Query statistics (HQL and SQL)
 * 
 * Note that for a cached query, the cache miss is equals to the db count
 * 
 * @author Gavin King
 */
public class QueryStatistics implements Serializable {
	long cacheHitCount;
	long cacheMissCount;
	long cachePutCount;
	private long executionCount;
	private long executionRowCount;
	private long executionAvgTime;
	private long executionMaxTime;
	private long executionMinTime;

	/**
	 * queries executed to the DB
	 */
	public long getExecutionCount() {
		return executionCount;
	}
	
	/**
	 * Queries retrieved successfully from the cache
	 */
	public long getCacheHitCount() {
		return cacheHitCount;
	}
	
	public long getCachePutCount() {
		return cachePutCount;
	}
	
	public long getCacheMissCount() {
		return cacheMissCount;
	}
	
	/**
	 * Number of lines returned by all the executions of this query (from DB)
	 * For now, {@link org.hibernate.Query#iterate()} 
	 * and {@link org.hibernate.Query#scroll()()} do not fill this statistic
	 * @return
	 */
	public long getExecutionRowCount() {
		return executionRowCount;
	}

	/**
	 * average time in ms taken by the excution of this query onto the DB
	 */
	public long getExecutionAvgTime() {
		return executionAvgTime;
	}

	/**
	 * max time in ms taken by the excution of this query onto the DB
	 */
	public long getExecutionMaxTime() {
		return executionMaxTime;
	}
	
	/**
	 * min time in ms taken by the excution of this query onto the DB
	 */
	public long getExecutionMinTime() {
		return executionMinTime;
	}
	
	/**
	 * add statistics report of a DB query
	 * 
	 * @param rows rows count returned
	 * @param time time taken
	 */
	void executed(long rows, long time) {
		if (time < executionMinTime) executionMinTime = time;
		if (time > executionMaxTime) executionMaxTime = time;
		executionAvgTime = ( executionAvgTime * executionCount + time ) / ( executionCount + 1 );
		executionCount++;
		executionRowCount += rows;
	}

	public String toString() {
		return new StringBuffer()
		    .append("QueryStatistics")
			.append("[cacheHitCount=").append(this.cacheHitCount)
			.append(",cacheMissCount=").append(this.cacheMissCount)
			.append(",cachePutCount=").append(this.cachePutCount)
			.append(",executionCount=").append(this.executionCount)
			.append(",executionRowCount=").append(this.executionRowCount)
			.append(",executionAvgTime=").append(this.executionAvgTime)
			.append(",executionMaxTime=").append(this.executionMaxTime)
			.append(",executionMinTime=").append(this.executionMinTime)
			.append(']')
			.toString();
	}

}
