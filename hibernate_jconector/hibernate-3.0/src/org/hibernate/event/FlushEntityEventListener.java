//$Id: FlushEntityEventListener.java,v 1.1 2004/08/28 08:38:24 oneovthafew Exp $
package org.hibernate.event;

import org.hibernate.HibernateException;

/**
 * @author Gavin King
 */
public interface FlushEntityEventListener {
	public void onFlushEntity(FlushEntityEvent event) throws HibernateException;
}
