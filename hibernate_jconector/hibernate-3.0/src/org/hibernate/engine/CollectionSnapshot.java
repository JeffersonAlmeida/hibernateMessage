//$Id: CollectionSnapshot.java,v 1.1 2004/06/03 16:30:07 steveebersole Exp $
package org.hibernate.engine;

import java.io.Serializable;

/**
 * A "snapshot" of collection state. A <tt>PersistentCollection</tt>
 * carries a snapshot of its state even when serialized and transported
 * to another process. This allows us to track exactly which rows changed
 * if the entity is passed to <tt>update()</tt>.
 *
 * @author Gavin King
 */
public interface CollectionSnapshot extends Serializable {
	public Serializable getKey();
	public String getRole();
	public Serializable getSnapshot();
	public boolean getDirty();
	public void setDirty();
	public boolean wasDereferenced();
	//public boolean isInitialized();
}
