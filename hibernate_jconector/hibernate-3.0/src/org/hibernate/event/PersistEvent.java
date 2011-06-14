//$Id: PersistEvent.java,v 1.2 2005/02/22 03:09:31 oneovthafew Exp $
package org.hibernate.event;

import org.hibernate.engine.SessionImplementor;


/** 
 * An event class for persist()
 *
 * @author Gavin King
 */
public class PersistEvent extends AbstractEvent {

	private Object object;
	private String entityName;

	public PersistEvent(String entityName, Object original, SessionImplementor source) {
		this(original, source);
		this.entityName = entityName;
	}

	public PersistEvent(Object object, SessionImplementor source) {
		super(source);
		if ( object == null ) {
			throw new IllegalArgumentException(
					"attempt to create create event with null entity"
			);
		}
		this.object = object;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

}
