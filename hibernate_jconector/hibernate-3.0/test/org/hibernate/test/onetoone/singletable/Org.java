//$Id: Org.java,v 1.2 2005/02/12 07:27:29 steveebersole Exp $
package org.hibernate.test.onetoone.singletable;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Gavin King
 */
public class Org extends Entity {
	public Set addresses = new HashSet();
}
