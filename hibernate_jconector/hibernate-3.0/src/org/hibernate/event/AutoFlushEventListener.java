//$Id: AutoFlushEventListener.java,v 1.2 2004/08/08 11:24:54 oneovthafew Exp $
package org.hibernate.event;

import org.hibernate.HibernateException;

import java.io.Serializable;

/**
 * Defines the contract for handling of session auto-flush events.
 *
 * @author Steve Ebersole
 */
public interface AutoFlushEventListener extends Serializable {

    /** Handle the given auto-flush event.
     *
     * @param event The auto-flush event to be handled.
     * @throws HibernateException
     */
	public boolean onAutoFlush(AutoFlushEvent event) throws HibernateException;
}
