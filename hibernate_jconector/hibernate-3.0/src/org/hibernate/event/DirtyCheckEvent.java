//$Id: DirtyCheckEvent.java,v 1.4 2005/02/21 13:15:24 oneovthafew Exp $
package org.hibernate.event;

import org.hibernate.engine.SessionImplementor;

/** Defines an event class for the dirty-checking of a session.
 *
 * @author Steve Ebersole
 */
public class DirtyCheckEvent extends FlushEvent {

	public DirtyCheckEvent(SessionImplementor source) {
		super(source);
	}

}
