//$Id: DefaultPreInsertEventListener.java,v 1.1 2005/02/13 11:50:01 oneovthafew Exp $
package org.hibernate.event.def;

import org.hibernate.event.PreInsertEvent;
import org.hibernate.event.PreInsertEventListener;

/**
 * @author Gavin King
 */
public class DefaultPreInsertEventListener 
	extends AbstractEventListener 
	implements PreInsertEventListener {

	public boolean onPreInsert(PreInsertEvent event) {
		return false;
	}
}
