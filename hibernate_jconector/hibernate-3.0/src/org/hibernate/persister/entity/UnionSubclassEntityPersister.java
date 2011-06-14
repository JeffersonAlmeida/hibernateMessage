//$Id: UnionSubclassEntityPersister.java,v 1.4 2005/03/30 16:51:33 oneovthafew Exp $
package org.hibernate.persister.entity;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.cache.CacheConcurrencyStrategy;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.IncrementGenerator;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Table;
import org.hibernate.sql.SelectFragment;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.JoinedIterator;

/**
 * Implementation of the "table-per-concrete-class" or "roll-down" mapping 
 * strategy for an entity and its inheritence hierarchy.
 *
 * @author Gavin King
 */
public class UnionSubclassEntityPersister extends BasicEntityPersister {

	// the class hierarchy structure
	private final String subquery;
	private final String tableName;
	//private final String rootTableName;
	private final String[] subclassClosure;
	private final String[] spaces;
	private final String[] subclassSpaces;
	private final String discriminatorSQLValue;
	private final Map subclassByDiscriminatorValue = new HashMap();

	//INITIALIZATION:

	public UnionSubclassEntityPersister(
			final PersistentClass persistentClass, 
			final CacheConcurrencyStrategy cache, 
			final SessionFactoryImplementor factory,
			final Mapping mapping)
	throws HibernateException {

		super(persistentClass, cache, factory);
		
		if ( getIdentifierGenerator() instanceof IdentityGenerator ) {
			throw new MappingException(
					"Cannot use identity column key generation with <union-subclass> mapping for: " + 
					getEntityName() 
			);
		}
		if ( getIdentifierGenerator() instanceof IncrementGenerator ) {
			throw new MappingException(
					"Cannot use increment key generation with <union-subclass> mapping for: " + 
					getEntityName() 
			);
		}

		// TABLE

		tableName = persistentClass.getTable().getQualifiedName( 
				factory.getDialect(), 
				factory.getSettings().getDefaultCatalogName(), 
				factory.getSettings().getDefaultSchemaName() 
		);
		/*rootTableName = persistentClass.getRootTable().getQualifiedName( 
				factory.getDialect(), 
				factory.getDefaultCatalog(), 
				factory.getDefaultSchema() 
		);*/

		//Custom SQL
		insertCallable = new boolean[] { persistentClass.isCustomInsertCallable() };
		updateCallable = new boolean[] { persistentClass.isCustomUpdateCallable() };
		deleteCallable = new boolean[] { persistentClass.isCustomDeleteCallable() };
		
		customSQLInsert = new String[] { persistentClass.getCustomSQLInsert() };
		customSQLUpdate = new String[] { persistentClass.getCustomSQLUpdate() };
		customSQLDelete = new String[] { persistentClass.getCustomSQLDelete() };
		
		discriminatorSQLValue = String.valueOf( persistentClass.getSubclassId() );

		// PROPERTIES

		int subclassSpan = persistentClass.getSubclassSpan() + 1;
		subclassClosure = new String[subclassSpan];
		subclassClosure[0] = getEntityName();

		// SUBCLASSES
		subclassByDiscriminatorValue.put( 
				new Integer( persistentClass.getSubclassId() ), 
				persistentClass.getEntityName() 
		);
		if ( persistentClass.isPolymorphic() ) {
			Iterator iter = persistentClass.getSubclassIterator();
			int k=1;
			while ( iter.hasNext() ) {
				Subclass sc = (Subclass) iter.next();
				subclassClosure[k++] = sc.getEntityName();
				subclassByDiscriminatorValue.put( new Integer( sc.getSubclassId() ), sc.getEntityName() );
			}
		}
		
		//SPACES
		//TODO: i'm not sure, but perhaps we should exclude
		//      abstract denormalized tables?
		
		int spacesSize = 1 + persistentClass.getSynchronizedTables().size();
		spaces = new String[spacesSize];
		spaces[0] = tableName;
		Iterator iter = persistentClass.getSynchronizedTables().iterator();
		for ( int i=1; i<spacesSize; i++ ) {
			spaces[i] = (String) iter.next();
		}
		
		HashSet subclassTables = new HashSet();
		iter = persistentClass.getSubclassTableClosureIterator();
		while ( iter.hasNext() ) {
			Table table = (Table) iter.next();
			subclassTables.add( table.getQualifiedName(
					factory.getDialect(), 
					factory.getSettings().getDefaultCatalogName(), 
					factory.getSettings().getDefaultSchemaName() 
			) );
		}
		subclassSpaces = ArrayHelper.toStringArray(subclassTables);

		subquery = generateSubquery(persistentClass, mapping);

		initLockers();

		initSubclassPropertyAliasesMap(persistentClass);
		
		postConstruct(mapping);

	}

	public Serializable[] getQuerySpaces() {
		return subclassSpaces;
	}
	
	public String getTableName() {
		return subquery;
	}

	public Type getDiscriminatorType() {
		return Hibernate.INTEGER;
	}

	public String getDiscriminatorSQLValue() {
		return discriminatorSQLValue;
	}

	public String[] getSubclassClosure() {
		return subclassClosure;
	}

	public String getSubclassForDiscriminatorValue(Object value) {
		return (String) subclassByDiscriminatorValue.get(value);
	}

	public Serializable[] getPropertySpaces() {
		return spaces;
	}

	protected boolean isDiscriminatorFormula() {
		return false;
	}

