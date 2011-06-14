//$Id: StatisticsServiceMBean.java,v 1.4 2004/08/15 12:55:27 oneovthafew Exp $
package org.hibernate.jmx;

import org.hibernate.stat.Statistics;

/**
 * MBean exposing Session Factory statistics
 * 
 * @see org.hibernate.stat.Statistics
 * @author Emmanuel Bernard
 */
public interface StatisticsServiceMBean extends Statistics {
	/**
	 * Publish the statistics of a session factory bound to 
	 * the default JNDI context
	 * @param sfJNDIName session factory jndi name
	 */
	public abstract void setSessionFactoryJNDIName(String sfJNDIName);
}