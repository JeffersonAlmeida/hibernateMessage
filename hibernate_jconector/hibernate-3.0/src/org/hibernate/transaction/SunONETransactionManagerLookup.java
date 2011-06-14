//$Id: SunONETransactionManagerLookup.java,v 1.2 2004/10/20 01:35:06 oneovthafew Exp $
package org.hibernate.transaction;

/**
 * TransactionManager lookup strategy for Sun ONE Application Server 7
 * @author Robert Davidson, Sanjeev Krishnan
 */
public class SunONETransactionManagerLookup extends JNDITransactionManagerLookup {

	protected String getName() {
		return "java:pm/TransactionManager";
	}

	public String getUserTransactionName() {
		return "java:comp/UserTransaction";
	}
	
}
