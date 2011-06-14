//$Id: AbstractLockUpgradeEventListener.java,v 1.3 2005/02/21 13:15:25 oneovthafew Exp $
package org.hibernate.event.def;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ObjectDeletedException;
import org.hibernate.cache.CacheConcurrencyStrategy;
import org.hibernate.cache.CacheKey;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.Status;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.pretty.MessageHelper;

/**
 * A convenience base class for listeners that respond to requests to perform a
 * pessimistic lock upgrade on an entity.
 *
 * @author Gavin King
 */
public class AbstractLockUpgradeEventListener extends AbstractReassociateEventListener {

	private static final Log log = LogFactory.getLog(AbstractLockUpgradeEventListener.class);

	/**
	 * Performs a pessimistic lock upgrade on a given entity, if needed.
	 *
	 * @param object The entity for which to upgrade the lock.
	 * @param entry The entity's EntityEntry instance.
	 * @param requestedLockMode The lock mode being requested for locking.
	 * @param source The session which is the source of the event being processed.
	 * @throws HibernateException
	 */
	protected void upgradeLock(Object object, EntityEntry entry, LockMode requestedLockMode, SessionImplementor source)
	throws HibernateException {

		if ( requestedLockMode.greaterThan( entry.getLockMode() ) ) {

			if ( entry.getStatus() != Status.MANAGED ) {
				throw new ObjectDeletedException(
						"attempted to lock a deleted instance",
						entry.getId(),
						entry.getPersister().getEntityName()
				);
			}

			final EntityPersister persister = entry.getPersister();

			if ( log.isTraceEnabled() )
				log.trace(
						"locking " +
						MessageHelper.infoString( persister, entry.getId(), source.getFactory() ) +
						" in mode: " +
						requestedLockMode
				);

			final CacheConcurrencyStrategy.SoftLock lock;
			final CacheKey ck;
			if ( persister.hasCache() ) {
				ck = new CacheKey( 
						entry.getId(), 
						persister.getIdentifierType(), 
						persister.getRootEntityName(), 
						source.getEntityMode() 
				);
				lock = persister.getCache().lock( ck, entry.getVersion() );
			}
			else {
				ck = null;
				lock = null;
			}
			
			try {
				persister.lock( entry.getId(), entry.getVersion(), object, requestedLockMode, source );
				entry.setLockMode(requestedLockMode);
			}
			finally {
				// the database now holds a lock + the object is flushed from the cache,
				// so release the soft lock
				if ( persister.hasCache() ) {
					persister.getCache().release(ck, lock );
				}
			}

		}
	}

}
