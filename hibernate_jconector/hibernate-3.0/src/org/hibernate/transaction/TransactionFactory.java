//$Id: TransactionFactory.java,v 1.3 2005/02/13 01:48:30 oneovthafew Exp $
package org.hibernate.transaction;

import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.engine.SessionFactoryImplementor;

/**
 * An abstract factory for <tt>Transaction</tt> instances. Concrete implementations
 * are specified by <tt>hibernate.transaction.factory_class</tt>.<br>
 * <br>
 * Implementors must be threadsafe and should declare a public default constructor.
 * @see Transaction
 *
 * @author Anton van Straaten, Gavin King
 */
public interface TransactionFactory {

	public static interface Context {
		public SessionFactoryImplementor getFactory();
		public boolean isOpen();

		public boolean isFlushModeNever();
		public boolean isFlushBeforeCompletionEnabled();
		public void managedFlush();

		public boolean shouldAutoClose();
		public void managedClose();
	}

	/**
	 * Begin a transaction and return the associated <tt>Transaction</tt> instance.
	 *
	 * @param jdbcContext  The jdbc context to which the transaction belongs
	 * @param context The contract regarding the context in which this transaction will operate.
	 * @return Transaction
	 * @throws HibernateException
	 */
	public Transaction beginTransaction(JDBCContext jdbcContext, Context context) throws HibernateException;

	/**
	 * Configure from the given properties.
	 * @param props
	 * @throws HibernateException
	 */
	public void configure(Properties props) throws HibernateException;
}





