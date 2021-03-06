//$Id: RefreshEventListener.java,v 1.2 2004/08/08 11:24:54 oneovthafew Exp $
package org.hibernate.event;

import org.hibernate.HibernateException;

import java.io.Serializable;

/**
 * Defines the contract for handling of refresh events generated from a session.
 *
 * @author Steve Ebersole
 */
public interface RefreshEventListener extends Serializable {

    /** Handle the given refresh event.
     *
     * @param event The refresh event to be handled.
     * @throws HibernateException
     */
	public void onRefresh(RefreshEvent event) throws HibernateException;

}
