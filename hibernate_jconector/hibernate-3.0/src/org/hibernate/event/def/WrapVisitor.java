//$Id: WrapVisitor.java,v 1.9 2005/03/04 10:52:21 oneovthafew Exp $
package org.hibernate.event.def;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

/**
 * Wrap collections in a Hibernate collection
 * wrapper.
 * @author Gavin King
 */
public class WrapVisitor extends ProxyVisitor {

	private static final Log log = LogFactory.getLog(WrapVisitor.class);

	boolean substitute = false;

	boolean isSubstitutionRequired() {
		return substitute;
	}

	WrapVisitor(SessionImplementor session) {
		super(session);
	}

	Object processCollection(Object collection, CollectionType collectionType)
	throws HibernateException {

		if ( collection!=null && (collection instanceof PersistentCollection) ) {

			final SessionImplementor session = getSession();
			PersistentCollection coll = (PersistentCollection) collection;
			if ( coll.setCurrentSession(session) ) {
				reattachCollection( coll, coll.getCollectionSnapshot() );
			}
			return null;

		}
		else {
			return processArrayOrNewCollection(collection, collectionType);
		}

	}

	final Object processArrayOrNewCollection(Object collection, CollectionType collectionType)
	throws HibernateException {

		final SessionImplementor session = getSession();

		if (collection==null) {
			//do nothing
			return null;
		}
		else {
			CollectionPersister persister = session.getFactory().getCollectionPersister( collectionType.getRole() );

			final PersistenceContext persistenceContext = session.getPersistenceContext();
			//TODO: move into collection type, so we can use polymorphism!
			if ( collectionType.hasHolder( session.getEntityMode() ) ) {
				
				if (collection==CollectionType.UNFETCHED_COLLECTION) return null;

				PersistentCollection ah = persistenceContext.getCollectionHolder(collection);
				if (ah==null) {
					ah = collectionType.wrap(session, collection);
					persistenceContext.addNewCollection( ah, persister );
					persistenceContext.addCollectionHolder(ah);
				}
				return null;
			}
			else {

				PersistentCollection persistentCollection = collectionType.wrap(session, collection);
				persistenceContext.addNewCollection( persistentCollection, persister );

				if ( log.isTraceEnabled() ) log.trace( "Wrapped collection in role: " + collectionType.getRole() );

				return persistentCollection; //Force a substitution!

			}

		}

	}

	void processValue(int i, Object[] values, Type[] types) {
		Object result = processValue( values[i], types[i] );
		if (result!=null) {
			substitute = true;
			values[i] = result;
		}
	}

	Object processComponent(Object component, AbstractComponentType componentType)
	throws HibernateException {

		if (component!=null) {
			Object[] values = componentType.getPropertyValues( component, getSession() );
			Type[] types = componentType.getSubtypes();
			boolean substituteComponent = false;
			for ( int i=0; i<types.length; i++ ) {
				Object result = processValue( values[i], types[i] );
				if (result!=null) {
					values[i] = result;
					substituteComponent = true;
				}
			}
			if (substituteComponent) {
				componentType.setPropertyValues( component, values, getSession().getEntityMode() );
			}
		}

		return null;
	}

	void process(Object object, EntityPersister persister) throws HibernateException {
		EntityMode entityMode = getSession().getEntityMode();
		Object[] values = persister.getPropertyValues( object, entityMode );
		Type[] types = persister.getPropertyTypes();
		processEntityPropertyValues(values, types);
		if ( isSubstitutionRequired() ) {
			persister.setPropertyValues( object, values, entityMode );
		}
	}

}
