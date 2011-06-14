//$Id: BasicEntityPersister.java,v 1.17 2005/03/23 15:41:17 steveebersole Exp $
package org.hibernate.persister.entity;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.sf.cglib.transform.impl.InterceptFieldCallback;
import net.sf.cglib.transform.impl.InterceptFieldEnabled;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.cache.CacheConcurrencyStrategy;
import org.hibernate.cache.CacheKey;
import org.hibernate.cache.entry.CacheEntryStructure;
import org.hibernate.cache.entry.StructuredCacheEntry;
import org.hibernate.cache.entry.UnstructuredCacheEntry;
import org.hibernate.engine.Cascades;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.Versioning;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentifierGeneratorFactory;
import org.hibernate.id.PostInsertIdentifierGenerator;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.impl.FilterImpl;
import org.hibernate.intercept.FieldInterceptor;
import org.hibernate.intercept.LazyPropertyInitializer;
import org.hibernate.loader.entity.BatchingEntityLoader;
import org.hibernate.loader.entity.EntityLoader;
import org.hibernate.loader.entity.UniqueEntityLoader;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.property.BackrefPropertyAccessor;
import org.hibernate.sql.Alias;
import org.hibernate.sql.Delete;
import org.hibernate.sql.Insert;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.Select;
import org.hibernate.sql.SelectFragment;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.sql.Template;
import org.hibernate.sql.Update;
import org.hibernate.tuple.EntityMetamodel;
import org.hibernate.tuple.EntityTuplizer;
import org.hibernate.tuple.Tuplizer;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;
import org.hibernate.type.VersionType;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.CollectionHelper;
import org.hibernate.util.GetGeneratedKeysHelper;
import org.hibernate.util.StringHelper;

/**
 * Basic functionality for persisting an entity via JDBC
 * through either generated or custom SQL
 *
 * @author Gavin King
 */
