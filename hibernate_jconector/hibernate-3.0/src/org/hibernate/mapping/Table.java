//$Id: Table.java,v 1.32 2005/03/30 15:04:37 oneovthafew Exp $
package org.hibernate.mapping;

import org.apache.commons.collections.SequencedHashMap;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.Mapping;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.tool.hbm2ddl.ColumnMetadata;
import org.hibernate.tool.hbm2ddl.TableMetadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A relational table
 *
 * @author Gavin King
 */
public class Table implements RelationalModel, Serializable {

	private String name;
	private String schema;
	private String catalog;
	/**
	 * contain all columns, including what is inside PrimaryKey *
	 */
	private Map columns = new SequencedHashMap();
	private KeyValue idValue;
	private PrimaryKey primaryKey;
	private Map indexes = new HashMap();
	private Map foreignKeys = new HashMap();
	private Map uniqueKeys = new HashMap();
	private final int uniqueInteger;
	private boolean quoted;
	private static int tableCounter = 0;
	private List checkConstraints = new ArrayList();
	private String rowId;
	private String subselect;
	private boolean isAbstract;
	private boolean hasDenormalizedTables = false;

	static class ForeignKeyKey implements Serializable {
		String referencedClassName;
		List columns;

		ForeignKeyKey(List columns, String referencedClassName) {
			this.referencedClassName = referencedClassName;
			this.columns = new ArrayList();
			this.columns.addAll( columns );
		}

		public int hashCode() {
			return columns.hashCode();
		}

		public boolean equals(Object other) {
			ForeignKeyKey fkk = ( ForeignKeyKey ) other;
			return fkk.columns.equals( columns ) &&
					fkk.referencedClassName.equals( referencedClassName );
		}
	}

	public Table() {
		uniqueInteger = tableCounter++;
	}
	
	public Table(String name) {
		this();
		setName(name);
	}

	public String getQualifiedName(Dialect dialect, String defaultCatalog, String defaultSchema) {
		if ( subselect != null ) return "( " + subselect + " )";
		String quotedName = getQuotedName( dialect );
		String usedSchema = schema == null ? defaultSchema : schema;
		String usedCatalog = catalog == null ? defaultCatalog : catalog;
		return Table.qualify( usedCatalog, usedSchema, quotedName, dialect.getSchemaSeparator() );
	}

	public static String qualify(String catalog, String schema, String table, char separator) {
		StringBuffer qualifiedName = new StringBuffer();
		if ( catalog != null ) {
			qualifiedName.append( catalog );
			qualifiedName.append( separator );
			qualifiedName.append( schema != null ? schema : "" );
			qualifiedName.append( separator );
		}
		else if ( schema != null ) {
			qualifiedName.append( schema );
			qualifiedName.append( separator );
		}
		qualifiedName.append( table );
		return qualifiedName.toString();
	}

	public String getName() {
		return name;
	}

	public String getQuotedName(Dialect dialect) {
		return quoted ?
				dialect.openQuote() + name + dialect.closeQuote() :
				name;
	}

	public void setName(String name) {
		if ( name.charAt( 0 ) == '`' ) {
			quoted = true;
			this.name = name.substring( 1, name.length() - 1 );
		}
		else {
			this.name = name;
		}
	}

	/**
	 * Return the column which is identified by column provided as argument.
	 *
	 * @param column column with atleast a name.
	 * @return the underlying column or null if not inside this table. Note: the instance *can* be different than the input parameter, but the name will be the same.
	 */
	public Column getColumn(Column column) {
		Column myColumn = ( Column ) columns.get( column.getName() );

		if ( column.equals( myColumn ) ) {
			return myColumn;
		}
		else {
			return null;
		}
	}

	public Column getColumn(int n) {
		Iterator iter = columns.values().iterator();
		for ( int i = 0; i < n - 1; i++ ) iter.next();
		return ( Column ) iter.next();
	}

	public void addColumn(Column column) {
		Column old = ( Column ) columns.get( column.getName() );
		if ( old == null ) {
			columns.put( column.getName(), column );
			column.uniqueInteger = columns.size();
		}
		else {
			column.uniqueInteger = old.uniqueInteger;
		}
	}

	public int getColumnSpan() {
		return columns.size();
	}

	public Iterator getColumnIterator() {
		return columns.values().iterator();
	}

	public Iterator getIndexIterator() {
		return indexes.values().iterator();
	}

	public Iterator getForeignKeyIterator() {
		return foreignKeys.values().iterator();
	}

	public Iterator getUniqueKeyIterator() {
		return uniqueKeys.values().iterator();
	}

