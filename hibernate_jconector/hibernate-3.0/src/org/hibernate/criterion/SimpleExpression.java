//$Id: SimpleExpression.java,v 1.10 2005/02/12 07:19:14 steveebersole Exp $
package org.hibernate.criterion;


import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.engine.TypedValue;
import org.hibernate.util.StringHelper;

/**
 * superclass for "simple" comparisons (with SQL binary operators)
 * @author Gavin King
 */
public class SimpleExpression implements Criterion {

	private final String propertyName;
	private final Object value;
	private boolean ignoreCase;
	private final String op;

	protected SimpleExpression(String propertyName, Object value, String op) {
		this.propertyName = propertyName;
		this.value = value;
		this.op = op;
	}

	protected SimpleExpression(String propertyName, Object value, String op, boolean ignoreCase) {
		this.propertyName = propertyName;
		this.value = value;
		this.ignoreCase = ignoreCase;
		this.op = op;
	}

	public SimpleExpression ignoreCase() {
		ignoreCase = true;
		return this;
	}

	public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {

		String[] columns = criteriaQuery.getColumnsUsingProjection(criteria, propertyName);
		if (ignoreCase) {
			if ( columns.length!=1 ) throw new HibernateException(
				"case insensitive expression may only be applied to single-column properties: " +
				propertyName
			);
			return new StringBuffer()
				.append( criteriaQuery.getFactory().getDialect().getLowercaseFunction() )
				.append('(')
				.append( columns[0] )
				.append(')')
				.append( getOp() )
				.append("?")
				.toString();
		}
		else {
			String result = StringHelper.join(
				" and ",
				StringHelper.suffix( columns, getOp() + "?" )
			);
			if (columns.length>1) result = '(' + result + ')';
			return result;
		}

		//TODO: get SQL rendering out of this package!
	}

	public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {
		Object icvalue = ignoreCase ? value.toString().toLowerCase() : value;
		return new TypedValue[] { criteriaQuery.getTypedValue(criteria, propertyName, icvalue) };
	}

	public String toString() {
		return propertyName + getOp() + value;
	}

	protected final String getOp() {
		return op;
	}

}
