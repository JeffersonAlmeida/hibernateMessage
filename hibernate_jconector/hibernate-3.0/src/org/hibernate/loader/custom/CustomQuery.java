//$Id: CustomQuery.java,v 1.2 2005/03/03 13:26:32 oneovthafew Exp $
package org.hibernate.loader.custom;

import java.util.Map;
import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.loader.EntityAliases;
import org.hibernate.type.Type;


/**
 * Extension point allowing any SQL query with named
 * and positional parameters to be executed by Hibernate, 
 * returning managed entities, collections and simple
 * scalar values.
 * 
 * 
 * @author Gavin King
 */
public interface CustomQuery {
	public String getSQL();
	public Set getQuerySpaces();

	/**
	 * Optional, may return null
	 */
	public Map getNamedParameterBindPoints();
	
	public String[] getEntityNames();
	public EntityAliases[] getEntityAliases();
	public LockMode[] getLockModes();
	/**
	 * Optional, may return null
	 */
	public int[] getEntityOwners();
	
	/**
	 * Optional, may return -1
	 */
	public int getCollectionOwner();
	/**
	 * Optional, may return null
	 */
	public String getCollectionRole();
	
	/**
	 * Optional, may return null
	 */
	public Type[] getScalarTypes();
	/**
	 * Optional, may return null
	 */
	public String[] getScalarColumnAliases();
	
}
