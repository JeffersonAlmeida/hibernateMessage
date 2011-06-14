//$Id: BuyNow.java,v 1.1 2004/06/03 16:30:01 steveebersole Exp $
package org.hibernate.auction;

/**
 * @author Gavin King
 */
public class BuyNow extends Bid {
	public boolean isBuyNow() {
		return true;
	}
	public String toString() {
		return super.toString() + " (buy now)";
	}
}
