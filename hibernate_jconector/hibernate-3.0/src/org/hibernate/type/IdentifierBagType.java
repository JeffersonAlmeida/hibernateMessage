//$Id: IdentifierBagType.java,v 1.9 2005/02/20 03:34:49 oneovthafew Exp $
package org.hibernate.type;

import java.io.Serializable;
import java.util.ArrayList;

import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentIdentifierBag;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;

public class IdentifierBagType extends CollectionType {

	public IdentifierBagType(String role, String propertyRef, boolean isEmbeddedInXML) {
		super(role, propertyRef, isEmbeddedInXML);
	}

	public PersistentCollection instantiate(
		SessionImplementor session,
		CollectionPersister persister, Serializable key)
		throws HibernateException {

		return new PersistentIdentifierBag(session);
	}

	public Object instantiate(Object original) {
		return new ArrayList();
	}
	
	public Class getReturnedClass() {
		return java.util.Collection.class;
	}

	public PersistentCollection wrap(SessionImplementor session, Object collection) {
		return new PersistentIdentifierBag( session, (java.util.Collection) collection );
	}

}






