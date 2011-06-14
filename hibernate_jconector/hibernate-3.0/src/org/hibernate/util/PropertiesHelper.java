//$Id: PropertiesHelper.java,v 1.3 2004/08/10 05:06:14 oneovthafew Exp $
package org.hibernate.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;


public final class PropertiesHelper {

	public static boolean getBoolean(String property, Properties properties) {
		return Boolean.valueOf( properties.getProperty(property) ).booleanValue();
	}

	public static boolean getBoolean(String property, Properties properties, boolean defaultValue) {
		String setting = properties.getProperty(property);
		return (setting==null) ? defaultValue : Boolean.valueOf(setting).booleanValue();
	}

	public static int getInt(String property, Properties properties, int defaultValue) {
		String propValue = properties.getProperty(property);
		return (propValue==null) ? defaultValue : Integer.parseInt(propValue);
	}

	public static String getString(String property, Properties properties, String defaultValue) {
		String propValue = properties.getProperty(property);
		return (propValue==null) ? defaultValue : propValue;
	}

	public static Integer getInteger(String property, Properties properties) {
		String propValue = properties.getProperty(property);
		return (propValue==null) ? null : Integer.valueOf(propValue);
	}

	public static Map toMap(String property, String delim, Properties properties) {
		Map map = new HashMap();
		String propValue = properties.getProperty(property);
		if (propValue!=null) {
			StringTokenizer tokens = new StringTokenizer(propValue, delim);
			while ( tokens.hasMoreTokens() ) {
				map.put(
					tokens.nextToken(),
					tokens.hasMoreElements() ? tokens.nextToken() : ""
				);
			}
		}
		return map;
	}

	public static String[] toStringArray(String property, String delim, Properties properties) {
		return toStringArray( properties.getProperty(property), delim );
	}

	public static String[] toStringArray(String propValue, String delim) {
		if (propValue!=null) {
			return StringHelper.split(delim, propValue);
		}
		else {
			return ArrayHelper.EMPTY_STRING_ARRAY;
		}
	}
	
	/**
	 * replace a property by a starred version
	 * 
	 * @param props properties to check
	 * @param key proeprty to mask
	 * @return cloned and masked properties
	 */
	public static Properties maskOut(Properties props, String key) {
		Properties clone = (Properties) props.clone();
		if (clone.get(key) != null) {
			clone.setProperty(key, "****");
		}
		return clone;
	}
	


	private PropertiesHelper() {}
}






