//$Id: LinkedHashCollectionHelper.java,v 1.1 2004/06/03 16:31:30 steveebersole Exp $
package org.hibernate.util;

import java.util.Map;
import java.util.Set;

import org.hibernate.AssertionFailure;

public final class LinkedHashCollectionHelper {

	private static final Class SET_CLASS;
	private static final Class MAP_CLASS;
	static {
		Class setClass;
		Class mapClass;
		try {
			setClass = Class.forName("java.util.LinkedHashSet");
			mapClass = Class.forName("java.util.LinkedHashMap");
		}
		catch (ClassNotFoundException cnfe) {
			setClass = null;
			mapClass = null;
		}
		SET_CLASS = setClass;
		MAP_CLASS = mapClass;
	}

	public static Set createLinkedHashSet() {
		try {
			return (Set) SET_CLASS.newInstance();
		}
		catch (Exception e) {
			throw new AssertionFailure("Could not instantiate LinkedHashSet", e);
		}
	}

	public static Map createLinkedHashMap() {
		try {
			return (Map) MAP_CLASS.newInstance();
		}
		catch (Exception e) {
			throw new AssertionFailure("Could not instantiate LinkedHashMap", e);
		}
	}

	private LinkedHashCollectionHelper() {}

}






