//$Id: DefaultPostInsertEventListener.java,v 1.1 2005/02/13 11:50:01 oneovthafew Exp $
package org.hibernate.event.def;

import org.hibernate.event.PostInsertEvent;
import org.hibernate.event.PostInsertEventListener;

/**
 * Default implementation is a noop.
 * 
 * @author Gavin King
 */
public class DefaultPostInsertEventListener extends AbstractEventListener implements PostInsertEventListener {

	public void onPostInsert(PostInsertEvent event) {}

}
