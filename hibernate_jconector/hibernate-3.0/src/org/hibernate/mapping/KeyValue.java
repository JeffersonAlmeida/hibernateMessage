//$Id: KeyValue.java,v 1.7 2005/02/12 07:19:26 steveebersole Exp $
package org.hibernate.mapping;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.id.IdentifierGenerator;

/**
 * Represents an identifying key of a table: the value for primary key
 * of an entity, or a foreign key of a collection or join table or
 * joined subclass table.
 * @author Gavin King
 */
public interface KeyValue extends Value {
	
	public void createForeignKeyOfEntity(String entityName);
	
	public boolean isCascadeDeleteEnabled();
	
	public String getNullValue();
	
	public boolean isUpdateable();

	public IdentifierGenerator createIdentifierGenerator(
			Dialect dialect, 
			String defaultCatalog, 
			String defaultSchema, 
			String entityName) throws MappingException;
}
