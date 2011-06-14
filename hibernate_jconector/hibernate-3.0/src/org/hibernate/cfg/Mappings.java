//$Id: Mappings.java,v 1.19 2005/03/22 16:36:34 epbernard Exp $
package org.hibernate.cfg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.hibernate.MappingException;
import org.hibernate.util.StringHelper;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.engine.NamedQueryDefinition;
import org.hibernate.engine.NamedSQLQueryDefinition;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.TypeDef;

/**
 * A collection of mappings from classes and collections to
 * relational database tables. (Represents a single
 * <tt>&lt;hibernate-mapping&gt;</tt> element.)
 * @author Gavin King
 */
public class Mappings implements Serializable {

	private static final Log log = LogFactory.getLog(Mappings.class);

	private final Map classes;
	private final Map collections;
	private final Map tables;
	private final Map queries;
	private final Map sqlqueries;
	private final Map typeDefs;
	private final List secondPasses;
	private final Map imports;
	private String schemaName;
    private String catalogName;
	private String defaultCascade;
	private String defaultPackage;
	private String defaultAccess;
	private boolean autoImport;
	private boolean defaultLazy;
	private final List propertyReferences;
	private final NamingStrategy namingStrategy;
	private final Map filterDefinitions;

    private final Map extendsQueue;


	Mappings(
		final Map classes,
		final Map collections,
		final Map tables,
		final Map queries,
		final Map sqlqueries,
		final Map imports,
		final List secondPasses,
		final List propertyReferences,
		final NamingStrategy namingStrategy,
		final Map typeDefs,
		final Map filterDefinitions, 
        final Map extendsQueue
	) {
		this.classes = classes;
		this.collections = collections;
		this.queries = queries;
		this.sqlqueries = sqlqueries;
		this.tables = tables;
		this.imports = imports;
		this.secondPasses = secondPasses;
		this.propertyReferences = propertyReferences;
		this.namingStrategy = namingStrategy;
		this.typeDefs = typeDefs;
		this.filterDefinitions = filterDefinitions;
        this.extendsQueue = extendsQueue;        
	}

	public void addClass(PersistentClass persistentClass) throws MappingException {
		Object old = classes.put( persistentClass.getEntityName(), persistentClass );
		if ( old!=null ) log.warn( "duplicate class mapping: " + persistentClass.getEntityName() );
	}
	public void addCollection(Collection collection) throws MappingException {
		Object old = collections.put( collection.getRole(), collection );
		if ( old!=null ) log.warn( "duplicate collection role: " + collection.getRole() );
	}
	public PersistentClass getClass(String className) {
		return (PersistentClass) classes.get(className);
	}
	public Collection getCollection(String role) {
		return (Collection) collections.get(role);
	}

	public void addImport(String className, String rename) throws MappingException {
		if ( imports.put(rename, className)!=null ) throw new MappingException("duplicate import: " + rename);
	}

	public Table addTable(String schema, 
			String catalog, 
			String name,
			String subselect,
			boolean isAbstract
	) {
        String key = subselect==null ?
			Table.qualify(catalog, schema, name, '.') :
			subselect;
		Table table = (Table) tables.get(key);

		if (table == null) {
			table = new Table();
			table.setAbstract(isAbstract);
			table.setName(name);
			table.setSchema(schema);
			table.setCatalog(catalog);
			table.setSubselect(subselect);
			tables.put(key, table);
		}
		else {
			if (!isAbstract) table.setAbstract(false);
		}

		return table;
	}

	public Table addDenormalizedTable(
			String schema, 
			String catalog, 
			String name,
			boolean isAbstract, 
			String subselect,
			Table includedTable)
	throws MappingException {
        String key = subselect==null ?
        		Table.qualify(catalog, schema, name, '.') :
        		subselect;
		if ( tables.containsKey(key) ) {
			throw new MappingException("duplicate table: " + name);
		}
		Table table = new DenormalizedTable(includedTable);
		table.setAbstract(isAbstract);
		table.setName(name);
		table.setSchema(schema);
		table.setCatalog(catalog);
		table.setSubselect(subselect);
		tables.put(key, table);
		return table;
	}

	public Table getTable(String schema, String catalog, String name) {
        String key = Table.qualify(catalog, schema, name, '.');
		return (Table) tables.get(key);
	}

	public String getSchemaName() {
		return schemaName;
	}

    public String getCatalogName() {
        return catalogName;
    }

	public String getDefaultCascade() {
		return defaultCascade;
	}

	/**
	 * Sets the schemaName.
	 * @param schemaName The schemaName to set
	 */
	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

    /**
     * Sets the catalogName.
     * @param catalogName The catalogName to set
     */
    public void setCatalogName(String catalogName) {
        this.catalogName = catalogName;
    }

