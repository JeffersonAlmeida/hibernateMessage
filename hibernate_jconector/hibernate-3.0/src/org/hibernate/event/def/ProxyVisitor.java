//$Id: ProxyVisitor.java,v 1.3 2005/02/21 13:15:25 oneovthafew Exp $
package org.hibernate.event.def;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.CollectionSnapshot;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.EntityType;

/**
 * Reassociates uninitialized proxies with the session
 * @author Gavin King
 */
public abstract class ProxyVisitor extends AbstractVisitor {


	public ProxyVisitor(SessionImplementor session) {
		super(session);
	}

	Object processEntity(Object value, EntityType entityType) throws HibernateException {

		if (value!=null) {
			getSession().getPersistenceContext().reassociateIfUninitializedProxy(value);
			// if it is an initialized proxy, let cascade
			// handle it later on
		}

		return null;
	}

	/**
	 * Has the owner of the collection changed since the collection
	 * was snapshotted and detached?
	 */
	protected static boolean isOwnerUnchanged(
			CollectionSnapshot snapshot, 
			CollectionPersister persister, 
			Serializable id
	) {
		return isCollectionSnapshotValid(snapshot) &&
				persister.getRole().equals( snapshot.getRole() ) &&
				id.equals( snapshot.getKey() );
	}

	private static boolean isCollectionSnapshotValid(CollectionSnapshot snapshot) {
		return snapshot != null &&
				snapshot.getRole() != null &&
				snapshot.getKey() != null;
	}
	
	/**
	 * Reattach a detached (disassociated) initialized or uninitialized
	 * collection wrapper, using a snapshot carried with the collection
	 * wrapper
	 */
	protected void reattachCollection(PersistentCollection collection, CollectionSnapshot snapshot)
	throws HibernateException {
		if ( collection.wasInitialized() ) {
			getSession().getPersistenceContext()
				.addInitializedDetachedCollection( collection, snapshot, getSession().getEntityMode() );
		}
		else {
			if ( !isCollectionSnapshotValid(snapshot) ) {
				throw new HibernateException( "could not reassociate uninitialized transient collection" );
			}
			getSession().getPersistenceContext().addUninitializedDetachedCollection(
					collection,
					getSession().getFactory().getCollectionPersister( snapshot.getRole() ),
					snapshot.getKey(),
					getSession().getEntityMode()
			);
		}
	}

}
