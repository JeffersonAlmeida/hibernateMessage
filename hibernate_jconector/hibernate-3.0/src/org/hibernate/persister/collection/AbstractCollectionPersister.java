//$Id: AbstractCollectionPersister.java,v 1.16 2005/03/24 18:33:23 oneovthafew Exp $
package org.hibernate.persister.collection;

import java.io.Serializable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.AssertionFailure;
import org.hibernate.FetchMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.cache.CacheConcurrencyStrategy;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.entry.CacheEntryStructure;
import org.hibernate.cache.entry.StructuredCollectionCacheEntry;
import org.hibernate.cache.entry.StructuredMapCacheEntry;
import org.hibernate.cache.entry.UnstructuredCacheEntry;
import org.hibernate.cfg.Configuration;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.SubselectFetch;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.exception.SQLExceptionConverter;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.impl.FilterImpl;
import org.hibernate.loader.collection.CollectionInitializer;
import org.hibernate.loader.collection.SubselectCollectionLoader;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.List;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.pretty.MessageHelper;
import org.hibernate.sql.Alias;
import org.hibernate.sql.SelectFragment;
import org.hibernate.sql.Template;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.CollectionHelper;
import org.hibernate.util.StringHelper;


/**
 * Base implementation of the <tt>QueryableCollection</tt> interface.
 *
 * @author Gavin King
 * @see BasicCollectionPersister
 * @see OneToManyPersister
 */
