//$Id: AbstractEvent.java,v 1.3 2005/02/22 03:09:30 oneovthafew Exp $
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.engine.SessionImplementor;


/**
 * Defines a base class for Session generated events.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractEvent implements Serializable {

	private final SessionImplementor session;

    /**
     * Constructs an event from the given event session.
     *
     * @param source The session event source.
     */
	public AbstractEvent(SessionImplementor source) {
		this.session = source;
	}

    /**
     * Returns the session event source for this event.  This is the underlying
     * session from which this event was generated.
     *
     * @return The session event source.
     */
	public final SessionImplementor getSession() {
		return session;
	}

}
