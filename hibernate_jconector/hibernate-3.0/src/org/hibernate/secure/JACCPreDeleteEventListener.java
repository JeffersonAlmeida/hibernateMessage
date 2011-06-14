//$Id: JACCPreDeleteEventListener.java,v 1.4 2005/02/13 11:50:09 oneovthafew Exp $
package org.hibernate.secure;

import java.security.AccessController;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.event.PreDeleteEvent;
import org.hibernate.event.def.DefaultPreDeleteEventListener;

/**
 * @author Gavin King
 */
public class JACCPreDeleteEventListener extends DefaultPreDeleteEventListener {
	
	private static final Log log = LogFactory.getLog(JACCPreDeleteEventListener.class);

	public boolean onPreDelete(PreDeleteEvent event) {
		HibernatePermission deletePermission = new HibernatePermission( 
				event.getPersister().getEntityName(),
				HibernatePermission.DELETE
		);
		log.debug( "checking delete permission on: " + deletePermission.getName() );
		AccessController.checkPermission(deletePermission);
		return super.onPreDelete(event);
	}
}