	/**
	 * Generate the SQL that selects a row by id
	 */
	protected String generateSelectString(LockMode lockMode) {
		SimpleSelect select = new SimpleSelect( getFactory().getDialect() )
			.setLockMode(lockMode)
			.setTableName( getTableName() )
			.addColumns( getIdentifierColumnNames() )
			.addColumns( 
					getSubclassColumnClosure(), 
					getSubclassColumnAliasClosure(),
					getSubclassColumnLazyiness()
			)
			.addColumns( 
					getSubclassFormulaClosure(), 
					getSubclassFormulaAliasClosure(),
					getSubclassFormulaLazyiness()
			);
		//TODO: include the rowids!!!!
		if ( hasSubclasses() ) {
			if ( isDiscriminatorFormula() ) {
				select.addColumn( getDiscriminatorFormula(), getDiscriminatorAlias() );
			}
			else {
				select.addColumn( getDiscriminatorColumnName(), getDiscriminatorAlias() );
			}
		}
		if ( getFactory().getSettings().isCommentsEnabled() ) {
			select.setComment( "load " + getEntityName() );
		}
		return select.addCondition( getIdentifierColumnNames(), "=?" ).toStatementString();
	}

	protected String getDiscriminatorFormula() {
		return null;
	}

	protected String getTableName(int j) {
		return tableName;
	}

	protected String[] getKeyColumns(int j) {
		return getIdentifierColumnNames();
	}
	
	protected boolean isTableCascadeDeleteEnabled(int j) {
		return false;
	}
	
	protected boolean isPropertyOfTable(int property, int j) {
		return true;
	}

	// Execute the SQL:

	public String fromTableFragment(String name) {
		return getTableName() + ' '  + name;
	}

	public String filterFragment(String name) {
		return hasWhere() ?
			" and " + getSQLWhereString(name) :
			"";
	}

	public String getSubclassPropertyTableName(int i) {
		return getTableName();//ie. the subquery! yuck!
	}

	protected void addDiscriminatorToSelect(SelectFragment select, String name, String suffix) {
		select.addColumn( name, getDiscriminatorColumnName(),  getDiscriminatorAlias() );
	}
	
	protected int[] getPropertyTableNumbersInSelect() {
		return new int[ getPropertySpan() ];
	}

	protected int getSubclassPropertyTableNumber(int i) {
		return 0;
	}

	protected int getSubclassPropertyTableNumber(String propertyName) {
		return 0;
	}

	protected int getTableSpan() {
		return 1;
	}

	protected int[] getSubclassColumnTableNumberClosure() {
		return new int[ getSubclassColumnClosure().length ];
	}

	protected int[] getSubclassFormulaTableNumberClosure() {
		return new int[ getSubclassFormulaClosure().length ];
	}

	protected boolean[] getTableHasColumns() {
		return new boolean[] { true };
	}

	protected int[] getPropertyTableNumbers() {
		return new int[ getPropertySpan() ];
	}

	protected String generateSubquery(PersistentClass model, Mapping mapping) {

		if ( !model.hasSubclasses() ) {
			return model.getTable().getQualifiedName(
					getFactory().getDialect(),
					getFactory().getSettings().getDefaultCatalogName(),
					getFactory().getSettings().getDefaultSchemaName()
			);
		}

		HashSet columns = new HashSet();
		Iterator titer = model.getSubclassTableClosureIterator();
		while ( titer.hasNext() ) {
			Table table = (Table) titer.next();
			if ( !table.isAbstractUnionTable() ) {
				Iterator citer = table.getColumnIterator();
				while ( citer.hasNext() ) columns.add( citer.next() );
			}
		}

		StringBuffer buf = new StringBuffer()
			.append("( ");

		Iterator siter = new JoinedIterator(
			Collections.singleton(model).iterator(),
			model.getSubclassIterator()
		);

		while ( siter.hasNext() ) {
			PersistentClass clazz = (PersistentClass) siter.next();
			Table table = clazz.getTable();
			if ( !table.isAbstractUnionTable() ) {
				//TODO: move to .sql package!!
				buf.append("select ");
				Iterator citer = columns.iterator();
				while ( citer.hasNext() ) {
					Column col = (Column) citer.next();
					if ( !table.containsColumn(col) ) {
						int sqlType = col.getSqlTypeCode(mapping);
						buf.append( getFactory().getDialect().getSelectClauseNullString(sqlType) )
							.append(" as ");
					}
					buf.append( col.getName() );
					buf.append(", ");
				}
				buf.append( clazz.getSubclassId() )
					.append(" as clazz_");
				buf.append(" from ")
					.append( table.getQualifiedName(
							getFactory().getDialect(),
							getFactory().getSettings().getDefaultCatalogName(),
							getFactory().getSettings().getDefaultSchemaName()
					) );
				if ( siter.hasNext() ) buf.append(" union ");
			}
		}

		return buf.append(" )").toString();
	}

	protected String[] getSubclassTableKeyColumns(int j) {
		if (j!=0) throw new AssertionFailure("only one table");
		return getIdentifierColumnNames();
	}

	protected String getSubclassTableName(int j) {
		if (j!=0) throw new AssertionFailure("only one table");
		return tableName;
	}

	protected int getSubclassTableSpan() {
		return 1;
	}

	protected boolean isClassOrSuperclassTable(int j) {
		if (j!=0) throw new AssertionFailure("only one table");
		return true;
	}

	public String getPropertyTableName(String propertyName) {
		//TODO: check this....
		return getTableName();
	}
}
