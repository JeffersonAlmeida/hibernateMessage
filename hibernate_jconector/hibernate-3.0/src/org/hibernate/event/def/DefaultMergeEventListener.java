//$Id: DefaultMergeEventListener.java,v 1.4 2005/02/22 03:09:34 oneovthafew Exp $
package org.hibernate.event.def;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.ObjectDeletedException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.WrongClassException;
import org.hibernate.engine.Cascades;
import org.hibernate.engine.Cascades.CascadingAction;
import org.hibernate.event.MergeEvent;
import org.hibernate.event.MergeEventListener;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.type.TypeFactory;
import org.hibernate.util.IdentityMap;

/**
 * Defines the default copy event listener used by hibernate for copying entities
 * in response to generated copy events.
 *
 * @author Gavin King
 */
public class DefaultMergeEventListener extends AbstractSaveEventListener 
	implements MergeEventListener {

	private static final Log log = LogFactory.getLog(DefaultMergeEventListener.class);
	
	private final boolean isSaveOrUpdateCopyListener;
	
	public DefaultMergeEventListener() {
		this(false);
	}

	public DefaultMergeEventListener(boolean isSaveOrUpdateCopyListener) {
		this.isSaveOrUpdateCopyListener = isSaveOrUpdateCopyListener;
	}

	/** 
	 * Handle the given merge event.
	 *
	 * @param event The merge event to be handled.
	 * @return The result (i.e., the copied entity).
	 * @throws HibernateException
	 */
	public Object onMerge(MergeEvent event) throws HibernateException {
		return onMerge( event, IdentityMap.instantiate(10) );
	}

	/** 
	 * Handle the given merge event.
	 *
	 * @param event The merge event to be handled.
	 * @return The result (i.e., the copied entity).
	 * @throws HibernateException
	 */
	public Object onMerge(MergeEvent event, Map copyCache) throws HibernateException {

		final SessionImplementor source = event.getSession();
		final Object original = event.getOriginal();

		final Object entity;
		if ( original == null ) {
			//EARLY EXIT!
			return null;
		}
		else if ( original instanceof HibernateProxy ) {
			LazyInitializer li = ( (HibernateProxy) original ).getHibernateLazyInitializer();
			if ( li.isUninitialized() ) {
				//EARLY EXIT!
				log.trace("ignoring uninitialized proxy");
				return source.load( li.getEntityName(), li.getIdentifier() );
			}
			else {
				entity = li.getImplementation();
			}
		}
		else {
			entity = original;
		}
		
		if ( copyCache.containsKey(entity) ) return entity; //EARLY EXIT!

		event.setEntity(entity);

		int entityState = getEntityState( 
				entity, 
				event.getEntityName(), 
				source.getPersistenceContext().getEntry(entity),
				source 
		);
		
		switch (entityState) {
			case DETACHED:
				return entityIsDetached(event, copyCache);
			case TRANSIENT:
				return entityIsTransient(event, copyCache);
			case PERSISTENT:
				return entityIsPersistent(event, copyCache);
			default: //DELETED
				throw new ObjectDeletedException( "deleted instance passed to merge", null, event.getEntityName() );			
		}
		
	}
	
	protected Object entityIsPersistent(MergeEvent event, Map copyCache) {
		log.trace("ignoring persistent instance");
		
		//TODO: check that entry.getIdentifier().equals(requestedId)
		
		final Object entity = event.getEntity();
		final EntityPersister persister = event.getSession()
			.getEntityPersister( event.getEntityName(), entity );
		
		copyCache.put(entity, entity); //before cascade!
		
		cascadeOnMerge(event, persister, entity, copyCache);
		
		return entity;
	}
	
	protected Object entityIsTransient(MergeEvent event, Map copyCache) {
		
		log.trace("merging transient instance");
		
		final Object entity = event.getEntity();
		final SessionImplementor source = event.getSession();

		final EntityPersister persister = source.getEntityPersister( event.getEntityName(), entity );
		final String entityName = persister.getEntityName();
		
		final Serializable id = persister.hasIdentifierProperty() ?
				persister.getIdentifier( entity, source.getEntityMode() ) :
		        null;
		
		final Object copy = persister.instantiate( id, source.getEntityMode() );  // todo : should this be Session.instantiate(Persister, ...)?
		copyCache.put(entity, copy); //before cascade!
		
		// cascade first, so that all unsaved objects get their 
		// copy created before we actually copy
		cascadeOnMerge(event, persister, entity, copyCache);
		copyValues(persister, entity, copy, source, copyCache);
		
		//this bit is only *really* absolutely necessary for handling 
		//requestedId, but is also good if we merge multiple object 
		//graphs, since it helps ensure uniqueness
		final Serializable requestedId = event.getRequestedId();
		if (requestedId==null) {
			saveWithGeneratedId(copy, entityName, copyCache, source);
		}
		else {
			saveWithRequestedId(copy, requestedId, entityName, copyCache, source);
		}
		
		return copy;

	}

	protected Object entityIsDetached(MergeEvent event, Map copyCache) {
		
		log.trace("merging detached instance");
		
		final Object entity = event.getEntity();
		final SessionImplementor source = event.getSession();

		final EntityPersister persister = source.getEntityPersister( event.getEntityName(), entity );
		final String entityName = persister.getEntityName();
			
		Serializable id = event.getRequestedId();
		if ( id == null ) {
			id = persister.getIdentifier( entity, source.getEntityMode() );
		}
		else {
			//TODO: check that entity id = requestedId
		}

		final Object result = source.get(entityName, id);
		if ( result == null ) {
			//TODO: we should throw an exception if we really *know* for sure  
			//      that this is a detached instance, rather than just assuming
			//throw new StaleObjectStateException(entityName, id);
			
			// we got here because we assumed that an instance
			// with an assigned id was detached, when it was
			// really persistent
			return entityIsTransient(event, copyCache);
		}
		else {
			copyCache.put(entity, result); //before cascade!
	
			final Object target = source.getPersistenceContext().unproxy(result);
			if ( target == entity ) {
				throw new AssertionFailure("entity was not detached");
			}
			else if ( !source.getEntityName(target).equals(entityName) ) {
				throw new WrongClassException(
					"class of the given object did not match class of persistent copy",
					event.getRequestedId(),
					entityName
				);
			}
			else if (
				persister.isVersioned() &&
				!persister.getVersionType().isSame( 
						persister.getVersion( target, source.getEntityMode() ), 
						persister.getVersion( entity, source.getEntityMode() ), 
						source.getEntityMode() 
				)
			) {
				throw new StaleObjectStateException( entityName, event.getRequestedId() );
			}
	
			// cascade first, so that all unsaved objects get their 
			// copy created before we actually copy
			cascadeOnMerge(event, persister, entity, copyCache);
			copyValues(persister, entity, target, source, copyCache);
			
			return result;
		}

	}
	
	protected void copyValues(
		final EntityPersister persister, 
		final Object entity, 
		final Object target, 
		final SessionImplementor source,
		final Map copyCache
	) {
		
		final Object[] copiedValues = TypeFactory.replace(
				persister.getPropertyValues( entity, source.getEntityMode() ),
				persister.getPropertyValues( target, source.getEntityMode() ),
				persister.getPropertyTypes(),
				source,
				target, 
				copyCache
		);

		persister.setPropertyValues( target, copiedValues, source.getEntityMode() );
	}

	/** Perform any cascades needed as part of this copy event.
	 *
	 * @param event The merge event being processed.
	 * @param persister The persister of the entity being copied.
	 * @param entity The entity being copied.
	 * @param copyCache A cache of already copied instance.
	 */
	protected void cascadeOnMerge(
		final MergeEvent event,
		final EntityPersister persister,
		final Object entity,
		final Map copyCache
	) {
		Cascades.cascade(
				event.getSession(),
				persister,
				entity,
				getCascadeAction(),
				Cascades.CASCADE_BEFORE_MERGE,
				copyCache
		);
	}


	protected CascadingAction getCascadeAction() {
		return isSaveOrUpdateCopyListener ? 
				Cascades.ACTION_SAVE_UPDATE_COPY :
				Cascades.ACTION_MERGE;
	}

	protected Boolean getAssumedUnsaved() {
		return Boolean.FALSE;
	}

}
