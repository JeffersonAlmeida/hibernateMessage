//$Id: PostDeleteEventListener.java,v 1.6 2004/12/19 20:56:53 oneovthafew Exp $
package org.hibernate.event;

import java.io.Serializable;

/**
 * @author Gavin King
 */
public interface PostDeleteEventListener extends Serializable {
	public void onPostDelete(PostDeleteEvent event);
}
