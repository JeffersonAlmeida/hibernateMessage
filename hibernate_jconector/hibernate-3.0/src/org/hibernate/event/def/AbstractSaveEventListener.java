//$Id: AbstractSaveEventListener.java,v 1.5 2005/03/04 10:19:52 oneovthafew Exp $
package org.hibernate.event.def;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.NonUniqueObjectException;
import org.hibernate.action.EntityIdentityInsertAction;
import org.hibernate.action.EntityInsertAction;
import org.hibernate.classic.Lifecycle;
import org.hibernate.classic.Validatable;
import org.hibernate.engine.Cascades;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.ForeignKeys;
import org.hibernate.engine.Nullability;
import org.hibernate.engine.Status;
import org.hibernate.engine.Versioning;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.id.IdentifierGeneratorFactory;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;

/**
 * A convenience bas class for listeners responding to save events.
 *
 * @author Steve Ebersole.
 */
public abstract class AbstractSaveEventListener extends AbstractReassociateEventListener {

	protected static final int PERSISTENT = 0;
	protected static final int TRANSIENT = 1;
	protected static final int DETACHED = 2;
	protected static final int DELETED = 3;

	private static final Log log = LogFactory.getLog(AbstractSaveEventListener.class);

	/**
	 * Prepares the save call using the given requested id.
	 * @param entity The entity to be saved.
	 * @param requestedId The id to which to associate the entity.
	 * @param source The session which is the source of this save event.
	 * @return The id used to save the entity.
	 * @throws HibernateException
	 */
	protected Serializable saveWithRequestedId(
			Object entity,
			Serializable requestedId,
			String entityName,
			Object anything,
			SessionImplementor source) 
	throws HibernateException {
		return performSave(
				entity,
				requestedId,
				source.getEntityPersister(entityName, entity),
				false,
				anything,
				source
		);
	}

	/**
	 * Prepares the save call using a newly generated id.
	 * @param entity The entity to be saved
	 * @param source The session which is the source of this save event.
	 * @return The id used to save the entity
	 * @throws HibernateException
	 */
	protected Serializable saveWithGeneratedId(
			Object entity,
			String entityName,
			Object anything,
			SessionImplementor source) 
	throws HibernateException {
		
		EntityPersister persister = source.getEntityPersister(entityName, entity);

		Serializable generatedId = persister.getIdentifierGenerator()
			.generate( source, entity );
		
		if ( log.isDebugEnabled() ) {
			log.debug(
					"generated identifier: " + 
					persister.getIdentifierType().toLoggableString(generatedId, source.getFactory()) + 
					", using strategy: " + 
					persister.getIdentifierGenerator().getClass().getName() //TODO: define toString()s for generators
			);
		}

		if ( generatedId == null ) {
			throw new IdentifierGenerationException( "null id generated for:" + entity.getClass() );
		}
		else if ( generatedId == IdentifierGeneratorFactory.SHORT_CIRCUIT_INDICATOR ) {
			return source.getIdentifier( entity );
		}
		else if ( generatedId == IdentifierGeneratorFactory.POST_INSERT_INDICATOR ) {
			return performSave(entity, null, persister, true, anything, source);
		}
		else {
			return performSave(entity, generatedId, persister, false, anything, source);
		}
	}

	/**
	 * Ppepares the save call by checking the session caches for a pre-existing
	 * entity and performing any lifecycle callbacks.
	 * @param entity The entity to be saved.
	 * @param id The id by which to save the entity.
	 * @param persister The entity's persister instance.
	 * @param useIdentityColumn Is an identity column in use?
	 * @param source The session from which the event originated.
	 * @return The id used to save the entity.
	 * @throws HibernateException
	 */
	protected Serializable performSave(
			Object entity,
			Serializable id,
			EntityPersister persister,
			boolean useIdentityColumn,
			Object anything,
			SessionImplementor source) 
	throws HibernateException {

		if ( log.isTraceEnabled() ) {
			log.trace( "saving " + MessageHelper.infoString(persister, id, source.getFactory()) );
		}

		if ( !useIdentityColumn ) {
			Object old = source.getPersistenceContext().getEntity( new EntityKey( id, persister, source.getEntityMode() ) );
			if (old != null) {
				if ( source.getPersistenceContext().getEntry(old).getStatus() == Status.DELETED ) {
					source.forceFlush( source.getPersistenceContext().getEntry(old) );
				}
				else {
					throw new NonUniqueObjectException( id, persister.getEntityName() );
				}
			}
			persister.setIdentifier(entity, id, source.getEntityMode());
		}

		// Sub-insertions should occur before containing insertion so
		// Try to do the callback now
		if ( persister.implementsLifecycle( source.getEntityMode() ) ) {
			log.debug( "calling onSave()" );
			if ( ( (Lifecycle) entity ).onSave(source) ) {
				log.debug( "insertion vetoed by onSave()" );
				return id;
			}
		}

		return performSaveOrReplicate(
				entity, 
				id, 
				persister, 
				useIdentityColumn, 
				anything, 
				source
		);
	}

