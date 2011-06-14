//$Id: ListType.java,v 1.13 2005/02/20 08:37:56 oneovthafew Exp $
package org.hibernate.type;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Element;
import org.hibernate.EntityMode;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.collection.PersistentList;
import org.hibernate.collection.PersistentListElementHolder;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;

public class ListType extends CollectionType {

	public ListType(String role, String propertyRef, boolean isEmbeddedInXML) {
		super(role, propertyRef, isEmbeddedInXML);
	}

	public PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister, Serializable key) {
		if ( session.getEntityMode()==EntityMode.DOM4J ) {
			return new PersistentListElementHolder(session, persister, key);
		}
		else {
			return new PersistentList(session);
		}
	}

	public Class getReturnedClass() {
		return List.class;
	}

	public PersistentCollection wrap(SessionImplementor session, Object collection) {
		if ( session.getEntityMode()==EntityMode.DOM4J ) {
			return new PersistentListElementHolder( session, (Element) collection );
		}
		else {
			return new PersistentList( session, (List) collection );
		}
	}

	public Object instantiate(Object original) {
		return new ArrayList();
	}
	
	public Object indexOf(Object collection, Object element) {
		List list = (List) collection;
		for ( int i=0; i<list.size(); i++ ) {
			//TODO: proxies!
			if ( list.get(i)==element ) return new Integer(i);
		}
		return null;
	}
	
}





