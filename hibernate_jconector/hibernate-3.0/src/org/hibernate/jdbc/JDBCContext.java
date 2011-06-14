// $Id: JDBCContext.java,v 1.1 2005/02/13 01:48:30 oneovthafew Exp $
package org.hibernate.jdbc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cfg.Settings;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.HibernateException;
import org.hibernate.TransactionException;
import org.hibernate.Transaction;
import org.hibernate.util.JTAHelper;
import org.hibernate.transaction.CacheSynchronization;
import org.hibernate.transaction.TransactionFactory;

import javax.transaction.TransactionManager;
import javax.transaction.SystemException;
import java.sql.Connection;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectInputStream;

/**
 * Implementation of JDBCContext.
 *
 * @author Steve Ebersole
 */
public class JDBCContext implements Serializable {

	private static final Log log = LogFactory.getLog( JDBCContext.class );

	public static interface Context extends TransactionFactory.Context {
		public void beforeTransactionCompletion(org.hibernate.Transaction tx);
		public void afterTransactionCompletion(boolean success, org.hibernate.Transaction tx);
	}

	private Context owner;
	private transient Batcher batcher;
	private transient Connection connection;
	private boolean autoClose;

	private transient boolean connect;
	private boolean isTransactionCallbackRegistered;


	public JDBCContext(Context owner, Connection connection, boolean autoClose) {
		
		this.owner = owner;
		this.connection = connection;
		this.connect = connection == null;
		this.autoClose = autoClose;

		final Settings settings = owner.getFactory().getSettings();
		
		this.batcher = settings.getBatcherFactory().createBatcher( this );
		
		final boolean registerSynchronization = settings.isAutoCloseSessionEnabled() || 
				settings.isFlushBeforeCompletionEnabled();
		if ( registerSynchronization ) registerSynchronizationIfPossible();
		
	}

	public SessionFactoryImplementor getFactory() {
		return owner.getFactory();
	}

	public Batcher getBatcher() {
		return batcher;
	}

	public boolean isConnected() {
		return connection != null || connect;
	}

	public Connection release() {
		if (connection==null) {
			connect = false;
			return null;
		}
		else {
			return disconnect();
		}
	}

	public Connection connection() throws HibernateException {
		if (connection==null) {
			if (connect) {
				connect();
			}
			else if ( owner.isOpen() ) {
				throw new HibernateException("Session is currently disconnected");
			}
			else {
				throw new HibernateException("Session is closed");
			}
		}
		return connection;
	}

	public boolean registerCallbackIfNecessary() {
		if ( isTransactionCallbackRegistered ) {
			return false;
		}
		else {
			isTransactionCallbackRegistered = true;
			return true;
		}

	}

	public boolean registerSynchronizationIfPossible() {
		if ( isTransactionCallbackRegistered ) return true;
		TransactionManager tm = owner.getFactory().getTransactionManager();
		if ( tm == null ) {
			return false;
		}
		else {
			try {
				javax.transaction.Transaction tx = tm.getTransaction();
				if ( isJTATransactionInProgress(tx) ) {
					tx.registerSynchronization( new CacheSynchronization(owner, this, tx, null) );
					isTransactionCallbackRegistered = true;
					log.debug("successfully registered Synchronization");
					return true;
				}
				else {
					return false;
				}
			}
			catch (Exception e) {
				throw new TransactionException( "could not register synchronization with JTA TransactionManager", e );
			}
		}
	}

	private boolean isJTATransactionInProgress(javax.transaction.Transaction tx) throws SystemException {
		return tx != null && JTAHelper.isInProgress( tx.getStatus() );
	}

	private void connect() throws HibernateException {
		connection = batcher.openConnection();
		connect = false;
		if ( !isTransactionCallbackRegistered ) {
			//if there is no current transaction callback registered
			//when we obtain the connection, try to register one now
			//note that this is not going to handle the case of
			//multiple-transactions-per-connection when the user is
			//manipulating transactions (need to use Hibernate txn)
			registerSynchronizationIfPossible();
		}

		if ( owner.getFactory().getStatistics().isStatisticsEnabled() ) {
			owner.getFactory().getStatisticsImplementor().connect();
		}

	}

	public Connection disconnect() throws HibernateException {
		try {
			if (connect) {
				connect = false;
				return null;
			}
			else {
				if (connection==null) {
					throw new HibernateException( "Already disconnected" );
				}

				batcher.closeStatements();
				Connection c = connection;
				connection = null;
				if (autoClose) {
					batcher.closeConnection(c);
					return null;
				}
				else {
					return c;
				}
			}
		}
		finally {
			if ( !isTransactionCallbackRegistered ) {
				afterTransactionCompletion(false, null); //false because we don't know the outcome of the transaction
			}
		}
	}

	public void reconnect() throws HibernateException {
		if ( isConnected() ) throw new HibernateException( "Already connected" );
		if ( !owner.isOpen() ) throw new HibernateException( "Session is closed" );

		log.debug( "reconnecting session" );

		connect = true;
		//connection = factory.openConnection();
	}

	public void reconnect(Connection conn) throws HibernateException {
		if ( isConnected() ) throw new HibernateException( "Already connected" );
		this.connection = conn;
	}

	public Transaction beginTransaction() throws HibernateException {
		return owner.getFactory().getSettings().getTransactionFactory().beginTransaction( this, owner );
	}

	public void beforeTransactionCompletion(Transaction tx) {
		log.trace( "before transaction completion" );
		owner.beforeTransactionCompletion(tx);
	}

	public void afterTransactionCompletion(boolean success, Transaction tx) {
		log.trace( "after transaction completion" );

		if ( getFactory().getStatistics().isStatisticsEnabled() ) {
			getFactory().getStatisticsImplementor().endTransaction(success);
		}
			
		isTransactionCallbackRegistered = false;
		owner.afterTransactionCompletion(success, tx);
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		if ( isConnected() ) throw new IllegalStateException( "Cannot serialize a JDBCContext while connected" );

		log.trace( "Serializing JDBCContext" );

		oos.writeObject( owner );
		oos.defaultWriteObject();
	}


	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		log.trace( "Deserializing JDBCContext" );

		owner = ( Context ) ois.readObject();
		ois.defaultReadObject();

		batcher = owner.getFactory().getSettings().getBatcherFactory().createBatcher( this );
	}

	/**
	 * Just in case user forgot to commit()/cancel() or close()
	 */
	protected void finalize() throws Throwable {

		log.debug( "running Session.finalize()" );

		if (isTransactionCallbackRegistered) {
			log.warn( "afterTransactionCompletion() was never called" );
		}

		if ( connection != null ) {

			if ( connection.isClosed() ) {
				log.warn( "finalizing unclosed session with closed connection" );
			}
			else {
				log.warn("unclosed connection, forgot to call close() on your session?");
				if (autoClose) {
					//TODO: Should I also call closeStatements() from here?
					connection.close();
				}
			}
		}
	}
}
