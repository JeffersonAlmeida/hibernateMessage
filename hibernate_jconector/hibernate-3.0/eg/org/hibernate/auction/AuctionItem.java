//$Id: AuctionItem.java,v 1.1 2004/06/03 16:30:01 steveebersole Exp $
package org.hibernate.auction;

import java.util.Date;
import java.util.List;

/**
 * @author Gavin King
 */
public class AuctionItem extends Persistent {
	private String description;
	private List bids;
	private Bid successfulBid;
	private User seller;
	private Date ends;
	private int condition;
	public List getBids() {
		return bids;
	}

	public String getDescription() {
		return description;
	}

	public User getSeller() {
		return seller;
	}

	public Bid getSuccessfulBid() {
		return successfulBid;
	}

	public void setBids(List bids) {
		this.bids = bids;
	}

	public void setDescription(String string) {
		description = string;
	}

	public void setSeller(User user) {
		seller = user;
	}

	public void setSuccessfulBid(Bid bid) {
		successfulBid = bid;
	}

	public Date getEnds() {
		return ends;
	}

	public void setEnds(Date date) {
		ends = date;
	}
	
	public int getCondition() {
		return condition;
	}

	public void setCondition(int i) {
		condition = i;
	}

	public String toString() {
		return description + " (" + condition + "/10)";
	}

}
