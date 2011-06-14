//$Id: DefaultPreLoadEventListener.java,v 1.2 2005/02/22 03:09:34 oneovthafew Exp $
package org.hibernate.event.def;

import org.hibernate.event.PreLoadEvent;
import org.hibernate.event.PreLoadEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Called before injecting property values into a newly 
 * loaded entity instance.
 *
 * @author Gavin King
 */
public class DefaultPreLoadEventListener extends AbstractEventListener implements PreLoadEventListener {
	public void onPreLoad(PreLoadEvent event) {
		EntityPersister persister = event.getPersister();
		event.getSession()
			.getInterceptor()
			.onLoad( 
					event.getEntity(), 
					event.getId(), 
					event.getState(), 
					persister.getPropertyNames(), 
					persister.getPropertyTypes() 
			);
	}
}
