//$Id: DefaultReplicateEventListener.java,v 1.5 2005/03/04 10:19:53 oneovthafew Exp $
package org.hibernate.event.def;

import org.hibernate.HibernateException;
import org.hibernate.TransientObjectException;
import org.hibernate.ReplicationMode;
import org.hibernate.LockMode;
import org.hibernate.engine.Cascades;
import org.hibernate.engine.Status;
import org.hibernate.engine.Cascades.CascadingAction;
import org.hibernate.event.ReplicateEvent;
import org.hibernate.event.ReplicateEventListener;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.type.Type;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Defines the default replicate event listener used by Hibernate to replicate
 * entities in response to generated replicate events.
 *
 * @author Steve Ebersole
 */
public class DefaultReplicateEventListener extends AbstractSaveEventListener implements ReplicateEventListener {

	private static final Log log = LogFactory.getLog(DefaultReplicateEventListener.class);

	/** 
	 * Handle the given replicate event.
	 *
	 * @param event The replicate event to be handled.
	 * @throws HibernateException
	 */
	public void onReplicate(ReplicateEvent event) throws HibernateException {

		final SessionImplementor source = event.getSession();
		
		if ( source.getPersistenceContext().reassociateIfUninitializedProxy( event.getObject() ) ) {
			log.trace("uninitialized proxy passed to replicate()");
			return;
		}

		Object entity = source.getPersistenceContext().unproxyAndReassociate( event.getObject() );

		if ( source.getPersistenceContext().isEntryFor(entity) ) {
			log.trace("ignoring persistent instance passed to replicate()");
			//hum ... should we cascade anyway? throw an exception? fine like it is?
			return;
		}

		EntityPersister persister = source.getEntityPersister( event.getEntityName(), entity );
		
		// get the id from the object
		/*if ( persister.isUnsaved(entity, source) ) {
			throw new TransientObjectException("transient instance passed to replicate()");
		}*/
		Serializable id = persister.getIdentifier( entity, source.getEntityMode() );
		if (id==null) {
			throw new TransientObjectException("transient instance passed to replicate()");
		}

		final ReplicationMode replicationMode = event.getReplicationMode();
		
		final Object oldVersion;
		if ( replicationMode == ReplicationMode.EXCEPTION ) {
			//always do an INSERT, and let it fail by constraint violation
			oldVersion = null;
		}
		else {
			//what is the version on the database?
			oldVersion = persister.getCurrentVersion(id, source);
		}

		if ( oldVersion!=null ) {
			//existing row - do an update if appropriate
			if ( log.isTraceEnabled() ) {
				log.trace( "found existing row for " + MessageHelper.infoString( persister, id, source.getFactory() ) );
			}

			boolean canReplicate = replicationMode.shouldOverwriteCurrentVersion(
					entity,
					oldVersion,
					persister.getVersion( entity, source.getEntityMode() ),
					persister.getVersionType()
			);

			if (canReplicate) {
				//will result in a SQL UPDATE:
				performReplication(entity, id, oldVersion, persister, replicationMode, source);
			}
			else {
				//else do nothing (don't even reassociate object!)
				log.trace("no need to replicate");
			}
			
			//TODO: would it be better to do a refresh from db?
		}
		else {
			// no existing row - do an insert
			if ( log.isTraceEnabled() ) {
				log.trace( "no existing row, replicating new instance " + MessageHelper.infoString( persister, id, source.getFactory() ) );
			}

			final boolean regenerate = persister.isIdentifierAssignedByInsert(); // prefer re-generation of identity!

			performSaveOrReplicate(
					entity,
					regenerate ? null : id,
					persister,
					regenerate,
					replicationMode,
					source
			);

		}
	}

	protected boolean visitCollections(Serializable id, Object[] values, Type[] types, SessionImplementor source) {
		//TODO: make OnReplicateVisitor extend WrapVisitor
		OnReplicateVisitor visitor = new OnReplicateVisitor(source, id);
		visitor.processEntityPropertyValues(values, types);
		return super.visitCollections(id, values, types, source);
	}

	protected boolean substituteValuesIfNecessary(
			Object entity, 
			Serializable id, 
			Object[] values, 
			EntityPersister persister,
			SessionImplementor source
	) {
		return false;
	}
	
	protected boolean isVersionIncrementDisabled() {
		return true;
	}
	
	private final void performReplication(
			Object entity,
			Serializable id,
			Object version,
			EntityPersister persister,
			ReplicationMode replicationMode,
			SessionImplementor source) 
	throws HibernateException {

		if ( log.isTraceEnabled() ) {
			log.trace( "replicating changes to " + MessageHelper.infoString( persister, id, source.getFactory() ) );
		}

		new OnReplicateVisitor(source, id).process( entity, persister );

		source.getPersistenceContext().addEntity( entity, Status.MANAGED, null, id, version, LockMode.NONE, true, persister, true );

		source.getPersistenceContext().incrementCascadeLevel();
		try {
			Cascades.cascade(
					source,
					persister,
					entity,
					Cascades.ACTION_REPLICATE,
					Cascades.CASCADE_AFTER_UPDATE,
					replicationMode
			);
		}
		finally {
			source.getPersistenceContext().decrementCascadeLevel();
		}
	}

	protected CascadingAction getCascadeAction() {
		return Cascades.ACTION_REPLICATE;
	}
}
