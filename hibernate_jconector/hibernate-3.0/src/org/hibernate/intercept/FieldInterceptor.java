//$Id: FieldInterceptor.java,v 1.9 2005/02/12 07:19:24 steveebersole Exp $
package org.hibernate.intercept;

import java.io.Serializable;
import java.util.Set;

import net.sf.cglib.transform.impl.InterceptFieldCallback;

import org.hibernate.LazyInitializationException;
import org.hibernate.engine.SessionImplementor;

/**
 * A field-level interceptor that initializes lazily fetched properties.
 * This interceptor can be attached to classes instrumented by CGLIB.
 * Note that this implementation assumes that the instance variable
 * name is the same as the name of the persistent property that must
 * be loaded.
 *
 * @author Gavin King
 */
public class FieldInterceptor implements InterceptFieldCallback, Serializable {

	private transient SessionImplementor session;
	private Set uninitializedFields;
	private final String entityName;
	private transient boolean initializing;

	public FieldInterceptor(SessionImplementor session, String entityName, Set uninitializedFields) {
		this.session = session;
		this.entityName = entityName;
		this.uninitializedFields = uninitializedFields;
	}

	public void setSession(SessionImplementor session) {
		this.session = session;
	}

	public boolean isInitialized() {
		return uninitializedFields == null || uninitializedFields.size() == 0;
	}

	public boolean isInitialized(String field) {
		return uninitializedFields == null || !uninitializedFields.contains( field );
	}

	private Object intercept(Object target, String fieldName, Object value) {
		if ( initializing ) return value;

		if ( uninitializedFields != null && uninitializedFields.contains( fieldName ) ) {
			if ( session == null ) {
				throw new LazyInitializationException( "entity with lazy properties is not associated with a session" );
			}
			else if ( !session.isOpen() || !session.isConnected() ) {
				throw new LazyInitializationException( "session is not connected" );
			}

			final Object result;
			initializing = true;
			try {
				result = ( ( LazyPropertyInitializer ) session.getFactory().getEntityPersister( entityName ) )
						.initializeLazyProperty( fieldName, target, session );
			}
			finally {
				initializing = false;
			}
			uninitializedFields = null; //let's assume that there is only one lazy fetch group, for now!
			return result;
		}
		else {
			return value;
		}
	}

	public boolean readBoolean(Object target, String name, boolean oldValue) {
		return ( ( Boolean ) intercept( target, name, oldValue  ? Boolean.TRUE : Boolean.FALSE ) )
				.booleanValue();
	}

	public byte readByte(Object target, String name, byte oldValue) {
		return ( ( Byte ) intercept( target, name, new Byte( oldValue ) ) ).byteValue();
	}

	public char readChar(Object target, String name, char oldValue) {
		return ( ( Character ) intercept( target, name, new Character( oldValue ) ) )
				.charValue();
	}

	public double readDouble(Object target, String name, double oldValue) {
		return ( ( Double ) intercept( target, name, new Double( oldValue ) ) )
				.doubleValue();
	}

	public float readFloat(Object target, String name, float oldValue) {
		return ( ( Float ) intercept( target, name, new Float( oldValue ) ) )
				.floatValue();
	}

	public int readInt(Object target, String name, int oldValue) {
		return ( ( Integer ) intercept( target, name, new Integer( oldValue ) ) )
				.intValue();
	}

	public long readLong(Object target, String name, long oldValue) {
		return ( ( Long ) intercept( target, name, new Long( oldValue ) ) ).longValue();
	}

	public short readShort(Object target, String name, short oldValue) {
		return ( ( Short ) intercept( target, name, new Short( oldValue ) ) )
				.shortValue();
	}

	public Object readObject(Object target, String name, Object oldValue) {
		return intercept( target, name, oldValue );
	}

	public boolean writeBoolean(Object target, String name, boolean oldValue, boolean newValue) {
		intercept( target, name, oldValue ? Boolean.TRUE : Boolean.FALSE );
		return newValue;
	}

	public byte writeByte(Object target, String name, byte oldValue, byte newValue) {
		intercept( target, name, new Byte( oldValue ) );
		return newValue;
	}

	public char writeChar(Object target, String name, char oldValue, char newValue) {
		intercept( target, name, new Character( oldValue ) );
		return newValue;
	}

	public double writeDouble(Object target, String name, double oldValue, double newValue) {
		intercept( target, name, new Double( oldValue ) );
		return newValue;
	}

	public float writeFloat(Object target, String name, float oldValue, float newValue) {
		intercept( target, name, new Float( oldValue ) );
		return newValue;
	}

	public int writeInt(Object target, String name, int oldValue, int newValue) {
		intercept( target, name, new Integer( oldValue ) );
		return newValue;
	}

	public long writeLong(Object target, String name, long oldValue, long newValue) {
		intercept( target, name, new Long( oldValue ) );
		return newValue;
	}

	public short writeShort(Object target, String name, short oldValue, short newValue) {
		intercept( target, name, new Short( oldValue ) );
		return newValue;
	}

	public Object writeObject(Object target, String name, Object oldValue, Object newValue) {
		intercept( target, name, oldValue );
		return newValue;
	}

	public String toString() {
		return "FieldInterceptor for " + entityName + " unitializedFields=" + uninitializedFields;
	}
}