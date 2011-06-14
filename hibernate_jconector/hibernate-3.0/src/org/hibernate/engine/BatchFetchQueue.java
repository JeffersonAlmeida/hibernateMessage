//$Id: BatchFetchQueue.java,v 1.10 2005/03/17 05:50:08 oneovthafew Exp $
package org.hibernate.engine;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.ReferenceMap;
import org.apache.commons.collections.SequencedHashMap;
import org.hibernate.EntityMode;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.util.MarkerObject;

/**
 * Tracks entity and collection keys that are available for batch
 * fetching, and the queries which were used to load entities, which
 * can be re-used as a subquery for loading owned collections.
 * 
 * @author Gavin King
 */
public class BatchFetchQueue {

	public static final Object MARKER = new MarkerObject("MARKER");
	
	// A set of entity keys that we predict we might need to load soon
	// TODO: this would be better as a SequencedReferenceSet, but no such beast exists!
	private final Map batchLoadableEntityKeys = new SequencedHashMap(8); //actually, a Set
	
	// The subqueries that were used to load the entity with the given key
	private final Map subselectsByEntityKey = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
	
	// The owning persistence context
	private final PersistenceContext context;
	
	public BatchFetchQueue(PersistenceContext context) {
		this.context = context;
	}
	
	public void clear() {
		batchLoadableEntityKeys.clear();
		subselectsByEntityKey.clear();
	}

	public SubselectFetch getSubselect(EntityKey key) {
		return (SubselectFetch) subselectsByEntityKey.get(key);
	}

	public void addSubselect(EntityKey key, SubselectFetch subquery) {
		subselectsByEntityKey.put(key, subquery);
	}

	public void clearSubselect() {
		subselectsByEntityKey.clear();
	}
	
	/**
	 * After evicting or deleting or loading an entity, we don't 
	 * need to batch fetch it anymore, remove it from the queue
	 * if necessary
	 */
	public void removeBatchLoadableEntityKey(EntityKey key) {
		if ( key.isBatchLoadable() ) batchLoadableEntityKeys.remove(key);
	}
	
	/**
	 * After evicting or deleting an entity, we don't need to 
	 * know the query that was used to load it anymore (don't 
	 * call this after loading the entity, since we might still
	 * need to load its collections)
	 */
	public void removeSubquery(EntityKey key) {
		subselectsByEntityKey.remove(key);
	}
	
	/**
	 * If an EntityKey represents a batch loadable entity, add
	 * it to the queue.
	 */
	public void addBatchLoadableEntityKey(EntityKey key) {
		if ( key.isBatchLoadable() ) batchLoadableEntityKeys.put(key, MARKER);
	}

	/**
	 * Get a batch of uninitialized collection keys for this role
	 * @param collectionPersister the collection role
	 * @param id a key that must be included
	 * @param batchSize the maximum number of keys to return
	 * @return an array of collection keys, of length batchSize (padded with nulls)
	 */
	public Serializable[] getCollectionBatch(
			CollectionPersister collectionPersister, 
			Serializable id, 
			int batchSize,
			EntityMode entityMode
	) {
		Serializable[] keys = new Serializable[batchSize];
		keys[0] = id;
		int i = 1;
		//int count = 0;
		int end = -1;
		boolean checkForEnd = false;
		//this only works because collection entries are kept in a sequenced
		//map by persistence context
		Iterator iter = context.getCollectionEntries().values().iterator();
		while ( iter.hasNext() ) {
			
			CollectionEntry ce = (CollectionEntry) iter.next();
			if ( !ce.isInitialized() && ce.getLoadedPersister() == collectionPersister ) {
				
				if ( checkForEnd && i == end ) return keys; //the first key found after the given key
				
				//if ( end == -1 && count > batchSize*10 ) return keys; //try out ten batches, max
				
				final boolean isEqual = collectionPersister.getKeyType()
						.isEqual( id, ce.getLoadedKey(), entityMode, collectionPersister.getFactory() );
				
				if ( isEqual ) {
					end = i;
					//checkForEnd = false;
				}
				else {
					keys[i++] = ce.getLoadedKey();
					//count++;
				}
				
				if ( i == batchSize ) {
					i = 1; //end of array, start filling again from start
					if (end!=-1) checkForEnd = true;
				}
			}
			
		}
		return keys; //we ran out of keys to try
	}

	/**
	 * Get a batch of unloaded identifiers for this class, using a slightly
	 * complex algorithm that tries to grab keys registered immediately after
	 * the given key.
	 * 
	 * @param entityName The name of the persistent class
	 * @param id an identifier that must be included
	 * @param batchSize the maximum number of keys to return
	 * @return an array of identifiers, of length batchSize (padded with nulls)
	 */
	public Serializable[] getEntityBatch(String entityName, Serializable id, int batchSize) {
		Serializable[] ids = new Serializable[batchSize];
		ids[0] = id; //first element of array is reserved for the actual instance we are loading!
		int i = 1;
		int end = -1;
		boolean checkForEnd = false;
		//int count = 0;
		Iterator iter = batchLoadableEntityKeys.keySet().iterator();
		while ( iter.hasNext() ) {
			
			EntityKey key = (EntityKey) iter.next();
			if ( key.getEntityName().equals(entityName) ) { //TODO: this needn't exclude subclasses...
				
				if ( checkForEnd && i == end ) return ids; //the first id found after the given id

				//if ( end == -1 && count > batchSize*10 ) return ids; //try out ten batches, max

				if ( id.equals( key.getIdentifier() ) ) { //TODO: use Type.isEqual()
					end = i;
					//checkForEnd = false;
				}
				else {
					ids[i++] = key.getIdentifier();
					//count++;
				}
				
				if ( i == batchSize ) {
					i = 1; //end of array, start filling again from start
					if (end!=-1) checkForEnd = true;
				}
				
			}
			
		}
		return ids; //we ran out of ids to try
	}

}
