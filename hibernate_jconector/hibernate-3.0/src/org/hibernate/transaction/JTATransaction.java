//$Id: JTATransaction.java,v 1.11 2005/02/13 01:48:30 oneovthafew Exp $
package org.hibernate.transaction;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.TransactionException;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.util.JTAHelper;

/**
 * Implements a basic transaction strategy for JTA transactions. Instances check to
 * see if there is an existing JTA transaction. If none exists, a new transaction
 * is started. If one exists, all work is done in the existing context. The
 * following properties are used to locate the underlying <tt>UserTransaction</tt>:
 * <br><br>
 * <table>
 * <tr><td><tt>hibernate.jndi.url</tt></td><td>JNDI initial context URL</td></tr>
 * <tr><td><tt>hibernate.jndi.class</tt></td><td>JNDI provider class</td></tr>
 * <tr><td><tt>jta.UserTransaction</tt></td><td>JNDI name</td></tr>
 * </table>
 * @author Gavin King
 */
public class JTATransaction implements Transaction {

	private static final Log log = LogFactory.getLog(JTATransaction.class);

	private final JDBCContext jdbcContext;
	private final TransactionFactory.Context transactionContext;

	private UserTransaction ut;
	private boolean newTransaction;
	private boolean synchronization;
	private boolean begun;
	private boolean commitFailed;
	private javax.transaction.Transaction transaction;

	public JTATransaction(JDBCContext jdbcContext, TransactionFactory.Context transactionContext) {
		this.jdbcContext = jdbcContext;
		this.transactionContext = transactionContext;
	}

	public void begin(InitialContext context, String utName) throws HibernateException {
		log.debug("begin");

		log.debug("Looking for UserTransaction under: " + utName);
		try {
			ut = (UserTransaction) context.lookup(utName);
		}
		catch (NamingException ne) {
			log.error("Could not find UserTransaction in JNDI", ne);
			throw new TransactionException("Could not find UserTransaction in JNDI: ", ne);
		}
		if (ut==null) {
			throw new AssertionFailure("A naming service lookup returned null");
		}

		log.debug("Obtained UserTransaction");

		try {
			newTransaction = ut.getStatus() == Status.STATUS_NO_TRANSACTION;
			if (newTransaction) {
				ut.begin();
				log.debug("Began a new JTA transaction");
			}
		}
		catch (Exception e) {
			log.error("JTA transaction begin failed", e);
			throw new TransactionException("JTA transaction begin failed", e);
		}

		/*if (newTransaction) {
			// don't need a synchronization since we are committing
			// or rolling back the transaction ourselves - assuming
			// that we do no work in beforeTransactionCompletion()
			synchronization = false;
		}*/

		synchronization = jdbcContext.registerSynchronizationIfPossible();

		if ( !newTransaction && !synchronization ) {
			log.warn("You should set hibernate.transaction.manager_lookup_class if cache is enabled");
		}

		if (!synchronization) {
			jdbcContext.registerCallbackIfNecessary();
		}

		begun = true;
	}

	public void commit() throws HibernateException {
		if (!begun) {
			throw new TransactionException("Transaction not successfully started");
		}

		log.debug("commit");

		boolean flush = !transactionContext.isFlushModeNever()
		        && ( !synchronization || !transactionContext.isFlushBeforeCompletionEnabled() );

		if (flush) {
			transactionContext.managedFlush(); //if an exception occurs during flush, user must call rollback()
		}

		if (!synchronization && newTransaction) {
			jdbcContext.beforeTransactionCompletion(this);
		}

		closeIfRequired();

		if (newTransaction) {
			try {
				ut.commit();
				log.debug("Committed JTA UserTransaction");
			}
			catch (Exception e) {
				commitFailed = true; // so the transaction is already rolled back, by JTA spec
				log.error("JTA commit failed", e);
				throw new TransactionException("JTA commit failed: ", e);
			}
			finally {
				afterCommitRollback();
			}
		}
		else {
			// this one only really needed for badly-behaved applications!
			// (if the TransactionManager has a Sychronization registered,
			// its a noop)
			// (actually we do need it for downgrading locks)
			afterCommitRollback();
		}

	}

	public void rollback() throws HibernateException {
		if (!begun) {
			throw new TransactionException("Transaction not successfully started");
		}

		log.debug("rollback");

		if (!synchronization && newTransaction && !commitFailed) {
			jdbcContext.beforeTransactionCompletion(this);
		}

		try {
			closeIfRequired();
		}
		catch (Exception e) {
			log.error("could not close session during rollback", e);
			//swallow it, and continue to roll back JTA transaction
		}

		try {
			if (newTransaction) {
				if (!commitFailed) {
					ut.rollback();
					log.debug("Rolled back JTA UserTransaction");
				}
			}
			else {
				ut.setRollbackOnly();
				log.debug("set JTA UserTransaction to rollback only");
			}
		}
		catch (Exception e) {
			log.error("JTA rollback failed", e);
			throw new TransactionException("JTA rollback failed", e);
		}
		finally {
			afterCommitRollback();
		}
	}

	private static final int NULL = Integer.MIN_VALUE;

	private void afterCommitRollback() throws TransactionException {

		if (!synchronization) { // this method is a noop if there is a Synchronization!

			if (!newTransaction) log.warn("You should set hibernate.transaction.manager_lookup_class if cache is enabled");
			int status=NULL;
			try {
				status = ut.getStatus();
			}
			catch (Exception e) {
				log.error("Could not determine transaction status after commit", e);
				throw new TransactionException("Could not determine transaction status after commit", e);
			}
			finally {
				/*if (status!=Status.STATUS_COMMITTED && status!=Status.STATUS_ROLLEDBACK) {
					log.warn("Transaction not complete - you should set hibernate.transaction.manager_lookup_class if cache is enabled");
					//throw exception??
				}*/
				jdbcContext.afterTransactionCompletion(status==Status.STATUS_COMMITTED, this);
			}

		}
	}

	public boolean wasRolledBack() throws TransactionException {

		if (!begun) return false;
		if (commitFailed) return true;

		final int status;
		try {
			status = ut.getStatus();
		}
		catch (SystemException se) {
			log.error("Could not determine transaction status", se);
			throw new TransactionException("Could not determine transaction status", se);
		}
		if (status==Status.STATUS_UNKNOWN) {
			throw new TransactionException("Could not determine transaction status");
		}
		else {
			return JTAHelper.isRollback(status);
		}
	}

	public boolean wasCommitted() throws TransactionException {

		if (!begun || commitFailed) return false;

		final int status;
		try {
			status = ut.getStatus();
		}
		catch (SystemException se) {
			log.error("Could not determine transaction status", se);
			throw new TransactionException("Could not determine transaction status: ", se);
		}
		if (status==Status.STATUS_UNKNOWN) {
			throw new TransactionException("Could not determine transaction status");
		}
		else {
			return status==Status.STATUS_COMMITTED;
		}
	}

	public void registerSynchronization(Synchronization sync) throws HibernateException {
		if (transaction!=null) {
			try {
				transaction.registerSynchronization(sync);
			}
			catch (Exception e) {
				throw new TransactionException("could not register synchronization", e);
			}
		}
		else {
			throw new IllegalStateException("JTA TransactionManager not available");
		}
	}

	private void closeIfRequired() throws HibernateException {
		boolean close = !synchronization && transactionContext.shouldAutoClose() && transactionContext.isOpen();
		if ( close ) {
			transactionContext.managedClose();
		}
	}
}
