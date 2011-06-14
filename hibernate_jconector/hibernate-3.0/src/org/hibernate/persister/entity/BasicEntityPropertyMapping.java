//$Id: BasicEntityPropertyMapping.java,v 1.1 2005/02/13 11:50:09 oneovthafew Exp $
package org.hibernate.persister.entity;

import org.hibernate.QueryException;
import org.hibernate.type.Type;
import org.hibernate.util.StringHelper;

/**
 * @author Gavin King
 */
public class BasicEntityPropertyMapping extends AbstractPropertyMapping {

	private final BasicEntityPersister persister;

	public BasicEntityPropertyMapping(BasicEntityPersister persister) {
		this.persister = persister;
	}
	
	public String[] getIdentifierColumnNames() {
		return persister.getIdentifierColumnNames();
	}

	protected String getEntityName() {
		return persister.getEntityName();
	}

	public Type getType() {
		return persister.getType();
	}

	public String[] toColumns(final String alias, final String propertyName) throws QueryException {
		final String rootPropertyName = StringHelper.root(propertyName);
		return super.toColumns( 
				persister.generateTableAlias( alias, persister.getSubclassPropertyTableNumber(rootPropertyName) ), 
				propertyName 
		);
	}
	
	
}
