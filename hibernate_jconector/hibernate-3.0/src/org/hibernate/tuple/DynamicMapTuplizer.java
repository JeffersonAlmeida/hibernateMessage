// $Id: DynamicMapTuplizer.java,v 1.8 2005/02/28 20:24:14 epbernard Exp $
package org.hibernate.tuple;

import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.PropertyAccessorFactory;
import org.hibernate.property.Setter;
import org.hibernate.proxy.MapProxyFactory;
import org.hibernate.proxy.ProxyFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of DynamicMapTuplizer.
 *
 * @author Steve Ebersole
 */
public class DynamicMapTuplizer extends AbstractTuplizer {

	static final Log log = LogFactory.getLog( DynamicMapTuplizer.class );

	DynamicMapTuplizer(EntityMetamodel entityMetamodel, PersistentClass mappedEntity) {
		super(entityMetamodel, mappedEntity);
	}
	
	public EntityMode getEntityMode() {
		return EntityMode.MAP;
	}

	private PropertyAccessor buildPropertyAccessor(Property mappedProperty) {
		if ( mappedProperty.isBackRef() ) {
			return mappedProperty.getPropertyAccessor(null);
		}
		else {
			return PropertyAccessorFactory.getDynamicMapPropertyAccessor();
		}
	}

	protected Getter buildPropertyGetter(Property mappedProperty, PersistentClass mappedEntity) {
		return buildPropertyAccessor(mappedProperty).getGetter( null, mappedProperty.getName() );
	}

	protected Setter buildPropertySetter(Property mappedProperty, PersistentClass mappedEntity) {
		return buildPropertyAccessor(mappedProperty).getSetter( null, mappedProperty.getName() );
	}

	protected Instantiator buildInstantiator(PersistentClass mappingInfo) {
        return new DynamicMapInstantiator( getEntityName() );
	}

	protected ProxyFactory buildProxyFactory(PersistentClass mappingInfo, Getter idGetter, Setter idSetter) {

		ProxyFactory pf = new MapProxyFactory();
		try {
			//TODO: design new lifecycle for ProxyFactory
			pf.postInstantiate(
					getEntityName(),
					null,
					null,
					null,
					null,
					null
			);
		}
		catch ( HibernateException he ) {
			log.warn( "could not create proxy factory for:" + getEntityName(), he );
			pf = null;
		}
		return pf;
	}

	public Class getMappedClass() {
		return Map.class;
	}

	public Class getConcreteProxyClass() {
		return Map.class;
	}

	public boolean isLazyPropertyLoadingAvailable() {
		return false;
	}
}
