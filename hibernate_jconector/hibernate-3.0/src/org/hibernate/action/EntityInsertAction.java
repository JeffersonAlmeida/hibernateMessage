//$Id: EntityInsertAction.java,v 1.21 2005/03/16 04:45:17 oneovthafew Exp $
package org.hibernate.action;

import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.cache.CacheKey;
import org.hibernate.cache.entry.CacheEntry;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.event.PostInsertEvent;
import org.hibernate.event.PreInsertEvent;
import org.hibernate.persister.entity.EntityPersister;

public final class EntityInsertAction extends EntityAction {
	private final Object[] state;
	private Object cacheEntry;
	private final Object version;

	public EntityInsertAction(Serializable id,
							  Object[] state,
							  Object instance,
							  Object version,
							  EntityPersister persister,
							  SessionImplementor session)
			throws HibernateException {
		super( session, id, instance, persister );
		this.state = state;
		this.version = version;
	}

	public void execute() throws HibernateException {
		EntityPersister persister = getPersister();
		SessionImplementor session = getSession();
		Object instance = getInstance();
		Serializable id = getId();

		PreInsertEvent preEvent = new PreInsertEvent( instance, id, state, persister, session );
		final boolean veto = session.getListeners().getPreInsertEventListener().onPreInsert( preEvent );

		// Don't need to lock the cache here, since if someone
		// else inserted the same pk first, the insert would fail

		if ( !veto ) persister.insert( id, state, instance, session );
		
		//postInsert:
		// After actually inserting a row, record the fact that the instance exists on the 
		// database (needed for identity-column key generation)
		EntityEntry entry = session.getPersistenceContext().getEntry( instance );
		if ( entry == null ) throw new AssertionFailure( "possible nonthreadsafe access to session" );
		entry.setExistsInDatabase( true );

		final SessionFactoryImplementor factory = getSession().getFactory();

		if ( persister.hasCache() && !persister.isCacheInvalidationRequired() ) {
			CacheEntry ce = new CacheEntry(
					state, 
					persister, 
					persister.hasUninitializedLazyProperties( instance, session.getEntityMode() ),
					session,
					instance
			);
			
			cacheEntry = persister.getCacheEntryStructure().structure(ce);
			final CacheKey ck = new CacheKey( 
					id, 
					persister.getIdentifierType(), 
					persister.getRootEntityName(), 
					session.getEntityMode() 
			);
			boolean put = persister.getCache().insert(ck, cacheEntry);
			
			if ( put && factory.getStatistics().isStatisticsEnabled() ) {
				factory.getStatisticsImplementor()
						.secondLevelCachePut( getPersister().getCache().getRegionName() );
			}
		}

		PostInsertEvent postEvent = new PostInsertEvent( instance, id, state, persister, session );
		session.getListeners().getPostInsertEventListener().onPostInsert( postEvent );

		if ( factory.getStatistics().isStatisticsEnabled() && !veto ) {
			factory.getStatisticsImplementor()
					.insertEntity( getPersister().getEntityName() );
		}

	}

	//Make 100% certain that this is called before any subsequent ScheduledUpdate.afterTransactionCompletion()!!
	public void afterTransactionCompletion(boolean success) throws HibernateException {
		EntityPersister persister = getPersister();
		if ( success && persister.hasCache() && !persister.isCacheInvalidationRequired() ) {
			final CacheKey ck = new CacheKey( 
					getId(), 
					persister.getIdentifierType(), 
					persister.getRootEntityName(), 
					getSession().getEntityMode() 
			);
			boolean put = persister.getCache().afterInsert(ck, cacheEntry, version );
			
			if ( put && getSession().getFactory().getStatistics().isStatisticsEnabled() ) {
				getSession().getFactory().getStatisticsImplementor()
						.secondLevelCachePut( getPersister().getCache().getRegionName() );
			}
		}
	}

}