public abstract class AbstractCollectionPersister
		implements CollectionMetadata, QueryableCollection {
	// TODO: encapsulate the protected instance variables!

	private final String role;

	//SQL statements
	private final String sqlDeleteString;
	private final String sqlInsertRowString;
	private final String sqlUpdateRowString;
	private final String sqlDeleteRowString;

	private final String sqlOrderByString;
	protected final String sqlWhereString;
	private final String sqlOrderByStringTemplate;
	private final String sqlWhereStringTemplate;
	private final boolean hasOrder;
	protected final boolean hasWhere;
	private final int baseIndex;
	
	private final String nodeName;
	private final String elementNodeName;
	private final String indexNodeName;

	protected final boolean indexIsFormula;
	protected final boolean elementIsFormula;
	
	//types
	private final Type keyType;
	private final Type indexType;
	protected final Type elementType;
	private final Type identifierType;

	//columns
	protected final String[] keyColumnNames;
	protected final String[] indexColumnNames;
	protected final String[] indexFormulaTemplates;
	protected final boolean[] indexColumnIsSettable;
	protected final String[] elementColumnNames;
	protected final String[] elementFormulaTemplates;
	protected final boolean[] elementColumnIsSettable;
	protected final String[] indexColumnAliases;
	protected final String[] elementColumnAliases;
	protected final String[] keyColumnAliases;
	
	protected final String identifierColumnName;
	private final String identifierColumnAlias;
	private final String unquotedIdentifierColumnName;

	protected final String qualifiedTableName;

	private final String queryLoaderName;

	private final boolean isPrimitiveArray;
	private final boolean isArray;
	protected final boolean hasIndex;
	protected final boolean hasIdentifier;
	private final boolean isLazy;
	private final boolean isInverse;
	private final boolean isVersioned;
	protected final int batchSize;
	private final FetchMode fetchMode;
	private final boolean hasOrphanDelete;
	private final boolean subselectLoadable;

	//extra information about the element type
	private final Class elementClass;
	private final String entityName;

	private final Dialect dialect;
	private final SQLExceptionConverter sqlExceptionConverter;
	private final SessionFactoryImplementor factory;
	private final EntityPersister ownerPersister;
	private final IdentifierGenerator identifierGenerator;
	private final PropertyMapping elementPropertyMapping;
	private final EntityPersister elementPersister;
	private final CacheConcurrencyStrategy cache;
	private final CollectionType collectionType;
	private CollectionInitializer initializer;
	
	private final CacheEntryStructure cacheEntryStructure;

	//information relating to dynamic filters
	private final String[] filterNames;
	private final String[] filterConditions;

	// custom sql
	private final boolean insertCallable;
	private final boolean updateCallable;
	private final boolean deleteCallable;
	private final boolean deleteAllCallable;

	private final Serializable[] spaces;

	private static final Log log = LogFactory.getLog( AbstractCollectionPersister.class );

	public AbstractCollectionPersister(
			final Collection collection,
			final CacheConcurrencyStrategy cache,
			final Configuration cfg,
			final SessionFactoryImplementor factory)
	throws MappingException, CacheException {

		this.factory = factory;
		this.cache = cache;
		if ( factory.getSettings().isStructuredCacheEntriesEnabled() ) {
			cacheEntryStructure = collection.isMap() ? 
					(CacheEntryStructure) new StructuredMapCacheEntry() : 
					(CacheEntryStructure) new StructuredCollectionCacheEntry();
		}
		else {
			cacheEntryStructure = new UnstructuredCacheEntry();
		}
		
		dialect = factory.getDialect();
		sqlExceptionConverter = factory.getSQLExceptionConverter();
		collectionType = collection.getCollectionType();
		role = collection.getRole();
		entityName = collection.getOwnerEntityName();
		ownerPersister = factory.getEntityPersister(entityName);
		queryLoaderName = collection.getLoaderName();
		Alias alias = new Alias("__");
		nodeName = collection.getNodeName();

		Table table = collection.getCollectionTable();
		fetchMode = collection.getElement().getFetchMode();
		elementType = collection.getElement().getType();
		//isSet = collection.isSet();
		//isSorted = collection.isSorted();
		isPrimitiveArray = collection.isPrimitiveArray();
		isArray = collection.isArray();
		subselectLoadable = collection.isSubselectLoadable();
		
		qualifiedTableName = table.getQualifiedName( 
				dialect,
				factory.getSettings().getDefaultCatalogName(),
				factory.getSettings().getDefaultSchemaName() 
		);

		int spacesSize = 1 + collection.getSynchronizedTables().size();
		spaces = new String[spacesSize];
		spaces[0] = qualifiedTableName;
		Iterator iter = collection.getSynchronizedTables().iterator();
		for ( int i = 1; i < spacesSize; i++ ) {
			spaces[i] = (String) iter.next();
		}
		
		sqlOrderByString = collection.getOrderBy();
		hasOrder = sqlOrderByString != null;
		sqlOrderByStringTemplate = hasOrder ?
				Template.renderOrderByStringTemplate(sqlOrderByString, dialect) :
				null;
		sqlWhereString = collection.getWhere();
		hasWhere = sqlWhereString != null;
		sqlWhereStringTemplate = hasWhere ?
				Template.renderWhereStringTemplate(sqlWhereString, dialect) :
				null;

		hasOrphanDelete = collection.hasOrphanDelete();

		int batch = collection.getBatchSize();
		if (batch==-1) batch = factory.getSettings().getDefaultBatchFetchSize();
		batchSize = batch;

		isVersioned = collection.isOptimisticLocked();
		
		// KEY

		keyType = collection.getKey().getType();
		iter = collection.getKey().getColumnIterator();
		int keySpan = collection.getKey().getColumnSpan();
		keyColumnNames = new String[keySpan];
		String[] keyAliases = new String[keySpan];
		int k = 0;
		while ( iter.hasNext() ) {
			Column col = ( (Column) iter.next() );
			keyColumnNames[k] = col.getQuotedName(dialect);
			keyAliases[k] = col.getAlias();
			k++;
		}
		keyColumnAliases = alias.toAliasStrings(keyAliases);
		//unquotedKeyColumnNames = StringHelper.unQuote(keyColumnAliases);

		//ELEMENT

		int elementSpan = collection.getElement().getColumnSpan();
		iter = collection.getElement().getColumnIterator();

		String elemNode = collection.getElementNodeName();
		if ( elementType.isEntityType() ) {
			String entityName = ( (EntityType) elementType ).getAssociatedEntityName();
			elementPersister = factory.getEntityPersister(entityName);
			if ( elemNode==null ) {
				elemNode = cfg.getClassMapping(entityName).getNodeName();
			}
		}
		else {
			elementPersister = null;
		}		
		elementNodeName = elemNode;
			
		String[] aliases = new String[elementSpan];
		elementColumnNames = new String[elementSpan];
		elementFormulaTemplates = new String[elementSpan];
		elementColumnIsSettable = new boolean[elementSpan];
		boolean isFormula = false;
		int j = 0;
		while ( iter.hasNext() ) {
			Selectable s = (Selectable) iter.next();
			aliases[j] = s.getAlias();
			if ( s.isFormula() ) {
				Formula form = (Formula) s;
				elementFormulaTemplates[j] = form.getTemplate(dialect);
				isFormula = true;
			}
			else {
				Column col = (Column) s;
				elementColumnNames[j] = col.getQuotedName(dialect);
				elementColumnIsSettable[j] = true;
			}
			j++;
		}
		elementColumnAliases = alias.toAliasStrings(aliases);
		elementIsFormula = isFormula;


		// INDEX AND ROW SELECT

		hasIndex = collection.isIndexed();
		if (hasIndex) {
			IndexedCollection indexedCollection = (IndexedCollection) collection;
			indexType = indexedCollection.getIndex().getType();
			int indexSpan = indexedCollection.getIndex().getColumnSpan();
			iter = indexedCollection.getIndex().getColumnIterator();
			indexColumnNames = new String[indexSpan];
			indexFormulaTemplates = new String[indexSpan];
			indexColumnIsSettable = new boolean[indexSpan];
			String[] indexAliases = new String[indexSpan];
			int i = 0;
			isFormula = false;
			while ( iter.hasNext() ) {
				Selectable s = (Selectable) iter.next();
				indexAliases[i] = s.getAlias();
				if ( s.isFormula() ) {
					Formula indexForm = (Formula) s;
					indexFormulaTemplates[i] = indexForm.getTemplate(dialect);
					isFormula = true;
				}
				else {
					Column indexCol = (Column) s;
					indexColumnNames[i] = indexCol.getQuotedName(dialect);
					indexColumnIsSettable[i] = true;
				}
				i++;
			}
			indexColumnAliases = alias.toAliasStrings(indexAliases);
			indexIsFormula = isFormula;			
			baseIndex = indexedCollection.isList() ? 
					( (List) indexedCollection ).getBaseIndex() : 0;

			indexNodeName = indexedCollection.getIndexNodeName(); 

		}
		else {
			indexIsFormula = false;
			indexColumnIsSettable = null;
			indexFormulaTemplates = null;
			indexType = null;
			indexColumnNames = null;
			indexColumnAliases = null;
			baseIndex = 0;
			indexNodeName = null;
		}
		
		hasIdentifier = collection.isIdentified();
		if (hasIdentifier) {
			if ( collection.isOneToMany() ) {
				throw new MappingException( "one-to-many collections with identifiers are not supported" );
			}
			IdentifierCollection idColl = (IdentifierCollection) collection;
			identifierType = idColl.getIdentifier().getType();
			iter = idColl.getIdentifier().getColumnIterator();
			Column col = ( Column ) iter.next();
			identifierColumnName = col.getQuotedName(dialect);
			identifierColumnAlias = alias.toAliasString( col.getAlias() );
			unquotedIdentifierColumnName = identifierColumnAlias;
			identifierGenerator = idColl.getIdentifier().createIdentifierGenerator( 
					factory.getDialect(),
					factory.getSettings().getDefaultCatalogName(),
					factory.getSettings().getDefaultSchemaName(),
					null
				);
		}
		else {
			identifierType = null;
			identifierColumnName = null;
			identifierColumnAlias = null;
			unquotedIdentifierColumnName = null;
			identifierGenerator = null;
		}
		
		//GENERATE THE SQL:
				
		//sqlSelectString = sqlSelectString();
		if ( collection.getCustomSQLDeleteAll() == null ) {
			sqlDeleteString = generateDeleteString();
			deleteAllCallable = false;
		}
		else {
			sqlDeleteString = collection.getCustomSQLDeleteAll();
			deleteAllCallable = collection.isCustomDeleteAllCallable();
		}
		//sqlSelectRowString = sqlSelectRowString();
		if ( collection.getCustomSQLInsert() == null ) {
			sqlInsertRowString = generateInsertRowString();
			insertCallable = false;
		}
		else {
			sqlInsertRowString = collection.getCustomSQLInsert();
			insertCallable = collection.isCustomInsertCallable();
		}

		if ( collection.getCustomSQLUpdate() == null ) {
			sqlUpdateRowString = generateUpdateRowString();
			updateCallable = false;
		}
		else {
			sqlUpdateRowString = collection.getCustomSQLUpdate();
			updateCallable = collection.isCustomUpdateCallable();
		}
		if ( collection.getCustomSQLDelete() == null ) {
			sqlDeleteRowString = generateDeleteRowString();
			deleteCallable = false;
		}
		else {
			sqlDeleteRowString = collection.getCustomSQLDelete();
			deleteCallable = collection.isCustomDeleteCallable();
		}
		logStaticSQL();
		isLazy = collection.isLazy();

		isInverse = collection.isInverse();

		if ( collection.isArray() ) {
			elementClass = ( (org.hibernate.mapping.Array) collection ).getElementClass();
		}
		else {
			// for non-arrays, we don't need to know the element class
			elementClass = null; //elementType.returnedClass();
		}

		if ( elementType.isComponentType() ) {
			elementPropertyMapping = new CompositeElementPropertyMapping( 
					elementColumnNames,
					elementFormulaTemplates,
					(AbstractComponentType) elementType,
					factory 
				);
		}
		else if ( !elementType.isEntityType() ) {
			elementPropertyMapping = new ElementPropertyMapping( 
					elementColumnNames,
					elementType 
				);
		}
		else {
			if ( elementPersister instanceof PropertyMapping ) { //not all classpersisters implement PropertyMapping!
				elementPropertyMapping = (PropertyMapping) elementPersister;
			}
			else {
				elementPropertyMapping = new ElementPropertyMapping( 
						elementColumnNames,
						elementType 
					);
			}
		}
			
		// Handle any filters applied to this collection
		int filterCount = collection.getFilterMap().size();
		filterNames = new String[filterCount];
		filterConditions = new String[filterCount];
		iter = collection.getFilterMap().entrySet().iterator();
		filterCount = 0;
		while ( iter.hasNext() ) {
			final Map.Entry entry = (Map.Entry) iter.next();
			filterNames[filterCount] = (String) entry.getKey();
			filterConditions[filterCount] = Template.renderWhereStringTemplate( 
					(String) entry.getValue(),
					FilterImpl.MARKER,
					dialect 
				);
			filterConditions[filterCount] = StringHelper.replace( filterConditions[filterCount],
					":",
					":" + filterNames[filterCount] + "." );
			filterCount++;
		}

	}

	public void postInstantiate() throws MappingException {
		initializer = queryLoaderName == null ?
				createCollectionInitializer( CollectionHelper.EMPTY_MAP ) :
				new NamedQueryCollectionInitializer( queryLoaderName, this );
	}

	protected void logStaticSQL() {
		if ( log.isDebugEnabled() ) {
			log.debug( "Static SQL for collection: " + getRole() );
			if ( getSQLInsertRowString() != null ) log.debug( " Row insert: " + getSQLInsertRowString() );
			if ( getSQLUpdateRowString() != null ) log.debug( " Row update: " + getSQLUpdateRowString() );
			if ( getSQLDeleteRowString() != null ) log.debug( " Row delete: " + getSQLDeleteRowString() );
			if ( getSQLDeleteString() != null ) log.debug( " One-shot delete: " + getSQLDeleteString() );
		}
	}

	public void initialize(Serializable key, SessionImplementor session) throws HibernateException {
		getAppropriateInitializer( key, session ).initialize( key, session );
	}

	protected CollectionInitializer getAppropriateInitializer(Serializable key, SessionImplementor session) {
		if ( queryLoaderName != null ) {
			//if there is a user-specified loader, return that
			//TODO: filters!?
			return initializer;
		}
		CollectionInitializer subselectInitializer = getSubselectInitializer( key, session );
		if ( subselectInitializer != null ) {
			return subselectInitializer;
		}
		else if ( session.getEnabledFilters().isEmpty() ) {
			return initializer;
		}
		else {
			return createCollectionInitializer( session.getEnabledFilters() );
		}
	}

	private CollectionInitializer getSubselectInitializer(Serializable key, SessionImplementor session) {
		
		if ( !isSubselectLoadable() ) return null;
		
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		
		SubselectFetch subselect = persistenceContext.getBatchFetchQueue()
			.getSubselect( new EntityKey( key, getOwnerEntityPersister(), session.getEntityMode() ) );
		
		if (subselect == null) {
			return null;
		}
		else {
			
			// Take care of any entities that might have
			// been evicted!	
			Iterator iter = subselect.getResult().iterator();
			while ( iter.hasNext() ) {
				if ( !persistenceContext.containsEntity( (EntityKey) iter.next() ) ) {
					iter.remove();
				}
			}	
			
			// Run a subquery loader
			return createSubselectInitializer( subselect, session );
		}
	}

	protected CollectionInitializer createSubselectInitializer(SubselectFetch subselect, SessionImplementor session) {
		return new SubselectCollectionLoader( this,
				subselect.toSubselectString( getCollectionType().getLHSPropertyName() ),
				subselect.getResult(),
				subselect.getQueryParameters(),
				session.getFactory(),
				session.getEnabledFilters() );
	}

	protected abstract CollectionInitializer createCollectionInitializer(Map enabledFilters)
			throws MappingException;

	public CacheConcurrencyStrategy getCache() {
		return cache;
	}

	public boolean hasCache() {
		return cache != null;
	}

	public CollectionType getCollectionType() {
		return collectionType;
	}

	protected String getSQLWhereString(String alias) {
		return StringHelper.replace( sqlWhereStringTemplate, Template.TEMPLATE, alias );
	}

	public String getSQLOrderByString(String alias) {
		return StringHelper.replace( sqlOrderByStringTemplate, Template.TEMPLATE, alias );
	}

	public FetchMode getFetchMode() {
		return fetchMode;
	}

	public boolean hasOrdering() {
		return hasOrder;
	}

	public boolean hasWhere() {
		return hasWhere;
	}

	protected String getSQLDeleteString() {
		return sqlDeleteString;
	}

	protected String getSQLInsertRowString() {
		return sqlInsertRowString;
	}

	protected String getSQLUpdateRowString() {
		return sqlUpdateRowString;
	}

	protected String getSQLDeleteRowString() {
		return sqlDeleteRowString;
	}

	public Type getKeyType() {
		return keyType;
	}

	public Type getIndexType() {
		return indexType;
	}

	public Type getElementType() {
		return elementType;
	}

	/**
	 * Return the element class of an array, or null otherwise
	 */
	public Class getElementClass() { //needed by arrays
		return elementClass;
	}

	public Object readElement(ResultSet rs, Object owner, SessionImplementor session) throws HibernateException, SQLException {
		Object element = getElementType().nullSafeGet( rs, elementColumnAliases, session, owner );
		return element;
	}

	public Object readIndex(ResultSet rs, SessionImplementor session) throws HibernateException, SQLException {
		Object index = getIndexType().nullSafeGet( rs, indexColumnAliases, session, null );
		if ( index == null ) throw new HibernateException( "null index column for collection: " + role );
		if (baseIndex!=0) {
			index = new Integer( ( (Integer) index ).intValue() - baseIndex );
		}
		return index;
	}

	public Object readIdentifier(ResultSet rs, SessionImplementor session) throws HibernateException, SQLException {
		Object id = getIdentifierType().nullSafeGet( rs, unquotedIdentifierColumnName, session, null );
		if ( id == null ) throw new HibernateException( "null identifier column for collection: " + role );
		return id;
	}

	public Object readKey(ResultSet rs, SessionImplementor session) throws HibernateException, SQLException {
		return getKeyType().nullSafeGet( rs, keyColumnAliases, session, null );
	}

	/**
	 * Write the key to a JDBC <tt>PreparedStatement</tt>
	 */
	protected int writeKey(PreparedStatement st, Serializable key, int i, SessionImplementor session)
			throws HibernateException, SQLException {
		
		if ( key == null ) throw new NullPointerException( "null key for collection: " + role );  //an assertion
		getKeyType().nullSafeSet( st, key, i, session );
		return i + keyColumnAliases.length;
	}

	/**
	 * Write the element to a JDBC <tt>PreparedStatement</tt>
	 */
	protected int writeElement(PreparedStatement st, Object elt, int i, SessionImplementor session)
			throws HibernateException, SQLException {
		getElementType().nullSafeSet(st, elt, i, elementColumnIsSettable, session);
		return i + ArrayHelper.countTrue(elementColumnIsSettable);

	}

	/**
	 * Write the index to a JDBC <tt>PreparedStatement</tt>
	 */
	protected int writeIndex(PreparedStatement st, Object index, int i, SessionImplementor session)
			throws HibernateException, SQLException {
		if (baseIndex!=0) {
			index = new Integer( ( (Integer) index ).intValue() + baseIndex );
		}
		getIndexType().nullSafeSet( st, index, i, indexColumnIsSettable, session );
		return i + ArrayHelper.countTrue(indexColumnIsSettable);
	}

	/**
	 * Write the element to a JDBC <tt>PreparedStatement</tt>
	 */
	protected int writeElementToWhere(PreparedStatement st, Object elt, int i, SessionImplementor session)
			throws HibernateException, SQLException {
		if (elementIsFormula) throw new AssertionFailure("cannot use a formula-based element in the where condition");
		getElementType().nullSafeSet(st, elt, i, session);
		return i + elementColumnAliases.length;

	}

	/**
	 * Write the index to a JDBC <tt>PreparedStatement</tt>
	 */
	protected int writeIndexToWhere(PreparedStatement st, Object index, int i, SessionImplementor session)
			throws HibernateException, SQLException {
		if (indexIsFormula) throw new AssertionFailure("cannot use a formula-based index in the where condition");
		if (baseIndex!=0) {
			index = new Integer( ( (Integer) index ).intValue() + baseIndex );
		}
		getIndexType().nullSafeSet( st, index, i, session );
		return i + indexColumnAliases.length;
	}

	/**
	 * Write the identifier to a JDBC <tt>PreparedStatement</tt>
	 */
	public int writeIdentifier(PreparedStatement st, Object id, int i, SessionImplementor session)
			throws HibernateException, SQLException {
		
		getIdentifierType().nullSafeSet( st, id, i, session );
		return i + 1;
	}

	public boolean isPrimitiveArray() {
		return isPrimitiveArray;
	}

	public boolean isArray() {
		return isArray;
	}

	/**
	 * Generate a list of collection index, key and element columns
	 */
	public String selectFragment(String alias) {
		SelectFragment frag = new SelectFragment()
				.setSuffix( "" )//always ignore suffix for collection columns
				.addColumns( alias, keyColumnNames, keyColumnAliases );
		
		for ( int i=0; i<elementColumnIsSettable.length; i++ ) {
			if ( elementColumnIsSettable[i] ) {
				frag.addColumn( alias, elementColumnNames[i], elementColumnAliases[i] );
			}
			else {
				frag.addFormula( alias, elementFormulaTemplates[i], elementColumnAliases[i] );
			}
		}
		
		if ( hasIndex ) {
			for ( int i=0; i<indexColumnIsSettable.length; i++ ) {
				if ( indexColumnIsSettable[i] ) {
					frag.addColumn( alias, indexColumnNames[i], indexColumnAliases[i] );
				}
				else {
					frag.addFormula( alias, indexFormulaTemplates[i], indexColumnAliases[i] );
				}
			}
		}
		
		if ( hasIdentifier ) frag.addColumn( alias, identifierColumnName, identifierColumnAlias );
		
		return frag.toFragmentString()
				.substring( 2 ); //strip leading ','
	}

	public String[] getIndexColumnNames() {
		return indexColumnNames;
	}

	public String[] getIndexColumnNames(String alias) {
		return indexIsFormula ?
				StringHelper.replace( indexFormulaTemplates, Template.TEMPLATE, alias ) :
				StringHelper.qualify(alias, indexColumnNames);
	}

	public String[] getElementColumnNames(String alias) {
		return elementIsFormula ?
				StringHelper.replace( elementFormulaTemplates, Template.TEMPLATE, alias ) :
				StringHelper.qualify(alias, elementColumnNames);
	}

	public String[] getElementColumnNames() {
		return elementColumnNames; //TODO: something with formulas...
	}

	public String[] getKeyColumnNames() {
		return keyColumnNames;
	}

	public boolean hasIndex() {
		return hasIndex;
	}

	public boolean isLazy() {
		return isLazy;
	}

	public boolean isInverse() {
		return isInverse;
	}

	public String getTableName() {
		return qualifiedTableName;
	}

	public void remove(Serializable id, SessionImplementor session) throws HibernateException {

		if ( !isInverse && isRowDeleteEnabled() ) {

			if ( log.isDebugEnabled() ) {
				log.debug( "Deleting collection: " + MessageHelper.collectionInfoString( this, id, getFactory() ) );
			}

			// Remove all the old entries

			try {
				int offset = 1;
				PreparedStatement st = null;
				if ( isDeleteCallable() ) {
					CallableStatement callstatement = session.getBatcher()
						.prepareBatchCallableStatement( getSQLDeleteString() );
					callstatement.registerOutParameter( offset++, Types.NUMERIC ); // TODO: should we require users to return number of update rows ?
					st = callstatement;
				}
				else {
					st = session.getBatcher().prepareBatchStatement( getSQLDeleteString() );
				}

				try {
					writeKey( st, id, offset, session );
					session.getBatcher().addToBatch( -1 );
				}
				catch ( SQLException sqle ) {
					session.getBatcher().abortBatch( sqle );
					throw sqle;
				}

				if ( log.isDebugEnabled() ) log.debug( "done deleting collection" );
			}
			catch ( SQLException sqle ) {
				throw JDBCExceptionHelper.convert(
				        sqlExceptionConverter,
				        sqle,
				        "could not delete collection: " + 
				        MessageHelper.collectionInfoString( this, id, getFactory() ),
				        getSQLDeleteString()
				);
			}

		}

	}

	public void recreate(PersistentCollection collection, Serializable id, SessionImplementor session)
			throws HibernateException {

		if ( !isInverse && isRowInsertEnabled() ) {

			if ( log.isDebugEnabled() ) {
				log.debug( "Inserting collection: " + MessageHelper.collectionInfoString( this, id, getFactory() ) );
			}

			try {
				//create all the new entries
				Iterator entries = collection.entries(this);
				if ( entries.hasNext() ) {
					try {
						collection.preInsert( this );
						int i = 0;
						int count = 0;
						while ( entries.hasNext() ) {

							final Object entry = entries.next();
							if ( collection.entryExists( entry, i ) ) {
								int offset = 1;
								PreparedStatement st = null;
								if ( isInsertCallable() ) {
									CallableStatement callstatement = session.getBatcher()
										.prepareBatchCallableStatement( getSQLInsertRowString() );
									callstatement.registerOutParameter( offset++, Types.NUMERIC ); // TODO: should we require users to return number of update rows ?
									st = callstatement;
								}
								else {
									st = session.getBatcher().prepareBatchStatement( getSQLInsertRowString() );
								}
								//TODO: copy/paste from insertRows()
								int loc = writeKey( st, id, offset, session );
								if ( hasIdentifier ) {
									loc = writeIdentifier( st, collection.getIdentifier(entry, i), loc, session );
								}
								if ( hasIndex /*&& !indexIsFormula*/ ) {
									loc = writeIndex( st, collection.getIndex(entry, i, this), loc, session );
								}
								//if ( !elementIsFormula ) {
									loc = writeElement(st, collection.getElement(entry), loc, session );
								//}
								session.getBatcher().addToBatch( 1 );
								collection.afterRowInsert( this, entry, i );
								count++;
							}
							i++;
						}
						if ( log.isDebugEnabled() ) log.debug( "done inserting collection: " + count + " rows inserted" );
					}
					catch ( SQLException sqle ) {
						session.getBatcher().abortBatch( sqle );
						throw sqle;
					}

				}
				else {
					if ( log.isDebugEnabled() ) log.debug( "collection was empty" );
				}
			}
			catch ( SQLException sqle ) {
				throw JDBCExceptionHelper.convert(
				        sqlExceptionConverter,
				        sqle,
				        "could not insert collection: " + 
				        MessageHelper.collectionInfoString( this, id, getFactory() ),
				        getSQLInsertRowString()
				);
			}
		}
	}
	
	protected boolean isRowDeleteEnabled() {
		return true;
	}

	public void deleteRows(PersistentCollection collection, Serializable id, SessionImplementor session)
			throws HibernateException {

		if ( !isInverse && isRowDeleteEnabled() ) {

			if ( log.isDebugEnabled() ) {
				log.debug( "Deleting rows of collection: " + MessageHelper.collectionInfoString( this, id, getFactory() ) );
			}
			
			boolean deleteByIndex = !isOneToMany() && hasIndex && !indexIsFormula;
			
			try {
				//delete all the deleted entries
				Iterator deletes = collection.getDeletes( this, !deleteByIndex );
				if ( deletes.hasNext() ) {
					int offset = 1;
					int count = 0;
					PreparedStatement st = null;
					if ( isDeleteCallable() ) {
						CallableStatement callstatement = session.getBatcher()
							.prepareBatchCallableStatement( getSQLDeleteRowString() );
						callstatement.registerOutParameter( offset++, Types.NUMERIC ); // TODO: should we require users to return number of update rows ?
						st = callstatement;
					}
					else {
						st = session.getBatcher().prepareBatchStatement( getSQLDeleteRowString() );
					}

					try {
						int i=0;
						while ( deletes.hasNext() ) {
							Object entry = deletes.next();
							int loc = offset;
							if ( hasIdentifier ) {
								loc = writeIdentifier( st, entry, loc, session );
							}
							else {
								//if ( !isOneToMany() ) {
									loc = writeKey( st, id, loc, session );
								//}
								if (deleteByIndex) {
									loc = writeIndexToWhere( st, entry, loc, session );
								}
								else {
									loc = writeElementToWhere( st, entry, loc, session );
								}
							}
							session.getBatcher().addToBatch( -1 );
							count++;
							i++;
						}
					}
					catch ( SQLException sqle ) {
						session.getBatcher().abortBatch( sqle );
						throw sqle;
					}

					if ( log.isDebugEnabled() ) log.debug( "done deleting collection rows: " + count + " deleted" );
				}
				else {
					if ( log.isDebugEnabled() ) log.debug( "no rows to delete" );
				}
			}
			catch ( SQLException sqle ) {
				throw JDBCExceptionHelper.convert(
				        sqlExceptionConverter,
				        sqle,
				        "could not delete collection rows: " + 
				        MessageHelper.collectionInfoString( this, id, getFactory() ),
				        getSQLDeleteRowString()
				);
			}
		}
	}
	
	protected boolean isRowInsertEnabled() {
		return true;
	}

	public void insertRows(PersistentCollection collection, Serializable id, SessionImplementor session)
			throws HibernateException {

		if ( !isInverse && isRowInsertEnabled() ) {

			if ( log.isDebugEnabled() ) {
				log.debug( "Inserting rows of collection: " + MessageHelper.collectionInfoString( this, id, getFactory() ) );
			}

			try {
				//insert all the new entries
				Iterator entries = collection.entries(this);
				boolean callable = isInsertCallable();
				try {
					collection.preInsert( this );
					int i = 0;
					int count = 0;
					int offset = 1;
					while ( entries.hasNext() ) {
						Object entry = entries.next();
						PreparedStatement st = null;
						if ( collection.needsInserting( entry, i, elementType ) ) {
							if ( st == null ) {
								if ( callable ) {
									CallableStatement callstatement = session.getBatcher()
										.prepareBatchCallableStatement( getSQLInsertRowString() );
									callstatement.registerOutParameter( offset++, Types.NUMERIC ); // TODO: should we require users to return number of update rows ?
									st = callstatement;
								}
								else {
									st = session.getBatcher().prepareBatchStatement( getSQLInsertRowString() );
								}
							}
							//TODO: copy/paste from recreate()
							int loc = writeKey( st, id, offset, session );
							if ( hasIdentifier ) {
								loc = writeIdentifier( st, collection.getIdentifier(entry, i), loc, session );
							}
							if ( hasIndex /*&& !indexIsFormula*/ ) {
								loc = writeIndex( st, collection.getIndex(entry, i, this), loc, session );
							}
							//if ( !elementIsFormula ) {
								loc = writeElement(st, collection.getElement(entry), loc, session );
							//}
							session.getBatcher().addToBatch( 1 );
							collection.afterRowInsert( this, entry, i );
							count++;
						}
						i++;
					}
					if ( log.isDebugEnabled() ) log.debug( "done inserting rows: " + count + " inserted" );
				}
				catch ( SQLException sqle ) {
					session.getBatcher().abortBatch( sqle );
					throw sqle;
				}
			}
			catch ( SQLException sqle ) {
				throw JDBCExceptionHelper.convert(
				        sqlExceptionConverter,
				        sqle,
				        "could not insert collection rows: " + 
				        MessageHelper.collectionInfoString( this, id, getFactory() ),
				        getSQLInsertRowString()
				);
			}

		}
	}


	public String getRole() {
		return role;
	}

	public String getOwnerEntityName() {
		return entityName;
	}

	public EntityPersister getOwnerEntityPersister() {
		return ownerPersister;
	}

	public IdentifierGenerator getIdentifierGenerator() {
		return identifierGenerator;
	}

	public Type getIdentifierType() {
		return identifierType;
	}

	public boolean hasOrphanDelete() {
		return hasOrphanDelete;
	}

	public Type toType(String propertyName) throws QueryException {
		if ( "index".equals( propertyName ) ) return indexType;
		return elementPropertyMapping.toType( propertyName );
	}

	public abstract boolean isManyToMany();

	public String[] toColumns(String alias, String propertyName)
			throws QueryException {

		if ( "index".equals( propertyName ) ) {
			if ( isManyToMany() ) {
				throw new QueryException( "index() function not supported for many-to-many association" );
			}
			return StringHelper.qualify( alias, indexColumnNames );
		}

		return elementPropertyMapping.toColumns( alias, propertyName );
	}

	public String[] toColumns(String propertyName)
			throws QueryException {

		if ( "index".equals( propertyName ) ) {
			if ( isManyToMany() ) {
				throw new QueryException( "index() function not supported for many-to-many association" );
			}
			return indexColumnNames;
		}

		return elementPropertyMapping.toColumns( propertyName );
	}

	public Type getType() {
		return elementPropertyMapping.getType(); //==elementType ??
	}

	public String getName() {
		return getRole();
	}

	public EntityPersister getElementPersister() {
		if ( elementPersister == null ) throw new AssertionFailure( "not an association" );
		return ( Loadable ) elementPersister;
	}

	public boolean isCollection() {
		return true;
	}

	public Serializable[] getCollectionSpaces() {
		return spaces;
	}

	protected abstract String generateDeleteString();

	protected abstract String generateDeleteRowString();

	protected abstract String generateUpdateRowString();

	protected abstract String generateInsertRowString();

	public void updateRows(PersistentCollection collection, Serializable id, SessionImplementor session) 
	throws HibernateException {

		if ( !isInverse && collection.isRowUpdatePossible() ) {

			if ( log.isDebugEnabled() ) log.debug( "Updating rows of collection: " + role + "#" + id );

			//update all the modified entries
			int count = doUpdateRows( id, collection, session );

			if ( log.isDebugEnabled() ) log.debug( "done updating rows: " + count + " updated" );
		}
	}

	protected abstract int doUpdateRows(Serializable key, PersistentCollection collection, SessionImplementor session) 
	throws HibernateException;

	public CollectionMetadata getCollectionMetadata() {
		return this;
	}

	public SessionFactoryImplementor getFactory() {
		return factory;
	}

	protected String filterFragment(String alias) throws MappingException {
		return hasWhere() ? " and " + getSQLWhereString( alias ) : "";
	}

	public String filterFragment(String alias, Map enabledFilters) throws MappingException {

		StringBuffer sessionFilterFragment = new StringBuffer();
		// if we have any defined filters, see if they have been enabled on the session;
		// and if so, prepend them to the filterFragment
		if ( getFilterNames() != null && getFilterNames().length > 0 ) {
			for ( int i = 0, max = getFilterNames().length; i < max; i++ ) {
				if ( enabledFilters.containsKey( getFilterNames()[i] ) ) {
					final String condition = getFilterConditions()[i];
					if ( StringHelper.isNotEmpty( condition ) ) {
						sessionFilterFragment.append( " and " )
								.append( StringHelper.replace( condition, FilterImpl.MARKER, alias ) );
					}
				}
			}
		}

		return sessionFilterFragment.append( filterFragment( alias ) ).toString();
	}

	public String oneToManyFilterFragment(String alias) throws MappingException {
		return "";
	}

	protected boolean isInsertCallable() {
		return insertCallable;
	}

	protected boolean isUpdateCallable() {
		return updateCallable;
	}

	protected boolean isDeleteCallable() {
		return deleteCallable;
	}

	protected boolean isDeleteAllCallable() {
		return deleteAllCallable;
	}

	protected String[] getFilterNames() {
		return filterNames;
	}

	protected String[] getFilterConditions() {
		return filterConditions;
	}

	public String toString() {
		return StringHelper.unqualify( getClass().getName() ) + '(' + role + ')';
	}

	public boolean isVersioned() {
		return isVersioned && getOwnerEntityPersister().isVersioned();
	}
	
	public String getNodeName() {
		return nodeName;
	}

	public String getElementNodeName() {
		return elementNodeName;
	}

	public String getIndexNodeName() {
		return indexNodeName;
	}

	protected SQLExceptionConverter getSQLExceptionConverter() {
		return sqlExceptionConverter;
	}

	public CacheEntryStructure getCacheEntryStructure() {
		return cacheEntryStructure;
	}

	public boolean isAffectedByEnabledFilters(SessionImplementor session) {
		final Map enabledFilters = session.getEnabledFilters();
		for ( int i = 0, max = filterNames.length; i < max; i++ ) {
			if ( enabledFilters.containsKey( filterNames[i] ) ) {
				return true;
			}
		}
		return false;
	}

	public boolean isSubselectLoadable() {
		return subselectLoadable;
	}

}
