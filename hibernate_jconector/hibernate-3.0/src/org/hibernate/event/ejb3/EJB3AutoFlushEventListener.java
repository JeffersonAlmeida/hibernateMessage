//$Id: EJB3AutoFlushEventListener.java,v 1.2 2005/02/21 14:39:02 oneovthafew Exp $
package org.hibernate.event.ejb3;

import org.hibernate.engine.Cascades;
import org.hibernate.event.def.DefaultAutoFlushEventListener;
import org.hibernate.util.IdentityMap;

/**
 * In EJB3, it is the create operation that is cascaded to unmanaged
 * ebtities at flush time (instead of the save-update operation in 
 * Hibernate).
 * 
 * @author Gavin King
 */
public class EJB3AutoFlushEventListener extends DefaultAutoFlushEventListener {

	protected Cascades.CascadingAction getCascadingAction() {
		return Cascades.ACTION_PERSIST;
	}

	protected Object getAnything() { return IdentityMap.instantiate(10); }

}
