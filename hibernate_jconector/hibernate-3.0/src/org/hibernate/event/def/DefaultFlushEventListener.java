//$Id: DefaultFlushEventListener.java,v 1.3 2005/02/22 03:09:34 oneovthafew Exp $
package org.hibernate.event.def;

import org.hibernate.HibernateException;
import org.hibernate.event.FlushEvent;
import org.hibernate.event.FlushEventListener;
import org.hibernate.engine.SessionImplementor;

/**
 * Defines the default flush event listeners used by hibernate for 
 * flushing session state in response to generated flush events.
 *
 * @author Steve Ebersole
 */
public class DefaultFlushEventListener extends AbstractFlushingEventListener implements FlushEventListener {

	/** Handle the given flush event.
	 *
	 * @param event The flush event to be handled.
	 * @throws HibernateException
	 */
	public void onFlush(FlushEvent event) throws HibernateException {
		final SessionImplementor source = event.getSession();
		if ( source.getPersistenceContext().hasNonReadOnlyEntities() ) {
			
			flushEverythingToExecutions(event);
			performExecutions(source);
			postFlush(source);
		
			if ( source.getFactory().getStatistics().isStatisticsEnabled() ) {
				source.getFactory().getStatisticsImplementor().flush();
			}
			
		}
	}
}
