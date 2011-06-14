//$Id: UniqueKey.java,v 1.6 2005/02/14 10:59:35 maxcsaucdk Exp $
package org.hibernate.mapping;

import java.util.Iterator;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.Mapping;
import org.hibernate.util.StringHelper;

/**
 * A relational unique key constraint
 * @author Gavin King
 */
public class UniqueKey extends Constraint {

	public String sqlConstraintString(Dialect dialect) {
		StringBuffer buf = new StringBuffer("unique (");
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			buf.append( ( (Column) iter.next() ).getQuotedName(dialect) );
			if ( iter.hasNext() ) buf.append(", ");
		}
		return buf.append(')').toString();
	}

	public String sqlConstraintString(Dialect dialect, String constraintName, String defaultCatalog, String defaultSchema) {
		StringBuffer buf = new StringBuffer(
			dialect.getAddPrimaryKeyConstraintString(constraintName)
		).append('(');
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			buf.append( ( (Column) iter.next() ).getQuotedName(dialect) );
			if ( iter.hasNext() ) buf.append(", ");
		}
		return StringHelper.replace( buf.append(')').toString(), "primary key", "unique" ); //TODO: improve this hack!
	}
	
	public String sqlCreateString(Dialect dialect, Mapping p, String defaultCatalog, String defaultSchema) {
        if ( dialect.supportsUniqueConstraintInCreateAlterTable() ) {
            return super.sqlCreateString(dialect, p, defaultCatalog, defaultSchema);
        } 
        else {
            return Index.buildSqlCreateIndexString(dialect, getName(), getTable(), getColumnIterator(), true, defaultCatalog, defaultSchema);
        }
    }
    
    public String sqlDropString(Dialect dialect, String defaultCatalog, String defaultSchema) {
        if( dialect.supportsUniqueConstraintInCreateAlterTable() ) {
            return super.sqlDropString(dialect, defaultCatalog, defaultSchema);
        } 
        else {
            return Index.buildSqlDropIndexString(dialect, getTable(), getName(), defaultCatalog, defaultSchema);
        }
    }
	
}
