//$Id: AvgProjection.java,v 1.4 2005/02/12 04:32:35 oneovthafew Exp $
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.type.Type;

/**
 * @author Gavin King
 */
public class AvgProjection extends AggregateProjection {

	public AvgProjection(String propertyName) {
		super("avg", propertyName);
	}
	
	public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {
		return new Type[] { Hibernate.DOUBLE };
	}
}
