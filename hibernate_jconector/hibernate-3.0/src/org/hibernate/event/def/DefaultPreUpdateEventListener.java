//$Id: DefaultPreUpdateEventListener.java,v 1.1 2005/02/13 11:50:01 oneovthafew Exp $
package org.hibernate.event.def;

import org.hibernate.event.PreUpdateEvent;
import org.hibernate.event.PreUpdateEventListener;

/**
 * @author Gavin King
 */
public class DefaultPreUpdateEventListener 
	extends AbstractEventListener 
	implements PreUpdateEventListener {

	public boolean onPreUpdate(PreUpdateEvent event) {
		return false;
	}
}
