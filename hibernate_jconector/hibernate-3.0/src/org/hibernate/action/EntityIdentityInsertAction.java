//$Id: EntityIdentityInsertAction.java,v 1.11 2005/02/21 13:15:22 oneovthafew Exp $
package org.hibernate.action;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.event.PostInsertEvent;
import org.hibernate.event.PreInsertEvent;
import org.hibernate.persister.entity.EntityPersister;

public final class EntityIdentityInsertAction extends EntityAction {
	private final Object[] state;
	//private CacheEntry cacheEntry;
	private Serializable generatedId;

	public EntityIdentityInsertAction(Object[] state, Object instance, EntityPersister persister, SessionImplementor session)
			throws HibernateException {
		super( session, null, instance, persister );
		this.state = state;
	}

	public void execute() throws HibernateException {
		EntityPersister persister = getPersister();
		SessionImplementor session = getSession();
		Object instance = getInstance();

		PreInsertEvent preEvent = new PreInsertEvent( instance, null, state, persister, session );
		final boolean veto = session.getListeners().getPreInsertEventListener().onPreInsert( preEvent );

		// Don't need to lock the cache here, since if someone
		// else inserted the same pk first, the insert would fail

		if ( !veto ) generatedId = persister.insert( state, instance, session );

		//TODO: this bit actually has to be called after all cascades!
		//      but since identity insert is called *synchronously*,
		//      instead of asynchronously as other actions, it isn't
		/*if ( persister.hasCache() && !persister.isCacheInvalidationRequired() ) {
			cacheEntry = new CacheEntry(object, persister, session);
			persister.getCache().insert(generatedId, cacheEntry);
		}*/
		
		PostInsertEvent postEvent = new PostInsertEvent( instance, generatedId, state, getPersister(), session );
		session.getListeners().getPostInsertEventListener().onPostInsert( postEvent );

		if ( getSession().getFactory().getStatistics().isStatisticsEnabled() && !veto ) {
			getSession().getFactory().getStatisticsImplementor()
					.insertEntity( getPersister().getEntityName() );
		}

	}

	//Make 100% certain that this is called before any subsequent ScheduledUpdate.afterTransactionCompletion()!!
	public void afterTransactionCompletion(boolean success) throws HibernateException {
		//TODO: reenable if we also fix the above todo
		/*EntityPersister persister = getEntityPersister();
		if ( success && persister.hasCache() && !persister.isCacheInvalidationRequired() ) {
			persister.getCache().afterInsert( getGeneratedId(), cacheEntry );
		}*/
	}

	public boolean hasAfterTransactionCompletion() {
		//TODO: simply remove this override
		//      if we fix the above todos
		return false;
	}

	public final Serializable getGeneratedId() {
		return generatedId;
	}

}







