//$Id: JTATransactionFactory.java,v 1.4 2005/02/13 01:48:30 oneovthafew Exp $
package org.hibernate.transaction;

import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.hibernate.jdbc.JDBCContext;
import org.hibernate.cfg.Environment;
import org.hibernate.util.NamingHelper;

/**
 * Factory for <tt>JTATransaction</tt>.
 *
 * @see JTATransaction
 * @author Gavin King
 */
public class JTATransactionFactory implements TransactionFactory {

	private static final Log log = LogFactory.getLog(JTATransactionFactory.class);
	private static final String DEFAULT_USER_TRANSACTION_NAME = "java:comp/UserTransaction";

	private InitialContext context;
	private String utName;

	public void configure(Properties props) throws HibernateException {
		try {
			context = NamingHelper.getInitialContext(props);
		}
		catch (NamingException ne) {
			log.error("Could not obtain initial context", ne);
			throw new HibernateException( "Could not obtain initial context", ne );
		}

		utName = props.getProperty(Environment.USER_TRANSACTION);
		
		if (utName==null) {
			TransactionManagerLookup lookup = TransactionManagerLookupFactory.getTransactionManagerLookup(props);
			if (lookup!=null) utName = lookup.getUserTransactionName();
		}

		if (utName==null) utName = DEFAULT_USER_TRANSACTION_NAME;
	}

	public Transaction beginTransaction(JDBCContext jdbcContext, Context transactionContext) throws HibernateException {
		JTATransaction tx = new JTATransaction(jdbcContext, transactionContext);
		tx.begin(context, utName);
		return tx;
	}

}
