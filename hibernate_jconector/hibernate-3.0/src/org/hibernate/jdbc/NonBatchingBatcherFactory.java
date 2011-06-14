//$Id: NonBatchingBatcherFactory.java,v 1.3 2005/02/13 01:48:30 oneovthafew Exp $
package org.hibernate.jdbc;


/**
 * @author Gavin King
 */
public class NonBatchingBatcherFactory implements BatcherFactory {

	public Batcher createBatcher(JDBCContext jdbcContext) {
		return new NonBatchingBatcher( jdbcContext );
	}

}
