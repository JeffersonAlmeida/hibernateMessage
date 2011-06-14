//$Id: EntityDeleteAction.java,v 1.19 2005/02/21 13:15:22 oneovthafew Exp $
package org.hibernate.action;

import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.cache.CacheKey;
import org.hibernate.cache.CacheConcurrencyStrategy.SoftLock;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.Status;
import org.hibernate.event.PostDeleteEvent;
import org.hibernate.event.PreDeleteEvent;
import org.hibernate.persister.entity.EntityPersister;

public final class EntityDeleteAction extends EntityAction {

	private final Object version;
	private SoftLock lock;
	private final boolean isCascadeDeleteEnabled;
	private final Object[] state;

	public EntityDeleteAction(final Serializable id,
							  final Object[] state,
							  final Object version,
							  final Object instance,
							  final EntityPersister persister,
							  final boolean isCascadeDeleteEnabled,
							  final SessionImplementor session) {

		super( session, id, instance, persister );
		this.version = version;
		this.isCascadeDeleteEnabled = isCascadeDeleteEnabled;
		this.state = state;
	}

	public void execute() throws HibernateException {
		Serializable id = getId();
		EntityPersister persister = getPersister();
		SessionImplementor session = getSession();
		Object instance = getInstance();

		PreDeleteEvent preEvent = new PreDeleteEvent(instance, id, state, persister, session);
		final boolean veto = session.getListeners().getPreDeleteEventListener().onPreDelete(preEvent);
		
		final CacheKey ck;
		if ( persister.hasCache() ) {
			ck = new CacheKey( id, getPersister().getIdentifierType(), persister.getRootEntityName(), session.getEntityMode() );
			lock = persister.getCache().lock(ck, version);
		}
		else {
			ck = null;
		}

		if ( !isCascadeDeleteEnabled && !veto ) {
			persister.delete( id, version, instance, session );
		}
		
		//postDelete:
		// After actually deleting a row, record the fact that the instance no longer 
		// exists on the database (needed for identity-column key generation), and
		// remove it from the session cache
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		EntityEntry entry = persistenceContext.removeEntry( instance );
		if ( entry == null ) throw new AssertionFailure( "possible nonthreadsafe access to session" );
		persistenceContext.setEntryStatus( entry, Status.GONE );
		entry.setExistsInDatabase( false );

		EntityKey key = new EntityKey( entry.getId(), entry.getPersister(), session.getEntityMode() );
		persistenceContext.removeEntity(key);
		persistenceContext.removeProxy(key);
		
		if ( persister.hasCache() ) persister.getCache().evict(ck);

		PostDeleteEvent postEvent = new PostDeleteEvent( instance, id, state, getPersister(), session );
		session.getListeners().getPostDeleteEventListener().onPostDelete( postEvent );

		if ( getSession().getFactory().getStatistics().isStatisticsEnabled() && !veto ) {
			getSession().getFactory().getStatisticsImplementor()
					.deleteEntity( getPersister().getEntityName() );
		}
	}

	public void afterTransactionCompletion(boolean success) throws HibernateException {
		final CacheKey ck = new CacheKey( 
				getId(), 
				getPersister().getIdentifierType(), 
				getPersister().getRootEntityName(),
				getSession().getEntityMode()
		);
		if ( getPersister().hasCache() ) getPersister().getCache().release(ck, lock);
	}

}







