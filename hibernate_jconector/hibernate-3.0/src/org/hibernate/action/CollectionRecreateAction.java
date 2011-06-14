//$Id: CollectionRecreateAction.java,v 1.6 2005/02/13 11:49:55 oneovthafew Exp $
package org.hibernate.action;

import org.hibernate.HibernateException;
import org.hibernate.cache.CacheException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;

import java.io.Serializable;

public final class CollectionRecreateAction extends CollectionAction {

	private final PersistentCollection collection;

	public CollectionRecreateAction(PersistentCollection collection, CollectionPersister persister, Serializable id, SessionImplementor session)
			throws CacheException {
		super( persister, id, session );
		this.collection = collection;
	}

	public void execute() throws HibernateException {
		getPersister().recreate( collection, getKey(), getSession() );
		evict();

		if ( getSession().getFactory().getStatistics().isStatisticsEnabled() ) {
			getSession().getFactory().getStatisticsImplementor()
					.recreateCollection( getPersister().getRole() );
		}
	}

}







