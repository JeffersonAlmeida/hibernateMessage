//$Id: Range.java,v 1.1 2004/06/03 16:31:30 steveebersole Exp $
package org.hibernate.util;

public final class Range {

	public static int[] range(int begin, int length) {
		int[] result = new int[length];
		for ( int i=0; i<length; i++ ) {
			result[i]=begin + i;
		}
		return result;
	}

	private Range() {}
}







