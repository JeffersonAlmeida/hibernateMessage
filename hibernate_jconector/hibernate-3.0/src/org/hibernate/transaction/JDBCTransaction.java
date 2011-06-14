//$Id: JDBCTransaction.java,v 1.9 2005/02/13 01:48:30 oneovthafew Exp $
package org.hibernate.transaction;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.jdbc.JDBCContext;

/**
 * Implements a basic transaction strategy for JDBC connections.This is the
 * default <tt>Transaction</tt> implementation used if none is explicitly
 * specified.
 * @author Anton van Straaten, Gavin King
 */
public class JDBCTransaction implements Transaction {

	private static final Log log = LogFactory.getLog(JDBCTransaction.class);

	private final JDBCContext jdbcContext;
	private final TransactionFactory.Context transactionContext;

	private boolean toggleAutoCommit;
	private boolean rolledBack;
	private boolean committed;
	private boolean begun;
	private boolean commitFailed;
	private List synchronizations;
	private boolean synchronization;

	public JDBCTransaction(JDBCContext jdbcContext, TransactionFactory.Context transactionContext) {
		this.jdbcContext = jdbcContext;
		this.transactionContext = transactionContext;
	}

	public void begin() throws HibernateException {
		log.debug("begin");

		try {
			toggleAutoCommit = jdbcContext.connection().getAutoCommit();
			if ( log.isDebugEnabled() ) log.debug("current autocommit status: " + toggleAutoCommit);
			if (toggleAutoCommit) {
				log.debug("disabling autocommit");
				jdbcContext.connection().setAutoCommit(false);
			}
		}
		catch (SQLException e) {
			log.error("JDBC begin failed", e);
			throw new TransactionException("JDBC begin failed: ", e);
		}

		synchronization = !jdbcContext.registerCallbackIfNecessary();

		begun = true;
	}
	
	private void closeIfRequired() throws HibernateException {
		if ( !synchronization && transactionContext.shouldAutoClose() && transactionContext.isOpen() ) {
			try {
				transactionContext.managedClose();
			}
			catch (HibernateException he) {
				log.error("Could not close session", he);
				//swallow, the transaction was finished
			}
		}
	}

	public void commit() throws HibernateException {
		if (!begun) {
			throw new TransactionException("Transaction not successfully started");
		}

		log.debug("commit");

		if ( !transactionContext.isFlushModeNever() && !synchronization ) {
			transactionContext.managedFlush(); //if an exception occurs during flush, user must call rollback()
		}

		beforeTransactionCompletion();
		if ( !synchronization ) {
			jdbcContext.beforeTransactionCompletion( this );
		}

		try {
			jdbcContext.connection().commit();
			log.debug("committed JDBC Connection");
			committed = true;
			if ( !synchronization ) {
				jdbcContext.afterTransactionCompletion( true, this );
			}
			afterTransactionCompletion( Status.STATUS_COMMITTED );
		}
		catch (SQLException e) {
			log.error("JDBC commit failed", e);
			commitFailed = true;
			if ( !synchronization ) {
				jdbcContext.afterTransactionCompletion( false, this );
			}
			afterTransactionCompletion( Status.STATUS_UNKNOWN );
			throw new TransactionException("JDBC commit failed", e);
		}
		finally {
			toggleAutoCommit();
			closeIfRequired();
		}
	}

	public void rollback() throws HibernateException {

		if (!begun) {
			throw new TransactionException("Transaction not successfully started");
		}

		log.debug("rollback");

		if (!commitFailed) {

			beforeTransactionCompletion();
			if ( !synchronization ) {
				jdbcContext.beforeTransactionCompletion( this );
			}

			try {
				jdbcContext.connection().rollback();
				log.debug("rolled back JDBC Connection");
				rolledBack = true;
				afterTransactionCompletion(Status.STATUS_ROLLEDBACK);
			}
			catch (SQLException e) {
				log.error("JDBC rollback failed", e);
				afterTransactionCompletion(Status.STATUS_UNKNOWN);
				throw new TransactionException("JDBC rollback failed", e);
			}
			finally {
				if ( !synchronization ) {
					jdbcContext.afterTransactionCompletion( false, this );
				}
				toggleAutoCommit();
				closeIfRequired();
			}
		}
	}

	private void toggleAutoCommit() {
		try {
			if (toggleAutoCommit) {
				log.debug("re-enabling autocommit");
				jdbcContext.connection().setAutoCommit( true );
			}
		}
		catch (Exception sqle) {
			log.error("Could not toggle autocommit", sqle);
			//swallow it (the transaction _was_ successful or successfully rolled back)
		}
	}

	public boolean wasRolledBack() {
		return rolledBack;
	}
	
	public boolean wasCommitted() {
		return committed;
	}

	public void registerSynchronization(Synchronization sync) throws HibernateException {
		if (sync==null) throw new NullPointerException("null Synchronization");
		if (synchronizations==null) {
			synchronizations = new ArrayList();
		}
		synchronizations.add(sync);
	}
	
	private void beforeTransactionCompletion() {
		if (synchronizations!=null) {
			for ( int i=0; i<synchronizations.size(); i++ ) {
				Synchronization sync = (Synchronization) synchronizations.get(i);
				try {
					sync.beforeCompletion();
				}
				catch (Throwable t) {
					log.error("exception calling user Synchronization", t);
				}
			}
		}
	}

	private void afterTransactionCompletion(int status) {
		if (synchronizations!=null) {
			for ( int i=0; i<synchronizations.size(); i++ ) {
				Synchronization sync = (Synchronization) synchronizations.get(i);
				try {
					sync.afterCompletion(status);
				}
				catch (Throwable t) {
					log.error("exception calling user Synchronization", t);
				}
			}
		}
	}
}