	/**
	 * Performs all the actual work needed to save an entity (well to get the save moved to
	 * the execution queue).
	 * @param entity The entity to be saved
	 * @param id The id to be used for saving the entity (or null, in the case of identity columns)
	 * @param persister The entity's persister instance.
	 * @param useIdentityColumn Should an identity column be used for id generation?
	 * @param source The session which is the source of the current event.
	 * @return The id used to save the entity.
	 * @throws HibernateException
	 */
	protected Serializable performSaveOrReplicate(
			Object entity,
			Serializable id,
			EntityPersister persister,
			boolean useIdentityColumn,
			Object anything,
			SessionImplementor source) 
	throws HibernateException {

		if ( persister.implementsValidatable( source.getEntityMode() ) ) {
            ( ( Validatable ) entity ).validate();
        }

		if (useIdentityColumn) {
			log.trace("executing insertions");
			source.getActionQueue().executeInserts();
		}

		// Put a placeholder in entries, so we don't recurse back and try to save() the
		// same object again. QUESTION: should this be done before onSave() is called?
		// likewise, should it be done before onUpdate()?
		source.getPersistenceContext().addEntry(
				entity,
				Status.SAVING,
				null,
				null,
				id,
				null,
				LockMode.WRITE,
				useIdentityColumn,
				persister, false
		);

		cascadeBeforeSave(source, persister, entity, anything);

		Object[] values = persister.getPropertyValuesToInsert(entity, source);
		Type[] types = persister.getPropertyTypes();

		boolean substitute = substituteValuesIfNecessary(entity, id, values, persister, source);

		if ( persister.hasCollections() ) {
			substitute = substitute || visitCollections(id, values, types, source);
		}

		if (substitute) persister.setPropertyValues( entity, values, source.getEntityMode() );

		TypeFactory.deepCopy( 
				values, 
				types, 
				persister.getPropertyUpdateability(), 
				values, 
				source
		);
		new ForeignKeys.Nullifier(entity, false, useIdentityColumn, source)
			.nullifyTransientReferences(values, types);
		new Nullability(source).checkNullability( values, persister, false );

		if (useIdentityColumn) {
			EntityIdentityInsertAction insert = new EntityIdentityInsertAction(values, entity, persister, source);
			source.getActionQueue().execute(insert);
			id = insert.getGeneratedId();
			persister.setIdentifier( entity, id, source.getEntityMode() );
			source.getPersistenceContext().checkUniqueness(id, persister, entity);
			//source.getBatcher().executeBatch(); //found another way to ensure that all batched joined inserts have been executed
		}

		Object version = Versioning.getVersion(values, persister);
		source.getPersistenceContext().addEntity(
				entity,
				Status.MANAGED,
				values,
				id,
				version,
				LockMode.WRITE,
				useIdentityColumn,
				persister,
				isVersionIncrementDisabled()
		);
		source.getPersistenceContext().removeNonExist( new EntityKey( id, persister, source.getEntityMode() ) );

		if ( !useIdentityColumn ) {
			source.getActionQueue().addAction( new EntityInsertAction(id, values, entity, version, persister, source) );
		}

		cascadeAfterSave(source, persister, entity, anything);

		return id;
	}
	
	/**
	 * After the save, will te version number be incremented
	 * if the instance is modified?
	 */
	protected boolean isVersionIncrementDisabled() {
		return false;
	}
	
