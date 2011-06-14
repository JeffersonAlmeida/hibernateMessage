//$Id: Folder.java,v 1.1 2004/08/12 01:49:29 oneovthafew Exp $
package org.hibernate.test.instrument;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Gavin King
 */
public class Folder {
	private Long id;
	private String name;
	private Folder parent;
	private Collection subfolders = new ArrayList();
	private Collection documents = new ArrayList();
	/**
	 * @return Returns the id.
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return Returns the documents.
	 */
	public Collection getDocuments() {
		return documents;
	}
	/**
	 * @param documents The documents to set.
	 */
	public void setDocuments(Collection documents) {
		this.documents = documents;
	}
	/**
	 * @return Returns the parent.
	 */
	public Folder getParent() {
		return parent;
	}
	/**
	 * @param parent The parent to set.
	 */
	public void setParent(Folder parent) {
		this.parent = parent;
	}
	/**
	 * @return Returns the subfolders.
	 */
	public Collection getSubfolders() {
		return subfolders;
	}
	/**
	 * @param subfolders The subfolders to set.
	 */
	public void setSubfolders(Collection subfolders) {
		this.subfolders = subfolders;
	}
}
