//$Id: CallbackException.java,v 1.4 2004/08/11 09:10:43 oneovthafew Exp $
package org.hibernate;


/**
 * Should be thrown by persistent objects from <tt>Lifecycle</tt>
 * or <tt>Interceptor</tt> callbacks.
 *
 * @see Lifecycle
 * @see Interceptor
 * @author Gavin King
 */

public class CallbackException extends HibernateException {

	public CallbackException(Exception root) {
		super("An exception occurred in a callback", root);
	}

	public CallbackException(String message) {
		super(message);
	}

	public CallbackException(String message, Exception e) {
		super(message, e);
	}

}






