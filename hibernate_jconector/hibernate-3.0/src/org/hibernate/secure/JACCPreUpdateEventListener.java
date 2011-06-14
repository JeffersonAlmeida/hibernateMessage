//$Id: JACCPreUpdateEventListener.java,v 1.4 2005/02/13 11:50:09 oneovthafew Exp $
package org.hibernate.secure;

import java.security.AccessController;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.event.PreUpdateEvent;
import org.hibernate.event.def.DefaultPreUpdateEventListener;

/**
 * @author Gavin King
 */
public class JACCPreUpdateEventListener extends DefaultPreUpdateEventListener {

	private static final Log log = LogFactory.getLog(JACCPreUpdateEventListener.class);

	public boolean onPreUpdate(PreUpdateEvent event) {
		HibernatePermission updatePermission = new HibernatePermission( 
				event.getPersister().getEntityName(),
				HibernatePermission.UPDATE
		);
		log.debug( "checking update permission on: " + updatePermission.getName() );
		AccessController.checkPermission(updatePermission);
		return super.onPreUpdate(event);
	}
}
