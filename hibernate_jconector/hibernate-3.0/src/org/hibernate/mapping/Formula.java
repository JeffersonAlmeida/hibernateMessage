//$Id: Formula.java,v 1.7 2005/02/12 07:19:26 steveebersole Exp $
package org.hibernate.mapping;

import java.io.Serializable;

import org.hibernate.dialect.Dialect;
import org.hibernate.sql.Template;

/**
 * A formula is a derived column value
 * @author Gavin King
 */
public class Formula implements Selectable, Serializable {
	private static int formulaUniqueInteger=0;

	private String formula;
	private int uniqueInteger;

	public Formula() {
		uniqueInteger = formulaUniqueInteger++;
	}

	public String getTemplate(Dialect dialect) {
		return Template.renderWhereStringTemplate(formula, dialect);
	}
	public String getText(Dialect dialect) {
		return getFormula();
	}
	public String getText() {
		return getFormula();
	}
	public String getAlias() {
		return "formula" + Integer.toString(uniqueInteger) + '_';
	}
	public String getAlias(Table table) {
		return getAlias();
	}
	public String getFormula() {
		return formula;
	}
	public void setFormula(String string) {
		formula = string;
	}
	public boolean isFormula() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return this.getClass().getName() + "( " + formula + " )";
	}
}
