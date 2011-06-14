//$Id: ForeignKey.java,v 1.11 2005/03/04 12:19:53 oneovthafew Exp $
package org.hibernate.mapping;

import java.util.Iterator;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;

/**
 * A foreign key constraint
 * @author Gavin King
 */
public class ForeignKey extends Constraint {

	private Table referencedTable;
	private String referencedEntityName;
	private boolean cascadeDeleteEnabled;    
    
	public String sqlConstraintString(Dialect dialect, String constraintName, String defaultCatalog, String defaultSchema) {
		String[] cols = new String[ getColumnSpan() ];
		String[] refcols = new String[ getColumnSpan() ];
		int i=0;
		Iterator refiter = referencedTable.getPrimaryKey().getColumnIterator();
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			cols[i] = ( (Column) iter.next() ).getQuotedName(dialect);
			refcols[i] = ( (Column) refiter.next() ).getQuotedName(dialect);
			i++;
		}
		String result = dialect.getAddForeignKeyConstraintString(
			constraintName, cols, referencedTable.getQualifiedName(dialect, defaultCatalog, defaultSchema), refcols
		);
		return cascadeDeleteEnabled ? 
			result + " on delete cascade" : 
			result;
	}

	public Table getReferencedTable() {
		return referencedTable;
	}

	private void appendColumns(StringBuffer buf, Iterator columns) {
		while( columns.hasNext() ) {
			Column column = (Column) columns.next();
			buf.append( column.getName() );
			if ( columns.hasNext() ) buf.append(",");
		}
	}

	public void setReferencedTable(Table referencedTable) throws MappingException {
		alignColumns(referencedTable); // TODO: possibly remove to allow more piecemal building of a foreignkey.  
		
		this.referencedTable = referencedTable;
	}

	/**
	 * Validates that columnspan of the foreignkey and the primarykey is the same.
	 * 
	 * Furthermore it aligns the length of the underlying tables.
	 * @param referencedTable
	 */
	/*public void alignColumns() {
		alignColumns(referencedTable);
	}*/
	
	/**
	 * Validates that columnspan of the foreignkey and the primarykey is the same.
	 * 
	 * Furthermore it aligns the length of the underlying tables.
	 * @param referencedTable
	 */
	private void alignColumns(Table referencedTable) {
		if ( referencedTable.getPrimaryKey().getColumnSpan()!=getColumnSpan() ) {
			StringBuffer sb = new StringBuffer();
			sb.append("Foreign key (")
                .append( getName() + ":")
				.append( getTable().getName() )
				.append(" [");
			appendColumns( sb, getColumnIterator() );
			sb.append("])")
				.append(") must have same number of columns as the referenced primary key (")
				.append( referencedTable.getName() )
				.append(" [");
			appendColumns( sb, referencedTable.getPrimaryKey().getColumnIterator() );
			sb.append("])");
			throw new MappingException( sb.toString() );
		}
		
		Iterator fkCols = getColumnIterator();
		Iterator pkCols = referencedTable.getPrimaryKey().getColumnIterator();
		while ( pkCols.hasNext() ) {
			( (Column) fkCols.next() ).setLength( ( (Column) pkCols.next() ).getLength() );
		}

	}

	public String getReferencedEntityName() {
		return referencedEntityName;
	}

	public void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName;
	}

	public String sqlDropString(Dialect dialect, String defaultCatalog, String defaultSchema) {
		return "alter table " + 
			getTable().getQualifiedName(dialect, defaultCatalog, defaultSchema) + 
			dialect.getDropForeignKeyString() + 
			getName();
	}

	public boolean isCascadeDeleteEnabled() {
		return cascadeDeleteEnabled;
	}

	public void setCascadeDeleteEnabled(boolean cascadeDeleteEnabled) {
		this.cascadeDeleteEnabled = cascadeDeleteEnabled;
	}
	
	public boolean isPhysicalConstraint() {
		return referencedTable.isPhysicalTable() && 
				getTable().isPhysicalTable() && 
				!referencedTable.hasDenormalizedTables();
	}
}
