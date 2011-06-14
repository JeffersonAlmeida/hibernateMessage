//$Id: MessageHelper.java,v 1.9 2005/02/21 02:46:40 oneovthafew Exp $
package org.hibernate.pretty;

import java.io.Serializable;

import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

/**
 * Helper methods for rendering log messages and exception
 * messages.
 * @author Max Andersen, Gavin King
 */
public final class MessageHelper {

	private MessageHelper() {}

	public static String infoString(String entityName, Serializable id) {
		StringBuffer s = new StringBuffer();
		s.append('[');
		if(entityName==null) {
			s.append("<null entity name>");
		}
		else {
			s.append(entityName);
		}
		s.append('#');

		if (id==null) {
			s.append("<null>");
		}
		else {
			s.append(id);
		}
		s.append(']');

		return s.toString();
	}

	/**
	 * Generate small message that can be used in traces and exception
	 * messages.
	 * @param persister The persister for the class in question
	 * @param id The id
	 * @return String on the form [FooBar#id]
	 */
	public static String infoString(
			EntityPersister persister, 
			Object id, 
			SessionFactoryImplementor factory
	) {
		StringBuffer s = new StringBuffer();
		s.append('[');
		if(persister==null) {
			s.append("<null EntityPersister>");
		}
		else {
			s.append( persister.getEntityName() );
		}
		s.append('#');

		if (id==null) {
			s.append("<null>");
		}
		else {
			s.append( persister.getIdentifierType().toLoggableString(id, factory) );
		}
		s.append(']');

		return s.toString();

	}

	public static String infoString(
			EntityPersister persister, 
			Object id, 
			Type identifierType,
			SessionFactoryImplementor factory
	) {
		StringBuffer s = new StringBuffer();
		s.append('[');
		if(persister==null) {
			s.append("<null EntityPersister>");
		}
		else {
			s.append( persister.getEntityName() );
		}
		s.append('#');

		if (id==null) {
			s.append("<null>");
		}
		else {
			s.append( identifierType.toLoggableString(id, factory) );
		}
		s.append(']');

		return s.toString();

	}

	public static String infoString(
			EntityPersister persister, 
			Serializable[] ids, 
			SessionFactoryImplementor factory
	) {
		StringBuffer s = new StringBuffer();
		s.append('[');
		if(persister==null) {
			s.append("<null EntityPersister>");
		}
		else {
			s.append( persister.getEntityName() )
				.append("#<");

			for ( int i=0; i<ids.length; i++ ) {
				s.append( persister.getIdentifierType().toLoggableString( ids[i], factory ) );
				if ( i<ids.length-1 ) s.append(", ");
			}
			s.append('>');
		}
		s.append(']');

		return s.toString();

	}

	public static String infoString(EntityPersister persister) {
		StringBuffer s = new StringBuffer();
		s.append('[');
		if (persister == null) {
			s.append("<null EntityPersister>");
		}
		else {
			s.append( persister.getEntityName() );
		}
		s.append(']');
		return s.toString();
	}

	public static String collectionInfoString(
			CollectionPersister persister, 
			Serializable[] ids, 
			SessionFactoryImplementor factory
	) {
		StringBuffer s = new StringBuffer();
		s.append('[');
		if (persister==null) {
			s.append("<unreferenced>");
		}
		else {
			s.append( persister.getRole() )
				.append("#<");

			for ( int i=0; i<ids.length; i++ ) {
				s.append( persister.getKeyType().toLoggableString( ids[i], factory ) );
				if ( i<ids.length-1 ) s.append(", ");
			}
			s.append('>');
		}
		s.append(']');

		return s.toString();

	}
	
	public static String collectionInfoString(
			CollectionPersister persister, 
			Serializable id, 
			SessionFactoryImplementor factory
	) {
		StringBuffer s = new StringBuffer();
		s.append('[');
		if(persister==null) {
			s.append("<unreferenced>");
		}
		else {
			s.append( persister.getRole() );
			s.append('#');

			if (id==null) {
				s.append("<null>");
			}
			else {
				s.append( persister.getKeyType().toLoggableString(id, factory) );
			}
		}
		s.append(']');

		return s.toString();

	}

	public static String collectionInfoString(String role, Serializable id) {
		StringBuffer s = new StringBuffer();
		s.append('[');
		if(role==null) {
			s.append("<unreferenced>");
		}
		else {
			s.append(role);
			s.append('#');

			if (id==null) {
				s.append("<null>");
			}
			else {
				s.append(id);
			}
		}
		s.append(']');

		return s.toString();

	}

	public static String infoString(String entityName, String propertyName, Object key) {
		StringBuffer s = new StringBuffer()
			.append('[')
			.append(entityName)
			.append('.')
			.append(propertyName)
			.append('#');

		if (key==null) {
			s.append("<null>");
		}
		else {
			s.append(key);
		}
		s.append(']');

		return s.toString();
	}

}
