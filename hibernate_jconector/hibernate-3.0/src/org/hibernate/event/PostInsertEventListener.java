//$Id: PostInsertEventListener.java,v 1.1 2004/12/19 20:56:53 oneovthafew Exp $
package org.hibernate.event;

import java.io.Serializable;

/**
 * @author Gavin King
 */
public interface PostInsertEventListener extends Serializable {
	public void onPostInsert(PostInsertEvent event);
}
