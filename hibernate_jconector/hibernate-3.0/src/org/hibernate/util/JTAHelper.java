//$Id: JTAHelper.java,v 1.1 2004/09/17 09:04:17 oneovthafew Exp $
package org.hibernate.util;

import javax.transaction.Status;

/**
 * @author Gavin King
 */
public final class JTAHelper {
	
	private JTAHelper() {}
	
	public static boolean isRollback(int status) {
		return status==Status.STATUS_MARKED_ROLLBACK ||
			status==Status.STATUS_ROLLING_BACK ||
			status==Status.STATUS_ROLLEDBACK;
	}
	
	public static boolean isInProgress(int status) {
		return status==Status.STATUS_ACTIVE || 
			status==Status.STATUS_MARKED_ROLLBACK;
	}
	
}
