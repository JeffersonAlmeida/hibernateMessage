//$Id: ResinTransactionManagerLookup.java,v 1.1 2004/06/03 16:31:29 steveebersole Exp $
package org.hibernate.transaction;

/**
 * TransactionManager lookup strategy for Resin
 * @author Aapo Laakkonen
 */
public class ResinTransactionManagerLookup
extends JNDITransactionManagerLookup {

	/**
	 * @see org.hibernate.transaction.JNDITransactionManagerLookup#getName()
	 */
	protected String getName() {
		return "java:comp/TransactionManager";
	}

	public String getUserTransactionName() {
		return "java:comp/UserTransaction";
	}

}






