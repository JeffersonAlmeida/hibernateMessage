//$Id: MergeEventListener.java,v 1.1 2004/09/04 11:37:51 oneovthafew Exp $
package org.hibernate.event;

import java.io.Serializable;
import java.util.Map;

import org.hibernate.HibernateException;

/**
 * Defines the contract for handling of merge events generated from a session.
 *
 * @author Gavin King
 */
public interface MergeEventListener extends Serializable {

    /** 
     * Handle the given merge event.
     *
     * @param event The merge event to be handled.
     * @throws HibernateException
     */
	public Object onMerge(MergeEvent event) throws HibernateException;

    /** 
     * Handle the given merge event.
     *
     * @param event The merge event to be handled.
     * @throws HibernateException
     */
	public Object onMerge(MergeEvent event, Map copiedAlready) throws HibernateException;

}
