//$Id: PreDeleteEventListener.java,v 1.1 2004/12/22 18:11:28 oneovthafew Exp $
package org.hibernate.event;

import java.io.Serializable;

/**
 * @author Gavin King
 */
public interface PreDeleteEventListener extends Serializable {
	public boolean onPreDelete(PreDeleteEvent event);
}
