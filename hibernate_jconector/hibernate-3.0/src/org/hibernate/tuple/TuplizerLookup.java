//$Id: TuplizerLookup.java,v 1.7 2005/03/21 19:04:43 oneovthafew Exp $
package org.hibernate.tuple;

import java.io.Serializable;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;

/**
 * Stores references to the tuplizers available for a
 * "tuplizable thing" (i.e., an entity or component).
 *
 * @author Gavin King
 */
public class TuplizerLookup implements Serializable {
	
	private final Tuplizer pojoTuplizer;
	private final Tuplizer dynamicMapTuplizer;
	private final Tuplizer dom4jTuplizer;

	/**
	 * TuplizerLookup constructor.
	 *
	 * @param pojoTuplizer The POJO-based tuplizer.
	 * @param dynamicMapTuplizer The java.util.Map-based tuplizer.
	 * @param dom4jTuplizer The org.dom4j.Element-based tuplizer.
	 */
	TuplizerLookup(Tuplizer pojoTuplizer, Tuplizer dynamicMapTuplizer, Tuplizer dom4jTuplizer) {
		this.pojoTuplizer = pojoTuplizer;
		this.dynamicMapTuplizer = dynamicMapTuplizer;
		this.dom4jTuplizer = dom4jTuplizer;
	}

	/**
	 * Generate a TuplizerLookup based on the given entity mapping and metamodel
	 * definitions.
	 *
	 * @param mappedEntity The entity mapping definition.
	 * @param em The entity metamodel definition.
	 * @return A TuplizerLookup containing the appropriate Tuplizers.
	 */
	public static TuplizerLookup create(PersistentClass mappedEntity, EntityMetamodel em) {
		Tuplizer dynamicMapTuplizer = new DynamicMapTuplizer( em, mappedEntity );

		Tuplizer pojoTuplizer = mappedEntity.hasPojoRepresentation() ?
				new PojoTuplizer( em, mappedEntity ) : dynamicMapTuplizer;

		Tuplizer dom4jTuplizer = mappedEntity.hasDom4jRepresentation() ?
				new Dom4jTuplizer( em, mappedEntity ) : null;


		return new TuplizerLookup(pojoTuplizer, dynamicMapTuplizer, dom4jTuplizer);
	}

	/**
	 * Generate a TuplizerLookup based on the given component mapping definition.
	 *
	 * @param component The component mapping definition.
	 * @return A TuplizerLookup containing the appropriate Tuplizers.
	 */
	public static TuplizerLookup create(Component component) {
		PersistentClass owner = component.getOwner();

		Tuplizer dmt = new DynamicMapComponentTuplizer(component);

		Tuplizer pt = owner.hasPojoRepresentation() && component.hasPojoRepresentation() ?
				new PojoComponentTuplizer(component) : dmt;

		Tuplizer d4jt = owner.hasDom4jRepresentation() ?
				new Dom4jComponentTuplizer(component) : null;

		return new TuplizerLookup(pt, dmt, d4jt);
	}

	/**
	 * Given a supposed instance of an entity/component, guess its entity mode.
	 *
	 * @param object The supposed instance of the entity/component.
	 * @return The guessed entity mode.
	 */
	public EntityMode guessEntityMode(Object object) {
		if ( pojoTuplizer != null && pojoTuplizer.isInstance(object) ) {
			return EntityMode.POJO;
		}

		if ( dom4jTuplizer != null && dom4jTuplizer.isInstance(object) ) {
			return EntityMode.DOM4J;
		}

		if ( dynamicMapTuplizer != null && dynamicMapTuplizer.isInstance(object) ) {
			return EntityMode.MAP;
		}

		return null;   // or should we throw an exception?
	}

	/**
	 * Locate the contained tuplizer responsible for the given entity-mode.  If
	 * no such tuplizer is defined on this lookup, then return null.
	 *
	 * @param entityMode The entity-mode for which the client wants a tuplizer.
	 * @return The tuplizer, or null if not found.
	 */
	public Tuplizer getTuplizerOrNull(EntityMode entityMode) {
		Tuplizer rtn = null;
		if ( EntityMode.POJO == entityMode ) {
			rtn = pojoTuplizer;
		}
		else if ( EntityMode.DOM4J == entityMode ) {
			rtn = dom4jTuplizer;
		}
		else if ( EntityMode.MAP == entityMode ) {
			rtn = dynamicMapTuplizer;
		}

		return rtn;
	}

	/**
	 * Locate the contained tuplizer responsible for the given entity-mode.  If
	 * no such tuplizer is defined on this lookup, then an exception is thrown.
	 *
	 * @param entityMode The entity-mode for which the client wants a tuplizer.
	 * @return The tuplizer.
	 * @throws HibernateException Unable to locate the requested tuplizer.
	 */
	public Tuplizer getTuplizer(EntityMode entityMode) {
		Tuplizer rtn = null;
		if ( EntityMode.POJO == entityMode ) {
			rtn = pojoTuplizer;
		}
		else if ( EntityMode.DOM4J == entityMode ) {
			rtn = dom4jTuplizer;
		}
		else if ( EntityMode.MAP == entityMode ) {
			rtn = dynamicMapTuplizer;
		}

		if ( rtn == null ) {
			throw new HibernateException( "No tuplizer found for entity-mode [" + entityMode + "]");
		}

		return rtn;
	}

}
