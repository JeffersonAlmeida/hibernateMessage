//$Id: RefreshEvent.java,v 1.5 2005/02/22 03:09:32 oneovthafew Exp $
package org.hibernate.event;

import org.hibernate.LockMode;
import org.hibernate.engine.SessionImplementor;

/**
 *  Defines an event class for the refreshing of an object.
 *
 * @author Steve Ebersole
 */
public class RefreshEvent extends AbstractEvent {

	private Object object;
	private LockMode lockMode = LockMode.READ;

	public RefreshEvent(Object object, SessionImplementor source) {
		super(source);
		if (object == null) {
			throw new IllegalArgumentException("Attempt to generate refresh event with null object");
		}
		this.object = object;
	}

	public RefreshEvent(Object object, LockMode lockMode, SessionImplementor source) {
		this(object, source);
		if (lockMode == null) {
			throw new IllegalArgumentException("Attempt to generate refresh event with null lock mode");
		}
		this.lockMode = lockMode;
	}

	public Object getObject() {
		return object;
	}

	public LockMode getLockMode() {
		return lockMode;
	}
}
