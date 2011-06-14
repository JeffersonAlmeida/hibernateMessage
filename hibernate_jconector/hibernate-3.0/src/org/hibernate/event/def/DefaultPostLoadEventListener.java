//$Id: DefaultPostLoadEventListener.java,v 1.2 2005/02/22 03:09:34 oneovthafew Exp $
package org.hibernate.event.def;

import org.hibernate.classic.Lifecycle;
import org.hibernate.event.PostLoadEvent;
import org.hibernate.event.PostLoadEventListener;

/**
 * Default implementation is a noop.
 *
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 */
public class DefaultPostLoadEventListener extends AbstractEventListener implements PostLoadEventListener {
	
	public void onPostLoad(PostLoadEvent event) {
		if ( event.getPersister().implementsLifecycle( event.getSession().getEntityMode() ) ) {
			//log.debug( "calling onLoad()" );
			( ( Lifecycle ) event.getEntity() ).onLoad( event.getSession(), event.getId() );
		}

	}
}
