//$Id: Joinable.java,v 1.1 2005/02/13 11:50:09 oneovthafew Exp $
package org.hibernate.persister.entity;

import org.hibernate.MappingException;

import java.util.Map;

/**
 * Anything that can be loaded by outer join - namely
 * persisters for classes or collections.
 *
 * @author Gavin King
 */
public interface Joinable {
	//should this interface extend PropertyMapping?

	/**
	 * An identifying name; a class name or collection role name.
	 */
	public String getName();
	/**
	 * The table to join to.
	 */
	public String getTableName();
	/**
	 * All columns to select, when loading.
	 */
	public String selectFragment(String alias, String suffix, boolean includeCollectionColumns);
	/**
	 * Get the where clause part of any joins
	 * (optional operation)
	 */
	public String whereJoinFragment(String alias, boolean innerJoin, boolean includeSubclasses);
	/**
	 * Get the from clause part of any joins
	 * (optional operation)
	 */
	public String fromJoinFragment(String alias, boolean innerJoin, boolean includeSubclasses);
	/**
	 * The columns to join on
	 */
	public String[] getKeyColumnNames();
	/**
	 * Get the where clause filter, given a query alias and considering enabled session filters
	 */
	public String filterFragment(String alias, Map enabledFilters) throws MappingException;

	public String oneToManyFilterFragment(String alias) throws MappingException;
	/**
	 * Is this instance actually a CollectionPersister?
	 */
	public boolean isCollection();
	/**
	 * Ugly, very ugly....
	 */
	public boolean consumesAlias();

}
