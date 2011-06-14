//$Id: DummyConnectionProvider.java,v 1.3 2005/02/13 01:59:07 oneovthafew Exp $
package org.hibernate.test.tm;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.transaction.SystemException;

import org.hibernate.HibernateException;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.connection.ConnectionProviderFactory;

/**
 * @author Gavin King
 */
public class DummyConnectionProvider implements ConnectionProvider {
	
	ConnectionProvider cp;
	boolean isTransaction;

	public void configure(Properties props) throws HibernateException {
		cp = ConnectionProviderFactory.newConnectionProvider();
	}
	
	public Connection getConnection() throws SQLException {
		try {
			DummyTransactionManager dtm = DummyTransactionManager.INSTANCE;
			if (dtm!=null && dtm.getTransaction()!=null) {
				isTransaction = true;
				return ( (DummyTransaction) dtm.getTransaction() ).getConnection();
			}
			else {
				isTransaction = false;
				return cp.getConnection();
			}
		}
		catch (SystemException se) {
			throw new SQLException();
		}
	}

	public void closeConnection(Connection conn) throws SQLException {
		if (!isTransaction) conn.close();
	}

	public void close() throws HibernateException {

	}

}
