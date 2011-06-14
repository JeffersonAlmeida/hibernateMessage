//$Id: TransientObjectException.java,v 1.1 2004/06/03 16:30:04 steveebersole Exp $
package org.hibernate;

/**
 * Throw when the user passes a transient instance to a <tt>Session</tt>
 * method that expects a persistent instance.
 *
 * @author Gavin King
 */

public class TransientObjectException extends HibernateException {

	public TransientObjectException(String s) {
		super(s);
	}

}






