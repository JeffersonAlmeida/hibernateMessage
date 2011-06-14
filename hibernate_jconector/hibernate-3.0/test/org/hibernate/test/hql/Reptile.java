//$Id: Reptile.java,v 1.3 2005/02/12 07:27:25 steveebersole Exp $
package org.hibernate.test.hql;

/**
 * @author Gavin King
 */
public class Reptile extends Animal {
	private float bodyTemperature;
	public float getBodyTemperature() {
		return bodyTemperature;
	}
	public void setBodyTemperature(float bodyTemperature) {
		this.bodyTemperature = bodyTemperature;
	}
}
