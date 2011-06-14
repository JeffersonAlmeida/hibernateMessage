//$Id: EntityUpdateAction.java,v 1.22 2005/03/16 04:45:18 oneovthafew Exp $
package org.hibernate.action;

import java.io.Serializable;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CacheKey;
import org.hibernate.cache.CacheConcurrencyStrategy.SoftLock;
import org.hibernate.cache.entry.CacheEntry;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.event.PostUpdateEvent;
import org.hibernate.event.PreUpdateEvent;
import org.hibernate.persister.entity.EntityPersister;

public final class EntityUpdateAction extends EntityAction {

	private final Object[] fields;
	private final Object[] oldFields;
	private final Object lastVersion;
	private final Object nextVersion;
	private final int[] dirtyFields;
	private final boolean hasDirtyCollection;
	private final Object[] updatedState;
	private final Object rowId;
	private Object cacheEntry;
	private SoftLock lock;

	public EntityUpdateAction(final Serializable id,
							  final Object[] fields,
							  final int[] dirtyProperties,
							  final boolean hasDirtyCollection,
							  final Object[] oldFields,
							  final Object lastVersion,
							  final Object nextVersion,
							  final Object instance,
							  final Object[] updatedState,
							  final Object rowId,
							  final EntityPersister persister,
							  final SessionImplementor session) throws HibernateException {
		super( session, id, instance, persister );
		this.fields = fields;
		this.oldFields = oldFields;
		this.lastVersion = lastVersion;
		this.nextVersion = nextVersion;
		this.dirtyFields = dirtyProperties;
		this.hasDirtyCollection = hasDirtyCollection;
		this.updatedState = updatedState;
		this.rowId = rowId;
	}

	public void execute() throws HibernateException {
		Serializable id = getId();
		EntityPersister persister = getPersister();
		SessionImplementor session = getSession();
		Object instance = getInstance();

		PreUpdateEvent preEvent = new PreUpdateEvent( instance, id, fields, oldFields, persister, session );
		final boolean veto = session.getListeners().getPreUpdateEventListener().onPreUpdate( preEvent );

		final SessionFactoryImplementor factory = getSession().getFactory();
		
		final CacheKey ck;
		if ( persister.hasCache() ) {
			ck = new CacheKey( id, persister.getIdentifierType(), persister.getRootEntityName(), session.getEntityMode() );
			lock = persister.getCache().lock(ck, lastVersion);
		}
		else {
			ck = null;
		}

		if ( !veto ) persister.update( id, fields, dirtyFields, hasDirtyCollection, oldFields, lastVersion, instance, rowId, session );
		
		//postUpdate:
		// After actually updating a row, record the fact that 
		// the database state has been updated
		EntityEntry entry = getSession().getPersistenceContext().getEntry( instance );
		if ( entry == null ) throw new AssertionFailure( "possible nonthreadsafe access to session" );
		entry.postUpdate( instance, updatedState, nextVersion );

		if ( persister.hasCache() ) {
			if ( persister.isCacheInvalidationRequired() ) {
				persister.getCache().evict(ck);
			}
			else {
				//TODO: inefficient if that cache is just going to ignore the updated state!
				CacheEntry ce = new CacheEntry(
						fields, 
						persister, 
						persister.hasUninitializedLazyProperties( instance, session.getEntityMode() ), 
						getSession(),
						instance
				);
				cacheEntry = persister.getCacheEntryStructure().structure(ce);
				boolean put = persister.getCache().update(ck, cacheEntry);
				
				if ( put && factory.getStatistics().isStatisticsEnabled() ) {
					factory.getStatisticsImplementor()
							.secondLevelCachePut( getPersister().getCache().getRegionName() );
				}

			}
		}

		PostUpdateEvent postEvent = new PostUpdateEvent( instance, id, fields, oldFields, persister, session );
		session.getListeners().getPostUpdateEventListener().onPostUpdate( postEvent );

		if ( factory.getStatistics().isStatisticsEnabled() && !veto ) {
			factory.getStatisticsImplementor()
					.updateEntity( getPersister().getEntityName() );
		}
	}

	public void afterTransactionCompletion(boolean success) throws CacheException {
		EntityPersister persister = getPersister();
		if ( persister.hasCache() ) {
			
			final CacheKey ck = new CacheKey( 
					getId(), 
					persister.getIdentifierType(), 
					persister.getRootEntityName(), 
					getSession().getEntityMode() 
			);
			
			if ( success && !persister.isCacheInvalidationRequired() ) {
				boolean put = persister.getCache().afterUpdate(ck, cacheEntry, nextVersion, lock );
				
				if ( put && getSession().getFactory().getStatistics().isStatisticsEnabled() ) {
					getSession().getFactory().getStatisticsImplementor()
							.secondLevelCachePut( getPersister().getCache().getRegionName() );
				}
			}
			else {
				persister.getCache().release(ck, lock );
			}
		}
	}

}







