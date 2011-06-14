//$Id: PersistentObjectException.java,v 1.2 2005/02/11 09:12:05 oneovthafew Exp $
package org.hibernate;

/**
 * Throw when the user passes a persistent instance to a <tt>Session</tt>
 * method that expects a transient instance.
 *
 * @author Gavin King
 */
public class PersistentObjectException extends HibernateException {
	
	public PersistentObjectException(String s) {
		super(s);
	}
}