public abstract class BasicEntityPersister
		implements OuterJoinLoadable, Queryable, ClassMetadata, UniqueKeyLoadable, SQLLoadable, LazyPropertyInitializer, PostInsertIdentityPersister {

	private static final Log log = LogFactory.getLog( BasicEntityPersister.class );

	public static final String ENTITY_CLASS = "class";

	// moved up from AbstractEntityPersister ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	private final SessionFactoryImplementor factory;
	private final CacheConcurrencyStrategy cache;
	private final CacheEntryStructure cacheEntryStructure;
	private final EntityMetamodel entityMetamodel;
	private final Map entityNameBySubclass = new HashMap();
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	private final String[] rootTableKeyColumnNames;
	private final String[] identifierAliases;
	private final int identifierColumnSpan;
	private final String versionColumnName;
	private final boolean hasFormulaProperties;
	private final boolean hasUpdateableColumns; //TODO: is this now obsolete? or even broken, since there are multiple tables?
	private final int batchSize;
	private final boolean hasSubselectLoadableCollections;
	protected final String rowIdName;

	private final Set lazyProperties;

	// The optional SQL string defined in the where attribute
	private final String sqlWhereString;
	private final String sqlWhereStringTemplate;

	//information about properties of this class,
	//including inherited properties
	//(only really needed for updatable/insertable properties)
	private final int[] propertyColumnSpans;
	private final String[] propertySubclassNames;
	private final String[][] propertyColumnAliases;
	private final String[][] propertyColumnNames;
	private final boolean[][] propertyColumnUpdateable;
	private final boolean[][] propertyColumnInsertable;
	private final boolean[] propertyUniqueness;
	private final boolean[] propertySelectable;

	//information about lazy properties of this class
	private final String[] lazyPropertyNames;
	private final int[] lazyPropertyNumbers;
	private final Type[] lazyPropertyTypes;
	private final String[][] lazyPropertyColumnAliases;

	//information about all properties in class hierarchy
	private final String[] subclassPropertyNameClosure;
	private final String[] subclassPropertySubclassNameClosure;
	private final Type[] subclassPropertyTypeClosure;
	private final String[][] subclassPropertyFormulaTemplateClosure;
	private final String[][] subclassPropertyColumnNameClosure;
	private final FetchMode[] subclassPropertyFetchModeClosure;
	private final boolean[] subclassPropertyNullabilityClosure;
	private final boolean[] propertyDefinedOnSubclass;
	private final int[][] subclassPropertyColumnNumberClosure;
	private final int[][] subclassPropertyFormulaNumberClosure;

	//information about all columns/formulas in class hierarchy
	private final String[] subclassColumnClosure;
	private final boolean[] subclassColumnLazyClosure;
	private final String[] subclassColumnAliasClosure;
	private final boolean[] subclassColumnSelectableClosure;
	private final String[] subclassFormulaClosure;
	private final String[] subclassFormulaTemplateClosure;
	private final String[] subclassFormulaAliasClosure;
	private final boolean[] subclassFormulaLazyClosure;

	//information relating to dynamic filters
	private final String[] filterNames;
	private final String[] filterConditions;

	private final Map uniqueKeyLoaders = new HashMap();
	private final Map lockers = new HashMap();
	private final Map loaders = new HashMap();
	
	private final Map propertyIndexes = new HashMap();

	// SQL strings
	private String sqlVersionSelectString;
	private String sqlSnapshotSelectString;
	private String sqlLazySelectString;

	private String sqlIdentityInsertString;
	private String sqlUpdateByRowIdString;
	private String sqlLazyUpdateByRowIdString;

	private String[] sqlDeleteStrings;
	private String[] sqlInsertStrings;
	private String[] sqlUpdateStrings;
	private String[] sqlLazyUpdateStrings;

	//Custom SQL (would be better if these were private)
	protected boolean[] insertCallable;
	protected boolean[] updateCallable;
	protected boolean[] deleteCallable;
	protected String[] customSQLInsert;
	protected String[] customSQLUpdate;
	protected String[] customSQLDelete;

	private boolean[] tableHasColumns;

	private final String loaderName;

	private UniqueEntityLoader queryLoader;

	private final Map subclassPropertyAliases = new HashMap();
	private final Map subclassPropertyColumnNames = new HashMap();

	protected final BasicEntityPropertyMapping propertyMapping;

	protected void addDiscriminatorToInsert(Insert insert) {}

	protected void addDiscriminatorToSelect(SelectFragment select, String name, String suffix) {}

	protected abstract int[] getSubclassColumnTableNumberClosure();

	protected abstract int[] getSubclassFormulaTableNumberClosure();

	protected abstract String getSubclassTableName(int j);

	protected abstract String[] getSubclassTableKeyColumns(int j);

	protected abstract boolean isClassOrSuperclassTable(int j);

	protected abstract int getSubclassTableSpan();

	protected abstract int getTableSpan();

	protected abstract boolean isTableCascadeDeleteEnabled(int j);

	protected abstract String getTableName(int j);

	protected abstract String[] getKeyColumns(int j);

	protected abstract boolean isPropertyOfTable(int property, int j);

	protected abstract int[] getPropertyTableNumbersInSelect();

	protected abstract int[] getPropertyTableNumbers();

	protected abstract int getSubclassPropertyTableNumber(int i);
	
	protected abstract String filterFragment(String alias) throws MappingException;

	private static final String DISCRIMINATOR_ALIAS = "clazz_";
	
	protected String getDiscriminatorColumnName() {
		return DISCRIMINATOR_ALIAS;
	}

	protected String getDiscriminatorAlias() {
		return DISCRIMINATOR_ALIAS;
	}

	protected String getDiscriminatorFormulaTemplate() {
		return null;
	}

	protected boolean isInverseTable(int j) {
		return false;
	}

	protected boolean isNullableTable(int j) {
		return false;
	}

	protected boolean isNullableSubclassTable(int j) {
		return false;
	}

	protected boolean isInverseSubclassTable(int j) {
		return false;
	}
	
	public boolean isSubclassEntityName(String entityName) {
		return entityMetamodel.getSubclassEntityNames().contains(entityName);
	}

	private boolean[] getTableHasColumns() {
		return tableHasColumns;
	}

	public String[] getRootTableKeyColumnNames() {
		return rootTableKeyColumnNames;
	}
	
	protected String[] getSQLUpdateByRowIdStrings() {
		if ( sqlUpdateByRowIdString == null ) {
			throw new AssertionFailure( "no update by row id" );
		}
		String[] result = new String[getTableSpan()];
		result[0] = sqlUpdateByRowIdString;
		for ( int i = 1; i < getTableSpan(); i++ ) result[i] = sqlUpdateStrings[i];
		return result;
	}

	protected String[] getSQLLazyUpdateByRowIdStrings() {
		if ( sqlLazyUpdateByRowIdString == null ) {
			throw new AssertionFailure( "no update by row id" );
		}
		String[] result = new String[getTableSpan()];
		result[0] = sqlLazyUpdateByRowIdString;
		for ( int i = 1; i < getTableSpan(); i++ ) result[i] = sqlLazyUpdateStrings[i];
		return result;
	}

	protected String getSQLSnapshotSelectString() {
		return sqlSnapshotSelectString;
	}

	protected String getSQLLazySelectString() {
		return sqlLazySelectString;
	}

	protected String[] getSQLDeleteStrings() {
		return sqlDeleteStrings;
	}

	protected String[] getSQLInsertStrings() {
		return sqlInsertStrings;
	}

	protected String[] getSQLUpdateStrings() {
		return sqlUpdateStrings;
	}

	protected String[] getSQLLazyUpdateStrings() {
		return sqlLazyUpdateStrings;
	}

	/**
	 * The query that inserts a row, letting the database generate an id
	 */
	protected String getSQLIdentityInsertString() {
		return sqlIdentityInsertString;
	}

	protected String getVersionSelectString() {
		return sqlVersionSelectString;
	}

	protected boolean isInsertCallable(int j) {
		return insertCallable[j];
	}

	protected boolean isUpdateCallable(int j) {
		return updateCallable[j];
	}

	protected boolean isDeleteCallable(int j) {
		return deleteCallable[j];
	}

	protected boolean isSubclassPropertyDeferred(String propertyName, String entityName) {
		return false;
	}

	protected boolean isSubclassTableSequentialSelect(int j) {
		return false;
	}

	public boolean hasSequentialSelect() {
		return false;
	}

	/**
	 * Decide which tables need to be updated
	 */
	protected boolean[] getTableUpdateNeeded(final int[] dirtyProperties, boolean hasDirtyCollection) {

		if ( dirtyProperties == null ) {
			return getTableHasColumns(); // for objects that came in via update()
		}
		else {
			int[] propertyTableNumbers = getPropertyTableNumbers();
			boolean[] tableUpdateNeeded = new boolean[getTableSpan()];
			for ( int i = 0; i < dirtyProperties.length; i++ ) {
				int table = propertyTableNumbers[dirtyProperties[i]];
				tableUpdateNeeded[table] = tableUpdateNeeded[table] ||
						getPropertyColumnSpan( dirtyProperties[i] ) > 0;
			}
			if ( isVersioned() ) {
				tableUpdateNeeded[0] = tableUpdateNeeded[0] || 
					Versioning.isVersionIncrementRequired( dirtyProperties, hasDirtyCollection, getPropertyVersionability() );
			}
			return tableUpdateNeeded;
		}
	}

	public boolean hasRowId() {
		return rowIdName != null;
	}

	public BasicEntityPersister(
			final PersistentClass persistentClass, 
			final CacheConcurrencyStrategy cache, 
			final SessionFactoryImplementor factory)
	throws HibernateException {

		// moved up from AbstractEntityPersister ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		this.factory = factory;
		this.cache = cache;
		this.cacheEntryStructure = factory.getSettings().isStructuredCacheEntriesEnabled() ?
				(CacheEntryStructure) new StructuredCacheEntry(this) :
				(CacheEntryStructure) new UnstructuredCacheEntry();

		this.entityMetamodel = new EntityMetamodel( persistentClass, factory );

		if ( persistentClass.hasPojoRepresentation() ) {
			//TODO: this is currently specific to pojos, but need to be available for all entity-modes
			Iterator iter = persistentClass.getSubclassIterator();
			while ( iter.hasNext() ) {
				PersistentClass pc = ( PersistentClass ) iter.next();
				entityNameBySubclass.put( pc.getMappedClass(), pc.getEntityName() );
			}
		}
		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

		int batch = persistentClass.getBatchSize();
		if (batch==-1) batch = factory.getSettings().getDefaultBatchFetchSize();
		batchSize = batch;
		hasSubselectLoadableCollections = persistentClass.hasSubselectLoadableCollections();

		propertyMapping = new BasicEntityPropertyMapping( this );

		// IDENTIFIER

		identifierColumnSpan = persistentClass.getIdentifier().getColumnSpan();
		rootTableKeyColumnNames = new String[identifierColumnSpan];
		identifierAliases = new String[identifierColumnSpan];

		rowIdName = persistentClass.getRootTable().getRowId();

		loaderName = persistentClass.getLoaderName();

		Iterator iter = persistentClass.getIdentifier().getColumnIterator();
		int i = 0;
		while ( iter.hasNext() ) {
			Column col = ( Column ) iter.next();
			rootTableKeyColumnNames[i] = col.getQuotedName( factory.getDialect() );
			identifierAliases[i] = col.getAlias(); // getClassAlias() handles quotes
			i++;
		}

		// VERSION

		if ( persistentClass.isVersioned() ) {
			versionColumnName = ( ( Column ) persistentClass.getVersion().getColumnIterator().next() ).getQuotedName( factory.getDialect() );
		}
		else {
			versionColumnName = null;
		}

		//WHERE STRING

		sqlWhereString = persistentClass.getWhere();
		sqlWhereStringTemplate = sqlWhereString == null ?
				null :
				Template.renderWhereStringTemplate( sqlWhereString, factory.getDialect() );

		// PROPERTIES

		final boolean lazyAvailable = isLazyAvailable();

		int hydrateSpan = entityMetamodel.getPropertySpan();
		propertyColumnSpans = new int[hydrateSpan];
		propertySubclassNames = new String[hydrateSpan];
		propertyColumnAliases = new String[hydrateSpan][];
		propertyColumnNames = new String[hydrateSpan][];
		propertyUniqueness = new boolean[hydrateSpan];
		propertySelectable = new boolean[hydrateSpan];
		propertyColumnUpdateable = new boolean[hydrateSpan][];
		propertyColumnInsertable = new boolean[hydrateSpan][];
		HashSet thisClassProperties = new HashSet();

		lazyProperties = new HashSet();
		ArrayList lazyNames = new ArrayList();
		ArrayList lazyNumbers = new ArrayList();
		ArrayList lazyTypes = new ArrayList();
		ArrayList lazyColAliases = new ArrayList();

		iter = persistentClass.getPropertyClosureIterator();
		i = 0;
		boolean foundUpdateableColumn = false;
		boolean foundFormula = false;
		while ( iter.hasNext() ) {
			Property prop = ( Property ) iter.next();
			thisClassProperties.add( prop );

			int span = prop.getColumnSpan();
			propertyColumnSpans[i] = span;
			propertySubclassNames[i] = prop.getPersistentClass().getEntityName();
			String[] colNames = new String[span];
			String[] colAliases = new String[span];
			Iterator colIter = prop.getColumnIterator();
			int k = 0;
			while ( colIter.hasNext() ) {
				Selectable thing = ( Selectable ) colIter.next();
				colAliases[k] = thing.getAlias( prop.getValue().getTable() );
				colNames[k] = thing.getTemplate( factory.getDialect() );
				if ( thing.isFormula() ) {
					foundFormula = true;
				}
				else {
					if ( prop.isUpdateable() ) foundUpdateableColumn = true;
				}
				k++;
			}
			propertyColumnNames[i] = colNames;
			propertyColumnAliases[i] = colAliases;

			if ( lazyAvailable && prop.isLazy() ) {
				lazyProperties.add( prop.getName() );
				lazyNames.add( prop.getName() );
				lazyNumbers.add( new Integer( i ) );
				lazyTypes.add( prop.getValue().getType() );
				lazyColAliases.add( colAliases );
			}
			
			propertyColumnUpdateable[i] = prop.getValue().getColumnUpdateability();
			propertyColumnInsertable[i] = prop.getValue().getColumnInsertability();

			propertySelectable[i] = prop.isSelectable();

			propertyIndexes.put( prop.getName(), new Integer( i ) );

			propertyUniqueness[i] = prop.getValue().isAlternateUniqueKey();

			i++;

		}
		hasFormulaProperties = foundFormula;
		hasUpdateableColumns = foundUpdateableColumn;
		lazyPropertyColumnAliases = ArrayHelper.to2DStringArray( lazyColAliases );
		lazyPropertyNames = ArrayHelper.toStringArray( lazyNames );
		lazyPropertyNumbers = ArrayHelper.toIntArray( lazyNumbers );
		lazyPropertyTypes = ArrayHelper.toTypeArray( lazyTypes );

		// SUBCLASS PROPERTY CLOSURE

		ArrayList columns = new ArrayList();
		ArrayList columnsLazy = new ArrayList();
		ArrayList aliases = new ArrayList();
		ArrayList formulas = new ArrayList();
		ArrayList formulaAliases = new ArrayList();
		ArrayList formulaTemplates = new ArrayList();
		ArrayList formulasLazy = new ArrayList();
		ArrayList types = new ArrayList();
		ArrayList names = new ArrayList();
		ArrayList classes = new ArrayList();
		ArrayList templates = new ArrayList();
		ArrayList propColumns = new ArrayList();
		ArrayList joinedFetchesList = new ArrayList();
		ArrayList definedBySubclass = new ArrayList();
		ArrayList propColumnNumbers = new ArrayList();
		ArrayList propFormulaNumbers = new ArrayList();
		ArrayList columnSelectables = new ArrayList();
		ArrayList propNullables = new ArrayList();

		iter = persistentClass.getSubclassPropertyClosureIterator();
		while ( iter.hasNext() ) {
			Property prop = ( Property ) iter.next();
			names.add( prop.getName() );
			classes.add( prop.getPersistentClass().getEntityName() );
			boolean isDefinedBySubclass = !thisClassProperties.contains( prop );
			definedBySubclass.add( new Boolean(isDefinedBySubclass) );
			propNullables.add( new Boolean( prop.isOptional() || isDefinedBySubclass) ); //TODO: is this completely correct?
			types.add( prop.getType() );

			Iterator colIter = prop.getColumnIterator();
			String[] cols = new String[prop.getColumnSpan()];
			String[] forms = new String[prop.getColumnSpan()];
			int[] colnos = new int[prop.getColumnSpan()];
			int[] formnos = new int[prop.getColumnSpan()];
			int l = 0;
			Boolean lazy = new Boolean( prop.isLazy() && lazyAvailable );
			while ( colIter.hasNext() ) {
				Selectable thing = ( Selectable ) colIter.next();
				if ( thing.isFormula() ) {
					String template = thing.getTemplate( factory.getDialect() );
					formnos[l] = formulaTemplates.size();
					colnos[l] = -1;
					formulaTemplates.add( template );
					forms[l] = template;
					formulas.add( thing.getText( factory.getDialect() ) );
					formulaAliases.add( thing.getAlias() );
					formulasLazy.add( lazy );
				}
				else {
					String colName = thing.getTemplate( factory.getDialect() );
					colnos[l] = columns.size(); //before add :-)
					formnos[l] = -1;
					columns.add( colName );
					cols[l] = colName;
					aliases.add( thing.getAlias( prop.getValue().getTable() ) );
					columnsLazy.add( lazy );
					columnSelectables.add( new Boolean( prop.isSelectable() ) );
				}
				l++;
			}
			propColumns.add( cols );
			templates.add( forms );
			propColumnNumbers.add( colnos );
			propFormulaNumbers.add( formnos );

			joinedFetchesList.add( prop.getValue().getFetchMode() );
		}
		subclassColumnClosure = ArrayHelper.toStringArray( columns );
		subclassColumnAliasClosure = ArrayHelper.toStringArray( aliases );
		subclassColumnLazyClosure = ArrayHelper.toBooleanArray( columnsLazy );
		subclassColumnSelectableClosure = ArrayHelper.toBooleanArray( columnSelectables );

		subclassFormulaClosure = ArrayHelper.toStringArray( formulas );
		subclassFormulaTemplateClosure = ArrayHelper.toStringArray( formulaTemplates );
		subclassFormulaAliasClosure = ArrayHelper.toStringArray( formulaAliases );
		subclassFormulaLazyClosure = ArrayHelper.toBooleanArray( formulasLazy );

		subclassPropertyNameClosure = ArrayHelper.toStringArray( names );
		subclassPropertySubclassNameClosure = ArrayHelper.toStringArray( classes );
		subclassPropertyTypeClosure = ArrayHelper.toTypeArray( types );
		subclassPropertyNullabilityClosure = ArrayHelper.toBooleanArray( propNullables );
		subclassPropertyFormulaTemplateClosure = ArrayHelper.to2DStringArray( templates );
		subclassPropertyColumnNameClosure = ArrayHelper.to2DStringArray( propColumns );
		subclassPropertyColumnNumberClosure = ArrayHelper.to2DIntArray( propColumnNumbers );
		subclassPropertyFormulaNumberClosure = ArrayHelper.to2DIntArray( propFormulaNumbers );

		subclassPropertyFetchModeClosure = new FetchMode[joinedFetchesList.size()];
		iter = joinedFetchesList.iterator();
		int j = 0;
		while ( iter.hasNext() ) subclassPropertyFetchModeClosure[j++] = ( FetchMode ) iter.next();

		propertyDefinedOnSubclass = new boolean[definedBySubclass.size()];
		iter = definedBySubclass.iterator();
		j = 0;
		while ( iter.hasNext() ) propertyDefinedOnSubclass[j++] = ( ( Boolean ) iter.next() ).booleanValue();

		// Handle any filters applied to the class level
		int filterCount = persistentClass.getFilterMap() == null ? 0 : persistentClass.getFilterMap().size();
		filterNames = new String[filterCount];
		filterConditions = new String[filterCount];
		if ( filterCount > 0 ) {
			iter = persistentClass.getFilterMap().entrySet().iterator();
			filterCount = 0;
			while ( iter.hasNext() ) {
				final Map.Entry entry = ( Map.Entry ) iter.next();
				filterNames[filterCount] = ( String ) entry.getKey();
				filterConditions[filterCount] = Template.renderWhereStringTemplate( ( String ) entry.getValue(),
						FilterImpl.MARKER,
						factory.getDialect() );
				filterConditions[filterCount] = StringHelper.replace( filterConditions[filterCount],
						":",
						":" + filterNames[filterCount] + "." );
				filterCount++;
			}
		}
		
	}

	protected String generateLazySelectString() {

		if ( !entityMetamodel.hasLazyProperties() ) return null;

		HashSet tableNumbers = new HashSet();
		ArrayList columnNumbers = new ArrayList();
		ArrayList formulaNumbers = new ArrayList();
		for ( int i = 0; i < lazyPropertyNames.length; i++ ) {
			// all this only really needs to consider properties
			// of this class, not its subclasses, but since we
			// are reusing code used for sequential selects, we
			// use the subclass closure
			int propertyNumber = getSubclassPropertyIndex( lazyPropertyNames[i] );

			int tableNumber = getSubclassPropertyTableNumber( propertyNumber );
			tableNumbers.add( new Integer( tableNumber ) );

			int[] colNumbers = subclassPropertyColumnNumberClosure[propertyNumber];
			for ( int j = 0; j < colNumbers.length; j++ ) {
				if ( colNumbers[j]!=-1 ) {
					columnNumbers.add( new Integer( colNumbers[j] ) );
				}
			}
			int[] formNumbers = subclassPropertyFormulaNumberClosure[propertyNumber];
			for ( int j = 0; j < formNumbers.length; j++ ) {
				if ( formNumbers[j]!=-1 ) {
					formulaNumbers.add( new Integer( formNumbers[j] ) );
				}
			}
		}
		
		if ( columnNumbers.size()==0 && formulaNumbers.size()==0 ) {
			// only one-to-one is lazy fetched
			return null;
		}

		return renderSelect( ArrayHelper.toIntArray( tableNumbers ),
				ArrayHelper.toIntArray( columnNumbers ),
				ArrayHelper.toIntArray( formulaNumbers ) );

	}

	public Object initializeLazyProperty(String fieldName, Object entity, SessionImplementor session)
			throws HibernateException {

		final Serializable id = session.getEntityIdentifier( entity );

		final EntityEntry entry = session.getPersistenceContext().getEntry( entity );
		if ( entry == null ) throw new HibernateException( "entity is not associated with the session: " + id );

		if ( log.isTraceEnabled() ) {
			log.trace( "initializing lazy properties of: " +
					MessageHelper.infoString( this, id, getFactory() ) +
					", field access: " + fieldName );
		}

		try {

			Object result = null;
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				final String lazySelect = getSQLLazySelectString();
				if (lazySelect!=null) { 
					// null sql means the lazy properties do not map to 
					// columns or formulas (eg. one-to-one associations)
					ps = session.getBatcher().prepareSelectStatement(lazySelect);
					getIdentifierType().nullSafeSet( ps, id, 1, session );
					rs = ps.executeQuery();
					rs.next();
				}
				final Object[] snapshot = entry.getLoadedState();
				for ( int j = 0; j < lazyPropertyNames.length; j++ ) {
					Object propValue = lazyPropertyTypes[j].nullSafeGet( rs, lazyPropertyColumnAliases[j], session, entity );
					setPropertyValue( entity, lazyPropertyNumbers[j], propValue, session.getEntityMode() );
					snapshot[ lazyPropertyNumbers[j] ] = lazyPropertyTypes[j].deepCopy( propValue, session.getEntityMode(), factory );
					if ( fieldName.equals( lazyPropertyNames[j] ) ) result = propValue;
				}
				if (rs!=null) rs.close();
			}
			finally {
				if (ps!=null) session.getBatcher().closeStatement(ps);
			}

			log.trace( "done initializing lazy properties" );

			return result;

		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
					getFactory().getSQLExceptionConverter(),
					sqle,
					"could not initialize lazy properties: " + 
					MessageHelper.infoString( this, id, getFactory() ),
					getSQLLazySelectString()
			);
		}

	}

	public boolean isBatchable() {
		return optimisticLockMode()==Versioning.OPTIMISTIC_LOCK_NONE || 
			( !isVersioned() && optimisticLockMode()==Versioning.OPTIMISTIC_LOCK_VERSION ) ||
			getFactory().getSettings().isJdbcBatchVersionedData();
	}

	public Serializable[] getQuerySpaces() {
		return getPropertySpaces();
	}

	protected Set getLazyProperties() {
		return lazyProperties;
	}

	private String getLockString(LockMode lockMode) {
		return ( String ) lockers.get( lockMode );
	}

	public boolean isBatchLoadable() {
		return batchSize > 1;
	}

	public String[] getIdentifierColumnNames() {
		return rootTableKeyColumnNames;
	}

	protected int getIdentifierColumnSpan() {
		return identifierColumnSpan;
	}

	protected String[] getIdentifierAliases() {
		return identifierAliases;
	}

	public String getVersionColumnName() {
		return versionColumnName;
	}

	protected String getVersionedTableName() {
		return getTableName( 0 );
	}

	protected boolean[] getSubclassColumnLazyiness() {
		return subclassColumnLazyClosure;
	}

	protected boolean[] getSubclassFormulaLazyiness() {
		return subclassFormulaLazyClosure;
	}

	public boolean isCacheInvalidationRequired() {
		return hasFormulaProperties() || ( !isVersioned() && ( entityMetamodel.isDynamicUpdate() || getTableSpan() > 1 ) );
	}

	public String selectFragment(String alias, String suffix) {
		return identifierSelectFragment( alias, suffix ) + propertySelectFragment( alias, suffix );
	}

	public String[] getIdentifierAliases(String suffix) {
		// NOTE: this assumes something about how propertySelectFragment is implemented by the subclass!
		// was toUnqotedAliasStrings( getIdentiferColumnNames() ) before - now tried
		// to remove that unqoting and missing aliases..
		return new Alias( suffix ).toAliasStrings( getIdentifierAliases() );
	}

	public String[] getPropertyAliases(String suffix, int i) {
		// NOTE: this assumes something about how propertySelectFragment is implemented by the subclass!
		return new Alias( suffix ).toUnquotedAliasStrings( propertyColumnAliases[i] );
	}

	public String getDiscriminatorAlias(String suffix) {
		// NOTE: this assumes something about how propertySelectFragment is implemented by the subclass!
		// was toUnqotedAliasStrings( getdiscriminatorColumnName() ) before - now tried
		// to remove that unqoting and missing aliases..
		return entityMetamodel.hasSubclasses() ?
				new Alias( suffix ).toAliasString( getDiscriminatorAlias() ) :
				null;
	}

	public String identifierSelectFragment(String name, String suffix) {
		return new SelectFragment()
				.setSuffix( suffix )
				.addColumns( name, getIdentifierColumnNames(), getIdentifierAliases() )
				.toFragmentString()
				.substring( 2 ); //strip leading ", "
	}


	public String propertySelectFragment(String name, String suffix) {

		SelectFragment select = new SelectFragment()
				.setSuffix( suffix )
				.setUsedAliases( getIdentifierAliases() );

		int[] columnTableNumbers = getSubclassColumnTableNumberClosure();
		String[] columnAliases = getSubclassColumnAliasClosure();
		String[] columns = getSubclassColumnClosure();
		for ( int i = 0; i < getSubclassColumnClosure().length; i++ ) {
			final boolean selectable = !subclassColumnLazyClosure[i] && 
				!isSubclassTableSequentialSelect( columnTableNumbers[i] ) &&
				subclassColumnSelectableClosure[i];
			if ( selectable ) {
				String subalias = generateTableAlias( name, columnTableNumbers[i] );
				select.addColumn( subalias, columns[i], columnAliases[i] );
			}
		}

		int[] formulaTableNumbers = getSubclassFormulaTableNumberClosure();
		String[] formulaTemplates = getSubclassFormulaTemplateClosure();
		String[] formulaAliases = getSubclassFormulaAliasClosure();
		for ( int i = 0; i < getSubclassFormulaTemplateClosure().length; i++ ) {
			if ( !subclassFormulaLazyClosure[i] && !isSubclassTableSequentialSelect( formulaTableNumbers[i] ) ) {
				String subalias = generateTableAlias( name, formulaTableNumbers[i] );
				select.addFormula( subalias, formulaTemplates[i], formulaAliases[i] );
			}
		}

		if ( entityMetamodel.hasSubclasses() ) addDiscriminatorToSelect( select, name, suffix );

		if ( hasRowId() ) select.addColumn( name, rowIdName, ROWID_ALIAS );

		return select.toFragmentString();
	}

	public Object[] getDatabaseSnapshot(Serializable id, SessionImplementor session)
			throws HibernateException {

		if ( log.isTraceEnabled() ) {
			log.trace( "Getting current persistent state for: " + MessageHelper.infoString( this, id, getFactory() ) );
		}

		try {
			PreparedStatement ps = session.getBatcher().prepareSelectStatement( getSQLSnapshotSelectString() );
			try {
				getIdentifierType().nullSafeSet( ps, id, 1, session );
				//if ( isVersioned() ) getVersionType().nullSafeSet( ps, version, getIdentifierColumnSpan()+1, session );
				ResultSet rs = ps.executeQuery();
				try {
					//if there is no resulting row, return null
					if ( !rs.next() ) return null;
					
					//otherwise return the "hydrated" state (ie. associations are not resolved)
					Type[] types = getPropertyTypes();
					Object[] values = new Object[types.length];
					boolean[] includeProperty = getPropertyUpdateability();
					for ( int i = 0; i < types.length; i++ ) {
						if ( includeProperty[i] ) {
							values[i] = types[i].hydrate( rs, getPropertyAliases( "", i ), session, null ); //null owner ok??
						}
					}
					return values;
				}
				finally {
					rs.close();
				}
			}
			finally {
				session.getBatcher().closeStatement( ps );
			}
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
					getFactory().getSQLExceptionConverter(),
					sqle,
					"could not retrieve snapshot: " + 
					MessageHelper.infoString( this, id, getFactory() ),
			        getSQLSnapshotSelectString()
			);
		}

	}

	/**
	 * Generate the SQL that selects the version number by id
	 */
	protected String generateSelectVersionString() {
		SimpleSelect select = new SimpleSelect( getFactory().getDialect() )
				.setTableName( getVersionedTableName() );
		if ( isVersioned() ) {
			select.addColumn( versionColumnName );
		}
		else {
			select.addColumns( rootTableKeyColumnNames );
		}
		if ( getFactory().getSettings().isCommentsEnabled() ) select.setComment( "get version " + getEntityName() );
		return select.addCondition( rootTableKeyColumnNames, "=?" ).toStatementString();
	}

	protected String concretePropertySelectFragment(String alias, boolean[] includeProperty) {
		int propertyCount = getPropertyNames().length;
		int[] propertyTableNumbers = getPropertyTableNumbersInSelect();
		SelectFragment frag = new SelectFragment();
		for ( int i = 0; i < propertyCount; i++ ) {
			if ( includeProperty[i] ) { //ie. updateable, not a formula
				frag.addColumns( generateTableAlias( alias, propertyTableNumbers[i] ),
						propertyColumnNames[i],
						propertyColumnAliases[i] );
				//don't need to handle formulas 'cos they aren't updateable!
			}
		}
		return frag.toFragmentString();
	}

	protected String generateSnapshotSelectString() {

		StringBuffer where = new StringBuffer()
				.append( StringHelper.join( "=? and ",
						StringHelper.qualify( getRootAlias(), getIdentifierColumnNames() ) ) )
				.append( "=?" )
				.append( whereJoinFragment( getRootAlias(), true, false ) );

		/*if ( isVersioned() ) {
			where.append(" and ")
				.append( getVersionColumnName() )
				.append("=?");
		}*/
		
		//TODO: should we use SELECT .. FOR UPDATE?

		Select select = new Select( getFactory().getDialect() );
		if ( getFactory().getSettings().isCommentsEnabled() ) {
			select.setComment( "get current state " + getEntityName() );
		}
		return select.setSelectClause( StringHelper.join( ", ",
				StringHelper.qualify( getRootAlias(), getIdentifierColumnNames() ) ) +
				concretePropertySelectFragment( getRootAlias(), getPropertyUpdateability() ) )
				.setFromClause( fromTableFragment( getRootAlias() ) + fromJoinFragment( getRootAlias(), true, false ) )
				.setOuterJoins( "", "" )
				.setWhereClause( where.toString() )
				.toStatementString();
	}

	/**
	 * Do a version check
	 */
	public void lock(Serializable id, Object version, Object object, LockMode lockMode, SessionImplementor session) throws HibernateException {

		if ( lockMode != LockMode.NONE ) {

			if ( log.isTraceEnabled() ) {
				log.trace( "Locking entity: " + MessageHelper.infoString( this, id, getFactory() ) );
				if ( isVersioned() ) log.trace( "Version: " + version );
			}

			final String sql = getLockString( lockMode );

			try {

				PreparedStatement st = session.getBatcher().prepareSelectStatement( sql );
				try {
					getIdentifierType().nullSafeSet( st, id, 1, session );
					if ( isVersioned() ) {
						getVersionType().nullSafeSet( st, version, getIdentifierColumnSpan() + 1, session );
					}

					ResultSet rs = st.executeQuery();
					try {
						if ( !rs.next() ) {
							throw new StaleObjectStateException( getEntityName(), id );
						}
					}
					finally {
						rs.close();
					}
				}
				finally {
					session.getBatcher().closeStatement( st );
				}

			}
			catch ( SQLException sqle ) {
				throw JDBCExceptionHelper.convert(
						getFactory().getSQLExceptionConverter(),
						sqle,
						"could not lock: " + 
						MessageHelper.infoString( this, id, getFactory() ),
						sql
				);
			}

		}

	}

	/**
	 * Retrieve the version number
	 */
	public Object getCurrentVersion(Serializable id, SessionImplementor session) throws HibernateException {

		if ( log.isTraceEnabled() ) {
			log.trace( "Getting version: " + MessageHelper.infoString( this, id, getFactory() ) );
		}

		try {

			PreparedStatement st = session.getBatcher().prepareSelectStatement( getVersionSelectString() );
			try {
				getIdentifierType().nullSafeSet( st, id, 1, session );

				ResultSet rs = st.executeQuery();
				try {
					if ( !rs.next() ) return null;
					if ( !isVersioned() ) return this;
					return getVersionType().nullSafeGet( rs, getVersionColumnName(), session, null );
				}
				finally {
					rs.close();
				}
			}
			finally {
				session.getBatcher().closeStatement( st );
			}

		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
					getFactory().getSQLExceptionConverter(),
					sqle,
					"could not retrieve version: " + 
					MessageHelper.infoString( this, id, getFactory() ),
					getVersionSelectString()
			);
		}

	}

	/**
	 * Generate the SQL that pessimistic locks a row by id (and version)
	 */
	protected String generateLockString(LockMode lockMode) {

		SimpleSelect select = new SimpleSelect( getFactory().getDialect() )
				.setLockMode( lockMode )
				.setTableName( getVersionedTableName() )
				.addColumn( rootTableKeyColumnNames[0] )
				.addCondition( rootTableKeyColumnNames, "=?" );
		if ( isVersioned() ) {
			select.addWhereToken( "and" )
					.addCondition( getVersionColumnName(), "=?" );
		}
		if ( getFactory().getSettings().isCommentsEnabled() ) {
			select.setComment( "lock " + getEntityName() );
		}
		return select.toStatementString();

	}

	protected void initLockers() {
		lockers.put( LockMode.READ, generateLockString( LockMode.READ ) );
		lockers.put( LockMode.UPGRADE, generateLockString( LockMode.UPGRADE ) );
		lockers.put( LockMode.UPGRADE_NOWAIT, generateLockString( LockMode.UPGRADE_NOWAIT ) );
	}

	public String[] toColumns(String alias, String propertyName) throws QueryException {
		return propertyMapping.toColumns( alias, propertyName );
	}

	public String[] toColumns(String propertyName) throws QueryException {
		return propertyMapping.getColumnNames( propertyName );
	}

	public Type toType(String propertyName) throws QueryException {
		return propertyMapping.toType( propertyName );
	}

	public String[] getPropertyColumnNames(String propertyName) {
		return propertyMapping.getColumnNames( propertyName );
	}

	/**
	 * Warning:
	 * When there are duplicated property names in the subclasses
	 * of the class, this method may return the wrong table
	 * number for the duplicated subclass property (note that
	 * SingleTableEntityPersister defines an overloaded form
	 * which takes the entity name.
	 */
	protected int getSubclassPropertyTableNumber(String propertyName) {
		Type type = propertyMapping.toType(propertyName);
		if ( type.isAssociationType() && ( (AssociationType) type ).useLHSPrimaryKey() ) return 0;
		int index = ArrayHelper.indexOf( getSubclassPropertyNameClosure(), propertyName); //TODO: optimize this better!
		return index==-1 ? 0 : getSubclassPropertyTableNumber(index);
	}

	protected String generateTableAlias(String rootAlias, int tableNumber) {
		if ( tableNumber == 0 ) return rootAlias;
		StringBuffer buf = new StringBuffer().append( rootAlias );
		if ( !rootAlias.endsWith( "_" ) ) buf.append( '_' );
		return buf.append( tableNumber ).append( '_' ).toString();
	}

	public String[] toColumns(String name, final int i) {
		final String alias = generateTableAlias( name, getSubclassPropertyTableNumber( i ) );
		String[] cols = getSubclassPropertyColumnNames( i );
		String[] templates = getSubclassPropertyFormulaTemplateClosure()[i];
		String[] result = new String[cols.length];
		for ( int j = 0; j < cols.length; j++ ) {
			if ( cols[j] == null ) {
				result[j] = StringHelper.replace( templates[j], Template.TEMPLATE, alias );
			}
			else {
				result[j] = StringHelper.qualify( alias, cols[j] );
			}
		}
		return result;
	}

	private int getSubclassPropertyIndex(String propertyName) {
		return ArrayHelper.indexOf(subclassPropertyNameClosure, propertyName);
	}
	
	protected String[] getPropertySubclassNames() {
		return propertySubclassNames;
	}

	private String[] getPropertyColumnNames(int i) {
		return propertyColumnNames[i];
	}

	protected int getPropertyColumnSpan(int i) {
		return propertyColumnSpans[i];
	}

	protected boolean hasFormulaProperties() {
		return hasFormulaProperties;
	}

	protected boolean hasUpdateableColumns() {
		return hasUpdateableColumns;
	}

	public FetchMode getFetchMode(int i) {
		return subclassPropertyFetchModeClosure[i];
	}

	public Type getSubclassPropertyType(int i) {
		return subclassPropertyTypeClosure[i];
	}

	public String getSubclassPropertyName(int i) {
		return subclassPropertyNameClosure[i];
	}

	public int countSubclassProperties() {
		return subclassPropertyTypeClosure.length;
	}

	public String[] getSubclassPropertyColumnNames(int i) {
		return subclassPropertyColumnNameClosure[i];
	}

	public boolean isDefinedOnSubclass(int i) {
		return propertyDefinedOnSubclass[i];
	}

	protected String[][] getSubclassPropertyFormulaTemplateClosure() {
		return subclassPropertyFormulaTemplateClosure;
	}

	protected Type[] getSubclassPropertyTypeClosure() {
		return subclassPropertyTypeClosure;
	}

	protected String[][] getSubclassPropertyColumnNameClosure() {
		return subclassPropertyColumnNameClosure;
	}

	protected String[] getSubclassPropertyNameClosure() {
		return subclassPropertyNameClosure;
	}

	protected String[] getSubclassPropertySubclassNameClosure() {
		return subclassPropertySubclassNameClosure;
	}

	protected String[] getSubclassColumnClosure() {
		return subclassColumnClosure;
	}

	protected String[] getSubclassColumnAliasClosure() {
		return subclassColumnAliasClosure;
	}

	protected String[] getSubclassFormulaClosure() {
		return subclassFormulaClosure;
	}

	protected String[] getSubclassFormulaTemplateClosure() {
		return subclassFormulaTemplateClosure;
	}

	protected String[] getSubclassFormulaAliasClosure() {
		return subclassFormulaAliasClosure;
	}

	public String[] getSubclassPropertyColumnAliases(String propertyName, String suffix) {
		String rawAliases[] = ( String[] ) subclassPropertyAliases.get( propertyName );

		if ( rawAliases == null ) return null;

		String result[] = new String[rawAliases.length];
		for ( int i = 0; i < rawAliases.length; i++ ) {
			result[i] = new Alias( suffix ).toUnquotedAliasString( rawAliases[i] );
		}
		return result;
	}
	
	public String[] getSubclassPropertyColumnNames(String propertyName) {
		//TODO: should we allow suffixes on these ?
		return ( String[] ) subclassPropertyColumnNames.get( propertyName );
	}
	
		
	
	//This is really ugly, but necessary:
	/**
	 * Must be called by subclasses, at the end of their constructors
	 */
	protected void initSubclassPropertyAliasesMap(PersistentClass model) throws MappingException {

		// ALIASES
		internalInitSubclassPropertyAliasesMap( null, model.getSubclassPropertyClosureIterator() );
		
		// aliases for identifier ( alias.id )
		subclassPropertyAliases.put( ENTITY_ID, getIdentifierAliases() );
		subclassPropertyColumnNames.put( ENTITY_ID, getIdentifierColumnNames() );
		
		// aliases named identifier ( alias.idname ) 
		if ( hasIdentifierProperty() ) {
			subclassPropertyAliases.put( getIdentifierPropertyName(), getIdentifierAliases() );
			subclassPropertyColumnNames.put( getIdentifierPropertyName(), getIdentifierColumnNames() );
		}

		// aliases for composite-id's  
		if ( getIdentifierType().isComponentType() ) {
			// Fetch embedded identifiers propertynames from the "virtual" identifier component
			AbstractComponentType componentId = ( AbstractComponentType ) getIdentifierType();
			String[] idPropertyNames = componentId.getPropertyNames();
			String[] idAliases = getIdentifierAliases();
			String[] idColumnNames = getIdentifierColumnNames();
			
			for ( int i = 0; i < idPropertyNames.length; i++ ) {
				subclassPropertyAliases.put( ENTITY_ID + "." + idPropertyNames[i], new String[]{idAliases[i]} );
				subclassPropertyColumnNames.put( ENTITY_ID + "." + getIdentifierPropertyName() + "." + idPropertyNames[i],  new String[]{idColumnNames[i]} );
				if (hasIdentifierProperty() && !ENTITY_ID.equals(getIdentifierPropertyName()) ) {
					subclassPropertyAliases.put( getIdentifierPropertyName() + "." + idPropertyNames[i], new String[]{idAliases[i]} );
					subclassPropertyColumnNames.put( getIdentifierPropertyName() + "." + idPropertyNames[i],  new String[]{idColumnNames[i]} );
				} else {
					// embedded composite ids ( alias.idname1, alias.idname2 )
					subclassPropertyAliases.put( idPropertyNames[i], new String[]{idAliases[i]} );
					subclassPropertyColumnNames.put( idPropertyNames[i],  new String[]{idColumnNames[i]} );
				}
			}
		}

		if ( entityMetamodel.isPolymorphic() ) {
			subclassPropertyAliases.put( ENTITY_CLASS,
					new String[]{getDiscriminatorAlias()} );
			subclassPropertyColumnNames.put( ENTITY_CLASS,
					new String[]{getDiscriminatorColumnName()} );
		}

	}

	private void internalInitSubclassPropertyAliasesMap(String path, Iterator propertyIterator) {
		while ( propertyIterator.hasNext() ) {

			Property prop = ( Property ) propertyIterator.next();
			String propname = path == null ? prop.getName() : path + "." + prop.getName();
			if ( prop.isComposite() ) {
				Component component = ( Component ) prop.getValue();
				Iterator compProps = component.getPropertyIterator();
				internalInitSubclassPropertyAliasesMap( propname, compProps );
			}
			else {
				String[] aliases = new String[prop.getColumnSpan()];
				String[] cols = new String[prop.getColumnSpan()];
				Iterator colIter = prop.getColumnIterator();
				int l = 0;
				while ( colIter.hasNext() ) {
					Selectable thing = ( Selectable ) colIter.next();
					aliases[l] = thing.getAlias( prop.getValue().getTable() );
					cols[l] = thing.getText( getFactory().getDialect() ); // TODO: skip formulas?
					l++;
				}

				subclassPropertyAliases.put( propname, aliases );
				subclassPropertyColumnNames.put( propname, cols );
			}
		}

	}

	public Object loadByUniqueKey(String propertyName, Object uniqueKey, SessionImplementor session)
			throws HibernateException {
		return getAppropriateUniqueKeyLoader( propertyName, session.getEnabledFilters() )
				.loadByUniqueKey( session, uniqueKey );
	}

	private EntityLoader getAppropriateUniqueKeyLoader(String propertyName, Map enabledFilters) {
		if ( enabledFilters == null || enabledFilters.isEmpty() ) {
			return ( EntityLoader ) uniqueKeyLoaders.get( propertyName );
		}
		else {
			int propertyIndex = getPropertyIndex( propertyName );
			return createUniqueKeyLoader( getPropertyTypes()[propertyIndex],
					getPropertyColumnNames( propertyIndex ),
					enabledFilters );
		}
	}

	protected Integer getPropertyIndexOrNull(String propertyName) {
		return (Integer) propertyIndexes.get(propertyName);
	}

	public int getPropertyIndex(String propertyName) {
		Integer result = getPropertyIndexOrNull(propertyName);
		if (result==null) {
			throw new HibernateException("could not resolve property-ref: " + propertyName);
		}
		return result.intValue();
	}

	protected void createUniqueKeyLoaders() throws MappingException {
		//TODO: does not handle components
		Type[] propertyTypes = getPropertyTypes();
		String[] propertyNames = getPropertyNames();
		for ( int i = 0; i < entityMetamodel.getPropertySpan(); i++ ) {
			if ( propertyUniqueness[i] ) {
				//don't need filters for the static loaders
				uniqueKeyLoaders.put( propertyNames[i],
						createUniqueKeyLoader( propertyTypes[i],
								getPropertyColumnNames( i ),
								CollectionHelper.EMPTY_MAP ) );
			}
		}
	}

	private EntityLoader createUniqueKeyLoader(Type uniqueKeyType, String[] columns, Map enabledFilters) {
		if ( uniqueKeyType.isEntityType() ) {
			String className = ( ( EntityType ) uniqueKeyType ).getAssociatedEntityName();
			uniqueKeyType = getFactory().getEntityPersister( className ).getIdentifierType();
		}

		return new EntityLoader( this, columns, uniqueKeyType, 1, LockMode.NONE, getFactory(), enabledFilters );
	}

	protected String getSQLWhereString(String alias) {
		return StringHelper.replace( sqlWhereStringTemplate, Template.TEMPLATE, alias );
	}

	protected boolean hasWhere() {
		return sqlWhereString != null;
	}

	private void initOrdinaryPropertyPaths(Mapping mapping) throws MappingException {
		for ( int i = 0; i < getSubclassPropertyNameClosure().length; i++ ) {
			propertyMapping.initPropertyPaths( getSubclassPropertyNameClosure()[i],
					getSubclassPropertyTypeClosure()[i],
					getSubclassPropertyColumnNameClosure()[i],
					getSubclassPropertyFormulaTemplateClosure()[i],
					mapping );
		}
	}

	private void initIdentifierPropertyPaths(Mapping mapping) throws MappingException {
		String idProp = getIdentifierPropertyName();
		if ( idProp != null ) {
			propertyMapping.initPropertyPaths( idProp, getIdentifierType(), getIdentifierColumnNames(), null, mapping );
		}
		if ( entityMetamodel.getIdentifierProperty().isEmbedded() ) {
			propertyMapping.initPropertyPaths( null, getIdentifierType(), getIdentifierColumnNames(), null, mapping );
		}
		propertyMapping.initPropertyPaths( ENTITY_ID, getIdentifierType(), getIdentifierColumnNames(), null, mapping );
	}

	private void initDiscriminatorPropertyPath(Mapping mapping) throws MappingException {
		propertyMapping.initPropertyPaths( ENTITY_CLASS,
				getDiscriminatorType(),
				new String[]{getDiscriminatorColumnName()},
				new String[]{getDiscriminatorFormulaTemplate()},
				getFactory() );
	}

	protected void initPropertyPaths(Mapping mapping) throws MappingException {
		initOrdinaryPropertyPaths(mapping);
		initOrdinaryPropertyPaths(mapping); //do two passes, for collection property-ref!
		initIdentifierPropertyPaths(mapping);
		if ( entityMetamodel.isPolymorphic() ) initDiscriminatorPropertyPath(mapping);
	}

	protected UniqueEntityLoader createEntityLoader(LockMode lockMode, Map enabledFilters) throws MappingException {
		//TODO: disable batch loading if lockMode > READ?
		return BatchingEntityLoader.createBatchingEntityLoader( this, batchSize, lockMode, getFactory(), enabledFilters );
	}

	protected UniqueEntityLoader createEntityLoader(LockMode lockMode) throws MappingException {
		return createEntityLoader( lockMode, CollectionHelper.EMPTY_MAP );
	}

	protected boolean check(int rows, Serializable id, int tableNumber) throws HibernateException {
		if ( rows < 1 ) {
			if ( !isNullableTable( tableNumber ) ) {
				throw new StaleObjectStateException( getEntityName(), id );
			}
		}
		else if ( rows > 1 ) {
			throw new HibernateException( 
					"Duplicate identifier in table for: " + 
					MessageHelper.infoString( this, id, getFactory() ) 
			);
		}
		return rows > 0;
	}

	protected String generateUpdateString(boolean[] includeProperty, int j, boolean useRowId) {
		return generateUpdateString( includeProperty, j, null, useRowId );
	}

	/**
	 * Generate the SQL that updates a row by id (and version)
	 */
	protected String generateUpdateString(final boolean[] includeProperty,
										  final int j,
										  final Object[] oldFields,
										  final boolean useRowId) {

		Update update = new Update().setTableName( getTableName( j ) );
		
		// select the correct row by either pk or rowid
		if ( useRowId ) {
			update.setPrimaryKeyColumnNames( new String[]{rowIdName} ); //TODO: eventually, rowIdName[j]
		}
		else {
			update.setPrimaryKeyColumnNames( getKeyColumns( j ) );
		}

		boolean hasColumns = false;
		for ( int i = 0; i < entityMetamodel.getPropertySpan(); i++ ) {
			if ( includeProperty[i] && isPropertyOfTable( i, j ) ) {
				// this is a property of the table, which we are updating
				update.addColumns( getPropertyColumnNames(i), propertyColumnUpdateable[i] );
				hasColumns = hasColumns || getPropertyColumnSpan( i ) > 0;
			}
		}

		if ( j == 0 && isVersioned() && entityMetamodel.getOptimisticLockMode() == Versioning.OPTIMISTIC_LOCK_VERSION ) {
			// this is the root (versioned) table, and we 
			// are using version-based optimistic locking
			if ( includeProperty[ getVersionProperty() ] ) { //if we are not updating the version, also don't check it!
				update.setVersionColumnName( getVersionColumnName() );
				hasColumns = true;
			}
		}
		else if ( entityMetamodel.getOptimisticLockMode() > Versioning.OPTIMISTIC_LOCK_VERSION && oldFields != null ) {
			// we are using "all" or "dirty" property-based optimistic locking
			
			boolean[] includeInWhere = entityMetamodel.getOptimisticLockMode() == Versioning.OPTIMISTIC_LOCK_ALL ?
					getPropertyUpdateability() : //optimistic-lock="all", include all updatable properties
					includeProperty; //optimistic-lock="dirty", include all properties we are updating this time

			for ( int i = 0; i < entityMetamodel.getPropertySpan(); i++ ) {
				boolean[] versionability = getPropertyVersionability();
				if ( includeInWhere[i] && isPropertyOfTable( i, j ) && versionability[i] ) {
					// this property belongs to the table, and it is not specifically 
					// excluded from optimistic locking by optimistic-lock="false"
					if ( oldFields[i] == null ) {
						update.addWhereColumns( getPropertyColumnNames( i ), " is null" );
					}
					else {
						update.addWhereColumns( getPropertyColumnNames( i ) );
					}
				}
			}

		}

		if ( getFactory().getSettings().isCommentsEnabled() ) {
			update.setComment( "update " + getEntityName() );
		}

		return hasColumns ? update.toStatementString() : null;
	}

	protected String generateInsertString(boolean[] includeProperty, int j) {
		return generateInsertString( false, includeProperty, j );
	}

	protected String generateInsertString(boolean identityInsert, boolean[] includeProperty) {
		return generateInsertString( identityInsert, includeProperty, 0 );
	}

	/**
	 * Generate the SQL that inserts a row
	 */
	protected String generateInsertString(boolean identityInsert, boolean[] includeProperty, int j) {

		Insert insert = new Insert( getFactory().getDialect() )
				.setTableName( getTableName( j ) );
		
		// add normal properties
		for ( int i = 0; i < entityMetamodel.getPropertySpan(); i++ ) {
			if ( includeProperty[i] && isPropertyOfTable( i, j ) ) {
				// this property belongs on the table and is to be inserted
				insert.addColumns( getPropertyColumnNames(i), propertyColumnInsertable[i] );
			}
		}
		
		// add the discriminator
		if ( j == 0 ) addDiscriminatorToInsert( insert );
		
		// add the primary key
		if ( j == 0 && identityInsert ) {
			insert.addIdentityColumn( getKeyColumns( 0 )[0] );
		}
		else {
			insert.addColumns( getKeyColumns( j ) );
		}

		if ( getFactory().getSettings().isCommentsEnabled() ) {
			insert.setComment( "insert " + getEntityName() );
		}

		String result = insert.toStatementString();
		
		// append the SQL to return the generated identifier
		if ( j == 0 && identityInsert && useInsertSelectIdentity() ) { //TODO: suck into Insert
			result = getFactory().getDialect().appendIdentitySelectToInsert( result );
		}

		return result;
	}

	/**
	 * Generate the SQL that deletes a row by id (and version)
	 */
	protected String generateDeleteString(int j) {
		Delete delete = new Delete()
				.setTableName( getTableName( j ) )
				.setPrimaryKeyColumnNames( getKeyColumns( j ) );
		if ( j == 0 ) delete.setVersionColumnName( getVersionColumnName() );
		if ( getFactory().getSettings().isCommentsEnabled() ) {
			delete.setComment( "delete " + getEntityName() );
		}
		return delete.toStatementString();
	}

	protected int dehydrate(
			Serializable id, 
			Object[] fields, 
			boolean[] includeProperty, 
			boolean[][] includeColumns, 
			int j, 
			PreparedStatement st, 
			SessionImplementor session)
	throws HibernateException, SQLException {
		return dehydrate( id, fields, null, includeProperty, includeColumns, j, st, session, 1 );
	}

	/**
	 * Marshall the fields of a persistent instance to a prepared statement
	 */
	protected int dehydrate(final Serializable id,
							final Object[] fields,
							final Object rowId,
							final boolean[] includeProperty,
							final boolean[][] includeColumns,
							final int j,
							final PreparedStatement ps,
							final SessionImplementor session,
							int index)
			throws SQLException, HibernateException {

		if ( log.isTraceEnabled() ) {
			log.trace( "Dehydrating entity: " + MessageHelper.infoString( this, id, getFactory() ) );
		}

		for ( int i = 0; i < entityMetamodel.getPropertySpan(); i++ ) {
			if ( includeProperty[i] && isPropertyOfTable( i, j ) ) {
				getPropertyTypes()[i].nullSafeSet( ps, fields[i], index, includeColumns[i], session );
				//index += getPropertyColumnSpan( i );
				index += ArrayHelper.countTrue( includeColumns[i] ); //TODO:  this is kinda slow...
			}
		}

		if ( rowId != null ) {
			ps.setObject( index, rowId );
			index += 1;
		}
		else if ( id != null ) {
			getIdentifierType().nullSafeSet( ps, id, index, session );
			index += getIdentifierColumnSpan();
		}

		return index;

	}

	/**
	 * Unmarshall the fields of a persistent instance from a result set,
	 * without resolving associations or collections. Question: should
	 * this really be here, or should it be sent back to Loader?
	 */
	public Object[] hydrate(final ResultSet rs,
							final Serializable id,
							final Object object,
							final Loadable rootLoadable,
							final SessionImplementor session,
							final String[][] suffixedPropertyColumns)
			throws SQLException, HibernateException {

		if ( log.isTraceEnabled() ) {
			log.trace( "Hydrating entity: " + MessageHelper.infoString( this, id, getFactory() ) );
		}

		final BasicEntityPersister rootPersister = (BasicEntityPersister) rootLoadable;

		final boolean hasDeferred = rootPersister.hasSequentialSelect();
		PreparedStatement sequentialSelect = null;
		ResultSet sequentialResultSet = null;
		try {

			if ( hasDeferred ) {
				final String sql = rootPersister.getSequentialSelect( getEntityName() );
				if ( sql != null ) {
					//TODO: I am not so sure about the exception handling in this bit!
					sequentialSelect = session.getBatcher().prepareSelectStatement( sql );
					rootPersister.getIdentifierType().nullSafeSet( sequentialSelect, id, 1, session );
					sequentialResultSet = sequentialSelect.executeQuery();
					sequentialResultSet.next();
				}
			}

			final String[] propNames = getPropertyNames();
			final Type[] types = getPropertyTypes();
			final Object[] values = new Object[types.length];
			final boolean[] laziness = getPropertyLaziness();
			final String[] propSubclassNames = getSubclassPropertySubclassNameClosure();

			for ( int i = 0; i < types.length; i++ ) {
				if ( !propertySelectable[i] ) {
					values[i] = BackrefPropertyAccessor.UNKNOWN;
				}
				else if ( !laziness[i] ) {
					//decide which ResultSet to get the property value from:
					final boolean propertyIsDeferred = hasDeferred &&
							rootPersister.isSubclassPropertyDeferred( propNames[i], propSubclassNames[i] );
					final ResultSet propertyResultSet = propertyIsDeferred ? sequentialResultSet : rs;
					final String[] cols = propertyIsDeferred ? propertyColumnAliases[i] : suffixedPropertyColumns[i];

					values[i] = types[i].hydrate( propertyResultSet, cols, session, object );
				}
				else {
					values[i] = LazyPropertyInitializer.UNFETCHED_PROPERTY;
				}
			}

			if ( sequentialResultSet != null ) sequentialResultSet.close();

			return values;

		}
		finally {
			if ( sequentialSelect != null ) {
				session.getBatcher().closeStatement( sequentialSelect );
			}
		}
	}

	protected boolean useInsertSelectIdentity() {
		return !useGetGeneratedKeys() && getFactory().getDialect().supportsInsertSelectIdentity();
	}

	protected boolean useGetGeneratedKeys() {
		return getFactory().getSettings().isGetGeneratedKeysEnabled();
	}
	
	protected String getSequentialSelect(String entityName) {
		throw new UnsupportedOperationException("no sequential selects");
	}

	/**
	 * Perform an SQL INSERT, and then retrieve a generated identifier
	 */
	protected Serializable insert(final Object[] fields,
								  final boolean[] notNull,
								  String sql,
								  final Object object,
								  final SessionImplementor session)
			throws HibernateException {

		if ( log.isTraceEnabled() ) {
			log.trace( "Inserting entity: " + getEntityName() + " (native id)" );
			if ( isVersioned() ) log.trace( "Version: " + Versioning.getVersion( fields, this ) );
		}

		try {

			//do the insert
			PreparedStatement insert = session.getBatcher().prepareStatement( sql, useGetGeneratedKeys() );
			try {
				dehydrate( null, fields, notNull, propertyColumnInsertable, 0, insert, session );

				if ( useInsertSelectIdentity() ) {
					if ( !insert.execute() ) {
						while ( !insert.getMoreResults() && insert.getUpdateCount() != -1 ) {
							continue;	// Do nothing (but stop checkstyle from complaining).
						}
					}
					//note early exit!
					ResultSet rs = insert.getResultSet();
					try {
						return IdentifierGeneratorFactory.getGeneratedIdentity( rs, getIdentifierType() );
					}
					finally {
						rs.close();
					}
				}
				else if ( useGetGeneratedKeys() ) {
					insert.executeUpdate();
					//note early exit!
					return IdentifierGeneratorFactory.getGeneratedIdentity( 
							GetGeneratedKeysHelper.getGeneratedKey(insert), 
							getIdentifierType() 
					);
				}
				else {
					insert.executeUpdate();
				}

			}
			finally {
				session.getBatcher().closeStatement( insert );
			}

		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
					getFactory().getSQLExceptionConverter(),
					sqle,
					"could not insert: " + MessageHelper.infoString(this),
			        sql
			);
		}
		
		return ( (PostInsertIdentifierGenerator) getIdentifierGenerator() ).getGenerated(session, object, this);

	}
	
	public String getIdentitySelectString() {
		//TODO: cache this in an instvar
		return getFactory().getDialect().getIdentitySelectString( 
				getTableName(0), 
				getKeyColumns(0)[0], 
				getIdentifierType().sqlTypes( getFactory() )[0]
		);
	}
	
	public String getSelectByUniqueKeyString(String propertyName) {
		return new SimpleSelect( getFactory().getDialect() )
			.setTableName( getTableName(0) )
			.addColumns( getKeyColumns(0) )
			.addCondition( getPropertyColumnNames(propertyName), "=?" )
			.toStatementString();
	}

	/**
	 * Perform an SQL INSERT
	 */
	protected void insert(final Serializable id,
						  final Object[] fields,
						  final boolean[] notNull,
						  final int j,
						  final String sql,
						  final Object object,
						  final SessionImplementor session)
			throws HibernateException {

		if ( isInverseTable( j ) ) return;
		
		//note: it is conceptually possible that a UserType could map null to
		//      a non-null value, so the following is arguable:
		if ( isNullableTable( j ) && isAllNull( fields, j ) ) return;

		if ( log.isTraceEnabled() ) {
			log.trace( "Inserting entity: " + MessageHelper.infoString( this, id, getFactory() ) );
			if ( j == 0 && isVersioned() ) log.trace( "Version: " + Versioning.getVersion( fields, this ) );
		}

		boolean callable = isInsertCallable( j );
		final boolean useBatch = j == 0 && !callable; //we can't batch joined inserts, *especially* not if it is an identity insert
		try {

			// Render the SQL query
			final PreparedStatement insert;
			if ( callable ) {
				insert = session.getBatcher().prepareCallableStatement( sql );
			}
			else if ( useBatch ) {
				insert = session.getBatcher().prepareBatchStatement( sql );
			}
			else {
				insert = session.getBatcher().prepareStatement( sql );
			}

			try {

				// Write the values of fields onto the prepared statement - we MUST use the state at the time the
				// insert was issued (cos of foreign key constraints). Not necessarily the object's current state

				dehydrate( id, fields, notNull, propertyColumnInsertable, j, insert, session );

				if ( useBatch ) {
					session.getBatcher().addToBatch( 1 );
				}
				else {
					insert.executeUpdate();
				}

			}
			catch ( SQLException sqle ) {
				if ( useBatch ) session.getBatcher().abortBatch( sqle );
				throw sqle;
			}
			finally {
				if ( !useBatch ) session.getBatcher().closeStatement( insert );
			}
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
					getFactory().getSQLExceptionConverter(),
					sqle,
					"could not insert: " + MessageHelper.infoString( this ),
			        sql
			);
		}

	}

	/**
	 * Perform an SQL UPDATE or SQL INSERT
	 */
	protected void updateOrInsert(final Serializable id,
								  final Object[] fields,
								  final Object[] oldFields,
								  final Object rowId,
								  final boolean[] includeProperty,
								  final int j,
								  final Object oldVersion,
								  final Object object,
								  final String sql,
								  final SessionImplementor session)
			throws HibernateException {

		if ( isInverseTable( j ) ) return;
		if ( !hasUpdateableColumns() ) return;

		final boolean isRowToUpdate;
		if ( isNullableTable( j ) && oldFields != null && isAllNull( oldFields, j ) ) {
			//don't bother trying to update, we know there is no row there yet
			isRowToUpdate = false;
		}
		else if ( isNullableTable( j ) && isAllNull( fields, j ) ) {
			//if all fields are null, we might need to delete existing row
			isRowToUpdate = true;
			delete( id, oldVersion, j, object, getSQLDeleteStrings()[j], session );
		}
		else {
			//there is probably a row there, so try to update
			//if no rows were updated, we will find out
			isRowToUpdate = update( id, fields, oldFields, rowId, includeProperty, j, oldVersion, object, sql, session );
		}

		if ( !isRowToUpdate && !isAllNull( fields, j ) ) {
			// assume that the row was not there since it previously had only null  
			// values, so do an INSERT instead
			//TODO: does not respect dynamic-insert
			insert( id, fields, getPropertyInsertability(), j, getSQLInsertStrings()[j], object, session );
		}


	}

	protected boolean update(final Serializable id,
							 final Object[] fields,
							 final Object[] oldFields,
							 final Object rowId,
							 final boolean[] includeProperty,
							 final int j,
							 final Object oldVersion,
							 final Object object,
							 final String sql,
							 final SessionImplementor session)
			throws HibernateException {

		final boolean useVersion = j == 0 && isVersioned();
		final boolean callable = isUpdateCallable( j );
		final boolean useBatch = j == 0 && !callable && isBatchable(); //note: updates to joined tables can't be batched...

		if ( log.isTraceEnabled() ) {
			log.trace( "Updating entity: " + MessageHelper.infoString( this, id, getFactory() ) );
			if ( useVersion ) log.trace( "Existing version: " + oldVersion + " -> New version: " + fields[getVersionProperty()] );
		}

		try {

			int index = 1; // starting index
			final PreparedStatement update;
			if ( callable ) {
				CallableStatement callstatement = session.getBatcher().prepareCallableStatement( sql );
				callstatement.registerOutParameter( index++, Types.NUMERIC ); // TODO: should we require users to return number of update rows ?
				update = callstatement;
			}
			else if ( useBatch ) {
				update = session.getBatcher().prepareBatchStatement( sql );
			}
			else {
				update = session.getBatcher().prepareStatement( sql );
			}

			try {

				//Now write the values of fields onto the prepared statement
				index = dehydrate( id, fields, rowId, includeProperty, propertyColumnUpdateable, j, update, session, index );
	
				// Write any appropriate versioning conditional parameters
				if ( useVersion && Versioning.OPTIMISTIC_LOCK_VERSION == entityMetamodel.getOptimisticLockMode() ) {
					getVersionType().nullSafeSet( update, oldVersion, index, session );
				}
				else if ( entityMetamodel.getOptimisticLockMode() > Versioning.OPTIMISTIC_LOCK_VERSION && oldFields != null ) {
					boolean[] includeOldField = entityMetamodel.getOptimisticLockMode() == Versioning.OPTIMISTIC_LOCK_ALL
							? getPropertyUpdateability()
							: includeProperty;
					for ( int i = 0; i < entityMetamodel.getPropertySpan(); i++ ) {
						if ( includeOldField[i] && isPropertyOfTable( i, j ) && oldFields[i] != null ) {
							getPropertyTypes()[i].nullSafeSet( update, oldFields[i], index, session );
							index += getPropertyColumnSpan( i );
						}
					}
				}

				if ( useBatch ) {
					session.getBatcher().addToBatch( 1 );
					return true;
				}
				else {
					return check( update.executeUpdate(), id, j );
				}

			}
			catch ( SQLException sqle ) {
				if ( useBatch ) session.getBatcher().abortBatch( sqle );
				throw sqle;
			}
			finally {
				if ( !useBatch ) session.getBatcher().closeStatement( update );
			}

		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
					getFactory().getSQLExceptionConverter(),
					sqle,
					"could not update: " + MessageHelper.infoString( this, id, getFactory() ),
			        sql
			);
		}
	}

	/**
	 * Perform an SQL DELETE
	 */
	protected void delete(final Serializable id,
						  final Object version,
						  final int j,
						  final Object object,
						  final String sql,
						  final SessionImplementor session)
			throws HibernateException {
		
		if ( isInverseTable( j ) ) return;

		final boolean useVersion = j == 0 && isVersioned();
		final boolean callable = isDeleteCallable( j );
		final boolean useBatch = j == 0 && isBatchable() && !callable;

		if ( log.isTraceEnabled() ) {
			log.trace( "Deleting entity: " + MessageHelper.infoString( this, id, getFactory() ) );
			if ( useVersion ) log.trace( "Version: " + version );
		}

		if ( isTableCascadeDeleteEnabled( j ) ) {
			if ( log.isTraceEnabled() ) {
				log.trace( "delete handled by foreign key constraint: " + getTableName( j ) );
			}
			return; //EARLY EXIT!
		}

		try {

			//Render the SQL query
			PreparedStatement delete;
			int index = 1;
			if ( callable ) {
				CallableStatement callstatement = session.getBatcher().prepareCallableStatement( sql );
				callstatement.registerOutParameter( index++, Types.NUMERIC ); // TODO: should we require users to return number of deleted rows ?
				delete = callstatement;
			}
			else if ( useBatch ) {
				delete = session.getBatcher().prepareBatchStatement( sql );
			}
			else {
				delete = session.getBatcher().prepareStatement( sql );
			}

			try {

				// Do the key. The key is immutable so we can use the _current_ object state - not necessarily
				// the state at the time the delete was issued

				getIdentifierType().nullSafeSet( delete, id, index, session );

				// We should use the _current_ object state (ie. after any updates that occurred during flush)

				if ( useVersion ) {
					getVersionType().nullSafeSet( delete, version, getIdentifierColumnSpan() + index, session );
				}

				if ( useBatch ) {
					session.getBatcher().addToBatch( 1 );
				}
				else {
					check( delete.executeUpdate(), id, j );
				}

			}
			catch ( SQLException sqle ) {
				if ( useBatch ) session.getBatcher().abortBatch( sqle );
				throw sqle;
			}
			finally {
				if ( !useBatch ) session.getBatcher().closeStatement( delete );
			}

		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
					getFactory().getSQLExceptionConverter(),
					sqle,
					"could not delete: " + 
					MessageHelper.infoString( this, id, getFactory() ),
			        sql
			);

		}

	}

	private String[] getUpdateStrings(boolean byRowId, boolean lazy) {
		if ( byRowId ) {
			return lazy ? getSQLLazyUpdateByRowIdStrings() : getSQLUpdateByRowIdStrings();
		}
		else {
			return lazy ? getSQLLazyUpdateStrings() : getSQLUpdateStrings();
		}
	}

	/**
	 * Update an object
	 */
	public void update(final Serializable id,
					   final Object[] fields,
					   final int[] dirtyFields,
					   final boolean hasDirtyCollection,
					   final Object[] oldFields,
					   final Object oldVersion,
					   final Object object,
					   final Object rowId,
					   final SessionImplementor session)
			throws HibernateException {

		//note: dirtyFields==null means we had no snapshot, and we couldn't get one using select-before-update
		//      oldFields==null just means we had no snapshot to begin with (we might have used select-before-update to get the dirtyFields)

		final boolean[] tableUpdateNeeded = getTableUpdateNeeded( dirtyFields, hasDirtyCollection );
		final int span = getTableSpan();

		final boolean[] propsToUpdate;
		final String[] updateStrings;
		if ( entityMetamodel.isDynamicUpdate() && dirtyFields != null ) {
			// For the case of dynamic-update="true", we need to generate the UPDATE SQL
			propsToUpdate = getPropertiesToUpdate( dirtyFields, hasDirtyCollection );
			// don't need to check property updatability or laziness 
			// (dirty checking algorithm handles that)
			updateStrings = new String[span];
			for ( int j = 0; j < span; j++ ) {
				updateStrings[j] = tableUpdateNeeded[j] ?
						generateUpdateString( propsToUpdate, j, oldFields, j == 0 && rowId != null ) :
						null;
			}
		}
		else {
			// For the case of dynamic-update="false", or no snapshot, we use the static SQL
			updateStrings = getUpdateStrings( rowId != null, hasUninitializedLazyProperties( object, session.getEntityMode() ) );
			propsToUpdate = getPropertyCheckability( object, session.getEntityMode() );
		}

		for ( int j = 0; j < span; j++ ) {
			// Now update only the tables with dirty properties (and the table with the version number)
			if ( tableUpdateNeeded[j] ) {
				updateOrInsert( id,
						fields,
						oldFields,
						j == 0 ? rowId : null,
						propsToUpdate,
						j,
						oldVersion,
						object,
						updateStrings[j],
						session );
			}
		}
	}

	public Serializable insert(Object[] fields, Object object, SessionImplementor session)
			throws HibernateException {

		final int span = getTableSpan();
		final Serializable id;
		if ( entityMetamodel.isDynamicInsert() ) {
			// For the case of dynamic-insert="true", we need to generate the INSERT SQL
			boolean[] notNull = getPropertiesToInsert( fields );
			id = insert( fields, notNull, generateInsertString( true, notNull ), object, session );
			for ( int j = 1; j < span; j++ ) {
				insert( id, fields, notNull, j, generateInsertString( notNull, j ), object, session );
			}
		}
		else {
			// For the case of dynamic-insert="false", use the static SQL
			id = insert( fields, getPropertyInsertability(), getSQLIdentityInsertString(), object, session );
			for ( int j = 1; j < span; j++ ) {
				insert( id, fields, getPropertyInsertability(), j, getSQLInsertStrings()[j], object, session );
			}
		}
		return id;
	}

	public void insert(Serializable id, Object[] fields, Object object, SessionImplementor session)
			throws HibernateException {

		final int span = getTableSpan();
		if ( entityMetamodel.isDynamicInsert() ) {
			// For the case of dynamic-insert="true", we need to generate the INSERT SQL
			boolean[] notNull = getPropertiesToInsert( fields );
			for ( int j = 0; j < span; j++ ) {
				insert( id, fields, notNull, j, generateInsertString( notNull, j ), object, session );
			}
		}
		else {
			// For the case of dynamic-insert="false", use the static SQL
			for ( int j = 0; j < span; j++ ) {
				insert( id, fields, getPropertyInsertability(), j, getSQLInsertStrings()[j], object, session );
			}
		}
	}

	/**
	 * Delete an object
	 */
	public void delete(Serializable id, Object version, Object object, SessionImplementor session)
			throws HibernateException {

		final int span = getTableSpan();
		for ( int j = span - 1; j >= 0; j-- ) {
			delete( id, version, j, object, getSQLDeleteStrings()[j], session );
		}

	}

	protected void logStaticSQL() {
		if ( log.isDebugEnabled() ) {
			log.debug( "Static SQL for entity: " + getEntityName() );
			if ( sqlLazySelectString != null ) log.debug( " Lazy select: " + sqlLazySelectString );
			if ( sqlVersionSelectString != null ) log.debug( " Version select: " + sqlVersionSelectString );
			if ( sqlSnapshotSelectString != null ) log.debug( " Snapshot select: " + sqlSnapshotSelectString );
			for ( int j = 0; j < getTableSpan(); j++ ) {
				log.debug( " Insert " + j + ": " + getSQLInsertStrings()[j] );
				log.debug( " Update " + j + ": " + getSQLUpdateStrings()[j] );
				log.debug( " Delete " + j + ": " + getSQLDeleteStrings()[j] );

			}
			if ( sqlIdentityInsertString != null ) log.debug( " Identity insert: " + sqlIdentityInsertString );
			if ( sqlUpdateByRowIdString != null ) log.debug( " Update by row id (all fields): " + sqlUpdateByRowIdString );
			if ( sqlLazyUpdateByRowIdString != null ) log.debug( " Update by row id (non-lazy fields): " + sqlLazyUpdateByRowIdString );
		}
	}

	public String filterFragment(String alias, Map enabledFilters) throws MappingException {
		// if we have any defined filters, see if they have been enabled on the session;
		// and if so, prepend them to the "static" filterFragment
		final StringBuffer sessionFilterFragment = new StringBuffer();
		final String filterConditionAlias = generateFilterConditionAlias( alias );
		for ( int i = 0; i < filterNames.length; i++ ) {
			if ( enabledFilters.containsKey( filterNames[i] ) ) {
				final String condition = filterConditions[i];
				if ( StringHelper.isNotEmpty( condition ) ) {
					sessionFilterFragment.append( " and " )
							.append( StringHelper.replace( condition, FilterImpl.MARKER, filterConditionAlias ) );
				}
			}
		}

		return sessionFilterFragment.append( filterFragment( alias ) ).toString();
	}

	protected String generateFilterConditionAlias(String rootAlias) {
		return rootAlias;
	}

	public String oneToManyFilterFragment(String alias) throws MappingException {
		return "";
	}

	public String fromJoinFragment(String alias, boolean innerJoin, boolean includeSubclasses) {
		return getSubclassTableSpan() == 1 ?
				"" : //just a performance opt!
				createJoin( alias, innerJoin, includeSubclasses ).toFromFragmentString();
	}

	public String whereJoinFragment(String alias, boolean innerJoin, boolean includeSubclasses) {
		return getSubclassTableSpan() == 1 ?
				"" : //just a performance opt!
				createJoin( alias, innerJoin, includeSubclasses ).toWhereFragmentString();
	}

	protected boolean isSubclassTableLazy(int j) {
		return false;
	}

	protected JoinFragment createJoin(String name, boolean innerJoin, boolean includeSubclasses) {
		final String[] idCols = StringHelper.qualify( name, getIdentifierColumnNames() ); //all joins join to the pk of the driving table
		final JoinFragment join = getFactory().getDialect().createOuterJoinFragment();
		final int tableSpan = getSubclassTableSpan();
		for ( int j = 1; j < tableSpan; j++ ) { //notice that we skip the first table; it is the driving table!
			final boolean joinIsIncluded = isClassOrSuperclassTable( j ) ||
					( includeSubclasses && !isSubclassTableSequentialSelect( j ) && !isSubclassTableLazy( j ) );
			if ( joinIsIncluded ) {
				join.addJoin( getSubclassTableName( j ),
						generateTableAlias( name, j ),
						idCols,
						getSubclassTableKeyColumns( j ),
						innerJoin && isClassOrSuperclassTable( j ) && !isInverseTable( j ) && !isNullableTable( j ) ?
						JoinFragment.INNER_JOIN : //we can inner join to superclass tables (the row MUST be there)
						JoinFragment.LEFT_OUTER_JOIN //we can never inner join to subclass tables
				);
			}
		}
		return join;
	}

	protected JoinFragment createJoin(int[] tableNumbers, String drivingAlias) {
		final String[] keyCols = StringHelper.qualify( drivingAlias, getSubclassTableKeyColumns( tableNumbers[0] ) );
		final JoinFragment jf = getFactory().getDialect().createOuterJoinFragment();
		for ( int i = 1; i < tableNumbers.length; i++ ) { //skip the driving table
			final int j = tableNumbers[i];
			jf.addJoin( getSubclassTableName( j ),
					generateTableAlias( getRootAlias(), j ),
					keyCols,
					getSubclassTableKeyColumns( j ),
					isInverseSubclassTable( j ) || isNullableSubclassTable( j ) ?
					JoinFragment.LEFT_OUTER_JOIN :
					JoinFragment.INNER_JOIN );
		}
		return jf;
	}

	protected SelectFragment createSelect(final int[] subclassColumnNumbers,
										  final int[] subclassFormulaNumbers) {

		SelectFragment selectFragment = new SelectFragment();

		int[] columnTableNumbers = getSubclassColumnTableNumberClosure();
		String[] columnAliases = getSubclassColumnAliasClosure();
		String[] columns = getSubclassColumnClosure();
		for ( int i = 0; i < subclassColumnNumbers.length; i++ ) {
			if ( subclassColumnSelectableClosure[i] ) {
				int columnNumber = subclassColumnNumbers[i];
				final String subalias = generateTableAlias( getRootAlias(), columnTableNumbers[columnNumber] );
				selectFragment.addColumn( subalias, columns[columnNumber], columnAliases[columnNumber] );
			}
		}

		int[] formulaTableNumbers = getSubclassFormulaTableNumberClosure();
		String[] formulaTemplates = getSubclassFormulaTemplateClosure();
		String[] formulaAliases = getSubclassFormulaAliasClosure();
		for ( int i = 0; i < subclassFormulaNumbers.length; i++ ) {
			int formulaNumber = subclassFormulaNumbers[i];
			final String subalias = generateTableAlias( getRootAlias(), formulaTableNumbers[formulaNumber] );
			selectFragment.addFormula( subalias, formulaTemplates[formulaNumber], formulaAliases[formulaNumber] );
		}

		return selectFragment;
	}

	protected String createFrom(int tableNumber, String alias) {
		return getSubclassTableName( tableNumber ) + ' ' + alias;
	}

	protected String createWhereByKey(int tableNumber, String alias) {
		//TODO: move to .sql package, and refactor with similar things!
		return StringHelper.join( "=? and ",
				StringHelper.qualify( alias, getSubclassTableKeyColumns( tableNumber ) ) ) + "=?";
	}

	protected String renderSelect(final int[] tableNumbers,
								  final int[] columnNumbers,
								  final int[] formulaNumbers) {

		Arrays.sort( tableNumbers ); //get 'em in the right order (not that it really matters)
		
		//render the where and from parts
		int drivingTable = tableNumbers[0];
		final String drivingAlias = generateTableAlias( getRootAlias(), drivingTable ); //we *could* regerate this inside each called method!
		final String where = createWhereByKey( drivingTable, drivingAlias );
		final String from = createFrom( drivingTable, drivingAlias );
	
		//now render the joins
		JoinFragment jf = createJoin( tableNumbers, drivingAlias );
		
		//now render the select clause
		SelectFragment selectFragment = createSelect( columnNumbers, formulaNumbers );
		
		//now tie it all together
		Select select = new Select( getFactory().getDialect() );
		select.setSelectClause( selectFragment.toFragmentString().substring( 2 ) );
		select.setFromClause( from );
		select.setWhereClause( where );
		select.setOuterJoins( jf.toFromFragmentString(), jf.toWhereFragmentString() );
		if ( getFactory().getSettings().isCommentsEnabled() ) {
			select.setComment( "sequential select " + getEntityName() );
		}
		return select.toStatementString();
	}

	private String getRootAlias() {
		return StringHelper.generateAlias( getEntityName() );
	}
	
	protected void postConstruct(Mapping mapping) throws MappingException {
		initPropertyPaths(mapping);
		
		//insert/update/delete SQL
		final int joinSpan = getTableSpan();
		sqlDeleteStrings = new String[joinSpan];
		sqlInsertStrings = new String[joinSpan];
		sqlUpdateStrings = new String[joinSpan];
		sqlLazyUpdateStrings = new String[joinSpan];

		sqlUpdateByRowIdString = rowIdName == null ?
				null :
				generateUpdateString( getPropertyUpdateability(), 0, true );
		sqlLazyUpdateByRowIdString = rowIdName == null ?
				null :
				generateUpdateString( getPropertyCheckability(), 0, true );

		for ( int j = 0; j < joinSpan; j++ ) {
			sqlInsertStrings[j] = customSQLInsert[j] == null ?
					generateInsertString( getPropertyInsertability(), j ) :
					customSQLInsert[j];
			sqlUpdateStrings[j] = customSQLUpdate[j] == null ?
					generateUpdateString( getPropertyUpdateability(), j, false ) :
					customSQLUpdate[j];
			sqlLazyUpdateStrings[j] = customSQLUpdate[j] == null ?
					generateUpdateString( getPropertyCheckability(), j, false ) :
					customSQLUpdate[j];
			sqlDeleteStrings[j] = customSQLDelete[j] == null ?
					generateDeleteString( j ) :
					customSQLDelete[j];
		}

		tableHasColumns = new boolean[joinSpan];
		for ( int j = 0; j < joinSpan; j++ ) {
			tableHasColumns[j] = sqlUpdateStrings[j] != null;
		}
		
		//select SQL
		sqlSnapshotSelectString = generateSnapshotSelectString();
		sqlLazySelectString = generateLazySelectString();
		sqlVersionSelectString = generateSelectVersionString();
		if ( isIdentifierAssignedByInsert() ) {
			sqlIdentityInsertString = customSQLInsert[0] == null ?
					generateInsertString( true, getPropertyInsertability() ) :
					customSQLInsert[0];
		}
		else {
			sqlIdentityInsertString = null;
		}

		logStaticSQL();
		
	}

	public void postInstantiate() throws MappingException {

		createLoaders();
		createUniqueKeyLoaders();
		createQueryLoader();

	}

	private void createLoaders() {
		loaders.put( LockMode.NONE, createEntityLoader( LockMode.NONE ) );

		UniqueEntityLoader readLoader = createEntityLoader( LockMode.READ );
		loaders.put( LockMode.READ, readLoader );
		
		//TODO: inexact, what we really need to know is: are any outer joins used?
		boolean disableForUpdate = getSubclassTableSpan() > 1 &&
				hasSubclasses() &&
				!getFactory().getDialect().supportsOuterJoinForUpdate();

		loaders.put( LockMode.UPGRADE,
				disableForUpdate ? readLoader : createEntityLoader( LockMode.UPGRADE ) );
		loaders.put( LockMode.UPGRADE_NOWAIT,
				disableForUpdate ? readLoader : createEntityLoader( LockMode.UPGRADE_NOWAIT ) );
	}

	protected void createQueryLoader() {
		if ( loaderName != null ) queryLoader = new NamedQueryLoader( loaderName, this );
	}

	/**
	 * Load an instance using either the <tt>forUpdateLoader</tt> or the outer joining <tt>loader</tt>,
	 * depending upon the value of the <tt>lock</tt> parameter
	 */
	public Object load(Serializable id, Object optionalObject, LockMode lockMode, SessionImplementor session)
			throws HibernateException {

		if ( log.isTraceEnabled() ) {
			log.trace( "Materializing entity: " + MessageHelper.infoString( this, id, getFactory() ) );
		}

		final UniqueEntityLoader loader = getAppropriateLoader( lockMode, session.getEnabledFilters() );
		return loader.load( id, optionalObject, session );
	}

	private UniqueEntityLoader getAppropriateLoader(LockMode lockMode, Map enabledFilters) {
		if ( queryLoader != null ) {
			return queryLoader;
		}
		else if ( enabledFilters == null || enabledFilters.isEmpty() ) {
			return (UniqueEntityLoader) loaders.get( lockMode );
		}
		else {
			return createEntityLoader( lockMode, enabledFilters );
		}
	}

	private boolean isAllNull(Object[] array, int tableNumber) {
		for ( int i = 0; i < array.length; i++ ) {
			if ( isPropertyOfTable( i, tableNumber ) && array[i] != null ) return false;
		}
		return true;
	}
	
	public boolean isSubclassPropertyNullable(int i) {
		return subclassPropertyNullabilityClosure[i];
	}

	/**
	 * Transform the array of property indexes to an array of booleans,
	 * true when the property is dirty (we do not need to check updatability,
	 * since the dirty checking algorithm already did that)
	 */
	protected final boolean[] getPropertiesToUpdate(final int[] dirtyProperties, final boolean hasDirtyCollection) {
		final boolean[] propsToUpdate = new boolean[ entityMetamodel.getPropertySpan() ];
		for ( int j = 0; j < dirtyProperties.length; j++ ) {
			propsToUpdate[dirtyProperties[j]] = true;
		}
		if ( isVersioned() ) {
			propsToUpdate[getVersionProperty()] = Versioning.isVersionIncrementRequired( dirtyProperties, hasDirtyCollection, getPropertyVersionability() );
		}
		return propsToUpdate;
	}

	/**
	 * Transform the array of property indexes to an array of booleans,
	 * true when the property is insertable and non-null
	 */
	protected boolean[] getPropertiesToInsert(Object[] fields) {
		boolean[] notNull = new boolean[fields.length];
		boolean[] insertable = getPropertyInsertability();
		for ( int i = 0; i < fields.length; i++ ) notNull[i] = insertable[i] && fields[i] != null;
		return notNull;
	}

    /**
     * Locate the property-indices of all properties considered to be dirty.
     *
     * @param currentState The current state of the entity (the state to be checked).
     * @param previousState The previous state of the entity (the state to be checked against).
     * @param entity The entity for which we are checking state dirtiness.
     * @param session The session in which the check is ccurring.
     * @return <tt>null</tt> or the indices of the dirty properties
     * @throws HibernateException
     */
	public int[] findDirty(Object[] currentState, Object[] previousState, Object entity, SessionImplementor session)
	throws HibernateException {
		int[] props = TypeFactory.findDirty(
		        entityMetamodel.getProperties(),
		        currentState,
		        previousState,
		        hasUninitializedLazyProperties( entity, session.getEntityMode() ),
		        session
		);
		if ( props == null ) {
			return null;
		}
		else {
			logDirtyProperties( props );
			return props;
		}
    }

    /**
     * Locate the property-indices of all properties considered to be dirty.
     *
     * @param old The old state of the entity.
     * @param current The current state of the entity.
     * @param entity The entity for which we are checking state modification.
     * @param session The session in which the check is ccurring.
     * @return <tt>null</tt> or the indices of the modified properties
     * @throws HibernateException
     */
	public int[] findModified(Object[] old, Object[] current, Object entity, SessionImplementor session)
	throws HibernateException {
		int[] props = TypeFactory.findModified(
		        entityMetamodel.getProperties(),
		        old,
		        current,
		        hasUninitializedLazyProperties( entity, session.getEntityMode() ),
		        session
		);
		if ( props == null ) {
			return null;
		}
		else {
			logDirtyProperties( props );
			return props;
		}
    }

	/**
	 * Which properties should we dirty check?
	 * (Initialized, updatable ones!)
	 */
	protected boolean[] getPropertyCheckability(Object entity, EntityMode entityMode) {
		return hasUninitializedLazyProperties( entity, entityMode ) ?
				getPropertyCheckability() :
				getPropertyUpdateability();
	}

	private void logDirtyProperties(int[] props) {
		if ( log.isTraceEnabled() ) {
			for ( int i = 0; i < props.length; i++ ) {
				log.trace( StringHelper.qualify( getEntityName(), entityMetamodel.getProperties()[ props[i] ].getName() ) + " is dirty" );
			}
		}
	}

	protected EntityTuplizer getTuplizer(SessionImplementor session) {
		return getTuplizer( session.getEntityMode() );
	}

	protected EntityTuplizer getTuplizer(EntityMode entityMode) {
		return entityMetamodel.getTuplizer( entityMode );
	}

	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	protected EntityMetamodel getEntityMetamodel() {
		return entityMetamodel;
	}

	public boolean hasCache() {
		return cache != null;
	}

	public CacheConcurrencyStrategy getCache() {
		return cache;
	}

	public CacheEntryStructure getCacheEntryStructure() {
		return cacheEntryStructure;
	}

	// temporary ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	public final String getEntityName() {
		return entityMetamodel.getName();
	}

	private String getSubclassEntityName(Class clazz) {
		return ( String ) entityNameBySubclass.get( clazz );
	}

	public boolean isPolymorphic() {
		return entityMetamodel.isPolymorphic();
	}

	public boolean isInherited() {
		return entityMetamodel.isInherited();
	}

	public boolean hasCascades() {
		return entityMetamodel.hasCascades();
	}

	public boolean hasIdentifierProperty() {
		return !entityMetamodel.getIdentifierProperty().isVirtual();
	}

	public VersionType getVersionType() {
		return ( VersionType ) locateVersionType();
	}

	private Type locateVersionType() {
		return entityMetamodel.getVersionProperty() == null ?
		        null :
		        entityMetamodel.getVersionProperty().getType();
	}

	public int getVersionProperty() {
		return entityMetamodel.getVersionPropertyIndex();
	}

	public boolean isVersioned() {
		return entityMetamodel.isVersioned();
	}

	public boolean isIdentifierAssignedByInsert() {
		return entityMetamodel.getIdentifierProperty().isIdentifierAssignedByInsert();
	}

	protected boolean hasLazyProperties() {
		return entityMetamodel.hasLazyProperties();
	}

