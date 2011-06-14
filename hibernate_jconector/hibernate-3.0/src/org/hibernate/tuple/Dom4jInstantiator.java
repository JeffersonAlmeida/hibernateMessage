//$Id: Dom4jInstantiator.java,v 1.1 2005/02/17 04:41:55 oneovthafew Exp $
package org.hibernate.tuple;

import java.io.Serializable;

import org.dom4j.Element;
import org.hibernate.util.XMLHelper;


public class Dom4jInstantiator implements Instantiator {
	private final String nodeName;

	Dom4jInstantiator(String nodeName) {
		this.nodeName = nodeName;
	}
	
	public Object instantiate(Serializable id) {
		return instantiate();
	}
	
	public Object instantiate() {
		return XMLHelper.generateDom4jElement(nodeName);
	}

	public boolean isInstance(Object object) {
		if ( object instanceof Element ) {
			return ( (Element) object ).getName().equals(nodeName);
		}
		else {
			return false;
		}
	}
}