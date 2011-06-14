//$Id: Configuration.java,v 1.65 2005/03/31 20:37:14 oneovthafew Exp $
package org.hibernate.cfg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.engine.Mapping;
import org.hibernate.event.AutoFlushEventListener;
import org.hibernate.event.PersistEventListener;
import org.hibernate.event.DeleteEventListener;
import org.hibernate.event.DirtyCheckEventListener;
import org.hibernate.event.EvictEventListener;
import org.hibernate.event.FlushEntityEventListener;
import org.hibernate.event.FlushEventListener;
import org.hibernate.event.InitializeCollectionEventListener;
import org.hibernate.event.LoadEventListener;
import org.hibernate.event.LockEventListener;
import org.hibernate.event.MergeEventListener;
import org.hibernate.event.PostDeleteEventListener;
import org.hibernate.event.PostInsertEventListener;
import org.hibernate.event.PostLoadEventListener;
import org.hibernate.event.PostUpdateEventListener;
import org.hibernate.event.PreDeleteEventListener;
import org.hibernate.event.PreInsertEventListener;
import org.hibernate.event.PreLoadEventListener;
import org.hibernate.event.PreUpdateEventListener;
import org.hibernate.event.RefreshEventListener;
import org.hibernate.event.ReplicateEventListener;
import org.hibernate.event.SaveOrUpdateEventListener;
import org.hibernate.event.SessionEventListenerConfig;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.Index;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.secure.JACCConfiguration;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.hibernate.tool.hbm2ddl.TableMetadata;
import org.hibernate.type.SerializationException;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.CollectionHelper;
import org.hibernate.util.SerializationHelper;
import org.hibernate.util.XMLHelper;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * An instance of <tt>Configuration</tt> allows the application
 * to specify properties and mapping documents to be used when
 * creating a <tt>SessionFactory</tt>. Usually an application will create
 * a single <tt>Configuration</tt>, build a single instance of
 * <tt>SessionFactory</tt> and then instantiate <tt>Session</tt>s in
 * threads servicing client requests. The <tt>Configuration</tt> is meant
 * only as an initialization-time object. <tt>SessionFactory</tt>s are
 * immutable and do not retain any association back to the
 * <tt>Configuration</tt>.<br>
 * <br>
 * A new <tt>Configuration</tt> will use the properties specified in
 * <tt>hibernate.properties</tt> by default.
 *
 * @author Gavin King
 * @see org.hibernate.SessionFactory
 */
public class Configuration implements Serializable {

	private static Log log = LogFactory.getLog( Configuration.class );

	protected Map classes;
	protected Map imports;
	protected Map collections;
	protected Map tables;
	protected Map namedQueries;
	protected Map namedSqlQueries;
	protected Map filterDefinitions;
	protected List secondPasses;
	protected List propertyReferences;
	protected Map extendsQueue; // key: classname -> value: dom4j document
	private Interceptor interceptor;
	private Properties properties;
	private EntityResolver entityResolver;

	private transient XMLHelper xmlHelper;
	protected transient Map typeDefs;

	protected NamingStrategy namingStrategy = DefaultNamingStrategy.INSTANCE;

	private SessionEventListenerConfig sessionEventListenerConfig;

	private final SettingsFactory settingsFactory;

	protected void reset() {
		classes = new HashMap();
		imports = new HashMap();
		collections = new HashMap();
		tables = new TreeMap();
		namedQueries = new HashMap();
		namedSqlQueries = new HashMap();
		xmlHelper = new XMLHelper();
		typeDefs = new HashMap();
		propertyReferences = new ArrayList();
		secondPasses = new ArrayList();
		interceptor = EMPTY_INTERCEPTOR;
		properties = Environment.getProperties();
		entityResolver = XMLHelper.DEFAULT_DTD_RESOLVER;
		sessionEventListenerConfig = new SessionEventListenerConfig();
		filterDefinitions = new HashMap();
		extendsQueue = new HashMap();
	}

	private transient Mapping mapping = buildMapping();

	protected Configuration(SettingsFactory settingsFactory) {
		this.settingsFactory = settingsFactory;
		reset();
	}

	public Configuration() {
		this( new SettingsFactory() );
	}

	/**
	 * Iterate the class mappings
	 */
	public Iterator getClassMappings() {
		return classes.values().iterator();
	}

	/**
	 * Iterate the collection mappings
	 */
	public Iterator getCollectionMappings() {
		return collections.values().iterator();
	}

	/**
	 * Iterate the table mappings
	 */
	public Iterator getTableMappings() {
		return tables.values().iterator();
	}

	/**
	 * Get the mapping for a particular class
	 */
	public PersistentClass getClassMapping(String persistentClass) {
		return ( PersistentClass ) classes.get( persistentClass );
	}

	/**
	 * Get the mapping for a particular collection role
	 *
	 * @param role a collection role
	 * @return Collection
	 */
	public Collection getCollectionMapping(String role) {
		return ( Collection ) collections.get( role );
	}

	/**
	 * Set a custom entity resolver. This entity resolver must be
	 * set before addXXX(misc) call.
	 * Default value is {@link org.hibernate.util.DTDEntityResolver}
	 *
	 * @param entityResolver entity resolver to use
	 */
	public void setEntityResolver(EntityResolver entityResolver) {
		this.entityResolver = entityResolver;
	}

	/**
	 * Read mappings from a particular XML file
	 *
	 * @param xmlFile a path to a file
	 */
	public Configuration addFile(String xmlFile) throws MappingException {
		log.info( "Mapping file: " + xmlFile );
		try {
			List errors = new ArrayList();
			org.dom4j.Document doc = xmlHelper.createSAXReader( xmlFile, errors, entityResolver ).read( new File( xmlFile ) );
			if ( errors.size() != 0 ) throw new MappingException( "invalid mapping", ( Throwable ) errors.get( 0 ) );
			add( doc );
			return this;
		}
		catch ( Exception e ) {
			log.error( "Could not configure datastore from file: " + xmlFile, e );
			throw new MappingException( "Could not configure datastore from file: " + xmlFile, e );
		}
	}

	/**
	 * Read mappings from a particular XML file
	 *
	 * @param xmlFile a path to a file
	 */
	public Configuration addFile(File xmlFile) throws MappingException {
		log.info( "Mapping file: " + xmlFile.getPath() );
		try {
			addInputStream( new FileInputStream( xmlFile ) );
		}
		catch ( Exception e ) {
			log.error( "Could not configure datastore from file: " + xmlFile.getPath(), e );
			throw new MappingException( "Could not configure datastore from file: " + xmlFile.getPath(), e );
		}
		return this;
	}

