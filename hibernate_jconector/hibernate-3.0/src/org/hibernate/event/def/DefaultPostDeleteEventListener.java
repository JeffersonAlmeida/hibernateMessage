//$Id: DefaultPostDeleteEventListener.java,v 1.1 2005/02/13 11:50:01 oneovthafew Exp $
package org.hibernate.event.def;

import org.hibernate.event.PostDeleteEvent;
import org.hibernate.event.PostDeleteEventListener;

/**
 * Default implementation is a noop.
 * 
 * @author Gavin King
 */
public class DefaultPostDeleteEventListener extends AbstractEventListener implements PostDeleteEventListener {

	public void onPostDelete(PostDeleteEvent event) {}

}
