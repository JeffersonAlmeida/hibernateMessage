//$Id: NumberedNode.java,v 1.2 2005/02/12 07:27:30 steveebersole Exp $
package org.hibernate.test.ops;

/**
 * @author Gavin King
 */
public class NumberedNode extends Node {
	
	private long id;

	public NumberedNode() {
		super();
	}


	public NumberedNode(String name) {
		super(name);
	}

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
}