	/**
	 * If a cached <tt>xmlFile + ".bin"</tt> exists and is newer than <tt>xmlFile</tt> the
	 * <tt>".bin"</tt> file will be read directly. Otherwise xmlFile is read and then
	 * serialized to <tt>xmlFile + ".bin"</tt> for use the next time.
	 */
	public Configuration addCacheableFile(File xmlFile) throws MappingException {
		try {
			File lazyfile = new File( xmlFile.getAbsolutePath() + ".bin" );
			org.dom4j.Document doc = null;
			List errors = new ArrayList();

			final boolean useCachedFile = xmlFile.exists() &&
					lazyfile.exists() &&
					xmlFile.lastModified() < lazyfile.lastModified();

			if ( useCachedFile ) {
				try {
					log.info( "Mapping cached file: " + lazyfile );
					doc = ( org.dom4j.Document ) SerializationHelper.deserialize( new FileInputStream( lazyfile ) );
				}
				catch ( SerializationException e ) {
					log.warn( "Could not deserialize cached file: " + lazyfile.getPath(), e );
				}
			}

			// If deserialization failed
			if ( doc == null ) {
				doc = xmlHelper.createSAXReader( xmlFile.getAbsolutePath(), errors, entityResolver ).read( xmlFile );
				try {
					log.info( "Writing cached file of " + xmlFile + " to " + lazyfile );
					SerializationHelper.serialize( ( Serializable ) doc, new FileOutputStream( lazyfile ) );
				}
				catch ( SerializationException e ) {
					log.warn( "Could not write cached file: " + lazyfile, e );
				}
			}

			if ( errors.size() != 0 ) throw new MappingException( "invalid mapping", ( Throwable ) errors.get( 0 ) );
			add( doc );
			return this;
		}
		catch ( Exception e ) {
			log.error( "Could not configure datastore from file: " + xmlFile, e );
			throw new MappingException( e );
		}
	}

	public Configuration addCacheableFile(String xmlFile) throws MappingException {
		return addCacheableFile( new File( xmlFile ) );
	}


	/**
	 * Read mappings from a <tt>String</tt>
	 *
	 * @param xml an XML string
	 */
	public Configuration addXML(String xml) throws MappingException {
		if ( log.isDebugEnabled() ) log.debug( "Mapping XML:\n" + xml );
		try {
			List errors = new ArrayList();
			org.dom4j.Document doc = xmlHelper.createSAXReader( "XML String", errors, entityResolver ).read( new StringReader( xml ) );
			if ( errors.size() != 0 ) throw new MappingException( "invalid mapping", ( Throwable ) errors.get( 0 ) );
			add( doc );
		}
		catch ( Exception e ) {
			log.error( "Could not configure datastore from XML", e );
			throw new MappingException( e );
		}
		return this;
	}

	/**
	 * Read mappings from a <tt>URL</tt>
	 *
	 * @param url
	 */
	public Configuration addURL(URL url) throws MappingException {
		if ( log.isDebugEnabled() ) log.debug( "Mapping URL:\n" + url );
		try {
			addInputStream( url.openStream() );
		}
		catch ( Exception e ) {
			log.error( "Could not configure datastore from URL: " + url , e );
			throw new MappingException( "Could not configure datastore from URL: " + url, e );
		}
		return this;
	}

	/**
	 * Read mappings from a DOM <tt>Document</tt>
	 *
	 * @param doc a DOM document
	 */
	public Configuration addDocument(Document doc) throws MappingException {
		if ( log.isDebugEnabled() ) log.debug( "Mapping XML:\n" + doc );
		try {
			add( xmlHelper.createDOMReader().read( doc ) );
		}
		catch ( Exception e ) {
			log.error( "Could not configure datastore from XML document", e );
			throw new MappingException( e );
		}
		return this;
	}

	protected void add(org.dom4j.Document doc) throws MappingException {
		try {
			HbmBinder.bindRoot( doc, createMappings(), CollectionHelper.EMPTY_MAP ); // TODO: possibly get inheritable meta's from cfg.xml
		}
		catch ( MappingException me ) {
			log.error( "Could not compile the mapping document", me );
			throw me;
		}
	}

	/**
	 * Create a new <tt>Mappings</tt> to add class and collection
	 * mappings to.
	 */
	public Mappings createMappings() {
		return new Mappings( classes,
				collections,
				tables,
				namedQueries,
				namedSqlQueries,
				imports,
				secondPasses,
				propertyReferences,
				namingStrategy,
				typeDefs,
				filterDefinitions,
				extendsQueue );
	}

	/**
	 * Read mappings from an <tt>InputStream</tt>
	 *
	 * @param xmlInputStream an <tt>InputStream</tt> containing XML
	 */
	public Configuration addInputStream(InputStream xmlInputStream) throws MappingException {
		try {
			List errors = new ArrayList();
			org.dom4j.Document doc = xmlHelper.createSAXReader( "XML InputStream", errors, entityResolver ).read( new InputSource( xmlInputStream ) );
			if ( errors.size() != 0 ) throw new MappingException( "invalid mapping", ( Throwable ) errors.get( 0 ) );
			add( doc );
			return this;
		}
		catch ( MappingException me ) {
			throw me;
		}
		catch ( Exception e ) {
			log.error( "Could not configure datastore from input stream", e );
			throw new MappingException( e );
		}
		finally {
			try {
				xmlInputStream.close();
			}
			catch ( IOException ioe ) {
				log.error( "could not close input stream", ioe );
			}
		}
	}

	/**
	 * Read mappings from an application resource
	 *
	 * @param path        a resource
	 * @param classLoader a <tt>ClassLoader</tt> to use
	 */
	public Configuration addResource(String path, ClassLoader classLoader) throws MappingException {
		log.info( "Mapping resource: " + path );
		InputStream rsrc = classLoader.getResourceAsStream( path );
		if ( rsrc == null ) throw new MappingException( "Resource: " + path + " not found" );
		try {
			return addInputStream( rsrc );
		}
		catch ( MappingException me ) {
			throw new MappingException( "Error reading resource: " + path, me );
		}
	}

