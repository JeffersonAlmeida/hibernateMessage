//$Id: NamedQueryCollectionInitializer.java,v 1.1 2005/02/13 11:50:09 oneovthafew Exp $
package org.hibernate.persister.collection;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.impl.AbstractQueryImpl;
import org.hibernate.loader.collection.CollectionInitializer;

/**
 * A wrapper around a named query.
 * @author Gavin King
 */
public final class NamedQueryCollectionInitializer implements CollectionInitializer {
	private final String queryName;
	private final CollectionPersister persister;
	
	private static final Log log = LogFactory.getLog(NamedQueryCollectionInitializer.class);

	public NamedQueryCollectionInitializer(String queryName, CollectionPersister persister) {
		super();
		this.queryName = queryName;
		this.persister = persister;
	}

	public void initialize(Serializable key, SessionImplementor session) 
	throws HibernateException {
		
		if ( log.isDebugEnabled() ) {
			log.debug("initializing collection: " + persister.getRole() + " using named query: " + queryName );
		}
		
		//TODO: is there a more elegant way than downcasting?
		AbstractQueryImpl query = (AbstractQueryImpl) session.getNamedQuery(queryName); 
		if ( query.getNamedParameters().length>0 ) {
			query.setParameter( 
					query.getNamedParameters()[0], 
					key, 
					persister.getKeyType() 
			);
		}
		else {
			query.setParameter( 0, key, persister.getKeyType() );
		}
		query.setCollectionKey(key)
			.setFlushMode(FlushMode.NEVER)
			.list();

	}
}