//$Id: Radio.java,v 1.2 2005/02/12 07:27:26 steveebersole Exp $
package org.hibernate.test.id;

/**
 * @author Emmanuel Bernard
 */
public class Radio {
	private Integer id;
	private String frequency;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getFrequency() {
		return frequency;
	}

	public void setFrequency(String frequency) {
		this.frequency = frequency;
	}
}
