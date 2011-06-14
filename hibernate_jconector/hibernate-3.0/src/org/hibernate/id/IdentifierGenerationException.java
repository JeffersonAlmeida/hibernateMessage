//$Id: IdentifierGenerationException.java,v 1.1 2004/06/03 16:30:08 steveebersole Exp $
package org.hibernate.id;

import org.hibernate.HibernateException;

/**
 * Thrown by <tt>IdentifierGenerator</tt> implementation class when
 * ID generation fails.
 *
 * @see IdentifierGenerator
 * @author Gavin King
 */

public class IdentifierGenerationException extends HibernateException {

	public IdentifierGenerationException(String msg) {
		super(msg);
	}

	public IdentifierGenerationException(String msg, Throwable t) {
		super(msg, t);
	}

}