//	public boolean hasUninitializedLazyProperties(Object entity) {
//		if ( hasLazyProperties() ) {
//			InterceptFieldCallback callback = ( ( InterceptFieldEnabled ) entity ).getInterceptFieldCallback();
//			return callback != null && !( ( FieldInterceptor ) callback ).isInitialized();
//		}
//		else {
//			return false;
//		}
//	}

	public void afterReassociate(Object entity, SessionImplementor session) {
		if ( hasLazyProperties() ) {
			InterceptFieldCallback interceptor = ( ( InterceptFieldEnabled ) entity ).getInterceptFieldCallback();
			if ( interceptor != null ) {
				( ( FieldInterceptor ) interceptor ).setSession( session );
			}
		}
	}

	public Boolean isTransient(Object entity, SessionImplementor session) throws HibernateException {
		final Serializable id;
		if ( hasIdentifierPropertyOrEmbeddedCompositeIdentifier() ) {
			id = getIdentifier( entity, session.getEntityMode() );
		}
		else {
			id = null;
		}
		// we *always* assume an instance with a null
		// identifier or no identifier property is unsaved!
		if ( id == null ) return Boolean.TRUE;

		// check the version unsaved-value, if appropriate
		final Object version = getVersion( entity, session.getEntityMode() );
		if ( isVersioned() ) {
			// let this take precedence if defined, since it works for
			// assigned identifiers
			Boolean result = entityMetamodel.getVersionProperty().getUnsavedValue().isUnsaved( version );
			if ( result != null ) return result;
		}

		// check the id unsaved-value
		Boolean result = entityMetamodel.getIdentifierProperty().getUnsavedValue().isUnsaved( id );
		if ( result != null ) return result;

		// check to see if it is in the second-level cache
		if ( hasCache() ) {
			CacheKey ck = new CacheKey( id, getIdentifierType(), getRootEntityName(), session.getEntityMode() );
			if ( getCache().get( ck, session.getTimestamp() ) != null ) return Boolean.FALSE;
		}

		return null;
	}

	public boolean hasCollections() {
		return entityMetamodel.hasCollections();
	}

	public boolean isMutable() {
		return entityMetamodel.isMutable();
	}

	public boolean isAbstract() {
		return entityMetamodel.isAbstract();
	}

	public boolean hasSubclasses() {
		return entityMetamodel.hasSubclasses();
	}

	public boolean hasProxy() {
		return entityMetamodel.isLazy();
	}

	public IdentifierGenerator getIdentifierGenerator() throws HibernateException {
		return entityMetamodel.getIdentifierProperty().getIdentifierGenerator();
	}

	public String getRootEntityName() {
		return entityMetamodel.getRootName();
	}

	public ClassMetadata getClassMetadata() {
		return this;
	}

	public String getMappedSuperclass() {
		return entityMetamodel.getSuperclass();
	}

	public boolean isExplicitPolymorphism() {
		return entityMetamodel.isExplicitPolymorphism();
	}

	protected boolean useDynamicUpdate() {
		return entityMetamodel.isDynamicUpdate();
	}

	protected boolean useDynamicInsert() {
		return entityMetamodel.isDynamicInsert();
	}

	protected boolean hasEmbeddedIdentifier() {
		return entityMetamodel.getIdentifierProperty().isEmbedded();
	}

	public boolean hasIdentifierPropertyOrEmbeddedCompositeIdentifier() {
		return hasIdentifierProperty() || hasEmbeddedIdentifier();
	}

	public String[] getKeyColumnNames() {
		return getIdentifierColumnNames();
	}

	public String getName() {
		return getEntityName();
	}

	public boolean isCollection() {
		return false;
	}

	public boolean consumesAlias() {
		return true;
	}

	public Type getPropertyType(String propertyName) throws MappingException {
		return getPropertyTypes() [ entityMetamodel.getPropertyIndex( propertyName ) ];
	}

	public Type getType() {
		return entityMetamodel.getEntityType();
	}

	public boolean isSelectBeforeUpdateRequired() {
		return entityMetamodel.isSelectBeforeUpdate();
	}

	protected final int optimisticLockMode() {
		return entityMetamodel.getOptimisticLockMode();
	}

	public Object createProxy(Serializable id, SessionImplementor session) throws HibernateException {
		return entityMetamodel.getTuplizer( session.getEntityMode() ).createProxy( id, session );
	}

	public String toString() {
		return StringHelper.unqualify( getClass().getName() ) + '(' + entityMetamodel.getName() + ')';
	}

	public final String selectFragment(String alias, String suffix, boolean includeCollectionColumns) {
		return selectFragment( alias, suffix );
	}

	/**
	 * Not called at runtime, only during constructors
	 */
	protected boolean isLazyAvailable() {
		EntityTuplizer t = entityMetamodel.getTuplizerOrNull(EntityMode.POJO);
		return t!=null && t.isLazyPropertyLoadingAvailable();
	}

	public void afterInitialize(Object entity, boolean lazyPropertiesAreUnfetched, SessionImplementor session) {
		getTuplizer( session ).afterInitialize( entity, lazyPropertiesAreUnfetched, session );
	}

	public String[] getPropertyNames() {
		return entityMetamodel.getPropertyNames();
	}

	public Type[] getPropertyTypes() {
		return entityMetamodel.getPropertyTypes();
	}

	public boolean[] getPropertyLaziness() {
		return entityMetamodel.getPropertyLaziness();
	}

	public boolean[] getPropertyUpdateability() {
		return entityMetamodel.getPropertyUpdateability();
	}

	public boolean[] getPropertyCheckability() {
		return entityMetamodel.getPropertyCheckability();
	}

	public boolean[] getPropertyInsertability() {
		return entityMetamodel.getPropertyInsertability();
	}

	public boolean[] getPropertyNullability() {
		return entityMetamodel.getPropertyNullability();
	}

	public boolean[] getPropertyVersionability() {
		return entityMetamodel.getPropertyVersionability();
	}

	public Cascades.CascadeStyle[] getPropertyCascadeStyles() {
		return entityMetamodel.getCascadeStyles();
	}

	public final Class getMappedClass(EntityMode entityMode) {
		Tuplizer tup = entityMetamodel.getTuplizerOrNull(entityMode);
		return tup==null ? null : tup.getMappedClass();
	}

	public boolean implementsLifecycle(EntityMode entityMode) {
		return getTuplizer( entityMode ).isLifecycleImplementor();
	}

	public boolean implementsValidatable(EntityMode entityMode) {
		return getTuplizer( entityMode ).isValidatableImplementor();
	}

	public Class getConcreteProxyClass(EntityMode entityMode) {
		return getTuplizer( entityMode ).getConcreteProxyClass();
	}

	public void setPropertyValues(Object object, Object[] values, EntityMode entityMode)
	        throws HibernateException {
		getTuplizer( entityMode ).setPropertyValues( object, values );
	}

	public void setPropertyValue(Object object, int i, Object value, EntityMode entityMode)
	        throws HibernateException {
		getTuplizer( entityMode ).setPropertyValue( object, i, value );
	}

	public Object[] getPropertyValues(Object object, EntityMode entityMode)
	        throws HibernateException {
		return getTuplizer( entityMode ).getPropertyValues( object );
	}

	public Object getPropertyValue(Object object, int i, EntityMode entityMode)
	        throws HibernateException {
		return getTuplizer( entityMode ).getPropertyValue( object , i );
	}

	public Object getPropertyValue(Object object, String propertyName, EntityMode entityMode)
	        throws HibernateException {
		return getTuplizer( entityMode ).getPropertyValue( object, propertyName );
	}

	public Serializable getIdentifier(Object object, EntityMode entityMode)
	        throws HibernateException {
		return getTuplizer( entityMode ).getIdentifier( object );
	}

	public void setIdentifier(Object object, Serializable id, EntityMode entityMode)
	        throws HibernateException {
		getTuplizer( entityMode ).setIdentifier( object, id );
	}

	public Object getVersion(Object object, EntityMode entityMode)
	        throws HibernateException {
		return getTuplizer( entityMode ).getVersion( object );
	}

	public Object instantiate(Serializable id, EntityMode entityMode)
	        throws HibernateException {
		return getTuplizer( entityMode ).instantiate( id );
	}

	public boolean isInstance(Object object, EntityMode entityMode) {
		return getTuplizer( entityMode ).isInstance( object );
	}

	public boolean hasUninitializedLazyProperties(Object object, EntityMode entityMode) {
		return getTuplizer( entityMode ).hasUninitializedLazyProperties( object );
	}

	public void resetIdentifier(Object entity, Serializable currentId, Object currentVersion, EntityMode entityMode) {
		getTuplizer( entityMode ).resetIdentifier( entity, currentId, currentVersion );
	}

	public EntityPersister getSubclassEntityPersister(Object instance, SessionFactoryImplementor factory, EntityMode entityMode) {
		if ( !hasSubclasses() ) {
			return this;
		}
		else {
			Class clazz = instance.getClass();
			if ( clazz == getMappedClass( entityMode ) ) {
				return this;
			}
			else {
				String subclassEntityName = getSubclassEntityName( clazz );
				if ( subclassEntityName == null ) {
					throw new HibernateException( "instance not of expected entity type: " + getEntityName() );
				}
				else {
					return factory.getEntityPersister( subclassEntityName );
				}
			}
		}
	}

	public EntityMode guessEntityMode(Object object) {
		return entityMetamodel.guessEntityMode(object);
	}


	protected int getPropertySpan() {
		return entityMetamodel.getPropertySpan();
	}

	public Object[] getPropertyValuesToInsert(Object object, SessionImplementor session) throws HibernateException {
		return getTuplizer( session.getEntityMode() ).getPropertyValuesToInsert( object, session );
	}

	public String getIdentifierPropertyName() {
		return entityMetamodel.getIdentifierProperty().getName();
	}

	public Type getIdentifierType() {
		return entityMetamodel.getIdentifierProperty().getType();
	}
	
	public boolean hasSubselectLoadableCollections() {
		return hasSubselectLoadableCollections;
	}

	public void setPropertyValue(Object object, String propertyName, Object value, EntityMode entityMode)
	        throws HibernateException {
		getTuplizer( entityMode ).setPropertyValue( object, propertyName, value );
	}
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

}
