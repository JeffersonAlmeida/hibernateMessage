//$Id: Subqueries.java,v 1.3 2005/02/12 07:19:14 steveebersole Exp $
package org.hibernate.criterion;

/**
 * Factory class for criterion instances that represent expressions
 * involving subqueries.
 * 
 * @see Restriction
 * @see Projection
 * @see org.hibernate.Criteria
 * @author Gavin King
 */
public class Subqueries {
		
	public static Criterion exists(DetachedCriteria dc) {
		return new ExistsSubqueryExpression("exists", dc);
	}
	
	public static Criterion notExists(DetachedCriteria dc) {
		return new ExistsSubqueryExpression("notExists", dc);
	}
	
	public static Criterion propertyEqAll(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, "=", "all", dc);
	}
	
	public static Criterion propertyIn(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, "in", null, dc);
	}
	
	public static Criterion propertyNotIn(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, "not in", null, dc);
	}
	
	public static Criterion propertyEq(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, "=", null, dc);
	}
	
	public static Criterion propertyNe(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, "<>", null, dc);
	}
	
	public static Criterion propertyGt(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, ">", null, dc);
	}
	
	public static Criterion propertyLt(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, "<", null, dc);
	}
	
	public static Criterion propertyGe(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, ">=", null, dc);
	}
	
	public static Criterion propertyLe(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, "<=", null, dc);
	}
	
	public static Criterion propertyGtAll(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, ">", "all", dc);
	}
	
	public static Criterion propertyLtAll(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, "<", "all", dc);
	}
	
	public static Criterion propertyGeAll(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, ">=", "all", dc);
	}
	
	public static Criterion propertyLeAll(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, "<=", "all", dc);
	}
	
	public static Criterion propertyGtSome(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, ">", "some", dc);
	}
	
	public static Criterion propertyLtSome(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, "<", "some", dc);
	}
	
	public static Criterion propertyGeSome(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, ">=", "some", dc);
	}
	
	public static Criterion propertyLeSome(String propertyName, DetachedCriteria dc) {
		return new PropertySubqueryExpression(propertyName, "<=", "some", dc);
	}
	
	public static Criterion eqAll(String value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, "=", "all", dc);
	}
	
	public static Criterion in(String value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, "in", null, dc);
	}
	
	public static Criterion notIn(String value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, "not in", null, dc);
	}
	
	public static Criterion eq(String value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, "=", null, dc);
	}
	
	public static Criterion gt(String value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, ">", null, dc);
	}
	
	public static Criterion lt(String value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, "<", null, dc);
	}
	
	public static Criterion ge(String value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, ">=", null, dc);
	}
	
	public static Criterion le(String value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, "<=", null, dc);
	}
	
	public static Criterion ne(String value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, "<>", null, dc);
	}
	
	public static Criterion gtAll(String value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, ">", "all", dc);
	}
	
	public static Criterion ltAll(String value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, "<", "all", dc);
	}
	
	public static Criterion geAll(String value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, ">=", "all", dc);
	}
	
	public static Criterion leAll(String value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, "<=", "all", dc);
	}
	
	public static Criterion gtSome(String value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, ">", "some", dc);
	}
	
	public static Criterion ltSome(String value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, "<", "some", dc);
	}
	
	public static Criterion geSome(String value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, ">=", "some", dc);
	}
	
	public static Criterion leSome(String value, DetachedCriteria dc) {
		return new SimpleSubqueryExpression(value, "<=", "some", dc);
	}
	

}
