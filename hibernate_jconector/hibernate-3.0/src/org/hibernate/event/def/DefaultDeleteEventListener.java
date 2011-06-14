//$Id: DefaultDeleteEventListener.java,v 1.7 2005/03/07 00:40:43 oneovthafew Exp $
package org.hibernate.event.def;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CacheMode;
import org.hibernate.CallbackException;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.TransientObjectException;
import org.hibernate.action.EntityDeleteAction;
import org.hibernate.classic.Lifecycle;
import org.hibernate.engine.Cascades;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.ForeignKeys;
import org.hibernate.engine.Nullability;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.Status;
import org.hibernate.event.DeleteEvent;
import org.hibernate.event.DeleteEventListener;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;


/**
 * Defines the default delete event listener used by hibernate for deleting entities
 * from the datastore in response to generated delete events.
 *
 * @author Steve Ebersole
 */
public class DefaultDeleteEventListener extends AbstractEventListener implements DeleteEventListener {

	private static final Log log = LogFactory.getLog(DefaultDeleteEventListener.class);

    /** Handle the given delete event.
     *
     * @param event The delete event to be handled.
     * @throws HibernateException
     */
	public void onDelete(DeleteEvent event) throws HibernateException {
		final SessionImplementor source = event.getSession();
		
		final PersistenceContext persistenceContext = source.getPersistenceContext();
		Object entity = persistenceContext.unproxyAndReassociate( event.getObject() );
		EntityEntry entityEntry = persistenceContext.getEntry(entity);

		final EntityPersister persister;
		final Serializable id;
		final Object version;
		if ( entityEntry == null ) {
			log.trace( "deleting a transient instance" );

			persister = source.getEntityPersister( event.getEntityName(), entity );
			id = persister.getIdentifier( entity, source.getEntityMode() );

			if ( id == null ) {
				throw new TransientObjectException(
					"the transient instance passed to delete() had a null identifier"
				);
			}

			persistenceContext.checkUniqueness(id, persister, entity);

			new OnUpdateVisitor( source, id ).process( entity, persister );
			
			version = persister.getVersion( entity, source.getEntityMode() );

			entityEntry = persistenceContext.addEntity(
				entity,
				Status.MANAGED,
				persister.getPropertyValues( entity, source.getEntityMode() ),
				id,
				version,
				LockMode.NONE,
				true,
				persister,
				false
			);
		}
		else {
			log.trace( "deleting a persistent instance" );

			if ( entityEntry.getStatus() == Status.DELETED || entityEntry.getStatus() == Status.GONE ) {
				log.trace( "object was already deleted" );
				return;
			}
			persister = entityEntry.getPersister();
			id = entityEntry.getId();
			version = entityEntry.getVersion();
		}

		if ( !persister.isMutable() ) {
			throw new HibernateException(
					"attempted to delete an object of immutable class: " +
					MessageHelper.infoString(persister)
			);
		}

		deleteEntity( source, entity, entityEntry, event.isCascadeDeleteEnabled(), persister );

		if ( source.getFactory().getSettings().isIdentifierRollbackEnabled() ) {
			persister.resetIdentifier( entity, id, version, source.getEntityMode() );
		}
		
	}