	/**
	 * Sets the defaultCascade.
	 * @param defaultCascade The defaultCascade to set
	 */
	public void setDefaultCascade(String defaultCascade) {
		this.defaultCascade = defaultCascade;
	}

	/**
	 * sets the default access strategy
	 * @param defaultAccess the default access strategy.
	 */
	public void setDefaultAccess(String defaultAccess) {
		this.defaultAccess = defaultAccess;
	}

	public String getDefaultAccess() {
		return defaultAccess;
	}

	public void addQuery(String name, NamedQueryDefinition query) throws MappingException {
		checkQueryExist(name);
		queries.put( name.intern(), query );
	}

	public void addSQLQuery(String name, NamedSQLQueryDefinition query) throws MappingException {
		checkQueryExist(name);
		sqlqueries.put( name.intern(), query );
	}

	private void checkQueryExist(String name) throws MappingException {
		if ( sqlqueries.containsKey(name) || queries.containsKey(name) ) {
			throw new MappingException("Duplicate query named: " + name);
		}
	}

	public NamedQueryDefinition getQuery(String name) {
		return (NamedQueryDefinition) queries.get(name);
	}

	void addSecondPass(HbmBinder.SecondPass sp) {
		addSecondPass(sp, false);
	}
    
    void addSecondPass(HbmBinder.SecondPass sp, boolean OnTopOfTheQueue) {
		if (OnTopOfTheQueue) {
			secondPasses.add(0, sp);
		}
		else {
			secondPasses.add(sp);
		}
	}

	/**
	 * Returns the autoImport.
	 * @return boolean
	 */
	public boolean isAutoImport() {
		return autoImport;
	}

	/**
	 * Sets the autoImport.
	 * @param autoImport The autoImport to set
	 */
	public void setAutoImport(boolean autoImport) {
		this.autoImport = autoImport;
	}

	void addUniquePropertyReference(String referencedClass, String propertyName) {
		PropertyReference upr = new PropertyReference();
		upr.referencedClass = referencedClass;
		upr.propertyName = propertyName;
		upr.unique = true;
		propertyReferences.add(upr);
	}

	void addPropertyReference(String referencedClass, String propertyName) {
		PropertyReference upr = new PropertyReference();
		upr.referencedClass = referencedClass;
		upr.propertyName = propertyName;
		propertyReferences.add(upr);
	}

	static final class PropertyReference implements Serializable {
		String referencedClass;
		String propertyName;
		boolean unique;
	}

	/**
	 * @return Returns the defaultPackage.
	 */
	public String getDefaultPackage() {
		return defaultPackage;
	}

	/**
	 * @param defaultPackage The defaultPackage to set.
	 */
	public void setDefaultPackage(String defaultPackage) {
		this.defaultPackage = defaultPackage;
	}

	public NamingStrategy getNamingStrategy() {
		return namingStrategy;
	}

	public void addTypeDef(String typeName, String typeClass, Properties paramMap) {
		TypeDef def = new TypeDef(typeClass, paramMap);
		typeDefs.put(typeName, def);
		log.debug("Added " + typeName + " with class " + typeClass);
	}

	public TypeDef getTypeDef(String typeName) {
		return (TypeDef) typeDefs.get(typeName);
	}

    public Iterator iterateCollections() {
        return collections.values().iterator();
    }
    
    public Iterator iterateTables() {
    	return tables.values().iterator();
    }

	public Map getFilterDefinitions() {
		return filterDefinitions;
	}

	public void addFilterDefinition(FilterDefinition definition) {
		filterDefinitions.put( definition.getFilterName(), definition );
	}
	
	public FilterDefinition getFilterDefinition(String name) {
		return (FilterDefinition) filterDefinitions.get(name);
	}
	
	public boolean isDefaultLazy() {
		return defaultLazy;
	}
	public void setDefaultLazy(boolean defaultLazy) {
		this.defaultLazy = defaultLazy;
	}

    /**
     * @param className
     * @param doc
     */
    public void addToExtendsQueue(String className, Document doc) {
		List existingQueue = (List) extendsQueue.get(className);
		if(existingQueue==null) {
			existingQueue = new ArrayList();
		}
		existingQueue.add(doc);
		extendsQueue.put(className, existingQueue);        
    }

	public PersistentClass locatePersistentClassByEntityName(String entityName) {
		PersistentClass persistentClass = ( PersistentClass ) classes.get( entityName );
		if ( persistentClass == null ) {
			String actualEntityName = ( String ) imports.get( entityName );
			if ( StringHelper.isNotEmpty( actualEntityName ) ) {
				persistentClass = ( PersistentClass ) classes.get( actualEntityName );
			}
		}
		return persistentClass;
	}
}