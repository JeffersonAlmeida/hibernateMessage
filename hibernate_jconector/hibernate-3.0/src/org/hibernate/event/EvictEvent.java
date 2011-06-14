//$Id: EvictEvent.java,v 1.5 2005/02/22 03:09:31 oneovthafew Exp $
package org.hibernate.event;

import org.hibernate.engine.SessionImplementor;

/**
 *  Defines an event class for the evicting of an entity.
 *
 * @author Steve Ebersole
 */
public class EvictEvent extends AbstractEvent {

	private Object object;

	public EvictEvent(Object object, SessionImplementor source) {
		super(source);
		this.object = object;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}
}
