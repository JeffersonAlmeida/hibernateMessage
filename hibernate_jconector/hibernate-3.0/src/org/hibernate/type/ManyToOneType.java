//$Id: ManyToOneType.java,v 1.18 2005/03/18 00:58:36 oneovthafew Exp $
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
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
 * A many-to-one association to an entity
 * @author Gavin King
 */
public class ManyToOneType extends EntityType {

	public int getColumnSpan(Mapping mapping) throws MappingException {
		return getIdentifierOrUniqueKeyType(mapping).getColumnSpan(mapping);
	}

	public int[] sqlTypes(Mapping mapping) throws MappingException {
		return getIdentifierOrUniqueKeyType(mapping).sqlTypes(mapping);
	}

	public ManyToOneType(String className) {
		this(className, null, true);
	}

	public ManyToOneType(String className, String uniqueKeyPropertyName, boolean isEmbeddedInXML) {
		super(className, uniqueKeyPropertyName, isEmbeddedInXML);
	}

	public void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SessionImplementor session) 
	throws HibernateException, SQLException {
		getIdentifierOrUniqueKeyType( session.getFactory() )
			.nullSafeSet(st, getIdentifier(value, session), index, settable, session);
	}

	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) 
	throws HibernateException, SQLException {
		getIdentifierOrUniqueKeyType( session.getFactory() )
			.nullSafeSet(st, getIdentifier(value, session), index, session);
	}

	public boolean isOneToOne() {
		return false;
	}

	public ForeignKeyDirection getForeignKeyDirection() {
		return ForeignKeyDirection.FOREIGN_KEY_FROM_PARENT;
	}

	public Object hydrate(ResultSet rs, String[] names, SessionImplementor session, Object owner)
	throws HibernateException, SQLException {

		Serializable id = (Serializable) getIdentifierOrUniqueKeyType( session.getFactory() )
			.nullSafeGet(rs, names, session, null); //note that the owner of the association is not really the owner of the id!

		if (id!=null) scheduleBatchLoad(id, session);
		
		return id;
	}

	protected Object resolveIdentifier(Serializable id, SessionImplementor session) 
	throws HibernateException {
		return session.internalLoad( getAssociatedEntityName(), id );
	}

	/**
	 * Register the entity as batch loadable, if enabled
	 */
	private void scheduleBatchLoad(Serializable id, SessionImplementor session) 
	throws MappingException {
		
		if (uniqueKeyPropertyName==null) { //cannot batch fetch by unique key
		
			EntityPersister persister = session.getFactory()
				.getEntityPersister( getAssociatedEntityName() );
			
			session.getPersistenceContext()
				.getBatchFetchQueue()
				.addBatchLoadableEntityKey( new EntityKey( id, persister, session.getEntityMode() ) );
		}
	}
	
	public boolean useLHSPrimaryKey() {
		return false;
	}

	public boolean isModified(Object old, Object current, SessionImplementor session)
	throws HibernateException {

		if (current==null) return old!=null;
		if (old==null) return current!=null;
		return getIdentifierOrUniqueKeyType( session.getFactory() )
			.isModified( old, getIdentifier(current, session), session );
	}

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
