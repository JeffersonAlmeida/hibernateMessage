//$Id: CollectionCacheEntry.java,v 1.1 2005/02/13 12:46:58 oneovthafew Exp $
package org.hibernate.cache.entry;

import java.io.Serializable;

import org.hibernate.collection.PersistentCollection;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.util.ArrayHelper;

/**
 * @author Gavin King
 */
public class CollectionCacheEntry implements Serializable {

	private final Serializable state;
	
	public Serializable[] getState() {
		//TODO: assumes all collections disassemble to an array!
		return (Serializable[]) state;
	}

	public CollectionCacheEntry(PersistentCollection collection, CollectionPersister persister) {
		this.state = collection.disassemble(persister);
	}
	
	CollectionCacheEntry(Serializable state) {
		this.state = state;
	}
	
	public void assemble(
		final PersistentCollection collection, 
		final CollectionPersister persister,
		final Object owner
	) {
		collection.initializeFromCache(persister, state, owner);	
	}
	
	public String toString() {
		return "CollectionCacheEntry" + ArrayHelper.toString( getState() );
	}

}
