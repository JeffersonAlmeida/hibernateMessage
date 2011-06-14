//$Id: JBossTransactionManagerLookup.java,v 1.1 2004/06/03 16:31:14 steveebersole Exp $
package org.hibernate.transaction;

/**
 * A <tt>TransactionManager</tt> lookup strategy for JBoss
 * @author Gavin King
 */
public final class JBossTransactionManagerLookup extends JNDITransactionManagerLookup {

	protected String getName() {
		return "java:/TransactionManager";
	}

	public String getUserTransactionName() {
		return "UserTransaction";
	}

}






