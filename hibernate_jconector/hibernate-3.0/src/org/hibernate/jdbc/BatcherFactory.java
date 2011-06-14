//$Id: BatcherFactory.java,v 1.3 2005/02/13 01:48:30 oneovthafew Exp $
package org.hibernate.jdbc;


/**
 * Factory for <tt>Batcher</tt> instances.
 * @author Gavin King
 */
public interface BatcherFactory {
	public Batcher createBatcher(JDBCContext jdbcContext);
}
