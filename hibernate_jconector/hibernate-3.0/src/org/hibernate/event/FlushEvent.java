//$Id: FlushEvent.java,v 1.6 2005/02/22 03:09:31 oneovthafew Exp $
package org.hibernate.event;

import org.hibernate.engine.SessionImplementor;

/** 
 * Defines an event class for the flushing of a session.
 *
 * @author Steve Ebersole
 */
public class FlushEvent extends AbstractEvent {
	
	public FlushEvent(SessionImplementor source) {
		super(source);
	}

}
