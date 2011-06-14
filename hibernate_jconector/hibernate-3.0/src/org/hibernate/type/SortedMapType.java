//$Id: SortedMapType.java,v 1.8 2005/03/22 21:10:09 oneovthafew Exp $
package org.hibernate.type;

import java.io.Serializable;
import java.util.Comparator;

import org.dom4j.Element;
import org.hibernate.EntityMode;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.collection.PersistentElementHolder;
import org.hibernate.collection.PersistentMapElementHolder;
import org.hibernate.collection.PersistentSortedMap;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;


public class SortedMapType extends MapType {

	private final Comparator comparator;

	public SortedMapType(String role, String propertyRef, Comparator comparator, boolean isEmbeddedInXML) {
		super(role, propertyRef, isEmbeddedInXML);
		this.comparator = comparator;
	}

	public PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister, Serializable key) {
		if ( session.getEntityMode()==EntityMode.DOM4J ) {
			return new PersistentMapElementHolder(session, persister, key);
		}
		else {
			PersistentSortedMap map = new PersistentSortedMap(session);
			map.setComparator(comparator);
			return map;
		}
	}

	public Class getReturnedClass() {
		return java.util.SortedMap.class;
	}

	public PersistentCollection wrap(SessionImplementor session, Object collection) {
		if ( session.getEntityMode()==EntityMode.DOM4J ) {
			return new PersistentElementHolder( session, (Element) collection );
		}
		else {
			return new PersistentSortedMap( session, (java.util.SortedMap) collection );
		}
	}

}






