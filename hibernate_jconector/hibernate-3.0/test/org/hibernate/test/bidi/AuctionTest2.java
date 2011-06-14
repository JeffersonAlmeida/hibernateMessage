//$Id: AuctionTest2.java,v 1.1 2005/02/14 15:56:06 oneovthafew Exp $
package org.hibernate.test.bidi;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Gavin King
 */
public class AuctionTest2 extends AuctionTest {
	public AuctionTest2(String str) {
		super(str);
	}

	protected String[] getMappings() {
		return new String[] { "bidi/Auction2.hbm.xml" };
	}

	public static Test suite() {
		return new TestSuite(AuctionTest2.class);
	}

}
