//$Id: SelectGenerator.java,v 1.4 2005/03/17 21:09:13 oneovthafew Exp $
package org.hibernate.id;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.Type;

/**
 * An IdentityGenerator that selects the inserted row, to determine
 * an identifier value assigned by the database. The correct row is
 * located using a unique key.
 * <br><br>
 * One mapping parameter is required: key.
 *
 * @author Gavin King
 */
public class SelectGenerator extends AbstractPostInsertGenerator implements Configurable {
	
	private String uniqueKeyPropertyName;
	private Type idType;
	private String entityName;

	public void configure(Type type, Properties params, Dialect d) throws MappingException {
		uniqueKeyPropertyName = params.getProperty("key");
		entityName = params.getProperty(ENTITY_NAME);
		this.idType = type;
	}
	
	protected String getSQL(PostInsertIdentityPersister persister) {
		return persister.getSelectByUniqueKeyString(uniqueKeyPropertyName);
	}

	protected void bindParameters(SessionImplementor session, PreparedStatement ps, Object object, PostInsertIdentityPersister persister) 
	throws SQLException {
		Type uniqueKeyPropertyType = session.getSessionFactory()
				.getClassMetadata(entityName)
				.getPropertyType(uniqueKeyPropertyName);
		Object uniqueKeyValue = persister.getPropertyValue( object, uniqueKeyPropertyName, session.getEntityMode() );
		uniqueKeyPropertyType.nullSafeSet( ps, uniqueKeyValue, 1, session );
	}

	protected Serializable getResult(SessionImplementor session, ResultSet rs, Object object, PostInsertIdentityPersister persister) 
	throws SQLException {
		if ( !rs.next() ) {
			throw new IdentifierGenerationException( "the inserted row could not be located by the unique key: " + uniqueKeyPropertyName );
		}
		return (Serializable) idType.nullSafeGet(rs, persister.getRootTableKeyColumnNames(), session, object);
	}
}






