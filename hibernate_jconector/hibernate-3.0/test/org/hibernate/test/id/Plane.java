//$Id: Plane.java,v 1.2 2005/02/12 07:27:26 steveebersole Exp $
package org.hibernate.test.id;

/**
 * @author Emmanuel Bernard
 */
public class Plane {
	private Long id;
	private int nbrOfSeats;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getNbrOfSeats() {
		return nbrOfSeats;
	}

	public void setNbrOfSeats(int nbrOfSeats) {
		this.nbrOfSeats = nbrOfSeats;
	}
}
