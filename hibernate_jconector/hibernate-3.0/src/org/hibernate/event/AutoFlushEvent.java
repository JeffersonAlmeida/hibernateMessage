///$Id: AutoFlushEvent.java,v 1.4 2005/02/21 13:15:24 oneovthafew Exp $
package org.hibernate.event;

import java.util.Set;

import org.hibernate.engine.SessionImplementor;


/** Defines an event class for the auto-flushing of a session.
 *
 * @author Steve Ebersole
 */
public class AutoFlushEvent extends FlushEvent {

	private Set querySpaces;

	public AutoFlushEvent(Set querySpaces, SessionImplementor source) {
		super(source);
		this.querySpaces = querySpaces;
	}

	public Set getQuerySpaces() {
		return querySpaces;
	}

	public void setQuerySpaces(Set querySpaces) {
		this.querySpaces = querySpaces;
	}
}
