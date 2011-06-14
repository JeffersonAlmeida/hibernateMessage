//$Id: BatchingBatcherFactory.java,v 1.4 2005/02/13 11:50:04 oneovthafew Exp $
package org.hibernate.jdbc;


/**
 * @author Gavin King
 */
public class BatchingBatcherFactory implements BatcherFactory {

	public Batcher createBatcher(JDBCContext jdbcContext) {
		return new BatchingBatcher(jdbcContext);
	}

}
