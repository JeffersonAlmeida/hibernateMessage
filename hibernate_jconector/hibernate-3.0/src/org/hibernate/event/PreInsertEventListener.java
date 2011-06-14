//$Id: PreInsertEventListener.java,v 1.1 2004/12/22 18:11:28 oneovthafew Exp $
package org.hibernate.event;

import java.io.Serializable;

/**
 * @author Gavin King
 */
public interface PreInsertEventListener extends Serializable {
	public boolean onPreInsert(PreInsertEvent event);
}
