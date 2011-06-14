//$Id: DefaultFlushEntityEventListener.java,v 1.7 2005/03/10 17:52:31 oneovthafew Exp $
package org.hibernate.event.def;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.EntityMode;
import org.hibernate.action.EntityUpdateAction;
import org.hibernate.classic.Validatable;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.Nullability;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.Status;
import org.hibernate.engine.Versioning;
import org.hibernate.event.FlushEntityEvent;
import org.hibernate.event.FlushEntityEventListener;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;
import org.hibernate.util.ArrayHelper;

/**
 * An event that occurs for each entity instance at flush time
 * 
 * @author Gavin King
 */
public class DefaultFlushEntityEventListener
	extends AbstractEventListener
	implements FlushEntityEventListener {
	
	private static final Log log = LogFactory.getLog(DefaultFlushEntityEventListener.class);

	/**
	 * make sure user didn't mangle the id
	 */
	public void checkId(Object object, EntityPersister persister, Serializable id, EntityMode entityMode)
	throws HibernateException {

		if ( persister.hasIdentifierPropertyOrEmbeddedCompositeIdentifier() ) {

			Serializable oid = persister.getIdentifier( object, entityMode );
			if (id==null) throw new AssertionFailure("null id in entry (don't flush the Session after an exception occurs)");
			if ( !persister.getIdentifierType().isEqual(id, oid, entityMode) ) {
				throw new HibernateException(
						"identifier of an instance of " +
						persister.getEntityName() +
						" altered from " + id +
						" to " + oid
				);
			}
		}

	}

	/**
	 * Flushes a single entity's state to the database, by scheduling
	 * an update action, if necessary
	 */
	public void onFlushEntity(FlushEntityEvent event) throws HibernateException {
		Object entity = event.getEntity();
		EntityEntry entry = event.getEntityEntry();
		SessionImplementor session = event.getSession();
		EntityPersister persister = entry.getPersister();
		Status status = entry.getStatus();

		Object[] values;
		if ( status == Status.DELETED ) {
			//grab its state saved at deletion
			values = entry.getDeletedState();
		}
		else if ( status==Status.READ_ONLY ) {
			values = entry.getLoadedState();
		}
		else {
			checkId( entity, persister, entry.getId(), session.getEntityMode() );
			// grab its current state
			values = persister.getPropertyValues( entity, session.getEntityMode() );
		}
		Type[] types = persister.getPropertyTypes();
		
		event.setPropertyValues(values);

		boolean substitute = false;

		if ( persister.hasCollections() ) {

			// wrap up any new collections directly referenced by the object
			// or its components

			// NOTE: we need to do the wrap here even if its not "dirty",
			// because collections need wrapping but changes to _them_
			// don't dirty the container. Also, for versioned data, we
			// need to wrap before calling searchForDirtyCollections

			WrapVisitor visitor = new WrapVisitor(session);
			// substitutes into values by side-effect
			visitor.processEntityPropertyValues(values, types);
			substitute = visitor.isSubstitutionRequired();
		}

		if ( status!=Status.READ_ONLY && persister.isMutable() ) dirtyCheck(event);

		// compare to cached state (ignoring collections unless versioned)
		if ( isUpdateNecessary(event) ) {

			if ( log.isTraceEnabled() ) {
				if ( status == Status.DELETED ) {
					log.trace(
							"Updating deleted entity: " +
							MessageHelper.infoString( persister, entry.getId(), session.getFactory() )
					);
				}
				else {
					log.trace(
							"Updating entity: " +
							MessageHelper.infoString( persister, entry.getId(), session.getFactory()  )
					);
				}
			}
			
			if ( !entry.isBeingReplicated() ) {
				//if the properties were modified by the Interceptor, we need to set them back to the object
				substitute = substitute || invokeInterceptor(event);
			}

			// validate() instances of Validatable
			if ( status == Status.MANAGED && persister.implementsValidatable( session.getEntityMode() ) ) {
				( (Validatable) entity ).validate();
			}

			// increment the version number (if necessary)
			final Object nextVersion = getNextVersion(event);

			// get the updated snapshot by cloning current state
			Object[] updatedState = null;
			if ( status==Status.MANAGED ) {
				updatedState = new Object[values.length];
				TypeFactory.deepCopy( 
						values, 
						types, 
						persister.getPropertyUpdateability(), 
						updatedState, 
						session 
				);
			}

			// if it was dirtied by a collection only
			int[] dirtyProperties = event.getDirtyProperties();
			if ( event.isDirtyCheckPossible() && dirtyProperties==null ) {
				if ( !event.hasDirtyCollection() ) throw new AssertionFailure("dirty, but no dirty properties");
				dirtyProperties = ArrayHelper.EMPTY_INT_ARRAY;
			}

			// check nullability but do not perform command execute
			// we'll use scheduled updates for that.
			new Nullability(session).checkNullability( values, persister, true );

			// schedule the update
			// note that we intentionally do _not_ pass in currentPersistentState!
			session.getActionQueue().addAction(
					new EntityUpdateAction(
							entry.getId(),
							values,
							dirtyProperties,
							event.hasDirtyCollection(),
							entry.getLoadedState(),
							entry.getVersion(),
							nextVersion,
							entity,
							updatedState,
							entry.getRowId(),
							persister,
							session
					)
			);

		}

		if ( status == Status.DELETED ) {
			//entry.status = GONE;
		}
		else {

			// now update the object .. has to be outside the main if block above (because of collections)
			if (substitute) persister.setPropertyValues( entity, values, session.getEntityMode() );

			// Search for collections by reachability, updating their role.
			// We don't want to touch collections reachable from a deleted object
			if ( persister.hasCollections() ) {
				new FlushVisitor(session, entity).processEntityPropertyValues(values, types);
			}
		}

	}
	
	private boolean invokeInterceptor(FlushEntityEvent event) {
		SessionImplementor session = event.getSession();
		EntityEntry entry = event.getEntityEntry();
		EntityPersister persister = entry.getPersister();
		Object entity = event.getEntity();
		
		//give the Interceptor a chance to modify property values
		final Object[] values = event.getPropertyValues();
		final boolean intercepted = session.getInterceptor().onFlushDirty(
				entity, 
				entry.getId(), 
				values, 
				entry.getLoadedState(), 
				persister.getPropertyNames(), 
				persister.getPropertyTypes()
		);
		
		//now we might need to recalculate the dirtyProperties array
		if ( intercepted && event.isDirtyCheckPossible() && !event.isDirtyCheckHandledByInterceptor() ) {
			int[] dirtyProperties;
			if ( event.hasDatabaseSnapshot() ) {
				dirtyProperties = persister.findModified( event.getDatabaseSnapshot(), values, entity, session );
			}
			else {
				dirtyProperties = persister.findDirty( values, entry.getLoadedState(), entity, session );
			}
			event.setDirtyProperties(dirtyProperties);
		}
		
		return intercepted;
	}

	/**
	 * Convience method to retreive an entities next version value
	 */
	private Object getNextVersion(FlushEntityEvent event) throws HibernateException {
		
		EntityEntry entry = event.getEntityEntry();
		EntityPersister persister = entry.getPersister();
		if ( persister.isVersioned() ) {

			Object[] values = event.getPropertyValues();
		    
			if ( entry.isBeingReplicated() ) {
				return Versioning.getVersion(values, persister);
			}
			else {
				int[] dirtyProperties = event.getDirtyProperties();
				
				final boolean isVersionIncrementRequired = entry.getStatus()!=Status.DELETED && ( 
						dirtyProperties==null || 
						Versioning.isVersionIncrementRequired( 
								dirtyProperties, 
								event.hasDirtyCollection(),
								persister.getPropertyVersionability()
						) 
				);
				
				final Object nextVersion = isVersionIncrementRequired ?
						Versioning.increment( entry.getVersion(), persister.getVersionType() ) :
						entry.getVersion(); //use the current version
						
				Versioning.setVersion(values, nextVersion, persister);
				
				return nextVersion;
			}
		}
		else {
			return null;
		}
		
	}

	/**
	 * Performs all necessary checking to determine if an entity needs an SQL update
	 * to synchronize its state to the database. Modifies the event by side-effect!
	 * Note: this method is quite slow, avoid calling if possible!
	 */
	protected final boolean isUpdateNecessary(FlushEntityEvent event) throws HibernateException {

		EntityPersister persister = event.getEntityEntry().getPersister();
		Status status = event.getEntityEntry().getStatus();
		
		if ( status==Status.READ_ONLY || !persister.isMutable() ) return false;
		if ( !event.isDirtyCheckPossible() ) return true;
		int[] dirtyProperties = event.getDirtyProperties();
		if ( dirtyProperties!=null && dirtyProperties.length!=0 ) return true; //TODO: suck into event class

		if ( 
				status==Status.MANAGED && 
				persister.isVersioned() && 
				persister.hasCollections() 
		) {
			DirtyCollectionSearchVisitor visitor = new DirtyCollectionSearchVisitor( 
					event.getSession(),
					persister.getPropertyVersionability()
			);
			visitor.processEntityPropertyValues( event.getPropertyValues(), persister.getPropertyTypes() );
			boolean hasDirtyCollections = visitor.wasDirtyCollectionFound();
			event.setHasDirtyCollection(hasDirtyCollections);
			return hasDirtyCollections;
		}
		else {
			return false;
		}
	}
	
	/**
	 * Perform a dirty check, and attach the results to the event
	 */
	protected void dirtyCheck(FlushEntityEvent event) throws HibernateException {
		
		Object entity = event.getEntity();
		Object[] values = event.getPropertyValues();
		SessionImplementor session = event.getSession();
		EntityEntry entry = event.getEntityEntry();
		EntityPersister persister = entry.getPersister();
		Serializable id = entry.getId();

		int[] dirtyProperties = session.getInterceptor().findDirty( 
				entity, 
				id, 
				values, 
				entry.getLoadedState(), 
				persister.getPropertyNames(), 
				persister.getPropertyTypes() 
		);
		
		event.setDatabaseSnapshot(null);

		final boolean interceptorHandledDirtyCheck;
		boolean cannotDirtyCheck;
		
		if ( dirtyProperties==null ) {
			// Interceptor returned null, so do the dirtycheck ourself, if possible
			interceptorHandledDirtyCheck = false;
			
			cannotDirtyCheck = entry.getLoadedState()==null; // object loaded by update()
			if ( !cannotDirtyCheck ) {
				// dirty check against the usual snapshot of the entity
				dirtyProperties = persister.findDirty( values, entry.getLoadedState(), entity, session );
				
			}
			else {
				// dirty check against the database snapshot, if possible/necessary
				final PersistenceContext persistenceContext = session.getPersistenceContext();
				Object[] databaseSnapshot = persister.isSelectBeforeUpdateRequired() ?
						persistenceContext.getDatabaseSnapshot(id, persister) :
						//TODO: optimize away this lookup for entities w/o unsaved-value="undefined"
						persistenceContext.getCachedDatabaseSnapshot( new EntityKey( id, persister, session.getEntityMode() ) ); 
						
				if ( databaseSnapshot != null ) {
					dirtyProperties = persister.findModified(databaseSnapshot, values, entity, session);
					cannotDirtyCheck = false;
					event.setDatabaseSnapshot(databaseSnapshot);
				}
				//do we even really need this? the update will fail anyway....
				else if ( persister.isSelectBeforeUpdateRequired() ) {
					throw new StaleObjectStateException( persister.getEntityName(), id );
				}
	
			}
		}
		else {
			// the Interceptor handled the dirty checking
			cannotDirtyCheck = false;
			interceptorHandledDirtyCheck = true;
		}
		
		event.setDirtyProperties(dirtyProperties);
		event.setDirtyCheckHandledByInterceptor(interceptorHandledDirtyCheck);
		event.setDirtyCheckPossible(!cannotDirtyCheck);
		
	}

}
