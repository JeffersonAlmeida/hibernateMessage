//$Id: EqualsHelper.java,v 1.2 2004/09/25 11:22:20 oneovthafew Exp $
package org.hibernate.util;

/**
 * @author Gavin King
 */
public final class EqualsHelper {

	public static boolean equals(Object x, Object y) {
		return x==y || ( x!=null && y!=null && x.equals(y) );
	}
	
	private EqualsHelper() {}

}
