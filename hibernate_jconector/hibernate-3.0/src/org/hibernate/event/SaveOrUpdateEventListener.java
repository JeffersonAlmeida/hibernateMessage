//$Id: SaveOrUpdateEventListener.java,v 1.1 2004/09/04 11:37:51 oneovthafew Exp $
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.HibernateException;

/**
 * Defines the contract for handling of update events generated from a session.
 *
 * @author Steve Ebersole
 */
public interface SaveOrUpdateEventListener extends Serializable {

    /** 
     * Handle the given update event.
     *
     * @param event The update event to be handled.
     * @throws HibernateException
     */
	public Serializable onSaveOrUpdate(SaveOrUpdateEvent event) throws HibernateException;

}
