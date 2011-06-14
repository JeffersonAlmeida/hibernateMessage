//$Id: CollectionInitializer.java,v 1.1 2005/02/13 11:50:06 oneovthafew Exp $
package org.hibernate.loader.collection;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;

/**
 * An interface for collection loaders
 * @see CollectionLoader
 * @see OneToManyLoader
 * @author Gavin King
 */
public interface CollectionInitializer {
	/**
	 * Initialize the given collection
	 */
	public void initialize(Serializable id, SessionImplementor session) throws HibernateException;
}






