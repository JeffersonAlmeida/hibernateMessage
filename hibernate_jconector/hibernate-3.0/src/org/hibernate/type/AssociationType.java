//$Id: AssociationType.java,v 1.9 2005/02/13 11:50:10 oneovthafew Exp $
package org.hibernate.type;

import org.hibernate.MappingException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.persister.entity.Joinable;

import java.util.Map;

/**
 * A type that represents some kind of association between entities.
 * @see org.hibernate.engine.Cascades
 * @author Gavin King
 */
public interface AssociationType extends Type {

	/**
	 * Get the foreign key directionality of this association
	 */
	public ForeignKeyDirection getForeignKeyDirection();

	//TODO: move these to a new JoinableType abstract class,
	//extended by EntityType and PersistentCollectionType:

	/**
	 * Is the primary key of the owning entity table
	 * to be used in the join?
	 */
	public boolean useLHSPrimaryKey();
	/**
	 * Get the name of a property in the owning entity 
	 * that provides the join key (null if the identifier)
	 */
	public String getLHSPropertyName();
	
	/**
	 * The name of a unique property of the associated entity 
	 * that provides the join key (null if the identifier of
	 * an entity, or key of a collection)
	 */
	public String getRHSUniqueKeyPropertyName();

	/**
	 * Get the "persister" for this association - a class or
	 * collection persister
	 */
	public Joinable getAssociatedJoinable(SessionFactoryImplementor factory) throws MappingException;
	
	/**
	 * Get the entity name of the associated entity
	 */
	public String getAssociatedEntityName(SessionFactoryImplementor factory) throws MappingException;
	
	/**
	 * Get the "filtering" SQL fragment that is applied in the 
	 * SQL on clause, in addition to the usual join condition
	 */	
	public String getOnCondition(String alias, SessionFactoryImplementor factory, Map enabledFilters) 
	throws MappingException;
	
}






