//$Id: JDBCTransactionFactory.java,v 1.3 2005/02/13 01:48:30 oneovthafew Exp $
package org.hibernate.transaction;

import java.util.Properties;

import org.hibernate.Transaction;
import org.hibernate.HibernateException;
import org.hibernate.jdbc.JDBCContext;

/**
 * Factory for <tt>JDBCTransaction</tt>.
 * @see JDBCTransaction
 * @author Anton van Straaten
 */
public final class JDBCTransactionFactory implements TransactionFactory {

	public Transaction beginTransaction(JDBCContext jdbcContext, Context transactionContext) throws HibernateException {
		JDBCTransaction tx = new JDBCTransaction( jdbcContext, transactionContext );
		tx.begin();
		return tx;
	}

	public void configure(Properties props) throws HibernateException {}

}