	protected final void deleteEntity(
		final SessionImplementor session,
		final Object entity,
		final EntityEntry entityEntry,
		final boolean isCascadeDeleteEnabled,
		final EntityPersister persister)
	throws HibernateException {

		if ( log.isTraceEnabled() ) {
			log.trace(
				"deleting " + MessageHelper.infoString( persister, entityEntry.getId(), session.getFactory() )
			);
		}

		Type[] propTypes = persister.getPropertyTypes();

		final Object version = entityEntry.getVersion();

		final Object[] currentState;
		if ( entityEntry.getLoadedState() == null ) { //ie. the entity came in from update()
			currentState = persister.getPropertyValues( entity, session.getEntityMode() );
		}
		else {
			currentState = entityEntry.getLoadedState();
		}
		final Object[] deletedState = new Object[propTypes.length];
		TypeFactory.deepCopy( 
				currentState, 
				propTypes, 
				persister.getPropertyUpdateability(), 
				deletedState, 
				session
		);
		entityEntry.setDeletedState(deletedState);

		session.getInterceptor().onDelete(
				entity,
				entityEntry.getId(),
				deletedState,
				persister.getPropertyNames(),
				propTypes
		);

		// before any callbacks, etc, so subdeletions see that this deletion happened first
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		persistenceContext.setEntryStatus(entityEntry, Status.DELETED);
		EntityKey key = new EntityKey( entityEntry.getId(), persister, session.getEntityMode()  );

		// todo : there's got to be a better way (more encapsuated way) to implement these
		Set nullifiablesAfterOnDelete = null;
		List suspendedDeletions = null;

		if ( persister.implementsLifecycle( session.getEntityMode() ) ) {
			HashSet oldNullifiables = (HashSet) persistenceContext.getNullifiableEntityKeys().clone();
			ArrayList oldDeletions = session.getActionQueue().cloneDeletions();
			persistenceContext.getNullifiableEntityKeys().add(key); //the deletion of the parent is actually executed BEFORE any deletion from onDelete()
			try {
				log.debug( "calling onDelete()" );
				if ( ( (Lifecycle) entity ).onDelete(session) ) {
					//rollback deletion
					persistenceContext.setEntryStatus(entityEntry, Status.MANAGED);
					entityEntry.setDeletedState(null);
					persistenceContext.setNullifiableEntityKeys(oldNullifiables);
					log.debug("deletion vetoed by onDelete()");
					return; //don't let it cascade
				}
			}
			catch (CallbackException ce) {
				//rollback deletion
				persistenceContext.setEntryStatus(entityEntry, Status.MANAGED);
				entityEntry.setDeletedState(null);
				persistenceContext.setNullifiableEntityKeys(oldNullifiables);
				throw ce;
			}
			//note, the following assumes that onDelete() didn't cause the session
			//to be flushed! TODO: add a better check that it doesn't
			if ( oldDeletions.size() > session.getActionQueue().numberOfDeletions() ) {
				throw new HibernateException("session was flushed during onDelete()");
			}
			suspendedDeletions = session.getActionQueue().suspendNewDeletions( oldDeletions );
			nullifiablesAfterOnDelete = persistenceContext.getNullifiableEntityKeys();
			persistenceContext.setNullifiableEntityKeys(oldNullifiables);
		}

		cascadeBeforeDelete(session, persister, entity, entityEntry);

		new ForeignKeys.Nullifier(entity, true, false, session)
			.nullifyTransientReferences( entityEntry.getDeletedState(), propTypes );
		new Nullability(session).checkNullability( entityEntry.getDeletedState(), persister, true );
		persistenceContext.getNullifiableEntityKeys().add(key);

		// Ensures that containing deletions happen before sub-deletions
		session.getActionQueue().addAction(
			new EntityDeleteAction( 
					entityEntry.getId(), 
					deletedState, 
					version, 
					entity, 
					persister, 
					isCascadeDeleteEnabled, 
					session 
			)
		);
		
		if ( persister.implementsLifecycle( session.getEntityMode() ) ) {
			// after nullify, because we don't want to nullify references to subdeletions
			persistenceContext.getNullifiableEntityKeys().addAll(nullifiablesAfterOnDelete);
			// after deletions.add(), to respect foreign key constraints
			session.getActionQueue().resumeSuspendedDeletions( suspendedDeletions );
		}

		cascadeAfterDelete(session, persister, entity);
		
		// the entry will be removed after the flush, and will no longer
		// override the stale snapshot
		// This is now handled by removeEntity() in EntityDeleteAction
		//persistenceContext.removeDatabaseSnapshot(key);

	}
	
	protected void cascadeBeforeDelete(
	        SessionImplementor session,
	        EntityPersister persister,
	        Object entity,
	        EntityEntry entityEntry) throws HibernateException {

		CacheMode cacheMode = session.getCacheMode();
		session.setCacheMode(CacheMode.GET);
		session.getPersistenceContext().incrementCascadeLevel();
		try {
			// cascade-delete to collections BEFORE the collection owner is deleted
			Cascades.cascade(
					session,
					persister,
					entity,
					Cascades.ACTION_DELETE,
					Cascades.CASCADE_AFTER_INSERT_BEFORE_DELETE
			);
		}
		finally {
			session.getPersistenceContext().decrementCascadeLevel();
			session.setCacheMode(cacheMode);
		}
	}

	protected void cascadeAfterDelete(
	        SessionImplementor session,
	        EntityPersister persister,
	        Object entity) throws HibernateException {

		CacheMode cacheMode = session.getCacheMode();
		session.setCacheMode(CacheMode.GET);
		session.getPersistenceContext().incrementCascadeLevel();
		try {
			// cascade-delete to many-to-one AFTER the parent was deleted
			Cascades.cascade(
					session,
					persister,
					entity,
					Cascades.ACTION_DELETE,
					Cascades.CASCADE_BEFORE_INSERT_AFTER_DELETE
			);
		}
		finally {
			session.getPersistenceContext().decrementCascadeLevel();
			session.setCacheMode(cacheMode);
		}
	}

}
