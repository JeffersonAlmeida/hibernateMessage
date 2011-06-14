//$Id: JACCPreLoadEventListener.java,v 1.4 2005/02/13 11:50:09 oneovthafew Exp $
package org.hibernate.secure;

import java.security.AccessController;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.event.PreLoadEvent;
import org.hibernate.event.def.DefaultPreLoadEventListener;

/**
 * @author Gavin King
 */
public class JACCPreLoadEventListener extends DefaultPreLoadEventListener {

	private static final Log log = LogFactory.getLog(JACCPreLoadEventListener.class);

	public void onPreLoad(PreLoadEvent event) {
		HibernatePermission loadPermission = new HibernatePermission( 
				event.getPersister().getEntityName(),
				HibernatePermission.READ
		);
		log.debug( "checking load permission on: " + loadPermission.getName() );
		AccessController.checkPermission(loadPermission);
		super.onPreLoad(event);
	}
}
