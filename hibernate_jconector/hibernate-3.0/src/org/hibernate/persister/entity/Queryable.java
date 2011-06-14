//$Id: Queryable.java,v 1.2 2005/03/30 16:51:20 oneovthafew Exp $
package org.hibernate.persister.entity;

/**
 * Extends the generic <tt>EntityPersister</tt> contract to add
 * operations required by the Hibernate Query Language
 *
 * @author Gavin King
 */
public interface Queryable extends Loadable, PropertyMapping, Joinable {

	/**
	 * Is this an abstract class?
	 */
	public boolean isAbstract();
	/**
	 * Is this class mapped as a subclass of another class?
	 */
	public boolean isInherited();
	/**
	 * Is this class explicit polymorphism only?
	 */
	public boolean isExplicitPolymorphism();
	/**
	 * Get the class that this class is mapped as a subclass of -
	 * not necessarily the direct superclass
	 */
	public String getMappedSuperclass();
	/**
	 * Get the discriminator value for this particular concrete subclass,
	 * as a string that may be embedded in a select statement
	 */
	public String getDiscriminatorSQLValue();

	/**
	 * Given a query alias and an identifying suffix, render the intentifier select fragment.
	 */
	public String identifierSelectFragment(String name, String suffix);
	/**
	 * Given a query alias and an identifying suffix, render the property select fragment.
	 */
	public String propertySelectFragment(String alias, String suffix);

	/**
	 * Get the names of columns used to persist the identifier
	 */
	public String[] getIdentifierColumnNames();

}
