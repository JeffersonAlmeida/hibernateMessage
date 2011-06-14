//$Id: DynamicMapInstantiator.java,v 1.1 2005/02/17 04:41:55 oneovthafew Exp $
package org.hibernate.tuple;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


public class DynamicMapInstantiator implements Instantiator {
	private String entityName;
	
	DynamicMapInstantiator(String entityName) {
		this.entityName = entityName;
	}

	public Object instantiate(Serializable id) {
		return instantiate();
	}
	public Object instantiate() {
		Map map = new HashMap();
		if (entityName!=null) map.put( "type", entityName );
		return map;
	}

	public boolean isInstance(Object object) {
		if ( object instanceof Map ) {
			//TODO: what about polymorphism?
			if (entityName==null) return true;
			String type = ( String ) ( ( Map ) object ).get( "type" );
			return type == null || entityName.equals( type );
		}
		else {
			return false;
		}
	}
}