//$Id: CollectionUpdateAction.java,v 1.6 2005/02/13 11:49:55 oneovthafew Exp $
package org.hibernate.action;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.cache.CacheException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;

import java.io.Serializable;

public final class CollectionUpdateAction extends CollectionAction {

	private final PersistentCollection collection;
	private final boolean emptySnapshot;

	public CollectionUpdateAction(PersistentCollection collection,
								  CollectionPersister persister,
								  Serializable id,
								  boolean emptySnapshot,
								  SessionImplementor session)
			throws CacheException {
		super( persister, id, session );
		this.collection = collection;
		this.emptySnapshot = emptySnapshot;
	}

	public void execute() throws HibernateException {
		Serializable id = getKey();
		SessionImplementor session = getSession();
		CollectionPersister persister = getPersister();

		if ( !collection.wasInitialized() ) {
			if ( !collection.hasQueuedAdditions() ) throw new AssertionFailure( "no queued adds" );
			//do nothing - we only need to notify the cache...
		}
		else if ( collection.empty() ) {
			if ( !emptySnapshot ) persister.remove( id, session );
		}
		else if ( collection.needsRecreate( getPersister() ) ) {
			if ( !emptySnapshot ) persister.remove( id, session );
			persister.recreate( collection, id, session );
		}
		else {
			persister.deleteRows( collection, id, session );
			persister.updateRows( collection, id, session );
			persister.insertRows( collection, id, session );
		}

		evict();

		if ( getSession().getFactory().getStatistics().isStatisticsEnabled() ) {
			getSession().getFactory().getStatisticsImplementor().
					updateCollection( getPersister().getRole() );
		}
	}

}







