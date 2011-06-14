//$Id: DefaultDirtyCheckEventListener.java,v 1.2 2005/02/22 03:09:34 oneovthafew Exp $
package org.hibernate.event.def;

import org.hibernate.HibernateException;
import org.hibernate.event.DirtyCheckEvent;
import org.hibernate.event.DirtyCheckEventListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Defines the default dirty-check event listener used by hibernate for
 * checking the session for dirtiness in response to generated dirty-check
 * events.
 *
 * @author Steve Ebersole
 */
public class DefaultDirtyCheckEventListener extends AbstractFlushingEventListener implements DirtyCheckEventListener {

	private static final Log log = LogFactory.getLog(DefaultDirtyCheckEventListener.class);

    /** Handle the given dirty-check event.
     *
     * @param event The dirty-check event to be handled.
     * @throws HibernateException
     */
	public boolean onDirtyCheck(DirtyCheckEvent event) throws HibernateException {

		boolean wasNeeded = false;
		int oldSize = event.getSession().getActionQueue().numberOfCollectionRemovals();

		try {
			flushEverythingToExecutions(event);
			wasNeeded = event.getSession().getActionQueue().hasAnyQueuedActions();
			log.debug( wasNeeded ? "session dirty" : "session not dirty" );
			return wasNeeded;
		}
		finally {
			event.getSession().getActionQueue().clearFromFlushNeededCheck( oldSize );
		}
	}
}
