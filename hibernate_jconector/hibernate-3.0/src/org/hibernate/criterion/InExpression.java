//$Id: InExpression.java,v 1.14 2005/02/18 03:47:27 oneovthafew Exp $
package org.hibernate.criterion;

import java.util.ArrayList;



import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;


import org.hibernate.engine.TypedValue;

import org.hibernate.type.AbstractComponentType;
import org.hibernate.type.Type;
import org.hibernate.util.StringHelper;

/**
 * Constrains the property to a specified list of values
 * @author Gavin King
 */
public class InExpression implements Criterion {

	private final String propertyName;
	private final Object[] values;

	protected InExpression(String propertyName, Object[] values) {
		this.propertyName = propertyName;
		this.values = values;
	}

	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {
		String params;
		if ( values.length>0 ) {
			params = StringHelper.repeat( "?, ", values.length-1 );
			params += "?";
		}
		else {
			params = "";
		}
		String condition = " in (" + params + ')';
		return StringHelper.join(
			" and ",
			StringHelper.suffix(
					criteriaQuery.getColumnsUsingProjection(criteria, propertyName),
					condition
			)
		);

		//TODO: get SQL rendering out of this package!
	}

	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		ArrayList list = new ArrayList();
		Type type = criteriaQuery.getTypeUsingProjection(criteria, propertyName);
		if ( type.isComponentType() ) {
			AbstractComponentType actype = (AbstractComponentType) type;
			Type[] types = actype.getSubtypes();
			for ( int i=0; i<types.length; i++ ) {
				for ( int j=0; j<values.length; j++ ) {
					Object subval = values[j]==null ? 
						null : 
						actype.getPropertyValues( values[j], EntityMode.POJO )[i];
					list.add( new TypedValue( types[i], subval, EntityMode.POJO ) );
				}
			}
		}
		else {
			for ( int j=0; j<values.length; j++ ) {
				list.add( new TypedValue( type, values[j], EntityMode.POJO ) );
			}
		}
		return (TypedValue[]) list.toArray( new TypedValue[ list.size() ] );
	}

	public String toString() {
		return propertyName + " in (" + StringHelper.toString(values) + ')';
	}

}
