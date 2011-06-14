//$Id: DeleteEventListener.java,v 1.2 2004/08/08 11:24:54 oneovthafew Exp $
package org.hibernate.event;

import org.hibernate.HibernateException;

import java.io.Serializable;

/**
 * Defines the contract for handling of deletion events generated from a session.
 *
 * @author Steve Ebersole
 */
public interface DeleteEventListener extends Serializable {

    /** Handle the given delete event.
     *
     * @param event The delete event to be handled.
     * @throws HibernateException
     */
	public void onDelete(DeleteEvent event) throws HibernateException;
}
