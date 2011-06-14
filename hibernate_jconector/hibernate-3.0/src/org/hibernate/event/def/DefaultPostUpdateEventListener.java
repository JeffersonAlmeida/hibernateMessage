//$Id: DefaultPostUpdateEventListener.java,v 1.1 2005/02/13 11:50:01 oneovthafew Exp $
package org.hibernate.event.def;

import org.hibernate.event.PostUpdateEvent;
import org.hibernate.event.PostUpdateEventListener;

/**
 * Default implementation is a noop.
 * 
 * @author Gavin King
 */
public class DefaultPostUpdateEventListener extends AbstractEventListener implements PostUpdateEventListener {

	public void onPostUpdate(PostUpdateEvent event) {}

}
