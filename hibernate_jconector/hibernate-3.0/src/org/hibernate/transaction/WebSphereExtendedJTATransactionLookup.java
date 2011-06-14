//$Id: WebSphereExtendedJTATransactionLookup.java,v 1.2 2005/01/05 20:13:06 oneovthafew Exp $
package org.hibernate.transaction;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Properties;

import javax.naming.NamingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.hibernate.HibernateException;
import org.hibernate.util.NamingHelper;

/**
 * Support for proprietary interfaces for registering synchronizations in WebSphere 5 and 6.
 * @author Gavin King
 */
public class WebSphereExtendedJTATransactionLookup implements TransactionManagerLookup {
	
	public TransactionManager getTransactionManager(Properties props)
	throws HibernateException {
		return new TransactionManagerAdapter(props);
	}

	public String getUserTransactionName() {
		return "java:comp/UserTransaction";
	}
	
	public static class TransactionManagerAdapter implements TransactionManager {

		private final Properties properties;
		private final Class synchronizationCallbackClass;
		private final Method registerSynchronizationMethod;
		
		private TransactionManagerAdapter(Properties props) {
			this.properties = props;
			try {
				synchronizationCallbackClass = Class.forName("com.ibm.websphere.jtaextensions.SynchronizationCallback");
				registerSynchronizationMethod = Class.forName("com.ibm.websphere.jtaextensions.ExtendedJTATransaction")
					.getMethod( "registerSynchronizationCallback", new Class[] { synchronizationCallbackClass } );
				
			}
			catch (ClassNotFoundException cnfe) {
				throw new HibernateException(cnfe);
			}
			catch (NoSuchMethodException nsme) {
				throw new HibernateException(nsme);
			}
		}
		
		public void begin() throws NotSupportedException, SystemException {
			throw new UnsupportedOperationException();
		}

		public void commit() throws RollbackException, HeuristicMixedException,
				HeuristicRollbackException, SecurityException,
				IllegalStateException, SystemException {
			throw new UnsupportedOperationException();
		}

		public int getStatus() throws SystemException {
			throw new UnsupportedOperationException();
		}

		public Transaction getTransaction() throws SystemException {
			return new TransactionAdapter(properties);
		}

		public void resume(Transaction txn) throws  InvalidTransactionException, 
				IllegalStateException, SystemException {
			throw new UnsupportedOperationException();
		}

		public void rollback() throws IllegalStateException, SecurityException,
				SystemException {
			throw new UnsupportedOperationException();
		}

		public void setRollbackOnly() throws IllegalStateException,
				SystemException {
			throw new UnsupportedOperationException();
		}

		public void setTransactionTimeout(int i) throws SystemException {
			throw new UnsupportedOperationException();
		}

		public Transaction suspend() throws SystemException {
			throw new UnsupportedOperationException();
		}

		public class TransactionAdapter implements Transaction {
			
			private final Object extendedJTATransaction;
			
			private TransactionAdapter(Properties props) {
				try {
					extendedJTATransaction = NamingHelper.getInitialContext(props)
						.lookup("java:comp/websphere/ExtendedJTATransaction");
				}
				catch (NamingException ne) {
					throw new HibernateException(ne);
				}
			}

			public void registerSynchronization(final Synchronization synchronization)
					throws RollbackException, IllegalStateException,
					SystemException {
				
				final InvocationHandler ih = new InvocationHandler() {
					
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						if ( "afterCompletion".equals( method.getName() ) ) {
							int status = args[3].equals(Boolean.TRUE) ? 
									Status.STATUS_COMMITTED : 
									Status.STATUS_UNKNOWN;
							synchronization.afterCompletion(status);
						}
						else if ( "beforeCompletion".equals( method.getName() ) ) {
							synchronization.beforeCompletion();
						}
						return null;
					}
					
				};
				
				final Object synchronizationCallback = Proxy.newProxyInstance( 
						getClass().getClassLoader(), 
						new Class[] { synchronizationCallbackClass }, 
						ih 
				);
				
				try {
					registerSynchronizationMethod.invoke( 
							extendedJTATransaction, 
							new Object[] { synchronizationCallback } 
					);
				}
				catch (Exception e) {
					throw new HibernateException(e);
				}

			}

			public void commit() throws RollbackException, HeuristicMixedException,
					HeuristicRollbackException, SecurityException,
					IllegalStateException, SystemException {
				throw new UnsupportedOperationException();
			}
		
			public boolean delistResource(XAResource resource, int i)
					throws IllegalStateException, SystemException {
				throw new UnsupportedOperationException();
			}
		
			public boolean enlistResource(XAResource resource)
					throws RollbackException, IllegalStateException,
					SystemException {
				throw new UnsupportedOperationException();
			}
		
			public int getStatus() throws SystemException {
				throw new UnsupportedOperationException();
			}

			public void rollback() throws IllegalStateException, SystemException {
				throw new UnsupportedOperationException();
			}

			public void setRollbackOnly() throws IllegalStateException,
					SystemException {
				throw new UnsupportedOperationException();
			}
		}
			
	}
	
}
