//$Id: PostLoadEventListener.java,v 1.4 2004/12/19 20:15:13 oneovthafew Exp $
package org.hibernate.event;

import java.io.Serializable;

/**
 * Occurs after an an entity instance is fully loaded.
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 */
public interface PostLoadEventListener extends Serializable {
	public void onPostLoad(PostLoadEvent event);
}
