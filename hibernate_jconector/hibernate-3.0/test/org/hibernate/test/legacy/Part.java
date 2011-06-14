//$Id: Part.java,v 1.1 2004/09/26 05:18:25 oneovthafew Exp $
package org.hibernate.test.legacy;

/**
 * @author Gavin King
 */
public class Part {

	private Long id;
	private String description;
	
	public String getDescription() {
		return description;
	}

	public Long getId() {
		return id;
	}

	public void setDescription(String string) {
		description = string;
	}

	public void setId(Long long1) {
		id = long1;
	}
	
	public static class SpecialPart extends Part {}

}
