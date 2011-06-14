//$Id: PreUpdateEventListener.java,v 1.1 2004/12/22 18:11:28 oneovthafew Exp $
package org.hibernate.event;

import java.io.Serializable;

/**
 * @author Gavin King
 */
public interface PreUpdateEventListener extends Serializable {
	public boolean onPreUpdate(PreUpdateEvent event);
}
