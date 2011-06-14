//$Id: CollectionRemoveAction.java,v 1.6 2005/02/13 11:49:55 oneovthafew Exp $
package org.hibernate.action;

import org.hibernate.HibernateException;
import org.hibernate.cache.CacheException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;

import java.io.Serializable;

public final class CollectionRemoveAction extends CollectionAction {

	private boolean emptySnapshot;

	public CollectionRemoveAction(CollectionPersister persister, Serializable id, boolean emptySnapshot, SessionImplementor session)
			throws CacheException {
		super( persister, id, session );
		this.emptySnapshot = emptySnapshot;
	}

	public void execute() throws HibernateException {
		if ( !emptySnapshot ) getPersister().remove( getKey(), getSession() );
		evict();

		if ( getSession().getFactory().getStatistics().isStatisticsEnabled() ) {
			getSession().getFactory().getStatisticsImplementor()
					.removeCollection( getPersister().getRole() );
		}
	}


}







