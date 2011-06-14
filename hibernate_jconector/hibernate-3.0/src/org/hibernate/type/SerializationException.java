//$Id: SerializationException.java,v 1.1 2004/06/03 16:31:30 steveebersole Exp $
package org.hibernate.type;

import org.hibernate.HibernateException;

/**
 * Thrown when a property cannot be serializaed/deserialized
 * @author Gavin King
 */
public class SerializationException extends HibernateException {

	public SerializationException(String message, Exception root) {
		super(message, root);
	}

}






