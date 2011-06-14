//$Id: PersistEventListener.java,v 1.1 2005/02/21 14:39:01 oneovthafew Exp $
package org.hibernate.event;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.HibernateException;

/**
 * Defines the contract for handling of create events generated from a session.
 *
 * @author Gavin King
 */
public interface PersistEventListener extends Serializable {

    /** 
     * Handle the given create event.
     *
     * @param event The create event to be handled.
     * @throws HibernateException
     */
	public void onPersist(PersistEvent event) throws HibernateException;

    /** 
     * Handle the given create event.
     *
     * @param event The create event to be handled.
     * @throws HibernateException
     */
	public void onPersist(PersistEvent event, Map createdAlready) throws HibernateException;

}