	/**
	 * Read mappings from an application resource trying different classloaders.
	 * This method will try to load the resource first from the thread context
	 * classloader and then from the classloader that loaded Hibernate.
	 */
	public Configuration addResource(String path) throws MappingException {
		log.info( "Mapping resource: " + path );
		InputStream rsrc = Thread.currentThread().getContextClassLoader().getResourceAsStream( path );
		if ( rsrc == null ) rsrc = Environment.class.getClassLoader().getResourceAsStream( path );
		if ( rsrc == null ) throw new MappingException( "Resource: " + path + " not found" );
		try {
			return addInputStream( rsrc );
		}
		catch ( MappingException me ) {
			throw new MappingException( "Error reading resource: " + path, me );
		}
	}

	/**
	 * Read a mapping from an application resource, using a convention.
	 * The class <tt>foo.bar.Foo</tt> is mapped by the file <tt>foo/bar/Foo.hbm.xml</tt>.
	 *
	 * @param persistentClass the mapped class
	 */
	public Configuration addClass(Class persistentClass) throws MappingException {
		String fileName = persistentClass.getName().replace( '.', '/' ) + ".hbm.xml";
		log.info( "Mapping resource: " + fileName );
		InputStream rsrc = persistentClass.getClassLoader().getResourceAsStream( fileName );
		if ( rsrc == null ) throw new MappingException( "Resource: " + fileName + " not found" );
		try {
			return addInputStream( rsrc );
		}
		catch ( MappingException me ) {
			throw new MappingException( "Error reading resource: " + fileName, me );
		}
	}

	/**
	 * Read all mappings from a jar file
	 *
	 * @param jar a jar file
	 */
	public Configuration addJar(File jar) throws MappingException {

		log.info( "Searching for mapping documents in jar: " + jar.getName() );

		final JarFile jarFile;
		try {
			jarFile = new JarFile( jar );
		}
		catch ( IOException ioe ) {
			log.error( "Could not configure datastore from jar: " + jar.getName(), ioe );
			throw new MappingException( "Could not configure datastore from jar: " + jar.getName(), ioe );
		}

		Enumeration jarEntries = jarFile.entries();
		while ( jarEntries.hasMoreElements() ) {

			ZipEntry ze = ( ZipEntry ) jarEntries.nextElement();

			if ( ze.getName().endsWith( ".hbm.xml" ) ) {
				log.info( "Found mapping documents in jar: " + ze.getName() );
				try {
					addInputStream( jarFile.getInputStream( ze ) );
				}
				catch ( MappingException me ) {
					throw me;
				}
				catch ( Exception e ) {
					log.error( "Could not configure datastore from jar: " + jar.getName(), e );
					throw new MappingException( "Could not configure datastore from jar: " + jar.getName(), e );
				}
			}
		}

		return this;

	}

	/**
	 * Read all mapping documents from a directory tree. Assume that any
	 * file named <tt>*.hbm.xml</tt> is a mapping document.
	 *
	 * @param dir a directory
	 */
	public Configuration addDirectory(File dir) throws MappingException {
		File[] files = dir.listFiles();
		for ( int i = 0; i < files.length; i++ ) {
			if ( files[i].isDirectory() ) {
				addDirectory( files[i] );
			}
			else if ( files[i].getName().endsWith( ".hbm.xml" ) ) {
				addFile( files[i] );
			}
		}
		return this;
	}

	private Iterator iterateGenerators(Dialect dialect) throws MappingException {

		TreeMap generators = new TreeMap();
		Iterator iter = classes.values().iterator();
		while ( iter.hasNext() ) {
			PersistentClass pc = ( PersistentClass ) iter.next();

			if ( !pc.isInherited() ) {

				IdentifierGenerator ig = pc.getIdentifier()
					.createIdentifierGenerator( 
							dialect, 
							properties.getProperty(Environment.DEFAULT_CATALOG),
							properties.getProperty(Environment.DEFAULT_SCHEMA),
							pc.getEntityName()
					);

				if ( ig instanceof PersistentIdentifierGenerator ) {
					generators.put( ( ( PersistentIdentifierGenerator ) ig ).generatorKey(), ig );
				}

			}
		}

		iter = collections.values().iterator();
		while ( iter.hasNext() ) {
			Collection collection = ( Collection ) iter.next();

			if ( collection.isIdentified() ) {

				IdentifierGenerator ig = ( ( IdentifierCollection ) collection ).getIdentifier()
						.createIdentifierGenerator( 
								dialect, 
								properties.getProperty(Environment.DEFAULT_CATALOG),
								properties.getProperty(Environment.DEFAULT_SCHEMA),
								null
						);

				if ( ig instanceof PersistentIdentifierGenerator ) {
					generators.put( ( ( PersistentIdentifierGenerator ) ig ).generatorKey(), ig );
				}

			}
		}

		return generators.values().iterator();
	}

	/**
	 * Generate DDL for dropping tables
	 *
	 * @see org.hibernate.tool.hbm2ddl.SchemaExport
	 */
	public String[] generateDropSchemaScript(Dialect dialect) throws HibernateException {

		secondPassCompile();

		ArrayList script = new ArrayList( 50 );

		if ( dialect.dropConstraints() ) {
			Iterator iter = getTableMappings();
			while ( iter.hasNext() ) {
				Table table = ( Table ) iter.next();
				if ( table.isPhysicalTable() ) {
					Iterator subIter = table.getForeignKeyIterator();
					while ( subIter.hasNext() ) {
						ForeignKey fk = ( ForeignKey ) subIter.next();
						if ( fk.isPhysicalConstraint() ) {
							script.add( fk.sqlDropString( 
									dialect, 
									properties.getProperty(Environment.DEFAULT_CATALOG),
									properties.getProperty(Environment.DEFAULT_SCHEMA) ) );
						}
					}
				}
			}
		}


		Iterator iter = getTableMappings();
		while ( iter.hasNext() ) {

			Table table = ( Table ) iter.next();
			if ( table.isPhysicalTable() ) {

				/*Iterator subIter = table.getIndexIterator();
				while ( subIter.hasNext() ) {
					Index index = (Index) subIter.next();
					if ( !index.isForeignKey() || !dialect.hasImplicitIndexForForeignKey() ) {
						script.add( index.sqlDropString(dialect) );
					}
				}*/
	
				script.add( table.sqlDropString( 
						dialect, 
						properties.getProperty(Environment.DEFAULT_CATALOG),
						properties.getProperty(Environment.DEFAULT_SCHEMA) ) );

			}

		}

		iter = iterateGenerators( dialect );
		while ( iter.hasNext() ) {
			String[] lines = ( ( PersistentIdentifierGenerator ) iter.next() ).sqlDropStrings( dialect );
			for ( int i = 0; i < lines.length; i++ ) script.add( lines[i] );
		}

		return ArrayHelper.toStringArray( script );
	}