	public Iterator sqlAlterStrings(Dialect dialect, Mapping p, TableMetadata tableInfo, String defaultCatalog, String defaultSchema)
			throws HibernateException {

		StringBuffer root = new StringBuffer( "alter table " )
				.append( getQualifiedName( dialect, defaultCatalog, defaultSchema ) )
				.append( ' ' )
				.append( dialect.getAddColumnString() );

		Iterator iter = getColumnIterator();
		List results = new ArrayList();
		while ( iter.hasNext() ) {
			Column col = ( Column ) iter.next();

			ColumnMetadata columnInfo = tableInfo.getColumnMetadata( col.getName() );

			if ( columnInfo == null ) {
				// the column doesnt exist at all.
				StringBuffer alter = new StringBuffer( root.toString() )
						.append( ' ' )
						.append( col.getQuotedName( dialect ) )
						.append( ' ' )
						.append( col.getSqlType( dialect, p ) );
				boolean useUniqueConstraint = col.isUnique() && 
						dialect.supportsUnique() && 
						( !col.isNullable() || dialect.supportsNotNullUnique() );
				if ( useUniqueConstraint ) {
					alter.append( " unique" );
				}
				if ( col.hasCheckConstraint() && dialect.supportsColumnCheck() ) {
					alter.append( " check(" )
							.append( col.getCheckConstraint() )
							.append( ")" );
				}
				results.add( alter.toString() );
			}

		}

		return results.iterator();
	}
	
	public boolean hasPrimaryKey() {
		return getPrimaryKey()!=null;
	}

	public String sqlCreateString(Dialect dialect, Mapping p, String defaultCatalog, String defaultSchema) throws HibernateException {
		StringBuffer buf = new StringBuffer( "create table " )
				.append( getQualifiedName( dialect , defaultCatalog, defaultSchema ) )
				.append( " (" );

		boolean identityColumn = idValue != null &&
				idValue.createIdentifierGenerator(dialect, defaultCatalog, defaultSchema, null) instanceof IdentityGenerator;

		// Try to find out the name of the primary key to create it as identity if the IdentityGenerator is used
		String pkname = null;
		if ( hasPrimaryKey() && identityColumn ) {
			pkname = ( ( Column ) getPrimaryKey().getColumnIterator().next() ).getQuotedName( dialect );
		}

		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			Column col = ( Column ) iter.next();

			buf.append( col.getQuotedName( dialect ) )
					.append( ' ' );

			if ( identityColumn && col.getQuotedName( dialect ).equals( pkname ) ) {
				// to support dialects that have their own identity data type
				if ( dialect.hasDataTypeInIdentityColumn() ) {
					buf.append( col.getSqlType( dialect, p ) );
				}
				buf.append( ' ' )
						.append( dialect.getIdentityColumnString( col.getSqlTypeCode( p ) ) );
			}
			else {
				buf.append( col.getSqlType( dialect, p ) );
				if ( col.isNullable() ) {
					buf.append( dialect.getNullColumnString() );
				}
				else {
					buf.append( " not null" );
				}
			}

			boolean useUniqueConstraint = col.isUnique() && 
					( !col.isNullable() || dialect.supportsNotNullUnique() );
			if ( useUniqueConstraint ) {
				if ( dialect.supportsUnique() ) {
					buf.append( " unique" );
				}
				else {
					UniqueKey uk = getOrCreateUniqueKey( col.getQuotedName( dialect ) + '_' );
					uk.addColumn( col );
				}
			}
			if ( col.hasCheckConstraint() && dialect.supportsColumnCheck() ) {
				buf.append( " check (" )
						.append( col.getCheckConstraint() )
						.append( ")" );
			}
			if ( iter.hasNext() ) buf.append( ", " );

		}
		if ( hasPrimaryKey() ) {
			buf.append( ", " )
				.append( getPrimaryKey().sqlConstraintString( dialect ) );
		}

		if ( dialect.supportsUniqueConstraintInCreateAlterTable() ) {
			Iterator ukiter = getUniqueKeyIterator();
			while ( ukiter.hasNext() ) {
				UniqueKey uk = ( UniqueKey ) ukiter.next();
				buf.append( ", " )
				.append( uk.sqlConstraintString( dialect ) );
			}
		}
		/*Iterator idxiter = getIndexIterator();
		while ( idxiter.hasNext() ) {
			Index idx = (Index) idxiter.next();
			buf.append(',').append( idx.sqlConstraintString(dialect) );
		}*/

		if ( dialect.supportsTableCheck() ) {
			Iterator chiter = checkConstraints.iterator();
			while ( chiter.hasNext() ) {
				buf.append( ", check (" )
				.append( chiter.next() )
				.append( ')' );
			}
		}
		
