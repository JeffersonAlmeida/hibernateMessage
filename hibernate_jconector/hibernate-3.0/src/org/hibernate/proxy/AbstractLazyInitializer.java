//$Id: AbstractLazyInitializer.java,v 1.9 2005/02/16 12:50:18 oneovthafew Exp $
package org.hibernate.proxy;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.LazyInitializationException;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.SessionImplementor;

/**
 * @author Gavin King
 */
public abstract class AbstractLazyInitializer implements LazyInitializer {
	
	private Object target;
	private String entityName;
	private Serializable id;
	private transient SessionImplementor session;
	
	protected AbstractLazyInitializer(String entityName, Serializable id, SessionImplementor session) {
		this.id = id;
		this.session = session;
		this.entityName = entityName;
	}

	public final Serializable getIdentifier() {
		return id;
	}

	public final void setIdentifier(Serializable id) {
		this.id = id;
	}

	public final String getEntityName() {
		return entityName;
	}

	public final boolean isUninitialized() {
		return target == null;
	}

	public final SessionImplementor getSession() {
		return session;
	}

	public final void initialize() throws HibernateException {
		if (target==null) {
			if ( session==null ) {
				throw new LazyInitializationException("could not initialize proxy - no Session");
			}
			else if ( !session.isOpen() ) {
				throw new LazyInitializationException("could not initialize proxy - the owning Session was closed");
			}
			else if ( !session.isConnected() ) {
				throw new LazyInitializationException("could not initialize proxy - the owning Session is disconnected");
			}
			else {
				target = session.immediateLoad(entityName, id);
			}
		}
	}

	public final void setSession(SessionImplementor s) throws HibernateException {
		if (s!=session) {
			if ( session!=null && session.isOpen() ) {
				//TODO: perhaps this should be some other RuntimeException...
				throw new HibernateException("illegally attempted to associate a proxy with two open Sessions");
			}
			else {
				session = s;
			}
		}
	}

	/**
	 * Return the underlying persistent object, initializing if necessary
	 */
	public final Object getImplementation() {
		initialize();
		return target;
	}

	/**
	 * Return the underlying persistent object in the given <tt>Session</tt>, or null,
	 * do not initialize the proxy
	 */
	public final Object getImplementation(SessionImplementor s) throws HibernateException {
		return s.getPersistenceContext().getEntity( new EntityKey(
			getIdentifier(),
			s.getFactory().getEntityPersister( getEntityName() ),
			s.getEntityMode()
		) );
	}

	protected final void setTarget(Object target) {
		this.target = target;
	}

	protected final Object getTarget() {
		return target;
	}

}