	/**
	 * Generate DDL for creating tables
	 *
	 * @see org.hibernate.tool.hbm2ddl.SchemaExport
	 */
	public String[] generateSchemaCreationScript(Dialect dialect) throws HibernateException {
		secondPassCompile();

		ArrayList script = new ArrayList( 50 );

		Iterator iter = getTableMappings();
		while ( iter.hasNext() ) {
			Table table = ( Table ) iter.next();
			if ( table.isPhysicalTable() ) {
				script.add( table.sqlCreateString( 
						dialect, 
						mapping, 
						properties.getProperty(Environment.DEFAULT_CATALOG),
						properties.getProperty(Environment.DEFAULT_SCHEMA) ) );
			}
		}

		iter = getTableMappings();
		while ( iter.hasNext() ) {
			Table table = ( Table ) iter.next();
			if ( table.isPhysicalTable() ) {
				
				if( !dialect.supportsUniqueConstraintInCreateAlterTable() ) {
				    Iterator subIter = table.getUniqueKeyIterator();
				    while ( subIter.hasNext() ) {
				        UniqueKey uk = (UniqueKey) subIter.next();
				        script.add( uk.sqlCreateString(dialect, mapping, properties.getProperty(Environment.DEFAULT_CATALOG), properties.getProperty(Environment.DEFAULT_SCHEMA) ) );
				    }
				}
	            
				
				Iterator subIter = table.getIndexIterator();
				while ( subIter.hasNext() ) {
					Index index = ( Index ) subIter.next();
					script.add( index.sqlCreateString( dialect, 
							mapping, 
							properties.getProperty(Environment.DEFAULT_CATALOG),
							properties.getProperty(Environment.DEFAULT_SCHEMA) ) );
				}

				if ( dialect.hasAlterTable() ) {
					subIter = table.getForeignKeyIterator();
					while ( subIter.hasNext() ) {
						ForeignKey fk = ( ForeignKey ) subIter.next();
						if ( fk.isPhysicalConstraint() ) script.add( fk.sqlCreateString( dialect, mapping, 
								properties.getProperty(Environment.DEFAULT_CATALOG),
								properties.getProperty(Environment.DEFAULT_SCHEMA) ) );
					}
				}

			}
		}

		iter = iterateGenerators( dialect );
		while ( iter.hasNext() ) {
			String[] lines = ( ( PersistentIdentifierGenerator ) iter.next() ).sqlCreateStrings( dialect );
			for ( int i = 0; i < lines.length; i++ ) script.add( lines[i] );
		}

		return ArrayHelper.toStringArray( script );
	}

	/**
	 * Generate DDL for altering tables
	 *
	 * @see org.hibernate.tool.hbm2ddl.SchemaUpdate
	 */
	public String[] generateSchemaUpdateScript(Dialect dialect, DatabaseMetadata databaseMetadata) throws HibernateException {
		secondPassCompile();

		ArrayList script = new ArrayList( 50 );

		Iterator iter = getTableMappings();
		while ( iter.hasNext() ) {
			Table table = ( Table ) iter.next();
			if ( table.isPhysicalTable() ) {

				TableMetadata tableInfo = databaseMetadata.getTableMetadata( table.getName(),
						table.getSchema(),
						table.getCatalog() );
				if ( tableInfo == null ) {
					script.add( table.sqlCreateString( 
							dialect, 
							mapping, 
							properties.getProperty(Environment.DEFAULT_CATALOG),
							properties.getProperty(Environment.DEFAULT_SCHEMA) ) );
				}
				else {
					Iterator subiter = table.sqlAlterStrings( 
							dialect, 
							mapping, 
							tableInfo, 
							properties.getProperty(Environment.DEFAULT_CATALOG),
							properties.getProperty(Environment.DEFAULT_SCHEMA) );
					while ( subiter.hasNext() ) script.add( subiter.next() );
				}

			}
		}

		iter = getTableMappings();
		while ( iter.hasNext() ) {
			Table table = ( Table ) iter.next();
			if ( table.isPhysicalTable() ) {

				TableMetadata tableInfo = databaseMetadata.getTableMetadata( table.getName(),
						table.getSchema(),
						table.getCatalog() );

				if ( dialect.hasAlterTable() ) {
					Iterator subIter = table.getForeignKeyIterator();
					while ( subIter.hasNext() ) {
						ForeignKey fk = ( ForeignKey ) subIter.next();
						if ( fk.isPhysicalConstraint() ) {
							boolean create = tableInfo == null || (
									tableInfo.getForeignKeyMetadata( fk.getName() ) == null && (
									//Icky workaround for MySQL bug:
									!( dialect instanceof MySQLDialect ) ||
									tableInfo.getIndexMetadata( fk.getName() ) == null
									)
									);
							if ( create ) script.add( fk.sqlCreateString( 
									dialect, 
									mapping, 
									properties.getProperty(Environment.DEFAULT_CATALOG),
									properties.getProperty(Environment.DEFAULT_SCHEMA) ) );
						}
					}
				}

			}

			/*//broken, 'cos we don't generate these with names in SchemaExport
			subIter = table.getIndexIterator();
			while ( subIter.hasNext() ) {
				Index index = (Index) subIter.next();
				if ( !index.isForeignKey() || !dialect.hasImplicitIndexForForeignKey() ) {
					if ( tableInfo==null || tableInfo.getIndexMetadata( index.getFilterName() ) == null ) {
						script.add( index.sqlCreateString(dialect, mapping) );
					}
				}
			}
			//broken, 'cos we don't generate these with names in SchemaExport
			subIter = table.getUniqueKeyIterator();
			while ( subIter.hasNext() ) {
				UniqueKey uk = (UniqueKey) subIter.next();
				if ( tableInfo==null || tableInfo.getIndexMetadata( uk.getFilterName() ) == null ) {
					script.add( uk.sqlCreateString(dialect, mapping) );
				}
			}*/
		}

		iter = iterateGenerators( dialect );
		while ( iter.hasNext() ) {
			PersistentIdentifierGenerator generator = ( PersistentIdentifierGenerator ) iter.next();
			Object key = generator.generatorKey();
			if ( !databaseMetadata.isSequence( key ) && !databaseMetadata.isTable( key ) ) {
				String[] lines = generator.sqlCreateStrings( dialect );
				for ( int i = 0; i < lines.length; i++ ) script.add( lines[i] );
			}
		}

		return ArrayHelper.toStringArray( script );
	}

