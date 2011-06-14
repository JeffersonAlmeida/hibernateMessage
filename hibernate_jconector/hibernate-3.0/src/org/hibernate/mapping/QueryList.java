//$Id: QueryList.java,v 1.6 2005/02/12 07:19:26 steveebersole Exp $
package org.hibernate.mapping;

import java.util.Iterator;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;
import org.hibernate.type.QueryType;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.EmptyIterator;

/**
 * @author Gavin King
 */
public class QueryList implements Value {
	
	private Table table;
	private String queryName;
	
	public QueryList(Table table) {
		this.table = table;
	}

	public int getColumnSpan() {
		return 0;
	}

	public Iterator getColumnIterator() {
		return EmptyIterator.INSTANCE;
	}

	public Type getType() throws MappingException {
		return new QueryType(queryName);
	}

	public FetchMode getFetchMode() {
		return FetchMode.SELECT;
	}

	public Table getTable() {
		return table;
	}

	public boolean hasFormula() {
		return false;
	}

	public boolean isAlternateUniqueKey() {
		return false;
	}

	public boolean isNullable() {
		return true;
	}

	public void createForeignKey() throws MappingException {
	}

	public boolean isSimpleValue() {
		return false;
	}

	public boolean isValid(Mapping mapping) throws MappingException {
		return true;
	}

	public void setTypeUsingReflection(String className, String propertyName)
			throws MappingException {
	}

	public String getQueryName() {
		return queryName;
	}

	public void setQueryName(String queryName) {
		this.queryName = queryName;
	}
	
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
	
	public boolean[] getColumnInsertability() {
		return ArrayHelper.EMPTY_BOOLEAN_ARRAY;
	}
	
	public boolean[] getColumnUpdateability() {
		return ArrayHelper.EMPTY_BOOLEAN_ARRAY;
	}
}
