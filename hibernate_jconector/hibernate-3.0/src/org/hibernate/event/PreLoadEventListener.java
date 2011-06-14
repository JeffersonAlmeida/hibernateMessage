//$Id: PreLoadEventListener.java,v 1.1 2004/12/19 20:15:13 oneovthafew Exp $
package org.hibernate.event;

import java.io.Serializable;

/**
 * Called before injecting property values into a newly 
 * loaded entity instance.
 *
 * @author Gavin King
 */
public interface PreLoadEventListener extends Serializable {
	public void onPreLoad(PreLoadEvent event);
}
