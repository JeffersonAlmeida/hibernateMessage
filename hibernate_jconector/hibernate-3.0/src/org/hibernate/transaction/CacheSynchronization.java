//$Id: CacheSynchronization.java,v 1.3 2005/02/13 01:48:30 oneovthafew Exp $
package org.hibernate.transaction;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.TransactionException;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.util.JTAHelper;

/**
 * @author Gavin King
 */
public final class CacheSynchronization implements Synchronization {

	private static final Log log = LogFactory.getLog(CacheSynchronization.class);

	private final TransactionFactory.Context ctx;
	private JDBCContext jdbcContext;
	private final Transaction transaction;
	private final org.hibernate.Transaction hibernateTransaction;

	public CacheSynchronization(TransactionFactory.Context ctx, JDBCContext jdbcContext, Transaction transaction, org.hibernate.Transaction tx) {
		this.ctx = ctx;
		this.jdbcContext = jdbcContext;
		this.transaction = transaction;
		this.hibernateTransaction = tx;
	}

	public void beforeCompletion() {
		log.trace("transaction before completion callback");

		boolean flush;
		try {
			flush = !ctx.isFlushModeNever() &&
			        ctx.isFlushBeforeCompletionEnabled() && !JTAHelper.isRollback( transaction.getStatus() );
		}
		catch (SystemException se) {
			log.error("could not determine transaction status", se);
			throw new TransactionException("could not determine transaction status in beforeCompletion()", se);
		}
		
		try {
			if (flush) {
				log.trace("automatically flushing session");
				ctx.managedFlush();
			}
		}
		finally {
			
			jdbcContext.beforeTransactionCompletion(hibernateTransaction);
			
			if ( ctx.shouldAutoClose() && ctx.isOpen() ) {
				log.trace("automatically closing session");
				ctx.managedClose();
			}
		}
	}

	public void afterCompletion(int status) {
		if ( log.isTraceEnabled() ) log.trace("transaction after completion callback, status: " + status);
		jdbcContext.afterTransactionCompletion(status==Status.STATUS_COMMITTED, hibernateTransaction);
	}

}