//$Id: CollectionAction.java,v 1.13 2005/02/16 12:50:06 oneovthafew Exp $
package org.hibernate.action;

import org.hibernate.cache.CacheConcurrencyStrategy.SoftLock;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CacheKey;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.util.StringHelper;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

abstract class CollectionAction implements Executable, Serializable, Comparable {

	private transient CollectionPersister persister;
	private Serializable key;
	private SessionImplementor session;
	private SoftLock lock;
	private String collectionRole;

	public CollectionAction(CollectionPersister persister, Serializable key, SessionImplementor session)
			throws CacheException {
		this.persister = persister;
		this.session = session;
		this.key = key;
		this.collectionRole = persister.getRole();
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		persister = session.getFactory().getCollectionPersister( collectionRole );
	}

	public void afterTransactionCompletion(boolean success) throws CacheException {
		final CacheKey ck = new CacheKey( key, persister.getKeyType(), persister.getRole(), session.getEntityMode() );
		if ( persister.hasCache() ) persister.getCache().release(ck, lock);
	}

	public boolean hasAfterTransactionCompletion() {
		return persister.hasCache();
	}

	public Serializable[] getPropertySpaces() {
		return persister.getCollectionSpaces();
	}

	protected final CollectionPersister getPersister() {
		return persister;
	}

	protected final Serializable getKey() {
		return key;
	}

	protected final SessionImplementor getSession() {
		return session;
	}

	public final void beforeExecutions() throws CacheException {
		// we need to obtain the lock before any actions are
		// executed, since this may be an inverse="true"
		// bidirectional association and it is one of the
		// earlier entity actions which actually updates
		// the database (this action is resposible for
		// second-level cache invalidation only)
		final CacheKey ck = new CacheKey( key, persister.getKeyType(), persister.getRole(), session.getEntityMode() );
		if ( persister.hasCache() ) lock = persister.getCache().lock(ck, null);
	}

	protected final void evict() throws CacheException {
		CacheKey ck = new CacheKey( key, persister.getKeyType(), persister.getRole(), session.getEntityMode() );
		if ( persister.hasCache() ) persister.getCache().evict(ck);
	}

	public String toString() {
		return StringHelper.unqualify( getClass().getName() ) + MessageHelper.infoString( collectionRole, key );
	}

	public int compareTo(Object other) {
		CollectionAction action = ( CollectionAction ) other;
		//sort first by role name
		int roleComparison = collectionRole.compareTo( action.collectionRole );
		if ( roleComparison != 0 ) {
			return roleComparison;
		}
		else {
			//then by fk
			return persister.getKeyType().compare( key, action.key, session.getEntityMode() );
		}
	}
}






