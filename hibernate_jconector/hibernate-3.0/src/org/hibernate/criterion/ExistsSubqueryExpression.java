//$Id: ExistsSubqueryExpression.java,v 1.2 2005/02/12 07:19:13 steveebersole Exp $
package org.hibernate.criterion;

import org.hibernate.Criteria;

/**
 * @author Gavin King
 */
public class ExistsSubqueryExpression extends SubqueryExpression {

	protected String toLeftSqlString(Criteria criteria, CriteriaQuery outerQuery) {
		return "";
	}
	
	protected ExistsSubqueryExpression(String quantifier, DetachedCriteria dc) {
		super(null, quantifier, dc);
	}
}
