//$Id: Down.java,v 1.1 2004/09/26 05:18:25 oneovthafew Exp $
package org.hibernate.test.legacy;


/**
 * @author Gavin King
 */
public class Down extends Up {

	private long value;

	public long getValue() {
		return value;
	}

	public void setValue(long l) {
		value = l;
	}

}