	private void validate() throws MappingException {
		Iterator iter = classes.values().iterator();
		while ( iter.hasNext() ) ( ( PersistentClass ) iter.next() ).validate( mapping );
		iter = collections.values().iterator();
		while ( iter.hasNext() ) ( ( Collection ) iter.next() ).validate( mapping );
	}

	/**
	 * Call this to ensure the mappings are fully compiled/built. Usefull to ensure getting
	 * access to all information in the metamodel when calling e.g. getClassMappings().
	 */
	public void buildMappings() {
		secondPassCompile();
	}

	/**
	 * Find the first possible element in the queue of extends.
	 */
	protected org.dom4j.Document findPossibleExtends() {
		Iterator iter = extendsQueue.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry entry = ( Entry ) iter.next();
			String superclass = ( String ) entry.getKey();
			if ( getClassMapping( superclass ) != null ) {
				List queue = (List) entry.getValue();
				if(queue.isEmpty()) {
					iter.remove();
					continue;
				} else {
					return ( org.dom4j.Document ) queue.remove(0);
				}
			}
		}
		return null;
	}

	// This method may be called many times!!
	protected void secondPassCompile() throws MappingException {
		log.info( "processing extends queue" );

		processExtendsQueue();

		log.info( "processing collection mappings" );

		Iterator iter = secondPasses.iterator();
		while ( iter.hasNext() ) {
			HbmBinder.SecondPass sp = ( HbmBinder.SecondPass ) iter.next();
			sp.doSecondPass( classes, CollectionHelper.EMPTY_MAP ); // TODO: align meta-attributes with normal bind...
			iter.remove();
		}

		log.info( "processing association property references" );

		iter = propertyReferences.iterator();
		while ( iter.hasNext() ) {
			Mappings.PropertyReference upr = ( Mappings.PropertyReference ) iter.next();
			PersistentClass clazz = getClassMapping( upr.referencedClass );
			if ( clazz == null ) throw new MappingException( "property-ref to unmapped class: " + upr.referencedClass );
			boolean found = false;
			Iterator propIter = clazz.isJoinedSubclass() ? 
				clazz.getPropertyIterator() : 
				clazz.getPropertyClosureIterator();
			while ( propIter.hasNext() ) {
				Property prop = ( Property ) propIter.next();
				if ( upr.propertyName.equals( prop.getName() ) ) {
					if ( upr.unique ) {
						( ( SimpleValue ) prop.getValue() ).setAlternateUniqueKey( true );
					}
					found = true;
					break;
				}
			}
			if ( !found ) {
				throw new MappingException( "property-ref not found: " + upr.propertyName +
						" in class: " + upr.referencedClass );
			}
		}

		//TODO: Somehow add the newly created foreign keys to the internal collection

		log.info( "processing foreign key constraints" );

		iter = getTableMappings();
		Set done = new HashSet();
		while ( iter.hasNext() ) secondPassCompileForeignKeys( ( Table ) iter.next(), done );

	}

	/**
	 * Try to empty the extends queue.
	 */
	private void processExtendsQueue() {
		org.dom4j.Document document = findPossibleExtends();
		while ( document != null ) {
			add( document );
			document = findPossibleExtends();
		}

		if ( extendsQueue.size() > 0 ) {
			Iterator iterator = extendsQueue.keySet().iterator();
			StringBuffer buf = new StringBuffer( "Following superclasses referenced in extends not found: " );
			while ( iterator.hasNext() ) {
				String element = ( String ) iterator.next();
				buf.append( element );
				if ( iterator.hasNext() ) buf.append( "," );
			}
			throw new MappingException( buf.toString() );
		}
	}

	protected void secondPassCompileForeignKeys(Table table, Set done) throws MappingException {

		table.createForeignKeys();

		Iterator iter = table.getForeignKeyIterator();
		while ( iter.hasNext() ) {

			ForeignKey fk = ( ForeignKey ) iter.next();
			if ( !done.contains( fk ) ) {
				done.add( fk );
				final String referencedEntityName = fk.getReferencedEntityName();
				if (referencedEntityName==null) {
					throw new MappingException("An association from the table "+
							fk.getTable().getName() +
							" does not specify the referenced entity" );
				}
				if ( log.isDebugEnabled() ) {
					log.debug( "resolving reference to class: " + referencedEntityName );
				}
				PersistentClass referencedClass = ( PersistentClass ) classes.get( referencedEntityName );
				if ( referencedClass == null ) {
					throw new MappingException( "An association from the table " +
							fk.getTable().getName() +
							" refers to an unmapped class: " +
							referencedEntityName );
				}
				if ( referencedClass.isJoinedSubclass() ) {
					secondPassCompileForeignKeys( referencedClass.getSuperclass().getTable(), done );
				}
				fk.setReferencedTable( referencedClass.getTable() );
			}
		}
	}

	/**
	 * Get the named queries
	 */
	public Map getNamedQueries() {
		return namedQueries;
	}

	private static final Interceptor EMPTY_INTERCEPTOR = new EmptyInterceptor();

	static final class EmptyInterceptor implements Interceptor, Serializable {

		public void onDelete(
				Object entity, 
				Serializable id, 
				Object[] state, 
				String[] propertyNames, 
				Type[] types) {}

		public boolean onFlushDirty(
				Object entity, 
				Serializable id, 
				Object[] currentState, 
				Object[] previousState, 
				String[] propertyNames, 
				Type[] types) {
			return false;
		}

		public boolean onLoad(
				Object entity, 
				Serializable id, 
				Object[] state, 
				String[] propertyNames, 
				Type[] types) {
			return false;
		}

		public boolean onSave(
				Object entity, 
				Serializable id, 
				Object[] state, 
				String[] propertyNames, 
				Type[] types) {
			return false;
		}

		public void postFlush(Iterator entities) {}
		public void preFlush(Iterator entities) {}

		public Boolean isTransient(Object entity) {
			return null;
		}

		public Object instantiate(String entityName, EntityMode entityMode, Serializable id) {
			return null;
		}

		public int[] findDirty(Object entity,
				Serializable id,
				Object[] currentState,
				Object[] previousState,
				String[] propertyNames,
				Type[] types) {
			return null;
		}

		public String getEntityName(Object object) {
			return null;
		}

		public Object getEntity(String entityName, Serializable id) {
			return null;
		}

		public void afterTransactionBegin(Transaction tx) {}
		public void afterTransactionCompletion(Transaction tx) {}
		public void beforeTransactionCompletion(Transaction tx) {}
		
	}

	/**
	 * Instantiate a new <tt>SessionFactory</tt>, using the properties and
	 * mappings in this configuration. The <tt>SessionFactory</tt> will be
	 * immutable, so changes made to the <tt>Configuration</tt> after
	 * building the <tt>SessionFactory</tt> will not affect it.
	 *
	 * @return a new factory for <tt>Session</tt>s
	 * @see org.hibernate.SessionFactory
	 */
	public SessionFactory buildSessionFactory() throws HibernateException {
		log.debug( "Preparing to build session factory with filters : " + filterDefinitions );
		secondPassCompile();
		validate();
		Environment.verifyProperties( properties );
		Properties copy = new Properties();
		copy.putAll( properties );
		Settings settings = buildSettings();
		return new SessionFactoryImpl( this, mapping, settings, sessionEventListenerConfig.shallowCopy() );
	}

	/**
	 * Return the configured <tt>Interceptor</tt>
	 */
	public Interceptor getInterceptor() {
		return interceptor;
	}

	/**
	 * Get all properties
	 */
	public Properties getProperties() {
		return properties;
	}

	/**
	 * Configure an <tt>Interceptor</tt>
	 */
	public Configuration setInterceptor(Interceptor interceptor) {
		this.interceptor = interceptor;
		return this;
	}

	/**
	 * Specify a completely new set of properties
	 */
	public Configuration setProperties(Properties properties) {
		this.properties = properties;
		return this;
	}

	/**
	 * Set the given properties
	 */
	public Configuration addProperties(Properties extraProperties) {
		this.properties.putAll( extraProperties );
		return this;
	}

	/**
	 * Set a property
	 */
	public Configuration setProperty(String propertyName, String value) {
		properties.setProperty( propertyName, value );
		return this;
	}

	/**
	 * Get a property
	 */
	public String getProperty(String propertyName) {
		return properties.getProperty( propertyName );
	}

	private void addProperties(Element parent) {
		Iterator iter = parent.elementIterator( "property" );
		while ( iter.hasNext() ) {
			Element node = ( Element ) iter.next();
			String name = node.attributeValue( "name" );
			String value = node.getText().trim();
			log.debug( name + "=" + value );
			properties.setProperty( name, value );
			if ( !name.startsWith( "hibernate" ) ) properties.setProperty( "hibernate." + name, value );
		}
		Environment.verifyProperties( properties );
	}

	/**
	 * Get the configuration file as an <tt>InputStream</tt>. Might be overridden
	 * by subclasses to allow the configuration to be located by some arbitrary
	 * mechanism.
	 */
	protected InputStream getConfigurationInputStream(String resource) throws HibernateException {

		log.info( "Configuration resource: " + resource );

		InputStream stream = Environment.class.getResourceAsStream( resource );
		if ( stream == null ) stream = Thread.currentThread().getContextClassLoader().getResourceAsStream( resource );
		if ( stream == null ) {
			log.warn( resource + " not found" );
			throw new HibernateException( resource + " not found" );
		}
		return stream;

	}

	/**
	 * Use the mappings and properties specified in an application
	 * resource named <tt>hibernate.cfg.xml</tt>.
	 */
	public Configuration configure() throws HibernateException {
		configure( "/hibernate.cfg.xml" );
		return this;
	}

	/**
	 * Use the mappings and properties specified in the given application
	 * resource. The format of the resource is defined in
	 * <tt>hibernate-configuration-3.0.dtd</tt>.
	 * <p/>
	 * The resource is found via <tt>getConfigurationInputStream(resource)</tt>.
	 */
	public Configuration configure(String resource) throws HibernateException {
		log.info( "configuring from resource: " + resource );
		InputStream stream = getConfigurationInputStream( resource );
		return doConfigure( stream, resource );
	}

	/**
	 * Use the mappings and properties specified in the given document.
	 * The format of the document is defined in
	 * <tt>hibernate-configuration-2.2.dtd</tt>.
	 *
	 * @param url URL from which you wish to load the configuration
	 * @return A configuration configured via the file
	 * @throws HibernateException
	 */
	public Configuration configure(URL url) throws HibernateException {
		log.info( "configuring from url: " + url.toString() );
		try {
			return doConfigure( url.openStream(), url.toString() );
		}
		catch ( IOException ioe ) {
			throw new HibernateException( "could not configure from URL: " + url, ioe );
		}
	}

	/**
	 * Use the mappings and properties specified in the given application
	 * file. The format of the file is defined in
	 * <tt>hibernate-configuration-3.0.dtd</tt>.
	 *
	 * @param configFile <tt>File</tt> from which you wish to load the configuration
	 * @return A configuration configured via the file
	 * @throws HibernateException
	 */
	public Configuration configure(File configFile) throws HibernateException {
		log.info( "configuring from file: " + configFile.getName() );
		try {
			return doConfigure( new FileInputStream( configFile ), configFile.toString() );
		}
		catch ( FileNotFoundException fnfe ) {
			throw new HibernateException( "could not find file: " + configFile, fnfe );
		}
	}

	/**
	 * Use the mappings and properties specified in the given application
	 * resource. The format of the resource is defined in
	 * <tt>hibernate-configuration-2.2.dtd</tt>.
	 *
	 * @param stream       Inputstream to be read from
	 * @param resourceName The name to use in warning/error messages
	 * @return A configuration configured via the stream
	 * @throws HibernateException
	 */
	protected Configuration doConfigure(InputStream stream, String resourceName) throws HibernateException {

		org.dom4j.Document doc;
		try {
			List errors = new ArrayList();
			doc = xmlHelper.createSAXReader( resourceName, errors, entityResolver ).read( new InputSource( stream ) );
			if ( errors.size() != 0 ) throw new MappingException( "invalid configuration", ( Throwable ) errors.get( 0 ) );
		}
		catch ( Exception e ) {
			log.error( "problem parsing configuration" + resourceName, e );
			throw new HibernateException( "problem parsing configuration" + resourceName, e );
		}
		finally {
			try {
				stream.close();
			}
			catch ( IOException ioe ) {
				log.error( "could not close stream on: " + resourceName, ioe );
			}
		}

		return doConfigure( doc );

	}

	/**
	 * Use the mappings and properties specified in the given XML document.
	 * The format of the file is defined in
	 * <tt>hibernate-configuration-2.2.dtd</tt>.
	 *
	 * @param document an XML document from which you wish to load the configuration
	 * @return A configuration configured via the <tt>Document</tt>
	 * @throws HibernateException if there is problem in accessing the file.
	 */
	public Configuration configure(Document document) throws HibernateException {
		log.info( "configuring from XML document" );
		org.dom4j.Document doc;
		try {
			doc = xmlHelper.createDOMReader().read( document );
		}
		catch ( Exception e ) {
			log.error( "problem parsing document", e );
			throw new HibernateException( "problem parsing document", e );
		}

		return doConfigure( doc );
	}

	protected Configuration doConfigure(org.dom4j.Document doc) throws HibernateException {

		Element sfNode = doc.getRootElement().element( "session-factory" );
		String name = sfNode.attributeValue( "name" );
		if ( name != null ) properties.setProperty( Environment.SESSION_FACTORY_NAME, name );
		addProperties( sfNode );
		parseSessionFactory( sfNode, name );

		Element secNode = doc.getRootElement().element( "security" );
		if ( secNode != null ) parseSecurity( secNode );

		log.info( "Configured SessionFactory: " + name );
		log.debug( "properties: " + properties );

		return this;

	}

	private void parseSessionFactory(Element sfNode, String name) {
		Iterator elements = sfNode.elementIterator();
		while ( elements.hasNext() ) {
			Element subelement = ( Element ) elements.next();
			String subelementName = subelement.getName();
			if ( "mapping".equals( subelementName ) ) {
				parseMappingElement(subelement, name);
			}
			else if ( "class-cache".equals( subelementName ) ) {
				String className = subelement.attributeValue( "class" );
				Attribute regionNode = subelement.attribute( "region" );
				final String region = ( regionNode == null ) ? className : regionNode.getValue();
				setCacheConcurrencyStrategy( className, subelement.attributeValue( "usage" ), region );
			}
			else if ( "collection-cache".equals( subelementName ) ) {
				String role = subelement.attributeValue( "collection" );
				Attribute regionNode = subelement.attribute( "region" );
				final String region = ( regionNode == null ) ? role : regionNode.getValue();
				setCollectionCacheConcurrencyStrategy( role, subelement.attributeValue( "usage" ), region );
			}
			else if ( "listener".equals( subelementName ) ) {
				parseListener( subelement );
			}
		}
	}

	protected void parseMappingElement(Element subelement, String name) {
		Attribute rsrc = subelement.attribute( "resource" );
		Attribute file = subelement.attribute( "file" );
		Attribute jar = subelement.attribute( "jar" );
		Attribute pkg = subelement.attribute( "package" );
		Attribute clazz = subelement.attribute( "class" );
		if ( rsrc != null ) {
			log.debug( name + "<-" + rsrc );
			addResource( rsrc.getValue() );
		}
		else if ( jar != null ) {
			log.debug( name + "<-" + jar );
			addJar( new File( jar.getValue() ) );
		}
		else if ( pkg != null ) {
			throw new MappingException("An AnnotationConfiguration instance is required to use <mapping package=\"" + pkg.getValue() + "\"/>");
		}
		else if ( clazz != null ) {
			throw new MappingException("An AnnotationConfiguration instance is required to use <mapping clazz=\"" + clazz.getValue() + "\"/>");
		}
		else {
			if ( file == null ) throw new MappingException( "<mapping> element in configuration specifies no attributes" );
			log.debug( name + "<-" + file );
			addFile( file.getValue() );
		}
	}

	private void parseSecurity(Element secNode) {
		String contextId = secNode.attributeValue( "context" );
		log.info( "JACC contextID: " + contextId );
		JACCConfiguration jcfg = new JACCConfiguration( contextId );
		Iterator grantElements = secNode.elementIterator();
		while ( grantElements.hasNext() ) {
			Element grantElement = ( Element ) grantElements.next();
			String elementName = grantElement.getName();
			if ( "grant".equals( elementName ) ) {
				jcfg.addPermission( grantElement.attributeValue( "role" ),
						grantElement.attributeValue( "entity-name" ),
						grantElement.attributeValue( "actions" ) );
			}
		}
	}

	private void parseListener(Element element) {
		String type = element.attributeValue( "type" );
		String impl = element.attributeValue( "class" );
		log.debug( "Encountered configured listener : " + type + "=" + impl );

		try {
			Object listener = Class.forName( impl ).newInstance();
			setListener( type, listener );
		}
		catch ( Throwable t ) {
			log.warn( "Unable to parsed specified listener config; using default", t );
		}
	}

	public void setListener(String type, Object listener) {
		if ( "auto-flush".equals( type ) ) {
			sessionEventListenerConfig.setAutoFlushEventListener( ( AutoFlushEventListener ) listener );
		}
		else if ( "merge".equals( type ) ) {
			sessionEventListenerConfig.setMergeEventListener( ( MergeEventListener ) listener );
		}
		else if ( "create".equals( type ) ) {
			sessionEventListenerConfig.setCreateEventListener( ( PersistEventListener ) listener );
		}
		else if ( "delete".equals( type ) ) {
			sessionEventListenerConfig.setDeleteEventListener( ( DeleteEventListener ) listener );
		}
		else if ( "dirty-check".equals( type ) ) {
			sessionEventListenerConfig.setDirtyCheckEventListener( ( DirtyCheckEventListener ) listener );
		}
		else if ( "evict".equals( type ) ) {
			sessionEventListenerConfig.setEvictEventListener( ( EvictEventListener ) listener );
		}
		else if ( "flush".equals( type ) ) {
			sessionEventListenerConfig.setFlushEventListener( ( FlushEventListener ) listener );
		}
		else if ( "flush-entity".equals( type ) ) {
			sessionEventListenerConfig.setFlushEntityEventListener( ( FlushEntityEventListener ) listener );
		}
		else if ( "load".equals( type ) ) {
			sessionEventListenerConfig.setLoadEventListener( ( LoadEventListener ) listener );
		}
		else if ( "load-collection".equals( type ) ) {
			sessionEventListenerConfig.setInitializeCollectionEventListener( ( InitializeCollectionEventListener ) listener );
		}
		else if ( "lock".equals( type ) ) {
			sessionEventListenerConfig.setLockEventListener( ( LockEventListener ) listener );
		}
		else if ( "refresh".equals( type ) ) {
			sessionEventListenerConfig.setRefreshEventListener( ( RefreshEventListener ) listener );
		}
		else if ( "replicate".equals( type ) ) {
			sessionEventListenerConfig.setReplicateEventListener( ( ReplicateEventListener ) listener );
		}
		else if ( "save-update".equals( type ) ) {
			sessionEventListenerConfig.setSaveOrUpdateEventListener( ( SaveOrUpdateEventListener ) listener );
		}
		else if ( "save".equals( type ) ) {
			sessionEventListenerConfig.setSaveEventListener( ( SaveOrUpdateEventListener ) listener );
		}
		else if ( "update".equals( type ) ) {
			sessionEventListenerConfig.setUpdateEventListener( ( SaveOrUpdateEventListener ) listener );
		}
		else if ( "pre-load".equals( type ) ) {
			sessionEventListenerConfig.setPreLoadEventListener( ( PreLoadEventListener ) listener );
		}
		else if ( "pre-update".equals( type ) ) {
			sessionEventListenerConfig.setPreUpdateEventListener( ( PreUpdateEventListener ) listener );
		}
		else if ( "pre-delete".equals( type ) ) {
			sessionEventListenerConfig.setPreDeleteEventListener( ( PreDeleteEventListener ) listener );
		}
		else if ( "pre-insert".equals( type ) ) {
			sessionEventListenerConfig.setPreInsertEventListener( ( PreInsertEventListener ) listener );
		}
		else if ( "post-load".equals( type ) ) {
			sessionEventListenerConfig.setPostLoadEventListener( ( PostLoadEventListener ) listener );
		}
		else if ( "post-update".equals( type ) ) {
			sessionEventListenerConfig.setPostUpdateEventListener( ( PostUpdateEventListener ) listener );
		}
		else if ( "post-delete".equals( type ) ) {
			sessionEventListenerConfig.setPostDeleteEventListener( ( PostDeleteEventListener ) listener );
		}
		else if ( "post-insert".equals( type ) ) {
			sessionEventListenerConfig.setPostInsertEventListener( ( PostInsertEventListener ) listener );
		}
		else {
			log.warn( "Unrecognized listener type [" + type + "]" );
		}
	}

	public SessionEventListenerConfig getSessionEventListenerConfig() {
		return sessionEventListenerConfig;
	}

	RootClass getRootClassMapping(String clazz) throws MappingException {
		try {
			return ( RootClass ) getClassMapping( clazz );
		}
		catch ( ClassCastException cce ) {
			throw new MappingException( "You may only specify a cache for root <class> mappings" );
		}
	}

	/**
	 * Set up a cache for an entity class
	 *
	 * @param clazz
	 * @param concurrencyStrategy
	 * @return Configuration
	 * @throws MappingException
	 */
	public Configuration setCacheConcurrencyStrategy(String clazz, String concurrencyStrategy)
			throws MappingException {
		setCacheConcurrencyStrategy( clazz, concurrencyStrategy, clazz );
		return this;
	}

	void setCacheConcurrencyStrategy(String clazz, String concurrencyStrategy, String region)
			throws MappingException {
		RootClass rootClass = getRootClassMapping( clazz );
		rootClass.setCacheConcurrencyStrategy( concurrencyStrategy );
	}

	/**
	 * Set up a cache for a collection role
	 *
	 * @param collectionRole
	 * @param concurrencyStrategy
	 * @return Configuration
	 * @throws MappingException
	 */
	public Configuration setCollectionCacheConcurrencyStrategy(String collectionRole, String concurrencyStrategy)
			throws MappingException {
		setCollectionCacheConcurrencyStrategy( collectionRole, concurrencyStrategy, collectionRole );
		return this;
	}

	void setCollectionCacheConcurrencyStrategy(String collectionRole, String concurrencyStrategy, String region)
			throws MappingException {
		Collection collection = getCollectionMapping( collectionRole );
		collection.setCacheConcurrencyStrategy( concurrencyStrategy );
	}

	/**
	 * Get the query language imports
	 *
	 * @return a mapping from "import" names to fully qualified class names
	 */
	public Map getImports() {
		return imports;
	}

	/**
	 * Create an object-oriented view of the configuration properties
	 */
	public Settings buildSettings() throws HibernateException {
		return settingsFactory.buildSettings( properties );
	}

	public Map getNamedSQLQueries() {
		return namedSqlQueries;
	}

	/**
	 * @return the NamingStrategy.
	 */
	public NamingStrategy getNamingStrategy() {
		return namingStrategy;
	}

	/**
	 * Set a custom naming strategy
	 *
	 * @param namingStrategy the NamingStrategy to set
	 */
	public Configuration setNamingStrategy(NamingStrategy namingStrategy) {
		this.namingStrategy = namingStrategy;
		return this;
	}

	private Mapping buildMapping() {
		return new Mapping() {
			/**
			 * Returns the identifier type of a mapped class
			 */
			public Type getIdentifierType(String persistentClass) throws MappingException {
				PersistentClass pc = ( ( PersistentClass ) classes.get( persistentClass ) );
				if (pc==null) throw new MappingException("persistent class not known: " + persistentClass);
				return pc.getIdentifier().getType();
			}

			public String getIdentifierPropertyName(String persistentClass) throws MappingException {
				final PersistentClass pc = ( PersistentClass ) classes.get( persistentClass );
				if (pc==null) throw new MappingException("persistent class not known: " + persistentClass);
				if ( !pc.hasIdentifierProperty() ) return null;
				return pc.getIdentifierProperty().getName();
			}

			public Type getPropertyType(String persistentClass, String propertyName) throws MappingException {
				final PersistentClass pc = ( PersistentClass ) classes.get( persistentClass );
				if (pc==null) throw new MappingException("persistent class not known: " + persistentClass);
				Property prop = pc.getProperty(propertyName);
				if (prop==null)  throw new MappingException("property not known: " + persistentClass + '.' + propertyName);
				return prop.getType();
			}
		};
	}
	
	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		this.mapping = buildMapping();
		xmlHelper = new XMLHelper();
	}

	public Map getFilterDefinitions() {
		return filterDefinitions;
	}

	public void addFilterDefinition(FilterDefinition definition) {
		filterDefinitions.put( definition.getFilterName(), definition );
	}
}