		return buf.append( ')' ).append( dialect.getTableTypeString() ).toString();
	}

	public String sqlDropString(Dialect dialect, String defaultCatalog, String defaultSchema) {
		StringBuffer buf = new StringBuffer( "drop table " );
		if ( dialect.supportsIfExistsBeforeTableName() ) buf.append( "if exists " );
		buf.append( getQualifiedName( dialect , defaultCatalog, defaultSchema ) )
				.append( dialect.getCascadeConstraintsString() );
		if ( dialect.supportsIfExistsAfterTableName() ) buf.append( " if exists" );
		return buf.toString();
	}

	public PrimaryKey getPrimaryKey() {
		return primaryKey;
	}

	public void setPrimaryKey(PrimaryKey primaryKey) {
		this.primaryKey = primaryKey;
	}

	/*public Index createIndex(String indexName, List indexColumns) {
		if ( indexName == null ) indexName = "IX" + uniqueColumnString( indexColumns.iterator() );
		Index idx = getOrCreateIndex( indexName );
		idx.addColumns( indexColumns.iterator() );
		return idx;
	}*/

	public Index getOrCreateIndex(String indexName) {
		Index index = ( Index ) indexes.get( indexName );

		if ( index == null ) {
			index = new Index();
			index.setName( indexName );
			index.setTable( this );
			indexes.put( indexName, index );
		}

		return index;
	}

	public Index getIndex(String indexName) {
		return ( Index ) indexes.get( indexName );
	}

	public Index addIndex(Index index) {
		Index current = ( Index ) indexes.get( index.getName() );
		if ( current != null ) {
			throw new MappingException( "Index " + index.getName() + " already exists!" );
		}
		indexes.put( index.getName(), index );
		return index;
	}

	public UniqueKey addUniqueKey(UniqueKey uniqueKey) {
		UniqueKey current = ( UniqueKey ) uniqueKeys.get( uniqueKey.getName() );
		if ( current != null ) {
			throw new MappingException( "UniqueKey " + uniqueKey.getName() + " already exists!" );
		}
		uniqueKeys.put( uniqueKey.getName(), uniqueKey );
		return uniqueKey;
	}

	public UniqueKey createUniqueKey(List keyColumns) {
		String keyName = "UK" + uniqueColumnString( keyColumns.iterator() );
		UniqueKey uk = getOrCreateUniqueKey( keyName );
		uk.addColumns( keyColumns.iterator() );
		return uk;
	}

	public UniqueKey getUniqueKey(String keyName) {
		return (UniqueKey) uniqueKeys.get( keyName );
	}
	
	public UniqueKey getOrCreateUniqueKey(String keyName) {
		UniqueKey uk = ( UniqueKey ) uniqueKeys.get( keyName );

		if ( uk == null ) {
			uk = new UniqueKey();
			uk.setName( keyName );
			uk.setTable( this );
			uniqueKeys.put( keyName, uk );
		}
		return uk;
	}

	public void createForeignKeys() {
	}

	public ForeignKey createForeignKey(String keyName, List keyColumns, String referencedEntityName) {

		Object key = new ForeignKeyKey( keyColumns, referencedEntityName );

		ForeignKey fk = ( ForeignKey ) foreignKeys.get( key );
		if ( fk == null ) {
			fk = new ForeignKey();
			if ( keyName != null ) {
				fk.setName( keyName );
			}
			else {
				fk.setName( "FK" + uniqueColumnString( keyColumns.iterator(), referencedEntityName ) ); 
				//TODO: add referencedClass to disambiguate to FKs on the same
				//      columns, pointing to different tables
			}
			fk.setTable( this );
			foreignKeys.put( key, fk );
			fk.setReferencedEntityName( referencedEntityName );
			fk.addColumns( keyColumns.iterator() );
		}

		if ( keyName != null ) fk.setName( keyName );

		return fk;
	}
	
	public String uniqueColumnString(Iterator iterator) {
		return uniqueColumnString(iterator, null);
	}

	public String uniqueColumnString(Iterator iterator, String referencedEntityName) {
		int result = 0;
		if (referencedEntityName!=null) result += referencedEntityName.hashCode();
		while ( iterator.hasNext() ) result += iterator.next().hashCode();
		return ( Integer.toHexString( name.hashCode() ) + Integer.toHexString( result ) ).toUpperCase();
	}


	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getCatalog() {
		return catalog;
	}

	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	public int getUniqueInteger() {
		return uniqueInteger;
	}

	public void setIdentifierValue(KeyValue idValue) {
		this.idValue = idValue;
	}

	public boolean isQuoted() {
		return quoted;
	}

	public void setQuoted(boolean quoted) {
		this.quoted = quoted;
	}

	public void addCheckConstraint(String constraint) {
		checkConstraints.add( constraint );
	}

	public boolean containsColumn(Column column) {
		return columns.containsValue( column );
	}

	public String getRowId() {
		return rowId;
	}

	public void setRowId(String rowId) {
		this.rowId = rowId;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer().append( getClass().getName() )
			.append('(');
		if ( getCatalog()!=null ) buf.append( getCatalog() );
		if ( getSchema()!=null ) buf.append( getSchema() );
		buf.append( getName() ).append(')');
		return buf.toString();
	}

	public String getSubselect() {
		return subselect;
	}

	public void setSubselect(String subselect) {
		this.subselect = subselect;
	}

	public boolean isSubselect() {
		return subselect != null;
	}

	public boolean isAbstractUnionTable() {
		return hasDenormalizedTables && isAbstract;
	}

	void setHasDenormalizedTables() {
		hasDenormalizedTables = true;
	}
	
	public boolean hasDenormalizedTables() {
		return hasDenormalizedTables;
	}

	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public boolean isPhysicalTable() {
		return !isSubselect() && !isAbstractUnionTable();
	}

}
