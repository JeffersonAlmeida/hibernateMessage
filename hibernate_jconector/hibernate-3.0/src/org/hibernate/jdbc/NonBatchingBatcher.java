//$Id: NonBatchingBatcher.java,v 1.4 2005/02/13 01:48:30 oneovthafew Exp $
package org.hibernate.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.StaleStateException;

/**
 * An implementation of the <tt>Batcher</tt> interface that does no batching
 *
 * @author Gavin King
 */
public class NonBatchingBatcher extends AbstractBatcher {

	public NonBatchingBatcher(JDBCContext jdbcContext) {
		super( jdbcContext );
	}

	public void addToBatch(int expectedRowCount) throws SQLException, HibernateException {
		final int rowCount = getStatement().executeUpdate();
		//negative expected row count means we don't know how many rows to expect
		if ( expectedRowCount>0 ) {
			if ( expectedRowCount<rowCount ) {
				throw new StaleStateException(
						"Unexpected row count: " + rowCount + 
						" expected: " + expectedRowCount
				);
			}
			if ( expectedRowCount>rowCount ) {
				throw new HibernateException(
						"Unexpected row count: " + rowCount + 
						" expected: " + expectedRowCount
				);
			}		
		}
	}

	protected void doExecuteBatch(PreparedStatement ps) throws SQLException, HibernateException {
	}

}
