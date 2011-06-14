//$Id: NotEmptyExpression.java,v 1.8 2005/02/13 11:49:56 oneovthafew Exp $
package org.hibernate.criterion;


import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.engine.TypedValue;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.sql.ConditionFragment;

/**
 * @author Gavin King
 */
public class NotEmptyExpression implements Criterion {
	
	private final String propertyName;
	
	private static final TypedValue[] NO_VALUES = new TypedValue[0];

	protected NotEmptyExpression(String propertyName) {
		this.propertyName = propertyName;
	}

	public String toString() {
		return propertyName + " is not empty";
	}
	
	//TODO: code duplication from EmptyExpression

	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {
		String role = criteriaQuery.getEntityName(criteria, propertyName) + 
			'.' +  
			criteriaQuery.getPropertyName(propertyName);
		QueryableCollection cp = (QueryableCollection) criteriaQuery.getFactory().getCollectionPersister(role);
		//String[] fk = StringHelper.qualify( "collection_", cp.getKeyColumnNames() );
		String[] fk = cp.getKeyColumnNames();
		String[] pk = ( (Loadable) cp.getOwnerEntityPersister() ).getIdentifierColumnNames(); //TODO: handle property-ref
		return "exists (select 1 from " +
			cp.getTableName() +
			//" collection_ where " +
			" where " +
			new ConditionFragment()
				.setTableAlias( criteriaQuery.getSQLAlias(criteria, propertyName) )
				.setCondition(pk, fk)
				.toFragmentString() +
			")";
	}

	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) 
	throws HibernateException {
		return NO_VALUES;
	}

}
