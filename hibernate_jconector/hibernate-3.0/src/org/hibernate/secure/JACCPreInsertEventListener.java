//$Id: JACCPreInsertEventListener.java,v 1.4 2005/02/13 11:50:09 oneovthafew Exp $
package org.hibernate.secure;

import java.security.AccessController;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.event.PreInsertEvent;
import org.hibernate.event.def.DefaultPreInsertEventListener;

/**
 * @author Gavin King
 */
public class JACCPreInsertEventListener extends DefaultPreInsertEventListener {

	private static final Log log = LogFactory.getLog(JACCPreInsertEventListener.class);

	public boolean onPreInsert(PreInsertEvent event) {
		HibernatePermission insertPermission = new HibernatePermission( 
				event.getPersister().getEntityName(),
				HibernatePermission.INSERT
		);
		log.debug( "checking insert permission on: " + insertPermission.getName() );
		AccessController.checkPermission(insertPermission);
		return super.onPreInsert(event);
	}
}
