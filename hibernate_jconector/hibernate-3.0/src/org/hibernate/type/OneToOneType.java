//$Id: OneToOneType.java,v 1.13 2005/03/16 04:45:25 oneovthafew Exp $
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.util.ArrayHelper;

/**
 * A one-to-one association to an entity
 * @author Gavin King
 */
public class OneToOneType extends EntityType {

	private final ForeignKeyDirection foreignKeyType;

	public int getColumnSpan(Mapping session) throws MappingException {
		return 0;
	}

	public int[] sqlTypes(Mapping session) throws MappingException {
		return ArrayHelper.EMPTY_INT_ARRAY;
	}

	public OneToOneType(
			String entityName, 
			ForeignKeyDirection foreignKeyType, 
			String uniqueKeyPropertyName, 
			boolean isEmbeddedInXML
	) {
		super(entityName, uniqueKeyPropertyName, isEmbeddedInXML);
		this.foreignKeyType = foreignKeyType;
	}

	public void nullSafeSet(PreparedStatement st, Object value, int index, boolean[] settable, SessionImplementor session) {
		//nothing to do
	}

	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) {
		//nothing to do
	}

	public boolean isOneToOne() {
		return true;
	}


	public boolean isDirty(Object old, Object current, SessionImplementor session) {
		return false;
	}

	public boolean isModified(Object old, Object current, SessionImplementor session) {
		return false;
	}

	public ForeignKeyDirection getForeignKeyDirection() {
		return foreignKeyType;
	}

	public Object hydrate(
		ResultSet rs,
		String[] names,
		SessionImplementor session,
		Object owner)
	throws HibernateException, SQLException {

		return session.getEntityIdentifier(owner);
	}

	protected Object resolveIdentifier(Serializable id, SessionImplementor session) 
	throws HibernateException {
		String entityName = getAssociatedEntityName();
		return isNullable() ?
			session.internalLoadOneToOne(entityName, id) : //no proxy allowed
			session.internalLoad(entityName, id); //proxy ok
	}

	public boolean isNullable() {
		return foreignKeyType==ForeignKeyDirection.FOREIGN_KEY_TO_PARENT;
	}

	public boolean useLHSPrimaryKey() {
		return true;
	}

	public Serializable disassemble(Object value, SessionImplementor session, Object owner)
	throws HibernateException {
		return null;
	}

	public Object assemble(Serializable oid, SessionImplementor session, Object owner)
	throws HibernateException {
		//this should be a call to resolve(), not resolveIdentifier(), 
		//'cos it might be a property-ref, and we did not cache the
		//referenced value
		return resolve( session.getEntityIdentifier(owner), session, owner );
	}
	
}

