//$Id: Selectable.java,v 1.2 2004/07/23 17:18:15 oneovthafew Exp $
package org.hibernate.mapping;

import org.hibernate.dialect.Dialect;

public interface Selectable {
	public String getAlias();
	public String getAlias(Table table);
	public boolean isFormula();
	public String getTemplate(Dialect dialect);
	public String getText(Dialect dialect);
	public String getText();
}
