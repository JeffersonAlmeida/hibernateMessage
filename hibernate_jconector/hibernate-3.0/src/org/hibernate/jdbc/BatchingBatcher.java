//$Id: BatchingBatcher.java,v 1.6 2005/02/13 01:48:30 oneovthafew Exp $
package org.hibernate.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.StaleStateException;

/**
 * An implementation of the <tt>Batcher</tt> interface that
 * actually uses batching
 * @author Gavin King
 */
public class BatchingBatcher extends AbstractBatcher {

	private int batchSize;
	private int[] expectedRowCounts;

	public BatchingBatcher(JDBCContext jdbcContext) {
		super(jdbcContext);
		expectedRowCounts = new int[ getFactory().getSettings().getJdbcBatchSize() ];
	}

	public void addToBatch(int expectedRowCount) throws SQLException, HibernateException {

		log.trace("Adding to batch");
		PreparedStatement batchUpdate = getStatement();
		batchUpdate.addBatch();
		expectedRowCounts[ batchSize++ ] = expectedRowCount;
		if ( batchSize==getFactory().getSettings().getJdbcBatchSize() ) {
			//try {
				doExecuteBatch(batchUpdate);
			/*}
			catch (SQLException sqle) {
				closeStatement(batchUpdate);
				throw sqle;
			}
			catch (HibernateException he) {
				closeStatement(batchUpdate);
				throw he;
			}*/
		}

	}

	protected void doExecuteBatch(PreparedStatement ps) throws SQLException, HibernateException {
		
		if (batchSize==0) {
			log.debug("no batched statements to execute");
		}
		else {
		
			if ( log.isDebugEnabled() ) log.debug("Executing batch size: " + batchSize );
	
			try {
				checkRowCounts( ps.executeBatch() );
			}
			catch (RuntimeException re) {
				log.error("Exception executing batch: ", re);
				throw re;
			}
			finally {
				batchSize=0;
				//ps.clearBatch();
			}
			
		}

	}

	private void checkRowCounts(int[] rowCounts) {
		for ( int i=0; i<batchSize; i++ ) {
			checkRowCount( rowCounts[i], expectedRowCounts[i], i );
		}
	}
	
	private void checkRowCount(int rowCount, int expectedRowCount, int i) {
		if ( rowCount==-2 ) {
			if ( log.isDebugEnabled() ) log.debug("success of batch update unknown: " + i);
		}
		else if ( rowCount==-3 ) {
			throw new HibernateException("Batch update failed: " + i);
		}
		else {
			if ( expectedRowCount>=0 ) {
				if ( rowCount<expectedRowCount ) {
					throw new StaleStateException(
							"Batch update returned unexpected row count from update: " + i +
							" actual row count: " + rowCount +
							" expected: " + expectedRowCount
					);
				}
				if ( rowCount>expectedRowCount ) {
					throw new HibernateException(
							"Batch update returned unexpected row count from update: " + i +
							" actual row count: " + rowCount +
							" expected: " + expectedRowCount
					);
				}
			}
		}
	}

}






