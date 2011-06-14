//$Id: DefaultAutoFlushEventListener.java,v 1.3 2005/02/22 03:09:34 oneovthafew Exp $
package org.hibernate.event.def;

import org.hibernate.HibernateException;
import org.hibernate.FlushMode;
import org.hibernate.event.AutoFlushEvent;
import org.hibernate.event.AutoFlushEventListener;
import org.hibernate.engine.SessionImplementor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Defines the default flush event listeners used by hibernate for 
 * flushing session state in response to generated auto-flush events.
 *
 * @author Steve Ebersole
 */
public class DefaultAutoFlushEventListener extends AbstractFlushingEventListener implements AutoFlushEventListener {

	private static final Log log = LogFactory.getLog(DefaultAutoFlushEventListener.class);

    /** Handle the given auto-flush event.
     *
     * @param event The auto-flush event to be handled.
     * @throws HibernateException
     */
	public boolean onAutoFlush(AutoFlushEvent event) throws HibernateException {

		final SessionImplementor source = event.getSession();
		
		final boolean flushMightBeNeeded = !source.getFlushMode().lessThan(FlushMode.AUTO) && 
			source.getDontFlushFromFind() == 0 &&
			source.getPersistenceContext().hasNonReadOnlyEntities();
		
		if (flushMightBeNeeded) {

			final int oldSize = source.getActionQueue().numberOfCollectionRemovals();

			flushEverythingToExecutions(event);
			
			final boolean flushIsReallyNeeded = source.getActionQueue().areTablesToBeUpdated( event.getQuerySpaces() ) || 
				source.getFlushMode()==FlushMode.ALWAYS;

			if (flushIsReallyNeeded) {

				log.trace("Need to execute flush");

				performExecutions(source);
				postFlush(source);
				// note: performExecutions() clears all collectionXxxxtion 
				// collections (the collection actions) in the session

				if ( source.getFactory().getStatistics().isStatisticsEnabled() ) {
					source.getFactory().getStatisticsImplementor().flush();
				}
				
			}
			else {

				log.trace("Dont need to execute flush");
				source.getActionQueue().clearFromFlushNeededCheck( oldSize );
			}
			
			return flushIsReallyNeeded;

		}

		return false;

	}

}
