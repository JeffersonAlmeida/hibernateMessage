//$Id: ReverseComparator.java,v 1.1 2004/09/26 05:18:25 oneovthafew Exp $
package org.hibernate.test.legacy;

import java.io.Serializable;
import java.util.Comparator;

public final class ReverseComparator implements Comparator, Serializable {
	public int compare(Object x, Object y) {
		return - ( (Comparable) x ).compareTo(y);
	}
	
	public boolean equals(Object obj) {
		return obj instanceof ReverseComparator;
	}
	
	public int hashCode() {
		return 0;
	}
	
}






