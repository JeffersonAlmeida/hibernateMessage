//$Id: DefaultPreDeleteEventListener.java,v 1.1 2005/02/13 11:50:01 oneovthafew Exp $
package org.hibernate.event.def;

import org.hibernate.event.PreDeleteEvent;
import org.hibernate.event.PreDeleteEventListener;

/**
 * @author Gavin King
 */
public class DefaultPreDeleteEventListener 
	extends AbstractEventListener 
	implements PreDeleteEventListener {

	public boolean onPreDelete(PreDeleteEvent event) {
		return false;
	}
}