	protected boolean visitCollections(Serializable id, Object[] values, Type[] types, SessionImplementor source) {
		WrapVisitor visitor = new WrapVisitor(source);
		// substitutes into values by side-effect
		visitor.processEntityPropertyValues(values, types);
		return visitor.isSubstitutionRequired();
	}
	
	/**
	 * Perform any property value substitution that is necessary
	 * (interceptor callback, version initialization...)
	 */
	protected boolean substituteValuesIfNecessary(
			Object entity, 
			Serializable id, 
			Object[] values, 
			EntityPersister persister,
			SessionImplementor source
	) {
		boolean substitute = source.getInterceptor().onSave(
				entity,
				id,
				values,
				persister.getPropertyNames(),
				persister.getPropertyTypes()
		);

		//keep the existing version number in the case of replicate!
		if ( persister.isVersioned() ) {
			substitute = Versioning.seedVersion(
					values, 
					persister.getVersionProperty(), 
					persister.getVersionType()
			) || substitute;
		}
		return substitute;
	}

	/**
	 * Handles the calls needed to perform pre-save cascades for the given entity.
	 * @param source The session from whcih the save event originated.
	 * @param persister The entity's persister instance.
	 * @param entity The entity to be saved.
	 * @throws HibernateException
	 */
	protected void cascadeBeforeSave(
			SessionImplementor source,
			EntityPersister persister,
			Object entity,
			Object anything)
	throws HibernateException {

		// cascade-save to many-to-one BEFORE the parent is saved
		source.getPersistenceContext().incrementCascadeLevel();
		try {
			Cascades.cascade(
					source,
					persister,
					entity,
					getCascadeAction(),
					Cascades.CASCADE_BEFORE_INSERT_AFTER_DELETE,
					anything
			);
		}
		finally {
			source.getPersistenceContext().decrementCascadeLevel();
		}
	}

	/**
	 * Handles to calls needed to perform post-save cascades.
	 * @param source The session from which the event originated.
	 * @param persister The entity's persister instance.
	 * @param entity The entity beng saved.
	 * @throws HibernateException
	 */
	protected void cascadeAfterSave(
	        SessionImplementor source,
	        EntityPersister persister,
	        Object entity,
			Object anything)
	throws HibernateException {

		// cascade-save to collections AFTER the collection owner was saved
		source.getPersistenceContext().incrementCascadeLevel();
		try {
			Cascades.cascade(
					source,
					persister,
					entity,
					getCascadeAction(),
					Cascades.CASCADE_AFTER_INSERT_BEFORE_DELETE,
					anything
			);
		}
		finally {
			source.getPersistenceContext().decrementCascadeLevel();
		}
	}
	
	protected abstract Cascades.CascadingAction getCascadeAction();
	
	/**
	 * Determine whether the entity is persistent, detached, or transient
	 */
	protected int getEntityState(
			Object entity, 
			String entityName, 
			EntityEntry entry, //pass this as an argument only to avoid double looking
			SessionImplementor source
	) {
				
		if ( entry!=null ) { // the object is persistent
			
			//the entity is associated with the session, so check its status
			if ( entry.getStatus() != Status.DELETED ) {
				// do nothing for persistent instances
				if ( log.isTraceEnabled() ) log.trace( "persistent instance of: " + getLoggableName(entityName, entity) );
				return PERSISTENT;
			}
			else { 
				//ie. e.status==DELETED
				if ( log.isTraceEnabled() ) log.trace( "deleted instance of: " + getLoggableName(entityName, entity) );
				return DELETED;
			}
			
		}
		else { // the object is transient or detached
			
			//the entity is not associated with the session, so
			//try interceptor and unsaved-value
			
			if ( ForeignKeys.isTransient( entityName, entity, getAssumedUnsaved(), source ) ) {
				if ( log.isTraceEnabled() ) log.trace( "transient instance of: " + getLoggableName(entityName, entity) );
				return TRANSIENT;
			}
			else {
				if ( log.isTraceEnabled() ) log.trace( "detached instance of: " + getLoggableName(entityName, entity) );
				return DETACHED;
			}

		}
	}
	
	private String getLoggableName(String entityName, Object entity) {
		return entityName==null ? entity.getClass().getName() : entityName;
	}
	
	protected Boolean getAssumedUnsaved() {
		return null;
	}

}
