//$Id: DummyTransactionManager.java,v 1.4 2005/03/16 05:11:34 oneovthafew Exp $
package org.hibernate.test.tm;

import java.sql.SQLException;
import java.util.Properties;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.connection.ConnectionProvider;
import org.hibernate.connection.ConnectionProviderFactory;

/**
 * @author Gavin King
 */
public class DummyTransactionManager implements TransactionManager {

	DummyTransaction current;
	ConnectionProvider connections;
	static DummyTransactionManager INSTANCE;
	
	public DummyTransactionManager(Properties props) {
		connections = ConnectionProviderFactory.newConnectionProvider();
	}
	
	public void begin() throws NotSupportedException, SystemException {
		try {
			current = new DummyTransaction( connections.getConnection() );
		}
		catch (SQLException sqle) {
			throw new SystemException();
		}
	}

	public void commit() throws RollbackException, HeuristicMixedException,
			HeuristicRollbackException, SecurityException,
			IllegalStateException, SystemException {
		current.commit();
	}


	public int getStatus() throws SystemException {
		return current.getStatus();
	}

	public Transaction getTransaction() throws SystemException {
		return current;
	}

	public void resume(Transaction arg0) throws InvalidTransactionException,
			IllegalStateException, SystemException {

	}

	public void rollback() throws IllegalStateException, SecurityException,
			SystemException {
		current.rollback();

	}

	public void setRollbackOnly() throws IllegalStateException, SystemException {
		current.setRollbackOnly();

	}

	public void setTransactionTimeout(int arg0) throws SystemException {
	}
	
	public Transaction suspend() throws SystemException {
		return null;
	}

}
