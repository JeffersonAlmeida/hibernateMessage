//$Id: InitializeCollectionEvent.java,v 1.5 2005/02/22 03:09:31 oneovthafew Exp $
package org.hibernate.event;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;

/**
 * An event that occurs when a collection wants to be
 * initialized
 * 
 * @author Gavin King
 */
public class InitializeCollectionEvent extends AbstractEvent {
	
	private final PersistentCollection collection;

	public InitializeCollectionEvent(PersistentCollection collection, SessionImplementor source) {
		super(source);
		this.collection = collection;
	}
	
	public PersistentCollection getCollection() {
		return collection;
	}
}
