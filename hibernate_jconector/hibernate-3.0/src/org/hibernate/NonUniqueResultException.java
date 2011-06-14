//$Id: NonUniqueResultException.java,v 1.1 2004/06/03 16:30:04 steveebersole Exp $
package org.hibernate;

/**
 * Thrown when the application calls <tt>Query.uniqueResult()</tt> and
 * the query returned more than one result. Unlike all other Hibernate
 * exceptions, this one is recoverable!
 *
 * @author Gavin King
 */
public class NonUniqueResultException extends HibernateException {

	public NonUniqueResultException(int resultCount) {
		super( "query did not return a unique result: " + resultCount );
	}

}
