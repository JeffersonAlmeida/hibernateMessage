//$Id: DummyTransaction.java,v 1.3 2005/03/16 05:11:33 oneovthafew Exp $
package org.hibernate.test.tm;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

/**
 * @author Gavin King
 */
public class DummyTransaction implements Transaction {
	
	int status;
	private Connection connection;
	List synchronizations = new ArrayList();
	
	DummyTransaction(Connection connection) {
		this.connection = connection;
		status = Status.STATUS_ACTIVE;
	}

	public void commit() throws RollbackException, HeuristicMixedException,
			HeuristicRollbackException, SecurityException,
			IllegalStateException, SystemException {
		
		if (status == Status.STATUS_MARKED_ROLLBACK) {
			rollback();
		}
		else {
			status = Status.STATUS_PREPARING;
			
			for ( int i=0; i<synchronizations.size(); i++ ) {
				Synchronization s = (Synchronization) synchronizations.get(i);
				s.beforeCompletion();
			}
			
			status = Status.STATUS_COMMITTING;
			
			try {
				connection.commit();
				connection.close();
			}
			catch (SQLException sqle) {
				status = Status.STATUS_UNKNOWN;
				throw new SystemException();
			}
			
			status = Status.STATUS_COMMITTED;

			for ( int i=0; i<synchronizations.size(); i++ ) {
				Synchronization s = (Synchronization) synchronizations.get(i);
				s.afterCompletion(status);
			}
		}

	}
	
	public boolean delistResource(XAResource arg0, int arg1)
			throws IllegalStateException, SystemException {
		// TODO Auto-generated method stub
		return false;
	}
	
	public boolean enlistResource(XAResource arg0) throws RollbackException,
			IllegalStateException, SystemException {
		// TODO Auto-generated method stub
		return false;
	}
	
	public int getStatus() throws SystemException {
		return status;
	}
	
	public void registerSynchronization(Synchronization sync)
			throws RollbackException, IllegalStateException, SystemException {
		synchronizations.add(sync);
	}
	
	public void rollback() throws IllegalStateException, SystemException {

		status = Status.STATUS_ROLLING_BACK;
		
		for ( int i=0; i<synchronizations.size(); i++ ) {
			Synchronization s = (Synchronization) synchronizations.get(i);
			s.beforeCompletion();
		}
		
		status = Status.STATUS_ROLLEDBACK;
		
		try {
			connection.rollback();
			connection.close();
		}
		catch (SQLException sqle) {
			status = Status.STATUS_UNKNOWN;
			throw new SystemException();
		}
		
		for ( int i=0; i<synchronizations.size(); i++ ) {
			Synchronization s = (Synchronization) synchronizations.get(i);
			s.afterCompletion(status);
		}
	}
	
	public void setRollbackOnly() throws IllegalStateException, SystemException {
		status = Status.STATUS_MARKED_ROLLBACK;
	}

	void setConnection(Connection connection) {
		this.connection = connection;
	}

	Connection getConnection() {
		return connection;
	}
}
