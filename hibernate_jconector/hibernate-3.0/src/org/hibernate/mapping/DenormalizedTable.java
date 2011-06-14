//$Id: DenormalizedTable.java,v 1.3 2004/08/22 15:43:15 oneovthafew Exp $
package org.hibernate.mapping;

import java.util.Iterator;

import org.hibernate.util.JoinedIterator;

/**
 * @author Gavin King
 */
public class DenormalizedTable extends Table {
	
	private final Table includedTable;
	
	public DenormalizedTable(Table includedTable) {
		this.includedTable = includedTable;
		includedTable.setHasDenormalizedTables();
	}
	
	public void createForeignKeys() {
		includedTable.createForeignKeys();
		Iterator iter = includedTable.getForeignKeyIterator();
		while ( iter.hasNext() ) {
			ForeignKey fk = (ForeignKey) iter.next();
			this.createForeignKey( 
					fk.getName() + Integer.toHexString( getName().hashCode() ), 
					fk.getColumns(), 
					fk.getReferencedEntityName() 
			);
		}
	}


	public Iterator getColumnIterator() {
		return new JoinedIterator(
			includedTable.getColumnIterator(),
			super.getColumnIterator()
		);
	}


	public boolean containsColumn(Column column) {
		return super.containsColumn(column) || includedTable.containsColumn(column);
	}

	public PrimaryKey getPrimaryKey() {
		return includedTable.getPrimaryKey();
	}
}
