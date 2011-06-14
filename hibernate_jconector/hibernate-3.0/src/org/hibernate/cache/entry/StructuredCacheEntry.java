//$Id: StructuredCacheEntry.java,v 1.1 2005/02/13 12:46:58 oneovthafew Exp $
package org.hibernate.cache.entry;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Gavin King
 */
public class StructuredCacheEntry implements CacheEntryStructure {

	private EntityPersister persister;

	public StructuredCacheEntry(EntityPersister persister) {
		this.persister = persister;
	}
	
	public Object destructure(Object item, SessionFactoryImplementor factory) {
		Map map = (Map) item;
		boolean lazyPropertiesUnfetched = ( (Boolean) map.get("_lazyPropertiesUnfetched") ).booleanValue();
		String subclass = (String) map.get("_subclass");
		EntityPersister subclassPersister = factory.getEntityPersister(subclass);
		String[] names = subclassPersister.getPropertyNames();
		Serializable[] state = new Serializable[names.length];
		for ( int i=0; i<names.length; i++ ) {
			state[i] = (Serializable) map.get( names[i] );
		}
		return new CacheEntry(state, subclass, lazyPropertiesUnfetched);
	}

	public Object structure(Object item) {
		CacheEntry entry = (CacheEntry) item;
		String[] names = persister.getPropertyNames();
		Map map = new HashMap(names.length+2);
		map.put( "_subclass", entry.getSubclass() );
		map.put( "_lazyPropertiesUnfetched", entry.areLazyPropertiesUnfetched() ? Boolean.TRUE : Boolean.FALSE );
		for ( int i=0; i<names.length; i++ ) {
			map.put( names[i], entry.getDisassembledState()[i] );
		}
		return map;
	}
}
