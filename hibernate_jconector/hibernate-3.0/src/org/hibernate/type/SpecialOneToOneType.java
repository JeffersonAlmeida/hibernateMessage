//$Id: SpecialOneToOneType.java,v 1.7 2005/03/16 04:45:25 oneovthafew Exp $
package org.hibernate.type;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.ForeignKeys;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Gavin King
 */
public class SpecialOneToOneType extends OneToOneType {
	
	private final String propertyName;

	public SpecialOneToOneType(
			String propertyName, 
			String entityName,
			ForeignKeyDirection foreignKeyType, 
			String uniqueKeyPropertyName) {
		super(entityName, foreignKeyType, uniqueKeyPropertyName, true);
		this.propertyName = propertyName;
	}
	
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return super.getIdentifierOrUniqueKeyType(mapping).getColumnSpan(mapping);
	}
	
	public int[] sqlTypes(Mapping mapping) throws MappingException {
		return super.getIdentifierOrUniqueKeyType(mapping).sqlTypes(mapping);
	}

	public boolean useLHSPrimaryKey() {
		return false;
	}
	
	public Object hydrate(ResultSet rs, String[] names, SessionImplementor session, Object owner)
	throws HibernateException, SQLException {
		return super.getIdentifierOrUniqueKeyType( session.getFactory() )
			.nullSafeGet(rs, names, session, owner);
	}
	
	public String getLHSPropertyName() {
		return propertyName;
	}
	
	public Object resolve(Object value, SessionImplementor session, Object owner)
			throws HibernateException {
		if (value==null) return null;
		EntityPersister ownerPersister = session.getFactory()
				.getEntityPersister( session.getEntityName(owner) ); //TODO: keep track of owner entity name at instantiation!
		Serializable id = session.getEntityIdentifier(owner);
		final boolean isPropertyNull = session.getPersistenceContext()
			.isPropertyNull( new EntityKey( id, ownerPersister, session.getEntityMode() ), propertyName );
		if ( isPropertyNull ) {
			return null;
		}
		else {
			return super.resolve(value, session, owner);
		}
	}
	
	// TODO: copy/paste from ManyToOneType

	public Serializable disassemble(Object value, SessionImplementor session, Object owner)
	throws HibernateException {

		if ( isNotEmbedded(session) ) {
			return getIdentifierType(session).disassemble(value, session, owner);
		}
		
		if (value==null) {
			return null;
		}
		else {
			// cache the actual id of the object, not the value of the
			// property-ref, which might not be initialized
			Object id = ForeignKeys.getEntityIdentifierIfNotUnsaved( getAssociatedEntityName(), value, session );
			if (id==null) {
				throw new AssertionFailure(
						"cannot cache a reference to an object with a null id: " + 
						getAssociatedEntityName() 
				);
			}
			return getIdentifierType(session).disassemble(id, session, owner);
		}
	}

	public Object assemble(Serializable oid, SessionImplementor session, Object owner)
	throws HibernateException {
		//TODO: currently broken for unique-key references (does not detect
		//      change to unique key property of the associated object)
		Serializable id = (Serializable) getIdentifierType(session).assemble(oid, session, null); //the owner of the association is not the owner of the id

		if ( isNotEmbedded(session) ) return id;
		
		if (id==null) {
			return null;
		}
		else {
			return resolveIdentifier(id, session);
		}
	}
	


}
