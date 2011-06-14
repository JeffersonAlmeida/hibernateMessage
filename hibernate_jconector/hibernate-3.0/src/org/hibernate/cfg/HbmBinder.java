// $Id: HbmBinder.java,v 1.77 2005/03/30 18:01:39 oneovthafew Exp $
package org.hibernate.cfg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.hibernate.FetchMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.engine.NamedQueryDefinition;
import org.hibernate.engine.NamedSQLQueryDefinition;
import org.hibernate.engine.Versioning;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.loader.custom.SQLQueryCollectionReturn;
import org.hibernate.loader.custom.SQLQueryJoinReturn;
import org.hibernate.loader.custom.SQLQueryReturn;
import org.hibernate.loader.custom.SQLQueryRootReturn;
import org.hibernate.loader.custom.SQLQueryScalarReturn;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Array;
import org.hibernate.mapping.Backref;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.Fetchable;
import org.hibernate.mapping.Filterable;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.IdentifierCollection;
import org.hibernate.mapping.IndexBackref;
import org.hibernate.mapping.IndexedCollection;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.List;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PrimitiveArray;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.QueryList;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Set;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.TypeDef;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.mapping.Value;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.type.DiscriminatorType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.CollectionHelper;
import org.hibernate.util.JoinedIterator;
import org.hibernate.util.ReflectHelper;
import org.hibernate.util.StringHelper;

/**
 * Walks an XML mapping document and produces the Hibernate configuration-time metamodel (the
 * classes in the <tt>mapping</tt> package)
 * 
 * @author Gavin King
 */
public final class HbmBinder {

	private static final Log log = LogFactory.getLog( HbmBinder.class );

	/**
	 * Private constructor to disallow instantiation.
	 */
	private HbmBinder() {
	}

	/**
	 * The main contract into the hbm.xml-based binder. Performs necessary binding operations
	 * represented by the given DOM.
	 * 
	 * @param doc The DOM to be parsed and bound.
	 * @param mappings Current bind state.
	 * @param inheritedMetas Any inherited meta-tag information.
	 * @throws MappingException
	 */
	public static void bindRoot(Document doc, Mappings mappings, java.util.Map inheritedMetas)
			throws MappingException {

		java.util.List names = HbmBinder.getExtendsNeeded( doc, mappings );
		if ( !names.isEmpty() ) {
			// classes mentioned in extends not available - so put it in queue
			for ( Iterator iter = names.iterator(); iter.hasNext(); ) {
				String className = (String) iter.next();
				mappings.addToExtendsQueue( className, doc );
			}
			return;
		}

		Element hmNode = doc.getRootElement();
		inheritedMetas = getMetas( hmNode, inheritedMetas, true ); // get meta's from
																	// <hibernate-mapping>
		extractRootAttributes( hmNode, mappings );

		Iterator filterDefs = hmNode.elementIterator( "filter-def" );
		while ( filterDefs.hasNext() ) {
			parseFilterDef( (Element) filterDefs.next(), mappings );
		}

		Iterator typeDefs = hmNode.elementIterator( "typedef" );
		while ( typeDefs.hasNext() ) {
			Element typeDef = (Element) typeDefs.next();
			String typeClass = typeDef.attributeValue( "class" );
			String typeName = typeDef.attributeValue( "name" );
			Iterator paramIter = typeDef.elementIterator( "param" );
			Properties parameters = new Properties();
			while ( paramIter.hasNext() ) {
				Element param = (Element) paramIter.next();
				parameters.setProperty( param.attributeValue( "name" ), param.getTextTrim() );
			}

			mappings.addTypeDef( typeName, typeClass, parameters );
		}

		Iterator nodes = hmNode.elementIterator( "class" );
		while ( nodes.hasNext() ) {
			Element n = (Element) nodes.next();
			RootClass rootclass = new RootClass();
			bindRootClass( n, rootclass, mappings, inheritedMetas );
			mappings.addClass( rootclass );
		}

		Iterator subclassnodes = hmNode.elementIterator( "subclass" );
		while ( subclassnodes.hasNext() ) {
			Element subnode = (Element) subclassnodes.next();
			PersistentClass superModel = getSuperclass( mappings, subnode );
			handleSubclass( superModel, mappings, subnode, inheritedMetas );
		}

		Iterator joinedsubclassnodes = hmNode.elementIterator( "joined-subclass" );
		while ( joinedsubclassnodes.hasNext() ) {
			Element subnode = (Element) joinedsubclassnodes.next();
			PersistentClass superModel = getSuperclass( mappings, subnode );
			handleJoinedSubclass( superModel, mappings, subnode, inheritedMetas );
		}

		Iterator unionsubclassnodes = hmNode.elementIterator( "union-subclass" );
		while ( unionsubclassnodes.hasNext() ) {
			Element subnode = (Element) unionsubclassnodes.next();
			PersistentClass superModel = getSuperclass( mappings, subnode );
			handleUnionSubclass( superModel, mappings, subnode, inheritedMetas );
		}

		nodes = hmNode.elementIterator( "query" );
		while ( nodes.hasNext() ) {
			bindNamedQuery( (Element) nodes.next(), mappings );
		}

		nodes = hmNode.elementIterator( "sql-query" );
		while ( nodes.hasNext() ) {
			bindNamedSQLQuery( (Element) nodes.next(), mappings );
		}

		nodes = hmNode.elementIterator( "import" );
		while ( nodes.hasNext() ) {
			Element n = (Element) nodes.next();
			String className = getClassName( n.attribute( "class" ), mappings );
			Attribute renameNode = n.attribute( "rename" );
			String rename = ( renameNode == null )
				? StringHelper.unqualify( className )
				: renameNode.getValue();
			log.debug( "Import: " + rename + " -> " + className );
			mappings.addImport( className, rename );
		}
	}

	private static void extractRootAttributes(Element hmNode, Mappings mappings) {
		Attribute schemaNode = hmNode.attribute( "schema" );
		mappings.setSchemaName( ( schemaNode == null ) ? null : schemaNode.getValue() );

		Attribute catalogNode = hmNode.attribute( "catalog" );
		mappings.setCatalogName( ( catalogNode == null ) ? null : catalogNode.getValue() );

		Attribute dcNode = hmNode.attribute( "default-cascade" );
		mappings.setDefaultCascade( ( dcNode == null ) ? "none" : dcNode.getValue() );

		Attribute daNode = hmNode.attribute( "default-access" );
		mappings.setDefaultAccess( ( daNode == null ) ? "property" : daNode.getValue() );

		Attribute dlNode = hmNode.attribute( "default-lazy" );
		mappings.setDefaultLazy( dlNode == null || dlNode.getValue().equals( "true" ) );

		Attribute aiNode = hmNode.attribute( "auto-import" );
		mappings.setAutoImport( ( aiNode == null ) ? true : "true".equals( aiNode.getValue() ) );

		Attribute packNode = hmNode.attribute( "package" );
		if ( packNode != null ) mappings.setDefaultPackage( packNode.getValue() );
	}

	/**
	 * Responsible for perfoming the bind operation related to an &lt;class/&gt; mapping element.
	 * 
	 * @param node The DOM Element for the &lt;class/&gt; element.
	 * @param rootClass The mapping instance to which to bind the information.
	 * @param mappings The current bind state.
	 * @param inheritedMetas Any inherited meta-tag information.
	 * @throws MappingException
	 */
	public static void bindRootClass(Element node, RootClass rootClass, Mappings mappings,
			java.util.Map inheritedMetas) throws MappingException {
		bindClass( node, rootClass, mappings, inheritedMetas );
		inheritedMetas = getMetas( node, inheritedMetas, true ); // get meta's from <class>
		bindRootPersistentClassCommonValues( node, inheritedMetas, mappings, rootClass );
	}

	private static void bindRootPersistentClassCommonValues(Element node,
			java.util.Map inheritedMetas, Mappings mappings, RootClass entity)
			throws MappingException {

		// DB-OBJECTNAME

		Attribute schemaNode = node.attribute( "schema" );
		String schema = schemaNode == null ? mappings.getSchemaName() : schemaNode.getValue();

		Attribute catalogNode = node.attribute( "catalog" );
		String catalog = catalogNode == null ? mappings.getCatalogName() : catalogNode.getValue();

		Table table = mappings.addTable(
			schema,
			catalog,
			getClassTableName( entity, node, mappings ),
			getSubselect( node ),
			entity.isAbstract() );
		entity.setTable( table );

		log
			.info( "Mapping class: "
				+ entity.getEntityName()
				+ " -> "
				+ entity.getTable().getName() );

		// MUTABLE
		Attribute mutableNode = node.attribute( "mutable" );
		entity.setMutable( ( mutableNode == null ) || mutableNode.getValue().equals( "true" ) );

		// WHERE
		Attribute whereNode = node.attribute( "where" );
		if ( whereNode != null ) entity.setWhere( whereNode.getValue() );

		// CHECK
		Attribute chNode = node.attribute( "check" );
		if ( chNode != null ) table.addCheckConstraint( chNode.getValue() );

		// POLYMORPHISM
		Attribute polyNode = node.attribute( "polymorphism" );
		entity.setExplicitPolymorphism( ( polyNode != null )
			&& polyNode.getValue().equals( "explicit" ) );

		// ROW ID
		Attribute rowidNode = node.attribute( "rowid" );
		if ( rowidNode != null ) table.setRowId( rowidNode.getValue() );

		Iterator subnodes = node.elementIterator();
		while ( subnodes.hasNext() ) {

			Element subnode = (Element) subnodes.next();
			String name = subnode.getName();

			if ( "id".equals( name ) ) {
				// ID
				bindSimpleId( subnode, entity, mappings, inheritedMetas );
			}
			else if ( "composite-id".equals( name ) ) {
				// COMPOSITE-ID
				bindCompositeId( subnode, entity, mappings, inheritedMetas );
			}
			else if ( "version".equals( name ) || "timestamp".equals( name ) ) {
				// VERSION / TIMESTAMP
				bindVersioningProperty( table, subnode, mappings, name, entity, inheritedMetas );
			}
			else if ( "discriminator".equals( name ) ) {
				// DISCRIMINATOR
				bindDiscriminatorProperty( table, entity, subnode, mappings );
			}
			else if ( "cache".equals( name ) ) {
				entity.setCacheConcurrencyStrategy( subnode.attributeValue( "usage" ) );
				entity.setCacheRegionName( subnode.attributeValue( "region" ) );
			}

		}

		// Primary key constraint
		entity.createPrimaryKey();

		createClassProperties( node, entity, mappings, inheritedMetas );
	}

	private static void bindSimpleId(Element idNode, RootClass entity, Mappings mappings,
			java.util.Map inheritedMetas) throws MappingException {
		String propertyName = idNode.attributeValue( "name" );

		SimpleValue id = new SimpleValue( entity.getTable() );
		entity.setIdentifier( id );

		// if ( propertyName == null || entity.getPojoRepresentation() == null ) {
		// bindSimpleValue( idNode, id, false, RootClass.DEFAULT_IDENTIFIER_COLUMN_NAME, mappings );
		// if ( !id.isTypeSpecified() ) {
		// throw new MappingException( "must specify an identifier type: " + entity.getEntityName()
		// );
		// }
		// }
		// else {
		// bindSimpleValue( idNode, id, false, propertyName, mappings );
		// PojoRepresentation pojo = entity.getPojoRepresentation();
		// id.setTypeUsingReflection( pojo.getClassName(), propertyName );
		//
		// Property prop = new Property();
		// prop.setValue( id );
		// bindProperty( idNode, prop, mappings, inheritedMetas );
		// entity.setIdentifierProperty( prop );
		// }

		if ( propertyName == null ) {
			bindSimpleValue( idNode, id, false, RootClass.DEFAULT_IDENTIFIER_COLUMN_NAME, mappings );
		}
		else {
			bindSimpleValue( idNode, id, false, propertyName, mappings );
		}

		if ( propertyName == null || !entity.hasPojoRepresentation() ) {
			if ( !id.isTypeSpecified() ) {
				throw new MappingException( "must specify an identifier type: "
					+ entity.getEntityName() );
			}
		}
		else {
			id.setTypeUsingReflection( entity.getClassName(), propertyName );
		}

		if ( propertyName != null ) {
			Property prop = new Property();
			prop.setValue( id );
			bindProperty( idNode, prop, mappings, inheritedMetas );
			entity.setIdentifierProperty( prop );
		}

		// TODO:
		/*
		 * if ( id.getHibernateType().getReturnedClass().isArray() ) throw new MappingException(
		 * "illegal use of an array as an identifier (arrays don't reimplement equals)" );
		 */
		makeIdentifier( idNode, id, mappings );
	}

	private static void bindCompositeId(Element idNode, RootClass entity, Mappings mappings,
			java.util.Map inheritedMetas) throws MappingException {
		String propertyName = idNode.attributeValue( "name" );
		Component id = new Component( entity );
		entity.setIdentifier( id );
		bindCompositeId( idNode, id, entity, propertyName, mappings, inheritedMetas );
		if ( propertyName == null ) {
			entity.setEmbeddedIdentifier( id.isEmbedded() );
			if ( id.isEmbedded() ) {
				// todo : what is the implication of this?
				id.setDynamic( !entity.hasPojoRepresentation() );
				/*
				 * Property prop = new Property(); prop.setName("id");
				 * prop.setPropertyAccessorName("embedded"); prop.setValue(id);
				 * entity.setIdentifierProperty(prop);
				 */
			}
		}
		else {
			Property prop = new Property();
			prop.setValue( id );
			bindProperty( idNode, prop, mappings, inheritedMetas );
			entity.setIdentifierProperty( prop );
		}

		makeIdentifier( idNode, id, mappings );

		if ( !id.isDynamic() ) {
			try {
				Class idClass = id.getComponentClass();
				if ( idClass != null && !ReflectHelper.overridesEquals( idClass ) ) {
					throw new MappingException( "composite-id class must override equals(): "
						+ id.getComponentClass().getName() );
				}
				if ( !ReflectHelper.overridesHashCode( idClass ) ) {
					throw new MappingException( "composite-id class must override hashCode(): "
						+ id.getComponentClass().getName() );
				}
				if ( !Serializable.class.isAssignableFrom( idClass ) ) {
					throw new MappingException( "composite-id class must implement Serializable: "
						+ id.getComponentClass().getName() );
				}
			}
			catch (MappingException cnfe) {
				log.warn( "Could not perform validation checks for component as the class "
					+ id.getComponentClassName()
					+ " was not found" );
			}
		}
	}

	private static void bindVersioningProperty(Table table, Element subnode, Mappings mappings,
			String name, RootClass entity, java.util.Map inheritedMetas) {

		String propertyName = subnode.attributeValue( "name" );
		SimpleValue val = new SimpleValue( table );
		bindSimpleValue( subnode, val, false, propertyName, mappings );
		if ( !val.isTypeSpecified() ) {
			val.setTypeName( "version".equals( name ) ? "integer" : "timestamp" );
		}
		Property prop = new Property();
		prop.setValue( val );
		bindProperty( subnode, prop, mappings, inheritedMetas );
		makeVersion( subnode, val );
		entity.setVersion( prop );
		entity.addProperty( prop );
	}

	private static void bindDiscriminatorProperty(Table table, RootClass entity, Element subnode,
			Mappings mappings) {
		SimpleValue discrim = new SimpleValue( table );
		entity.setDiscriminator( discrim );
		bindSimpleValue(
			subnode,
			discrim,
			false,
			RootClass.DEFAULT_DISCRIMINATOR_COLUMN_NAME,
			mappings );
		if ( !discrim.isTypeSpecified() ) {
			discrim.setTypeName( "string" );
			// ( (Column) discrim.getColumnIterator().next() ).setType(type);
		}
		entity.setPolymorphic( true );
		if ( "true".equals( subnode.attributeValue( "force" ) ) )
			entity.setForceDiscriminator( true );
		if ( "false".equals( subnode.attributeValue( "insert" ) ) )
			entity.setDiscriminatorInsertable( false );
	}

	public static void bindClass(Element node, PersistentClass persistentClass, Mappings mappings,
			java.util.Map inheritedMetas) throws MappingException {
		// transfer an explicitly defined entity name
		// handle the lazy attribute
		Attribute lazyNode = node.attribute( "lazy" );
		boolean lazy = lazyNode == null ? mappings.isDefaultLazy() : "true".equals( lazyNode
			.getValue() );
		// go ahead and set the lazy here, since pojo.proxy can override it.
		persistentClass.setLazy( lazy );

		String entityName = node.attributeValue( "entity-name" );
		if ( entityName == null ) entityName = getClassName( node.attribute("name"), mappings );
		if ( entityName==null ) {
			throw new MappingException( "Unable to determine entity name" );
		}
		persistentClass.setEntityName( entityName );

		bindPojoRepresentation( node, persistentClass, mappings, inheritedMetas );
		bindDom4jRepresentation( node, persistentClass, mappings, inheritedMetas );
		bindMapRepresentation( node, persistentClass, mappings, inheritedMetas );

		bindPersistentClassCommonValues( node, persistentClass, mappings, inheritedMetas );

	}

	private static void bindPojoRepresentation(Element node, PersistentClass entity,
			Mappings mappings, java.util.Map metaTags) {

		String className = getClassName( node.attribute( "name" ), mappings );
		String proxyName = getClassName( node.attribute( "proxy" ), mappings );

		entity.setClassName( className );

		if ( proxyName != null ) {
			entity.setProxyInterfaceName( proxyName );
			entity.setLazy( true );
		}
		else if ( entity.isLazy() ) {
			entity.setProxyInterfaceName( className );
		}

	}

	private static void bindDom4jRepresentation(Element node, PersistentClass entity,
			Mappings mappings, java.util.Map inheritedMetas) {
		String nodeName = node.attributeValue( "node" );
		if (nodeName==null) nodeName = StringHelper.unqualify( entity.getEntityName() );
		entity.setNodeName(nodeName);
	}

	private static void bindMapRepresentation(Element node, PersistentClass entity,
			Mappings mappings, java.util.Map inheritedMetas) {
		// nothing to do
	}

	private static void bindPersistentClassCommonValues(Element node, PersistentClass entity,
			Mappings mappings, java.util.Map inheritedMetas) throws MappingException {
		// DISCRIMINATOR
		Attribute discriminatorNode = node.attribute( "discriminator-value" );
		entity.setDiscriminatorValue( ( discriminatorNode == null )
			? entity.getEntityName()
			: discriminatorNode.getValue() );

		// DYNAMIC UPDATE
		Attribute dynamicNode = node.attribute( "dynamic-update" );
		entity.setDynamicUpdate( ( dynamicNode == null ) ? false : "true".equals( dynamicNode
			.getValue() ) );

		// DYNAMIC INSERT
		Attribute insertNode = node.attribute( "dynamic-insert" );
		entity.setDynamicInsert( ( insertNode == null ) ? false : "true".equals( insertNode
			.getValue() ) );

		// IMPORT
		mappings.addImport( entity.getEntityName(), entity.getEntityName() );
		if ( mappings.isAutoImport() && entity.getEntityName().indexOf( '.' ) > 0 ) {
			mappings.addImport( entity.getEntityName(), StringHelper.unqualify( entity
				.getEntityName() ) );
		}

		// BATCH SIZE
		Attribute batchNode = node.attribute( "batch-size" );
		if ( batchNode != null ) entity.setBatchSize( Integer.parseInt( batchNode.getValue() ) );

		// SELECT BEFORE UPDATE
		Attribute sbuNode = node.attribute( "select-before-update" );
		if ( sbuNode != null ) entity.setSelectBeforeUpdate( "true".equals( sbuNode.getValue() ) );

		// OPTIMISTIC LOCK MODE
		Attribute olNode = node.attribute( "optimistic-lock" );
		entity.setOptimisticLockMode( getOptimisticLockMode( olNode ) );

		entity.setMetaAttributes( getMetas( node, inheritedMetas ) );

		// PERSISTER
		Attribute persisterNode = node.attribute( "persister" );
		if ( persisterNode == null ) {
			// persister = SingleTableEntityPersister.class;
		}
		else {
			try {
				entity.setEntityPersisterClass( ReflectHelper.classForName( persisterNode
					.getValue() ) );
			}
			catch (ClassNotFoundException cnfe) {
				throw new MappingException( "Could not find persister class: "
					+ persisterNode.getValue() );
			}
		}

		// CUSTOM SQL
		handleCustomSQL( node, entity );

		Iterator tables = node.elementIterator( "synchronize" );
		while ( tables.hasNext() ) {
			entity.addSynchronizedTable( ( (Element) tables.next() ).attributeValue( "table" ) );
		}

		Attribute abstractNode = node.attribute( "abstract" );
		if ( abstractNode != null ) entity.setAbstract( "true".equals( abstractNode.getValue() ) );
	}

	private static void handleCustomSQL(Element node, PersistentClass model)
			throws MappingException {
		Element element = node.element( "sql-insert" );
		if ( element != null ) {
			boolean callable = false;
			callable = isCallable( element );
			model.setCustomSQLInsert( element.getText(), callable );
		}

		element = node.element( "sql-delete" );
		if ( element != null ) {
			boolean callable = false;
			callable = isCallable( element );
			model.setCustomSQLDelete( element.getText(), callable );
		}

		element = node.element( "sql-update" );
		if ( element != null ) {
			boolean callable = false;
			callable = isCallable( element );
			model.setCustomSQLUpdate( element.getText(), callable );
		}

		element = node.element( "loader" );
		if ( element != null ) {
			model.setLoaderName( element.attributeValue( "query-ref" ) );
		}
	}

	private static void handleCustomSQL(Element node, Join model) throws MappingException {
		Element element = node.element( "sql-insert" );
		if ( element != null ) {
			boolean callable = false;
			callable = isCallable( element );
			model.setCustomSQLInsert( element.getText(), callable );
		}

		element = node.element( "sql-delete" );
		if ( element != null ) {
			boolean callable = false;
			callable = isCallable( element );
			model.setCustomSQLDelete( element.getText(), callable );
		}

		element = node.element( "sql-update" );
		if ( element != null ) {
			boolean callable = false;
			callable = isCallable( element );
			model.setCustomSQLUpdate( element.getText(), callable );
		}
	}

	private static void handleCustomSQL(Element node, Collection model) throws MappingException {
		Element element = node.element( "sql-insert" );
		if ( element != null ) {
			boolean callable = false;
			callable = isCallable( element, true );
			model.setCustomSQLInsert( element.getText(), callable );
		}

		element = node.element( "sql-delete" );
		if ( element != null ) {
			boolean callable = false;
			callable = isCallable( element, true );
			model.setCustomSQLDelete( element.getText(), callable );
		}

		element = node.element( "sql-update" );
		if ( element != null ) {
			boolean callable = false;
			callable = isCallable( element, true );
			model.setCustomSQLUpdate( element.getText(), callable );
		}

		element = node.element( "sql-delete-all" );
		if ( element != null ) {
			boolean callable = false;
			callable = isCallable( element, true );
			model.setCustomSQLDeleteAll( element.getText(), callable );
		}
	}

	private static boolean isCallable(Element e) throws MappingException {
		return isCallable( e, true );
	}

	/**
	 * @param element
	 * @param supportsCallable
	 * @return
	 */
	private static boolean isCallable(Element element, boolean supportsCallable)
			throws MappingException {
		Attribute attrib = element.attribute( "callable" );
		if ( attrib != null && "true".equals( attrib.getValue() ) ) {
			if ( !supportsCallable ) {
				throw new MappingException( "callable attribute not supported yet!" );
			}
			return true;
		}
		return false;
	}

	public static void bindUnionSubclass(Element node, UnionSubclass unionSubclass,
			Mappings mappings, java.util.Map inheritedMetas) throws MappingException {

		bindClass( node, unionSubclass, mappings, inheritedMetas );
		inheritedMetas = getMetas( node, inheritedMetas, true ); // get meta's from <subclass>

		if ( unionSubclass.getEntityPersisterClass() == null ) {
			unionSubclass.getRootClass().setEntityPersisterClass(
				UnionSubclassEntityPersister.class );
		}

		Attribute schemaNode = node.attribute( "schema" );
		String schema = schemaNode == null ? mappings.getSchemaName() : schemaNode.getValue();

		Attribute catalogNode = node.attribute( "catalog" );
		String catalog = catalogNode == null ? mappings.getCatalogName() : catalogNode.getValue();

		Table mytable = mappings.addDenormalizedTable( 
				schema, 
				catalog, 
				getClassTableName(unionSubclass, node, mappings ), 
				unionSubclass.isAbstract(), 
				getSubselect( node ), 
				unionSubclass.getSuperclass().getTable() );
		unionSubclass.setTable( mytable );

		log.info( "Mapping union-subclass: "
			+ unionSubclass.getEntityName()
			+ " -> "
			+ unionSubclass.getTable().getName() );

		createClassProperties( node, unionSubclass, mappings, inheritedMetas );

	}

	public static void bindSubclass(Element node, Subclass subclass, Mappings mappings,
			java.util.Map inheritedMetas) throws MappingException {

		bindClass( node, subclass, mappings, inheritedMetas );
		inheritedMetas = getMetas( node, inheritedMetas, true ); // get meta's from <subclass>

		if ( subclass.getEntityPersisterClass() == null ) {
			subclass.getRootClass().setEntityPersisterClass( SingleTableEntityPersister.class );
		}

		log.info( "Mapping subclass: "
			+ subclass.getEntityName()
			+ " -> "
			+ subclass.getTable().getName() );

		// properties
		createClassProperties( node, subclass, mappings, inheritedMetas );
	}

	private static String getClassTableName(PersistentClass model, Element node, Mappings mappings) {
		Attribute tableNameNode = node.attribute( "table" );
		if ( tableNameNode == null ) {
			return mappings.getNamingStrategy().classToTableName( model.getEntityName() );
		}
		else {
			return mappings.getNamingStrategy().tableName( tableNameNode.getValue() );
		}
	}

	public static void bindJoinedSubclass(Element node, JoinedSubclass joinedSubclass,
			Mappings mappings, java.util.Map inheritedMetas) throws MappingException {

		bindClass( node, joinedSubclass, mappings, inheritedMetas );
		inheritedMetas = getMetas( node, inheritedMetas, true ); // get meta's from
																	// <joined-subclass>

		// joined subclasses
		if ( joinedSubclass.getEntityPersisterClass() == null ) {
			joinedSubclass.getRootClass().setEntityPersisterClass(
				JoinedSubclassEntityPersister.class );
		}

		Attribute schemaNode = node.attribute( "schema" );
		String schema = schemaNode == null ? mappings.getSchemaName() : schemaNode.getValue();

		Attribute catalogNode = node.attribute( "catalog" );
		String catalog = catalogNode == null ? mappings.getCatalogName() : catalogNode.getValue();

		Table mytable = mappings.addTable( schema, catalog, getClassTableName(
			joinedSubclass,
			node,
			mappings ), getSubselect( node ), false );
		joinedSubclass.setTable( mytable );

		log.info( "Mapping joined-subclass: "
			+ joinedSubclass.getEntityName()
			+ " -> "
			+ joinedSubclass.getTable().getName() );

		// KEY
		Element keyNode = node.element( "key" );
		SimpleValue key = new DependantValue( mytable, joinedSubclass.getIdentifier() );
		joinedSubclass.setKey( key );
		key.setCascadeDeleteEnabled( "cascade".equals( keyNode.attributeValue( "on-delete" ) ) );
		bindSimpleValue( keyNode, key, false, joinedSubclass.getEntityName(), mappings );

		// model.getKey().setType( new Type( model.getIdentifier() ) );
		joinedSubclass.createPrimaryKey();
		joinedSubclass.createForeignKey();

		// CHECK
		Attribute chNode = node.attribute( "check" );
		if ( chNode != null ) mytable.addCheckConstraint( chNode.getValue() );

		// properties
		createClassProperties( node, joinedSubclass, mappings, inheritedMetas );

	}

	private static void bindJoin(Element node, Join join, Mappings mappings,
			java.util.Map inheritedMetas) throws MappingException {

		PersistentClass persistentClass = join.getPersistentClass();
		String path = persistentClass.getEntityName();

		// TABLENAME

		Attribute schemaNode = node.attribute( "schema" );
		String schema = schemaNode == null ? mappings.getSchemaName() : schemaNode.getValue();

		Attribute catalogNode = node.attribute( "catalog" );
		String catalog = catalogNode == null ? mappings.getCatalogName() : catalogNode.getValue();

		Table table = mappings.addTable( schema, catalog, getClassTableName(
			persistentClass,
			node,
			mappings ), getSubselect( node ), false );
		join.setTable( table );

		Attribute fetchNode = node.attribute( "fetch" );
		if ( fetchNode != null )
			join.setSequentialSelect( "select".equals( fetchNode.getValue() ) );

		Attribute invNode = node.attribute( "inverse" );
		if ( invNode != null ) join.setInverse( "true".equals( invNode.getValue() ) );

		Attribute nullNode = node.attribute( "optional" );
		if ( nullNode != null ) join.setOptional( "true".equals( nullNode.getValue() ) );

		log.info( "Mapping class join: "
			+ persistentClass.getEntityName()
			+ " -> "
			+ join.getTable().getName() );

		// KEY
		Element keyNode = node.element( "key" );
		SimpleValue key = new DependantValue( table, persistentClass.getIdentifier() );
		join.setKey( key );
		key.setCascadeDeleteEnabled( "cascade".equals( keyNode.attributeValue( "on-delete" ) ) );
		bindSimpleValue( keyNode, key, false, persistentClass.getEntityName(), mappings );

		// join.getKey().setType( new Type( lazz.getIdentifier() ) );
		join.createPrimaryKey();
		join.createForeignKey();

		// PROPERTIES
		Iterator iter = node.elementIterator();
		while ( iter.hasNext() ) {
			Element subnode = (Element) iter.next();
			String name = subnode.getName();
			String propertyName = subnode.attributeValue( "name" );

			Value value = null;
			if ( "many-to-one".equals( name ) ) {
				value = new ManyToOne( table );
				bindManyToOne( subnode, (ManyToOne) value, propertyName, true, mappings );
			}
			else if ( "any".equals( name ) ) {
				value = new Any( table );
				bindAny( subnode, (Any) value, true, mappings );
			}
			else if ( "property".equals( name ) ) {
				value = new SimpleValue( table );
				bindSimpleValue( subnode, (SimpleValue) value, true, propertyName, mappings );
			}
			else if ( "component".equals( name ) || "dynamic-component".equals( name ) ) {
				String subpath = StringHelper.qualify( path, propertyName );
				value = new Component( join );
				bindComponent(
					subnode,
					(Component) value,
					join.getPersistentClass().getClassName(),
					propertyName,
					subpath,
					true,
					false,
					mappings,
					inheritedMetas );
			}

			if ( value != null ) {
				Property prop = createProperty( value, propertyName, persistentClass
					.getEntityName(), subnode, mappings, inheritedMetas );
				prop.setOptional( join.isOptional() );
				join.addProperty( prop );
			}

		}

		// CUSTOM SQL
		handleCustomSQL( node, join );

	}

	public static void bindColumns(final Element node, final SimpleValue simpleValue,
			final boolean isNullable, final boolean autoColumn, final String propertyPath,
			final Mappings mappings) throws MappingException {

		// COLUMN(S)
		Attribute columnAttribute = node.attribute( "column" );
		if ( columnAttribute == null ) {
			Iterator iter = node.elementIterator();
			int count = 0;
			Table table = simpleValue.getTable();
			while ( iter.hasNext() ) {
				Element columnElement = (Element) iter.next();
				if ( columnElement.getName().equals( "column" ) ) {
					Column column = new Column();
					column.setValue( simpleValue );
					column.setTypeIndex( count++ );
					bindColumn( columnElement, column, isNullable );
					column.setName( mappings.getNamingStrategy().columnName(
						columnElement.attributeValue( "name" ) ) );
					if ( table != null ) table.addColumn( column ); // table=null -> an association
																	// - fill it in later
					simpleValue.addColumn( column );
					// column index
					bindIndex( columnElement.attribute( "index" ), table, column );
					// column group index (although can server as a separate column index)
					bindIndex( node.attribute( "index" ), table, column );
					bindUniqueKey( columnElement.attribute( "unique-key" ), table, column );
				}
				else if ( columnElement.getName().equals( "formula" ) ) {
					Formula formula = new Formula();
					formula.setFormula( columnElement.getText() );
					simpleValue.addFormula( formula );
				}
			}
		}
		else {
			if ( node.elementIterator( "column" ).hasNext() ) {
				throw new MappingException(
					"column attribute may not be used together with <column> subelement" );
			}
			if ( node.elementIterator( "formula" ).hasNext() ) {
				throw new MappingException(
					"column attribute may not be used together with <formula> subelement" );
			}

			Column column = new Column();
			column.setValue( simpleValue );
			bindColumn( node, column, isNullable );
			column.setName( mappings.getNamingStrategy().columnName( columnAttribute.getValue() ) );
			Table table = simpleValue.getTable();
			if ( table != null ) table.addColumn( column ); // table=null -> an association - fill
															// it in later
			simpleValue.addColumn( column );
			// column group index (although can serve as a seperate column index)
			bindIndex( node.attribute( "index" ), table, column );
		}

		if ( autoColumn && simpleValue.getColumnSpan() == 0 ) {
			Column col = new Column();
			col.setValue( simpleValue );
			bindColumn( node, col, isNullable );
			col.setName( mappings.getNamingStrategy().propertyToColumnName( propertyPath ) );
			simpleValue.getTable().addColumn( col );
			simpleValue.addColumn( col );
		}

	}

	private static void bindIndex(Attribute indexAttribute, Table table, Column column) {
		if ( indexAttribute != null && table != null ) {
			StringTokenizer tokens = new StringTokenizer( indexAttribute.getValue(), ", " );
			while ( tokens.hasMoreTokens() ) {
				table.getOrCreateIndex( tokens.nextToken() ).addColumn( column );
			}
		}
	}

	private static void bindUniqueKey(Attribute uniqueKeyAttribute, Table table, Column column) {
		if ( uniqueKeyAttribute != null && table != null ) {
			StringTokenizer tokens = new StringTokenizer( uniqueKeyAttribute.getValue(), ", " );
			while ( tokens.hasMoreTokens() ) {
				table.getOrCreateUniqueKey( tokens.nextToken() ).addColumn( column );
			}
		}
	}

	// automatically makes a column with the default name if none is specifed by XML
	public static void bindSimpleValue(Element node, SimpleValue simpleValue, boolean isNullable,
			String path, Mappings mappings) throws MappingException {
		bindSimpleValueType( node, simpleValue, mappings );

		bindColumnsOrFormula( node, simpleValue, path, isNullable, mappings );

		Attribute fkNode = node.attribute( "foreign-key" );
		if ( fkNode != null ) simpleValue.setForeignKeyName( fkNode.getValue() );
	}

	private static void bindSimpleValueType(Element node, SimpleValue simpleValue, Mappings mappings)
			throws MappingException {
		String typeName = null;

		Properties parameters = new Properties();

		Attribute typeNode = node.attribute( "type" );
		if ( typeNode == null ) typeNode = node.attribute( "id-type" ); // for an any
		if ( typeNode != null ) typeName = typeNode.getValue();

		Element typeChild = node.element( "type" );
		if ( typeName == null && typeChild != null ) {
			typeName = typeChild.attribute( "name" ).getValue();
			Iterator typeParameters = typeChild.elementIterator( "param" );

			while ( typeParameters.hasNext() ) {
				Element paramElement = (Element) typeParameters.next();
				parameters.setProperty( paramElement.attributeValue( "name" ), paramElement
					.getTextTrim() );
			}
		}

		TypeDef typeDef = mappings.getTypeDef( typeName );
		if ( typeDef != null ) {
			typeName = typeDef.getTypeClass();
			// parameters on the property mapping should
			// override parameters in the typedef
			Properties allParameters = new Properties();
			allParameters.putAll( typeDef.getParameters() );
			allParameters.putAll( parameters );
			parameters = allParameters;
		}

		if ( !parameters.isEmpty() ) simpleValue.setTypeParameters( parameters );

		if ( typeName != null ) simpleValue.setTypeName( typeName );
	}

	public static void bindProperty(Element node, Property property, Mappings mappings,
			java.util.Map inheritedMetas) throws MappingException {
		
		String propName = node.attributeValue( "name" );
		property.setName( propName );
		String nodeName = node.attributeValue( "node" );
		if (nodeName==null) nodeName = propName;
		property.setNodeName( nodeName );
		
		// TODO:
		//Type type = model.getValue().getType(); 
		//if (type==null) throw new MappingException(
		//"Could not determine a property type for: " + model.getName() );
		
		Attribute accessNode = node.attribute( "access" );
		if ( accessNode != null ) {
			property.setPropertyAccessorName( accessNode.getValue() );
		}
		else if ( node.getName().equals( "properties" ) ) {
			property.setPropertyAccessorName( "embedded" );
		}
		else {
			property.setPropertyAccessorName( mappings.getDefaultAccess() );
		}

		Attribute cascadeNode = node.attribute( "cascade" );
		property.setCascade( cascadeNode == null ? mappings.getDefaultCascade() : cascadeNode
			.getValue() );

		Attribute updateNode = node.attribute( "update" );
		property.setUpdateable( updateNode == null || "true".equals( updateNode.getValue() ) );

		Attribute insertNode = node.attribute( "insert" );
		property.setInsertable( insertNode == null || "true".equals( insertNode.getValue() ) );

		Attribute lockNode = node.attribute( "optimistic-lock" );
		property.setOptimisticLocked( lockNode == null || "true".equals( lockNode.getValue() ) );

		boolean isLazyable = "property".equals( node.getName() )
			|| "component".equals( node.getName() )
			|| "many-to-one".equals( node.getName() )
			|| "one-to-one".equals( node.getName() )
			|| "any".equals( node.getName() );
		if ( isLazyable ) {
			Attribute lazyNode = node.attribute( "lazy" );
			property.setLazy( lazyNode != null && "true".equals( lazyNode.getValue() ) );
		}

		if ( log.isDebugEnabled() ) {
			String msg = "Mapped property: " + property.getName();
			String columns = columns( property.getValue() );
			if ( columns.length() > 0 ) msg += " -> " + columns;
			// TODO: this fails if we run with debug on!
			// if ( model.getType()!=null ) msg += ", type: " + model.getType().getName();
			log.debug( msg );
		}

		property.setMetaAttributes( getMetas( node, inheritedMetas ) );

	}

	private static String columns(Value val) {
		StringBuffer columns = new StringBuffer();
		Iterator iter = val.getColumnIterator();
		while ( iter.hasNext() ) {
			columns.append( ( (Selectable) iter.next() ).getText() );
			if ( iter.hasNext() ) columns.append( ", " );
		}
		return columns.toString();
	}

	/**
	 * Called for all collections
	 */
	public static void bindCollection(Element node, Collection collection, String className,
			String path, Mappings mappings) throws MappingException {

		// ROLENAME
		collection.setRole( StringHelper.qualify( className, path ) );

		Attribute inverseNode = node.attribute( "inverse" );
		if ( inverseNode != null ) {
			collection.setInverse( "true".equals( inverseNode.getValue() ) );
		}

		Attribute olNode = node.attribute( "optimistic-lock" );
		collection.setOptimisticLocked( olNode == null || "true".equals( olNode.getValue() ) );

		Attribute orderNode = node.attribute( "order-by" );
		if ( orderNode != null ) {
			if ( Environment.jvmSupportsLinkedHashCollections() || ( collection instanceof Bag ) ) {
				collection.setOrderBy( orderNode.getValue() );
			}
			else {
				log.warn( "Attribute \"order-by\" ignored in JDK1.3 or less" );
			}
		}
		Attribute whereNode = node.attribute( "where" );
		if ( whereNode != null ) {
			collection.setWhere( whereNode.getValue() );
		}
		Attribute batchNode = node.attribute( "batch-size" );
		if ( batchNode != null ) {
			collection.setBatchSize( Integer.parseInt( batchNode.getValue() ) );
		}

		String nodeName = node.attributeValue( "node" );
		if ( nodeName == null ) nodeName = node.attributeValue( "name" );
		collection.setNodeName( nodeName );
		String embed = node.attributeValue( "embed-xml" );
		collection.setEmbedded( embed==null || "true".equals(embed) );
		

		// PERSISTER
		Attribute persisterNode = node.attribute( "persister" );
		if ( persisterNode != null ) {
			try {
				collection.setCollectionPersisterClass( ReflectHelper.classForName( persisterNode
					.getValue() ) );
			}
			catch (ClassNotFoundException cnfe) {
				throw new MappingException( "Could not find collection persister class: "
					+ persisterNode.getValue() );
			}
		}

		Attribute typeNode = node.attribute( "collection-type" );
		if ( typeNode != null ) collection.setTypeName( typeNode.getValue() );

		initOuterJoinFetchSetting( node, collection );
		
		if ( "subselect".equals( node.attributeValue("fetch") ) ) {
			collection.setSubselectLoadable(true);
			collection.getOwner().setSubselectLoadableCollections(true);
		}

		Element oneToManyNode = node.element( "one-to-many" );
		if ( oneToManyNode != null ) {
			OneToMany oneToMany = new OneToMany( collection.getOwner() );
			collection.setElement( oneToMany );
			bindOneToMany( oneToManyNode, oneToMany, mappings );
			// we have to set up the table later!! yuck
		}
		else {
			// TABLE
			Attribute tableNode = node.attribute( "table" );
			String tableName;
			if ( tableNode != null ) {
				tableName = mappings.getNamingStrategy().tableName( tableNode.getValue() );
			}
			else {
				tableName = mappings.getNamingStrategy().propertyToTableName( className, path );
			}
			Attribute schemaNode = node.attribute( "schema" );
			String schema = schemaNode == null ? mappings.getSchemaName() : schemaNode.getValue();

			Attribute catalogNode = node.attribute( "catalog" );
			String catalog = catalogNode == null ? mappings.getCatalogName() : catalogNode
				.getValue();

			collection.setCollectionTable( mappings.addTable(
				schema,
				catalog,
				tableName,
				getSubselect( node ),
				false ) );

			log.info( "Mapping collection: "
				+ collection.getRole()
				+ " -> "
				+ collection.getCollectionTable().getName() );
		}

		// LAZINESS
		Attribute lazyNode = node.attribute( "lazy" );
		boolean isLazyTrue = lazyNode == null ? mappings.isDefaultLazy() : "true".equals( lazyNode
			.getValue() );
		collection.setLazy( isLazyTrue );

		// SORT
		Attribute sortedAtt = node.attribute( "sort" );
		// unsorted, natural, comparator.class.name
		if ( sortedAtt == null || sortedAtt.getValue().equals( "unsorted" ) ) {
			collection.setSorted( false );
		}
		else {
			collection.setSorted( true );
			String comparatorClassName = sortedAtt.getValue();
			if ( !comparatorClassName.equals( "natural" ) ) {
				try {
					collection.setComparator( (Comparator) ReflectHelper.classForName(
						comparatorClassName ).newInstance() );
				}
				catch (Exception e) {
					throw new MappingException( "Could not instantiate comparator class: "
						+ comparatorClassName );
				}
			}
		}

		// ORPHAN DELETE (used for programmer error detection)
		Attribute cascadeAtt = node.attribute( "cascade" );
		if ( cascadeAtt != null && cascadeAtt.getValue().indexOf( "delete-orphan" ) >= 0 ) {
			collection.setOrphanDelete( true );
		}

		// CUSTOM SQL
		handleCustomSQL( node, collection );
		// set up second pass
		if ( collection instanceof List ) {
			mappings.addSecondPass( new ListSecondPass( node, mappings, (List) collection ) );
		}
		else if ( collection instanceof Map ) {
			mappings.addSecondPass( new MapSecondPass( node, mappings, (Map) collection ) );
		}
		else if ( collection instanceof IdentifierCollection ) {
			mappings.addSecondPass( new IdentifierCollectionSecondPass(
				node,
				mappings,
				(IdentifierCollection) collection ) );
		}
		else {
			mappings.addSecondPass( new CollectionSecondPass( node, mappings, collection ) );
		}

		Iterator iter = node.elementIterator( "filter" );
		while ( iter.hasNext() ) {
			final Element filter = (Element) iter.next();
			parseFilter( filter, collection, mappings );
		}

		Iterator tables = node.elementIterator( "synchronize" );
		while ( tables.hasNext() ) {
			collection.getSynchronizedTables().add(
				( (Element) tables.next() ).attributeValue( "table" ) );
		}

		Element element = node.element( "loader" );
		if ( element != null ) {
			collection.setLoaderName( element.attributeValue( "query-ref" ) );
		}

		collection
			.setReferencedPropertyName( node.element( "key" ).attributeValue( "property-ref" ) );
	}

	private static void bindColumnsOrFormula(Element node, SimpleValue simpleValue, String path,
			boolean isNullable, Mappings mappings) {
		Attribute formulaNode = node.attribute( "formula" );
		if ( formulaNode != null ) {
			Formula f = new Formula();
			f.setFormula( formulaNode.getText() );
			simpleValue.addFormula( f );
		}
		else {
			bindColumns( node, simpleValue, isNullable, true, path, mappings );
		}
	}

	public static void bindManyToOne(Element node, ManyToOne manyToOne, String path,
			boolean isNullable, Mappings mappings) throws MappingException {

		bindColumnsOrFormula( node, manyToOne, path, isNullable, mappings );
		initOuterJoinFetchSetting( node, manyToOne );

		Attribute ukName = node.attribute( "property-ref" );
		if ( ukName != null ) manyToOne.setReferencedPropertyName( ukName.getValue() );

		manyToOne.setReferencedEntityName( getEntityName( node, mappings ) );

		final String embed = node.attributeValue( "embed-xml" );
		manyToOne.setEmbedded( embed == null || "true".equals( embed ) );

		Attribute fkNode = node.attribute( "foreign-key" );
		if ( fkNode != null ) manyToOne.setForeignKeyName( fkNode.getValue() );
	}

	public static void bindAny(Element node, Any any, boolean isNullable, Mappings mappings)
			throws MappingException {
		any.setIdentifierType( getTypeFromXML( node ) );
		Attribute metaAttribute = node.attribute( "meta-type" );
		if ( metaAttribute != null ) {
			any.setMetaType( metaAttribute.getValue() );

			Iterator iter = node.elementIterator( "meta-value" );
			if ( iter.hasNext() ) {
				HashMap values = new HashMap();
				org.hibernate.type.Type metaType = TypeFactory.heuristicType( any.getMetaType() );
				while ( iter.hasNext() ) {
					Element metaValue = (Element) iter.next();
					try {
						Object value = ( (DiscriminatorType) metaType ).stringToObject( metaValue
							.attributeValue( "value" ) );
						String entityName = getClassName( metaValue.attribute( "class" ), mappings );
						values.put( value, entityName );
					}
					catch (ClassCastException cce) {
						throw new MappingException( "meta-type was not a DiscriminatorType: "
							+ metaType.getName() );
					}
					catch (Exception e) {
						throw new MappingException( "could not interpret meta-value", e );
					}
				}
				any.setMetaValues( values );
			}

		}

		bindColumns( node, any, isNullable, false, null, mappings );
	}

	public static void bindOneToOne(Element node, OneToOne oneToOne, boolean isNullable,
			Mappings mappings) throws MappingException {
		bindColumns( node, oneToOne, isNullable, false, null, mappings );
		initOuterJoinFetchSetting( node, oneToOne );

		Attribute constrNode = node.attribute( "constrained" );
		boolean constrained = constrNode != null && constrNode.getValue().equals( "true" );
		oneToOne.setConstrained( constrained );

		oneToOne.setForeignKeyType( constrained
			? ForeignKeyDirection.FOREIGN_KEY_FROM_PARENT
			: ForeignKeyDirection.FOREIGN_KEY_TO_PARENT );

		oneToOne.setEmbedded( "true".equals( node.attributeValue( "embed-xml" ) ) );

		Attribute fkNode = node.attribute( "foreign-key" );
		if ( fkNode != null ) oneToOne.setForeignKeyName( fkNode.getValue() );

		Attribute ukName = node.attribute( "property-ref" );
		if ( ukName != null ) oneToOne.setReferencedPropertyName( ukName.getValue() );
		oneToOne.setPropertyName( node.attributeValue( "name" ) );

		oneToOne.setReferencedEntityName( getEntityName( node, mappings ) );

	}

	public static void bindOneToMany(Element node, OneToMany oneToMany, Mappings mappings)
			throws MappingException {
		oneToMany.setReferencedEntityName( getEntityName( node, mappings ) );
		final String embed = node.attributeValue( "embed-xml" );
		oneToMany.setEmbedded( embed == null || "true".equals( embed ) );
	}

	public static void bindColumn(Element node, Column column, boolean isNullable) {
		Attribute lengthNode = node.attribute( "length" );
		if ( lengthNode != null ) column.setLength( Integer.parseInt( lengthNode.getValue() ) );
		Attribute scalNode = node.attribute( "scale" );
		if ( scalNode != null ) column.setScale( Integer.parseInt( scalNode.getValue() ) );
		Attribute precNode = node.attribute( "precision" );
		if ( precNode != null ) column.setPrecision( Integer.parseInt( precNode.getValue() ) );

		Attribute nullNode = node.attribute( "not-null" );
		column.setNullable( nullNode == null ? isNullable : nullNode.getValue().equals( "false" ) );

		Attribute unqNode = node.attribute( "unique" );
		if ( unqNode != null ) column.setUnique( unqNode.getValue().equals( "true" ) );

		column.setCheckConstraint( node.attributeValue( "check" ) );

		Attribute typeNode = node.attribute( "sql-type" );
		if ( typeNode != null ) column.setSqlType( typeNode.getValue() );
	}

	/**
	 * Called for arrays and primitive arrays
	 */
	public static void bindArray(Element node, Array array, String prefix, String path,
			Mappings mappings) throws MappingException {

		bindCollection( node, array, prefix, path, mappings );

		Attribute att = node.attribute( "element-class" );
		if ( att != null ) array.setElementClassName( getClassName( att, mappings ) );

	}

	private static Class reflectedPropertyClass(String className, String propertyName)
			throws MappingException {
		if ( className == null ) return null;
		return ReflectHelper.reflectedPropertyClass( className, propertyName );
	}

	public static void bindComposite(Element node, Component component, String path,
			boolean isNullable, Mappings mappings, java.util.Map inheritedMetas)
			throws MappingException {
		bindComponent(
			node,
			component,
			null,
			null,
			path,
			isNullable,
			false,
			mappings,
			inheritedMetas );
	}

	public static void bindCompositeId(Element node, Component component,
			PersistentClass persistentClass, String propertyName, Mappings mappings,
			java.util.Map inheritedMetas) throws MappingException {

		component.setKey( true );

		String path = StringHelper.qualify( persistentClass.getEntityName(), propertyName == null
			? "id"
			: propertyName );

		bindComponent(
			node,
			component,
			persistentClass.getClassName(),
			propertyName,
			path,
			false,
			node.attribute( "class" ) == null && propertyName == null,
			mappings,
			inheritedMetas );

	}

	public static void bindComponent(Element node, Component component, String ownerClassName,
			String parentProperty, String path, boolean isNullable, boolean isEmbedded,
			Mappings mappings, java.util.Map inheritedMetas) throws MappingException {

		component.setEmbedded( isEmbedded );

		component.setMetaAttributes( getMetas( node, inheritedMetas ) );

		Attribute classNode = node.attribute( "class" );
		if ( classNode != null ) {
			component.setComponentClassName( getClassName( classNode, mappings ) );
		}
		else if ( "dynamic-component".equals( node.getName() ) ) {
			component.setDynamic( true );
		}
		else if ( isEmbedded ) {
			// an "embedded" component (composite ids and unique)
			// note that this does not handle nested components
			// todo : how *should* this work for non-pojo entities?{
			component.setComponentClassName( component.getOwner().getClassName() );
		}
		else {
			// todo : again, how *should* this work for non-pojo entities?
			Class reflectedClass = reflectedPropertyClass( ownerClassName, parentProperty );
			if ( reflectedClass != null )
				component.setComponentClassName( reflectedClass.getName() );
		}

		String nodeName = node.attributeValue( "node" );
		if ( nodeName == null ) nodeName = node.attributeValue( "name" );
		component.setNodeName( nodeName );

		Iterator iter = node.elementIterator();
		while ( iter.hasNext() ) {

			Element subnode = (Element) iter.next();
			String name = subnode.getName();
			String propertyName = getPropertyName( subnode );
			String subpath = propertyName == null ? null : StringHelper
				.qualify( path, propertyName );

			CollectionType collectType = CollectionType.collectionTypeFromString( name );
			Value value = null;
			if ( collectType != null ) {
				Collection collection = collectType.create(
					subnode,
					subpath,
					component.getOwner(),
					mappings );
				mappings.addCollection( collection );
				value = collection;
			}
			else if ( "many-to-one".equals( name ) || "key-many-to-one".equals( name ) ) {
				value = new ManyToOne( component.getTable() );
				bindManyToOne( subnode, (ManyToOne) value, propertyName, isNullable, mappings );
			}
			else if ( "one-to-one".equals( name ) ) {
				value = new OneToOne( component.getTable(), component.getOwner().getKey() );
				bindOneToOne( subnode, (OneToOne) value, isNullable, mappings );
			}
			else if ( "any".equals( name ) ) {
				value = new Any( component.getTable() );
				bindAny( subnode, (Any) value, isNullable, mappings );
			}
			else if ( "property".equals( name ) || "key-property".equals( name ) ) {
				value = new SimpleValue( component.getTable() );
				bindSimpleValue( subnode, (SimpleValue) value, isNullable, propertyName, mappings );
			}
			else if ( "component".equals( name )
				|| "dynamic-component".equals( name )
				|| "nested-composite-element".equals( name ) ) {
				value = new Component( component ); // a nested composite element
				bindComponent(
					subnode,
					(Component) value,
					component.getComponentClassName(),
					propertyName,
					subpath,
					isNullable,
					isEmbedded,
					mappings,
					inheritedMetas );
			}
			else if ( "parent".equals( name ) ) {
				component.setParentProperty( propertyName );
			}

			if ( value != null ) {
				component.addProperty( createProperty( value, propertyName, component
					.getComponentClassName(), subnode, mappings, inheritedMetas ) );
			}
		}

		if ( "true".equals( node.attributeValue( "unique" ) ) ) {
			iter = component.getColumnIterator();
			ArrayList cols = new ArrayList();
			while ( iter.hasNext() )
				cols.add( iter.next() );
			component.getOwner().getTable().createUniqueKey( cols );
		}

	}

	private static String getTypeFromXML(Element node) throws MappingException {
		// TODO: handle TypeDefs
		Attribute typeNode = node.attribute( "type" );
		if ( typeNode == null ) typeNode = node.attribute( "id-type" ); // for an any
		if ( typeNode == null ) return null; // we will have to use reflection
		return typeNode.getValue();
	}

	private static void initOuterJoinFetchSetting(Element node, Fetchable model) {
		Attribute fetchNode = node.attribute( "fetch" );
		final FetchMode fetchStyle;
		if ( fetchNode == null ) {
			Attribute jfNode = node.attribute( "outer-join" );
			if ( jfNode == null ) {
				if ( "many-to-many".equals( node.getName() ) ) {
					// default to join for the "second join" of the many-to-many
					fetchStyle = FetchMode.JOIN;
				}
				else if ( "one-to-one".equals( node.getName() ) ) {
					// one-to-one constrained=false cannot be proxied,
					// so default to join
					fetchStyle = ( (OneToOne) model ).isConstrained()
						? FetchMode.DEFAULT
						: FetchMode.JOIN;
				}
				else {
					fetchStyle = FetchMode.DEFAULT;
				}
			}
			else {
				// use old (HB 2.1) defaults if outer-join is specified
				String eoj = jfNode.getValue();
				if ( "auto".equals( eoj ) ) {
					fetchStyle = FetchMode.DEFAULT;
				}
				else {
					fetchStyle = "true".equals( eoj ) ? FetchMode.JOIN : FetchMode.SELECT;
				}
			}
		}
		else {
			fetchStyle = "join".equals( fetchNode.getValue() ) ? FetchMode.JOIN : FetchMode.SELECT;
		}
		model.setFetchMode( fetchStyle );
	}

	private static void makeIdentifier(Element node, SimpleValue model, Mappings mappings) {

		// GENERATOR
		Element subnode = node.element( "generator" );
		if ( subnode != null ) {
			model.setIdentifierGeneratorStrategy( subnode.attributeValue( "class" ) );

			Properties params = new Properties();

			if ( mappings.getSchemaName() != null ) {
				params.setProperty( PersistentIdentifierGenerator.SCHEMA, mappings.getSchemaName() );
			}
			if ( mappings.getCatalogName() != null ) {
				params.setProperty( PersistentIdentifierGenerator.CATALOG, mappings.getCatalogName() );
			}

			Iterator iter = subnode.elementIterator( "param" );
			while ( iter.hasNext() ) {
				Element childNode = (Element) iter.next();
				params.setProperty( childNode.attributeValue( "name" ), childNode.getText() );
			}

			model.setIdentifierGeneratorProperties( params );
		}

		model.getTable().setIdentifierValue( model );

		// ID UNSAVED-VALUE
		Attribute nullValueNode = node.attribute( "unsaved-value" );
		if ( nullValueNode != null ) {
			model.setNullValue( nullValueNode.getValue() );
		}
		else {
			if ( "assigned".equals( model.getIdentifierGeneratorStrategy() ) ) {
				model.setNullValue( "undefined" );
			}
			else {
				model.setNullValue( null );
			}
		}
	}

	private static final void makeVersion(Element node, SimpleValue model) {

		// VERSION UNSAVED-VALUE
		Attribute nullValueNode = node.attribute( "unsaved-value" );
		if ( nullValueNode != null ) {
			model.setNullValue( nullValueNode.getValue() );
		}
		else {
			model.setNullValue( "undefined" );
		}

	}

	protected static void createClassProperties(Element node, PersistentClass persistentClass,
			Mappings mappings, java.util.Map inheritedMetas) throws MappingException {

		String entityName = persistentClass.getEntityName();
		Table table = persistentClass.getTable();

		Iterator iter = node.elementIterator();
		while ( iter.hasNext() ) {
			Element subnode = (Element) iter.next();
			String name = subnode.getName();
			String propertyName = subnode.attributeValue( "name" );

			CollectionType collectType = CollectionType.collectionTypeFromString( name );
			Value value = null;
			if ( collectType != null ) {
				Collection collection = collectType.create(
					subnode,
					propertyName,
					persistentClass,
					mappings );
				mappings.addCollection( collection );
				value = collection;
			}
			else if ( "many-to-one".equals( name ) ) {
				value = new ManyToOne( table );
				bindManyToOne( subnode, (ManyToOne) value, propertyName, true, mappings );
			}
			else if ( "any".equals( name ) ) {
				value = new Any( table );
				bindAny( subnode, (Any) value, true, mappings );
			}
			else if ( "one-to-one".equals( name ) ) {
				OneToOne oneToOne = new OneToOne( table, persistentClass.getKey() );
				bindOneToOne( subnode, oneToOne, true, mappings );
				value = oneToOne;
			}
			else if ( "property".equals( name ) ) {
				value = new SimpleValue( table );
				bindSimpleValue( subnode, (SimpleValue) value, true, propertyName, mappings );
			}
			else if ( "component".equals( name )
				|| "dynamic-component".equals( name )
				|| "properties".equals( name ) ) {
				String subpath = StringHelper.qualify( entityName, propertyName );
				value = new Component( persistentClass );

				bindComponent(
					subnode,
					(Component) value,
					persistentClass.getClassName(),
					propertyName,
					subpath,
					true,
					"properties".equals( name ),
					mappings,
					inheritedMetas );
			}
			else if ( "query-list".equals( name ) ) {
				value = new QueryList( table );
				( (QueryList) value ).setQueryName( subnode.attributeValue( "query-ref" ) );
			}
			else if ( "join".equals( name ) ) {
				Join join = new Join();
				join.setPersistentClass( persistentClass );
				bindJoin( subnode, join, mappings, inheritedMetas );
				persistentClass.addJoin( join );
			}
			else if ( "subclass".equals( name ) ) {
				handleSubclass( persistentClass, mappings, subnode, inheritedMetas );
			}
			else if ( "joined-subclass".equals( name ) ) {
				handleJoinedSubclass( persistentClass, mappings, subnode, inheritedMetas );
			}
			else if ( "union-subclass".equals( name ) ) {
				handleUnionSubclass( persistentClass, mappings, subnode, inheritedMetas );
			}
			else if ( "filter".equals( name ) ) {
				parseFilter( subnode, persistentClass, mappings );
			}

			if ( value != null ) {
				persistentClass.addProperty( createProperty( value, propertyName, persistentClass
					.getClassName(), subnode, mappings, inheritedMetas ) );
			}

		}
	}

	private static Property createProperty(final Value value, final String propertyName,
			final String className, final Element subnode, final Mappings mappings,
			java.util.Map inheritedMetas) throws MappingException {

		value.setTypeUsingReflection( className, propertyName );

		// this is done here 'cos we might only know the type here (ugly!)
		// TODO: improve this a lot:
		if ( value instanceof ToOne ) {
			ToOne toOne = (ToOne) value;
			String propertyRef = toOne.getReferencedPropertyName();
			if ( propertyRef != null ) {
				mappings.addUniquePropertyReference( toOne.getReferencedEntityName(), propertyRef );
			}
		}
		else if ( value instanceof Collection ) {
			Collection coll = (Collection) value;
			String propertyRef = coll.getReferencedPropertyName();
			// not necessarily a *unique* property reference
			if ( propertyRef != null ) {
				mappings.addPropertyReference( coll.getOwnerEntityName(), propertyRef );
			}
		}

		value.createForeignKey();
		Property prop = new Property();
		prop.setValue( value );
		bindProperty( subnode, prop, mappings, inheritedMetas );
		return prop;
	}

	private static void handleUnionSubclass(PersistentClass model, Mappings mappings,
			Element subnode, java.util.Map inheritedMetas) throws MappingException {
		UnionSubclass subclass = new UnionSubclass( model );
		bindUnionSubclass( subnode, subclass, mappings, inheritedMetas );
		model.addSubclass( subclass );
		mappings.addClass( subclass );
	}

	private static void handleJoinedSubclass(PersistentClass model, Mappings mappings,
			Element subnode, java.util.Map inheritedMetas) throws MappingException {
		JoinedSubclass subclass = new JoinedSubclass( model );
		bindJoinedSubclass( subnode, subclass, mappings, inheritedMetas );
		model.addSubclass( subclass );
		mappings.addClass( subclass );
	}

	private static void handleSubclass(PersistentClass model, Mappings mappings, Element subnode,
			java.util.Map inheritedMetas) throws MappingException {
		Subclass subclass = new SingleTableSubclass( model );
		bindSubclass( subnode, subclass, mappings, inheritedMetas );
		model.addSubclass( subclass );
		mappings.addClass( subclass );
	}

	/**
	 * Called for Lists, arrays, primitive arrays
	 */
	public static void bindListSecondPass(Element node, List list, java.util.Map classes,
			Mappings mappings, java.util.Map inheritedMetas) throws MappingException {

		bindCollectionSecondPass( node, list, classes, mappings, inheritedMetas );

		Element subnode = node.element( "list-index" );
		if ( subnode == null ) subnode = node.element( "index" );
		SimpleValue iv = new SimpleValue( list.getCollectionTable() );
		bindSimpleValue(
			subnode,
			iv,
			list.isOneToMany(),
			IndexedCollection.DEFAULT_INDEX_COLUMN_NAME,
			mappings );
		iv.setTypeName( "integer" );
		list.setIndex( iv );
		String baseIndex = subnode.attributeValue( "base" );
		if ( baseIndex != null ) list.setBaseIndex( Integer.parseInt( baseIndex ) );
		list.setIndexNodeName( subnode.attributeValue("node") );

		if ( list.isOneToMany() && !list.getKey().isNullable() && !list.isInverse() ) {
			String entityName = ( (OneToMany) list.getElement() ).getReferencedEntityName();
			PersistentClass referenced = mappings.getClass( entityName );
			IndexBackref ib = new IndexBackref();
			ib.setName( '_' + node.attributeValue( "name" ) + "IndexBackref" );
			ib.setUpdateable( false );
			ib.setSelectable( false );
			ib.setCollectionRole( list.getRole() );
			ib.setValue( list.getIndex() );
			// ( (Column) ( (SimpleValue) ic.getIndex() ).getColumnIterator().next()
			// ).setNullable(false);
			referenced.addProperty( ib );
		}
	}

	public static void bindIdentifierCollectionSecondPass(Element node,
			IdentifierCollection collection, java.util.Map persistentClasses, Mappings mappings,
			java.util.Map inheritedMetas) throws MappingException {

		bindCollectionSecondPass( node, collection, persistentClasses, mappings, inheritedMetas );

		Element subnode = node.element( "collection-id" );
		SimpleValue id = new SimpleValue( collection.getCollectionTable() );
		bindSimpleValue(
			subnode,
			id,
			false,
			IdentifierCollection.DEFAULT_IDENTIFIER_COLUMN_NAME,
			mappings );
		collection.setIdentifier( id );
		makeIdentifier( subnode, id, mappings );

	}

	/**
	 * Called for Maps
	 */
	public static void bindMapSecondPass(Element node, Map map, java.util.Map classes,
			Mappings mappings, java.util.Map inheritedMetas) throws MappingException {

		bindCollectionSecondPass( node, map, classes, mappings, inheritedMetas );

		Iterator iter = node.elementIterator();
		while ( iter.hasNext() ) {
			Element subnode = (Element) iter.next();
			String name = subnode.getName();

			if ( "index".equals( name ) || "map-key".equals( name ) ) {
				SimpleValue value = new SimpleValue( map.getCollectionTable() );
				bindSimpleValue(
					subnode,
					value,
					map.isOneToMany(),
					IndexedCollection.DEFAULT_INDEX_COLUMN_NAME,
					mappings );
				if ( !value.isTypeSpecified() ) {
					throw new MappingException( "map index element must specify a type: "
						+ map.getRole() );
				}
				map.setIndex( value );
				map.setIndexNodeName( subnode.attributeValue("node") );
			}
			else if ( "index-many-to-many".equals( name ) || "map-key-many-to-many".equals( name ) ) {
				ManyToOne mto = new ManyToOne( map.getCollectionTable() );
				bindManyToOne( subnode, mto, IndexedCollection.DEFAULT_INDEX_COLUMN_NAME, map
					.isOneToMany(), mappings );
				map.setIndex( mto );

			}
			else if ( "composite-index".equals( name ) || "composite-map-key".equals( name ) ) {
				Component component = new Component( map );
				bindComposite(
					subnode,
					component,
					map.getRole() + ".index",
					map.isOneToMany(),
					mappings,
					inheritedMetas );
				map.setIndex( component );
			}
			else if ( "index-many-to-any".equals( name ) ) {
				Any any = new Any( map.getCollectionTable() );
				bindAny( subnode, any, map.isOneToMany(), mappings );
				map.setIndex( any );
			}
		}

		// TODO: this is a bit of copy/paste from IndexedCollection.createPrimaryKey()
		boolean indexIsFormula = false;
		Iterator colIter = map.getIndex().getColumnIterator();
		while ( colIter.hasNext() ) {
			if ( ( (Selectable) colIter.next() ).isFormula() ) indexIsFormula = true;
		}

		if ( map.isOneToMany() && !map.getKey().isNullable() && !map.isInverse() && !indexIsFormula ) {
			String entityName = ( (OneToMany) map.getElement() ).getReferencedEntityName();
			PersistentClass referenced = mappings.getClass( entityName );
			IndexBackref ib = new IndexBackref();
			ib.setName( '_' + node.attributeValue( "name" ) + "IndexBackref" );
			ib.setUpdateable( false );
			ib.setSelectable( false );
			ib.setCollectionRole( map.getRole() );
			ib.setValue( map.getIndex() );
			// ( (Column) ( (SimpleValue) ic.getIndex() ).getColumnIterator().next()
			// ).setNullable(false);
			referenced.addProperty( ib );
		}
	}

	/**
	 * Called for all collections
	 */
	public static void bindCollectionSecondPass(Element node, Collection collection,
			java.util.Map persistentClasses, Mappings mappings, java.util.Map inheritedMetas)
			throws MappingException {

		if ( collection.isOneToMany() ) {
			OneToMany oneToMany = (OneToMany) collection.getElement();
			String assocClass = oneToMany.getReferencedEntityName();
			PersistentClass persistentClass = (PersistentClass) persistentClasses.get( assocClass );
			if ( persistentClass == null ) {
				throw new MappingException( "Association references unmapped class: " + assocClass );
			}
			oneToMany.setAssociatedClass( persistentClass );
			collection.setCollectionTable( persistentClass.getTable() );

			log.info( "Mapping collection: "
				+ collection.getRole()
				+ " -> "
				+ collection.getCollectionTable().getName() );
		}

		// CHECK
		Attribute chNode = node.attribute( "check" );
		if ( chNode != null ) {
			collection.getCollectionTable().addCheckConstraint( chNode.getValue() );
		}

		// contained elements:
		Iterator iter = node.elementIterator();
		while ( iter.hasNext() ) {
			Element subnode = (Element) iter.next();
			String name = subnode.getName();

			if ( "key".equals( name ) ) {
				KeyValue keyVal;
				String propRef = collection.getReferencedPropertyName();
				if ( propRef == null ) {
					keyVal = collection.getOwner().getIdentifier();
				}
				else {
					keyVal = (KeyValue) collection.getOwner().getProperty( propRef ).getValue();
				}
				SimpleValue key = new DependantValue( collection.getCollectionTable(), keyVal );
				key.setCascadeDeleteEnabled( "cascade"
					.equals( subnode.attributeValue( "on-delete" ) ) );
				bindSimpleValue(
					subnode,
					key,
					collection.isOneToMany(),
					Collection.DEFAULT_KEY_COLUMN_NAME,
					mappings );
				collection.setKey( key );

				Attribute notNull = subnode.attribute( "not-null" );
				( (DependantValue) key ).setNullable( notNull == null
					|| notNull.getValue().equals( "false" ) );
				Attribute updateable = subnode.attribute( "update" );
				( (DependantValue) key ).setUpdateable( updateable == null
					|| updateable.getValue().equals( "true" ) );

			}
			else if ( "element".equals( name ) ) {
				SimpleValue elt = new SimpleValue( collection.getCollectionTable() );
				collection.setElement( elt );
				bindSimpleValue(
					subnode,
					elt,
					true,
					Collection.DEFAULT_ELEMENT_COLUMN_NAME,
					mappings );
			}
			else if ( "many-to-many".equals( name ) ) {
				ManyToOne element = new ManyToOne( collection.getCollectionTable() );
				collection.setElement( element );
				bindManyToOne(
					subnode,
					element,
					Collection.DEFAULT_ELEMENT_COLUMN_NAME,
					false,
					mappings );
			}
			else if ( "composite-element".equals( name ) ) {
				Component element = new Component( collection );
				collection.setElement( element );
				bindComposite(
					subnode,
					element,
					collection.getRole() + ".element",
					true,
					mappings,
					inheritedMetas );
			}
			else if ( "many-to-any".equals( name ) ) {
				Any element = new Any( collection.getCollectionTable() );
				collection.setElement( element );
				bindAny( subnode, element, true, mappings );
			}
			else if ( "cache".equals( name ) ) {
				collection.setCacheConcurrencyStrategy( subnode.attributeValue( "usage" ) );
				collection.setCacheRegionName( subnode.attributeValue( "region" ) );
			}

			String nodeName = subnode.attributeValue( "node" );
			if ( nodeName != null ) collection.setElementNodeName( nodeName );

		}

		if ( collection.isOneToMany()
			&& !collection.isInverse()
			&& !collection.getKey().isNullable() ) {
			// for non-inverse one-to-many, with a not-null fk, add a backref!
			String entityName = ( (OneToMany) collection.getElement() ).getReferencedEntityName();
			PersistentClass referenced = mappings.getClass( entityName );
			Backref prop = new Backref();
			prop.setName( '_' + node.attributeValue( "name" ) + "Backref" );
			prop.setUpdateable( false );
			prop.setSelectable( false );
			prop.setCollectionRole( collection.getRole() );
			prop.setValue( collection.getKey() );
			referenced.addProperty( prop );
		}
	}

	private static final LockMode getLockMode(String lockMode) {
		if ( lockMode == null || "read".equals( lockMode ) ) {
			return LockMode.READ;
		}
		else if ( "none".equals( lockMode ) ) {
			return LockMode.NONE;
		}
		else if ( "upgrade".equals( lockMode ) ) {
			return LockMode.UPGRADE;
		}
		else if ( "upgrade-nowait".equals( lockMode ) ) {
			return LockMode.UPGRADE_NOWAIT;
		}
		else if ( "upgrade-nowait".equals( lockMode ) ) {
			return LockMode.UPGRADE_NOWAIT;
		}
		else if ( "write".equals( lockMode ) ) {
			return LockMode.WRITE;
		}
		else {
			throw new MappingException( "unknown lockmode" );
		}
	}

	private static final FlushMode getFlushMode(String flushMode) {
		if ( flushMode == null ) {
			return null;
		}
		else if ( "auto".equals( flushMode ) ) {
			return FlushMode.AUTO;
		}
		else if ( "commit".equals( flushMode ) ) {
			return FlushMode.COMMIT;
		}
		else if ( "never".equals( flushMode ) ) {
			return FlushMode.NEVER;
		}
		else if ( "always".equals( flushMode ) ) {
			return FlushMode.ALWAYS;
		}
		else {
			throw new MappingException( "unknown flushmode" );
		}
	}

	private static void bindNamedQuery(Element queryElem, Mappings mappings) {
		String qname = queryElem.attributeValue( "name" );
		String query = queryElem.getText();
		log.debug( "Named query: " + qname + " -> " + query );

		boolean cacheable = "true".equals( queryElem.attributeValue( "cacheable" ) );
		String region = queryElem.attributeValue( "cache-region" );
		Attribute tAtt = queryElem.attribute( "timeout" );
		Integer timeout = tAtt == null ? null : new Integer( tAtt.getValue() );
		Attribute fsAtt = queryElem.attribute( "fetch-size" );
		Integer fetchSize = fsAtt == null ? null : new Integer( fsAtt.getValue() );

		NamedQueryDefinition namedQuery = new NamedQueryDefinition(
			query,
			cacheable,
			region,
			timeout,
			fetchSize,
			getFlushMode( queryElem.attributeValue( "flush-mode" ) ) );

		mappings.addQuery( qname, namedQuery );
	}

	private static void bindReturn(Element returnElem, java.util.List queryReturns,
			Mappings mappings) {
		String alias = returnElem.attributeValue( "alias" );
		String entityName = getEntityName(returnElem, mappings);
		if(entityName==null) {
			throw new MappingException( "<return alias='" + alias + "'> must specify either a class or entity-name");
		}
		LockMode lockMode = getLockMode( returnElem.attributeValue( "lock-mode" ) );
				
		java.util.Map propertyResults = bindPropertyResults(alias, returnElem);
		
		queryReturns.add( new SQLQueryRootReturn(
			alias,
			entityName,
			propertyResults,
			lockMode ) );
	}

	/**
	 * @param alias 
	 * @param returnElement
	 * @return
	 */
	private static java.util.Map bindPropertyResults(String alias, Element returnElement) {
		
		HashMap propertyresults = new HashMap(); // maybe a concrete SQLpropertyresult type, but Map is exactly what is required at the moment
		
		Element discriminatorResult = returnElement.element("return-discriminator");
		if(discriminatorResult!=null) {
			ArrayList resultColumns = getResultColumns(discriminatorResult);			
			propertyresults.put("class", ArrayHelper.toStringArray(resultColumns));
		}
		
		Iterator iterator = returnElement.elementIterator("return-property");
		while ( iterator.hasNext() ) {
			Element propertyresult = (Element) iterator.next();
			String name = propertyresult.attributeValue("name");
			if ( "class".equals(name) ) {
				throw new MappingException("class is not a valid property name to use in a <return-property>. Use <return-discriminator> instead.");
			}
			//TODO: validate existing of property with the chosen name. (secondpass )
			ArrayList allResultColumns = getResultColumns(propertyresult);
			
			if ( allResultColumns.isEmpty() ) {
				throw new MappingException("return-property for alias " + alias + " must specify at least one column or return-column name");
			}
			String[] existing = (String[]) propertyresults.put( name, ArrayHelper.toStringArray(allResultColumns) );
			if (existing!=null) {
				throw new MappingException("duplicate return-property for property " + name + " on alias " + alias);
			}
		}
		return propertyresults.isEmpty() ? CollectionHelper.EMPTY_MAP : propertyresults;
	}

	/**
	 * @param return-property
	 * @return
	 */
	private static ArrayList getResultColumns(Element propertyresult) {
		String column = propertyresult.attributeValue("column");
		ArrayList allResultColumns = new ArrayList();
		if(column!=null) allResultColumns.add(column);
		Iterator resultColumns = propertyresult.elementIterator("return-column");
		while (resultColumns.hasNext()) {
			Element element = (Element) resultColumns.next();
			allResultColumns.add(element.attributeValue("name"));
		}
		return allResultColumns;
	}

	private static void bindReturnJoin(Element returnElem, java.util.List queryReturns,
			Mappings mappings) {
		String alias = returnElem.attributeValue( "alias" );
		String roleAttribute = returnElem.attributeValue( "property" );
		LockMode lockMode = getLockMode( returnElem.attributeValue( "lock-mode" ) );
		int dot = roleAttribute.lastIndexOf( '.' );
		if ( dot == -1 ) {
			throw new MappingException( "Role attribute for sql query return [alias="
				+ alias
				+ "] not formatted correctly {owningAlias.propertyName}" );
		}
		String roleOwnerAlias = roleAttribute.substring( 0, dot );
		String roleProperty = roleAttribute.substring( dot + 1 );
		queryReturns.add( new SQLQueryJoinReturn( 
					alias, 
					roleOwnerAlias, 
					roleProperty, 
					CollectionHelper.EMPTY_MAP, // TODO: bindpropertyresults(alias, returnElem) 
					lockMode ) );
	}

	private static void bindLoadCollection(Element returnElem, java.util.List queryReturns,
			Mappings mappings) {
		String alias = returnElem.attributeValue( "alias" );
		String collectionAttribute = returnElem.attributeValue( "role" );
		LockMode lockMode = getLockMode( returnElem.attributeValue( "lock-mode" ) );
		int dot = collectionAttribute.lastIndexOf( '.' );
		if ( dot == -1 ) {
			throw new MappingException( "Collection attribute for sql query return [alias="
				+ alias
				+ "] not formatted correctly {OwnerClassName.propertyName}" );
		}
		String ownerClassName = getClassName( collectionAttribute.substring( 0, dot ), mappings );
		String ownerPropertyName = collectionAttribute.substring( dot + 1 );

		queryReturns.add( new SQLQueryCollectionReturn(
			alias,
			ownerClassName,
			ownerPropertyName,
			CollectionHelper.EMPTY_MAP, // TODO: bindpropertyresults(alias, returnElem)
			lockMode ) );
	}

	private static void bindNamedSQLQuery(Element queryElem, Mappings mappings) {
		String queryName = queryElem.attribute( "name" ).getValue();

		ArrayList scalarQueryReturns = new ArrayList();
		Iterator returns = queryElem.elementIterator( "return-scalar" );
		while ( returns.hasNext() ) {
			Element returnElem = (Element) returns.next();
			String column = returnElem.attributeValue( "column" );
			Type type = TypeFactory.heuristicType( getTypeFromXML( returnElem ) );
			if ( type == null ) {
				throw new MappingException( "could not determine type " + type );
			}
			scalarQueryReturns.add( new SQLQueryScalarReturn( column, type ) );
		}

		ArrayList queryReturns = new ArrayList();
		returns = queryElem.elementIterator();
		while ( returns.hasNext() ) {
			Element returnElem = (Element) returns.next();
			String name = returnElem.getName();
			if ( "return".equals( name ) ) {
				bindReturn( returnElem, queryReturns, mappings );
			}
			else if ( "return-join".equals( name ) ) {
				bindReturnJoin( returnElem, queryReturns, mappings );
			}
			else if ( "load-collection".equals( name ) ) {
				bindLoadCollection( returnElem, queryReturns, mappings );
			}
		}

		boolean cacheable = "true".equals( queryElem.attributeValue( "cacheable" ) );
		String region = queryElem.attributeValue( "cache-region" );
		Attribute tAtt = queryElem.attribute( "timeout" );
		Integer timeout = tAtt == null ? null : new Integer( tAtt.getValue() );
		Attribute fsAtt = queryElem.attribute( "fetch-size" );
		Integer fetchSize = fsAtt == null ? null : new Integer( fsAtt.getValue() );

		java.util.List synchronizedTables = new ArrayList();
		Iterator tables = queryElem.elementIterator( "synchronize" );
		while ( tables.hasNext() ) {
			synchronizedTables.add( ( (Element) tables.next() ).attributeValue( "table" ) );
		}

		boolean callable = "true".equals( queryElem.attributeValue( "callable" ) );
		NamedSQLQueryDefinition namedQuery = new NamedSQLQueryDefinition(
			queryElem.getTextTrim() /* trim done to workaround stupid oracle bug that cant handle whitespaces before a { in a sp */,
			(SQLQueryReturn[]) queryReturns.toArray( new SQLQueryReturn[0] ),
			(SQLQueryScalarReturn[]) scalarQueryReturns.toArray( new SQLQueryScalarReturn[0] ),
			synchronizedTables,
			cacheable,
			region,
			timeout,
			fetchSize,
			getFlushMode( queryElem.attributeValue( "flush-mode" ) ), 
			callable);

		log.debug( "Named SQL query: " + queryName + " -> " + namedQuery.getQueryString() );
		mappings.addSQLQuery( queryName, namedQuery );

	}

	private static String getPropertyName(Element node) {
		return node.attributeValue( "name" );
	}

	private static PersistentClass getSuperclass(Mappings mappings, Element subnode)
			throws MappingException {
		String superClass = getClassName( subnode.attributeValue( "extends" ), mappings );
		PersistentClass superModel = mappings.getClass( superClass );

		if ( superModel == null ) {
			throw new MappingException( "Cannot extend unmapped class " + superClass );
		}
		return superModel;
	}

	abstract static class SecondPass implements Serializable {
		Element node;
		Mappings mappings;
		Collection collection;

		SecondPass(Element node, Mappings mappings, Collection collection) {
			this.node = node;
			this.collection = collection;
			this.mappings = mappings;
		}

		void doSecondPass(java.util.Map persistentClasses, java.util.Map inheritedMetas)
				throws MappingException {
			if ( log.isDebugEnabled() )
				log.debug( "Second pass for collection: " + collection.getRole() );

			secondPass( persistentClasses, inheritedMetas );
			collection.createAllKeys();

			if ( log.isDebugEnabled() ) {
				String msg = "Mapped collection key: " + columns( collection.getKey() );
				if ( collection.isIndexed() )
					msg += ", index: " + columns( ( (IndexedCollection) collection ).getIndex() );
				if ( collection.isOneToMany() ) {
					msg += ", one-to-many: "
						+ ( (OneToMany) collection.getElement() ).getReferencedEntityName();
				}
				else {
					msg += ", element: " + columns( collection.getElement() );
				}
				log.debug( msg );
			}
		}

		abstract void secondPass(java.util.Map persistentClasses, java.util.Map inheritedMetas)
				throws MappingException;
	}

	static class CollectionSecondPass extends SecondPass {
		CollectionSecondPass(Element node, Mappings mappings, Collection collection) {
			super( node, mappings, collection );
		}

		void secondPass(java.util.Map persistentClasses, java.util.Map inheritedMetas)
				throws MappingException {
			HbmBinder.bindCollectionSecondPass(
				node,
				collection,
				persistentClasses,
				mappings,
				inheritedMetas );
		}

	}

	static class IdentifierCollectionSecondPass extends SecondPass {
		IdentifierCollectionSecondPass(Element node, Mappings mappings,
				IdentifierCollection collection) {
			super( node, mappings, collection );
		}

		void secondPass(java.util.Map persistentClasses, java.util.Map inheritedMetas)
				throws MappingException {
			HbmBinder.bindIdentifierCollectionSecondPass(
				node,
				(IdentifierCollection) collection,
				persistentClasses,
				mappings,
				inheritedMetas );
		}

	}

	static class MapSecondPass extends SecondPass {
		MapSecondPass(Element node, Mappings mappings, Map collection) {
			super( node, mappings, collection );
		}

		void secondPass(java.util.Map persistentClasses, java.util.Map inheritedMetas)
				throws MappingException {
			HbmBinder.bindMapSecondPass(
				node,
				(Map) collection,
				persistentClasses,
				mappings,
				inheritedMetas );
		}

	}

	static class ListSecondPass extends SecondPass {
		ListSecondPass(Element node, Mappings mappings, List collection) {
			super( node, mappings, collection );
		}

		void secondPass(java.util.Map persistentClasses, java.util.Map inheritedMetas)
				throws MappingException {
			HbmBinder.bindListSecondPass(
				node,
				(List) collection,
				persistentClasses,
				mappings,
				inheritedMetas );
		}

	}

	// This inner class implements a case statement....perhaps im being a bit over-clever here
	abstract static class CollectionType {
		private String xmlTag;

		public abstract Collection create(Element node, String path, PersistentClass owner,
				Mappings mappings) throws MappingException;

		CollectionType(String xmlTag) {
			this.xmlTag = xmlTag;
		}

		public String toString() {
			return xmlTag;
		}

		private static final CollectionType MAP = new CollectionType( "map" ) {
			public Collection create(Element node, String path, PersistentClass owner,
					Mappings mappings) throws MappingException {
				Map map = new Map( owner );
				bindCollection( node, map, owner.getEntityName(), path, mappings );
				return map;
			}
		};
		private static final CollectionType SET = new CollectionType( "set" ) {
			public Collection create(Element node, String path, PersistentClass owner,
					Mappings mappings) throws MappingException {
				Set set = new Set( owner );
				bindCollection( node, set, owner.getEntityName(), path, mappings );
				return set;
			}
		};
		private static final CollectionType LIST = new CollectionType( "list" ) {
			public Collection create(Element node, String path, PersistentClass owner,
					Mappings mappings) throws MappingException {
				List list = new List( owner );
				bindCollection( node, list, owner.getEntityName(), path, mappings );
				return list;
			}
		};
		private static final CollectionType BAG = new CollectionType( "bag" ) {
			public Collection create(Element node, String path, PersistentClass owner,
					Mappings mappings) throws MappingException {
				Bag bag = new Bag( owner );
				bindCollection( node, bag, owner.getEntityName(), path, mappings );
				return bag;
			}
		};
		private static final CollectionType IDBAG = new CollectionType( "idbag" ) {
			public Collection create(Element node, String path, PersistentClass owner,
					Mappings mappings) throws MappingException {
				IdentifierBag bag = new IdentifierBag( owner );
				bindCollection( node, bag, owner.getEntityName(), path, mappings );
				return bag;
			}
		};
		private static final CollectionType ARRAY = new CollectionType( "array" ) {
			public Collection create(Element node, String path, PersistentClass owner,
					Mappings mappings) throws MappingException {
				Array array = new Array( owner );
				bindArray( node, array, owner.getEntityName(), path, mappings );
				return array;
			}
		};
		private static final CollectionType PRIMITIVE_ARRAY = new CollectionType( "primitive-array" ) {
			public Collection create(Element node, String path, PersistentClass owner,
					Mappings mappings) throws MappingException {
				PrimitiveArray array = new PrimitiveArray( owner );
				bindArray( node, array, owner.getEntityName(), path, mappings );
				return array;
			}
		};
		private static final HashMap INSTANCES = new HashMap();

		static {
			INSTANCES.put( MAP.toString(), MAP );
			INSTANCES.put( BAG.toString(), BAG );
			INSTANCES.put( IDBAG.toString(), IDBAG );
			INSTANCES.put( SET.toString(), SET );
			INSTANCES.put( LIST.toString(), LIST );
			INSTANCES.put( ARRAY.toString(), ARRAY );
			INSTANCES.put( PRIMITIVE_ARRAY.toString(), PRIMITIVE_ARRAY );
		}

		public static CollectionType collectionTypeFromString(String xmlTagName) {
			return (CollectionType) INSTANCES.get( xmlTagName );
		}
	}

	private static int getOptimisticLockMode(Attribute olAtt) throws MappingException {

		if ( olAtt == null ) return Versioning.OPTIMISTIC_LOCK_VERSION;
		String olMode = olAtt.getValue();
		if ( olMode == null || "version".equals( olMode ) ) {
			return Versioning.OPTIMISTIC_LOCK_VERSION;
		}
		else if ( "dirty".equals( olMode ) ) {
			return Versioning.OPTIMISTIC_LOCK_DIRTY;
		}
		else if ( "all".equals( olMode ) ) {
			return Versioning.OPTIMISTIC_LOCK_ALL;
		}
		else if ( "none".equals( olMode ) ) {
			return Versioning.OPTIMISTIC_LOCK_NONE;
		}
		else {
			throw new MappingException( "Unsupported optimistic-lock style: " + olMode );
		}
	}

	private static final java.util.Map getMetas(Element node, java.util.Map inheritedMeta) {
		return getMetas( node, inheritedMeta, false );
	}

	private static final java.util.Map getMetas(Element node, java.util.Map inheritedMeta,
			boolean onlyInheritable) {
		java.util.Map map = new HashMap();
		map.putAll( inheritedMeta );

		Iterator iter = node.elementIterator( "meta" );
		while ( iter.hasNext() ) {
			Element metaNode = (Element) iter.next();
			boolean inheritable = Boolean
				.valueOf( metaNode.attributeValue( "inherit" ) )
				.booleanValue();
			if ( onlyInheritable & !inheritable ) {
				continue;
			}
			String name = metaNode.attributeValue( "attribute" );

			MetaAttribute meta = (MetaAttribute) map.get( name );
			if ( meta == null ) {
				meta = new MetaAttribute( name );
				map.put( name, meta );
			}
			meta.addValue( metaNode.getText() );
		}
		return map;
	}

	private static String getEntityName(Element elem, Mappings model) {
		String entityName = elem.attributeValue( "entity-name" );
		return entityName == null ? getClassName( elem.attribute( "class" ), model ) : entityName;
	}

	private static String getClassName(Attribute att, Mappings model) {
		if ( att == null ) return null;
		return getClassName( att.getValue(), model );
	}

	private static String getClassName(String unqualifiedName, Mappings model) {
		if ( unqualifiedName == null ) return null;
		if ( unqualifiedName.indexOf( '.' ) < 0 && model.getDefaultPackage() != null ) {
			return model.getDefaultPackage() + '.' + unqualifiedName;
		}
		return unqualifiedName;
	}

	private static void parseFilterDef(Element element, Mappings mappings) {
		String name = element.attributeValue( "name" );
		log.debug( "Parsing filter-def [" + name + "]" );
		FilterDefinition def = new FilterDefinition( name );
		Iterator params = element.elementIterator( "filter-param" );
		while ( params.hasNext() ) {
			final Element param = (Element) params.next();
			final String paramName = param.attributeValue( "name" );
			final String paramType = param.attributeValue( "type" );
			log.debug( "adding filter parameter : " + paramName + " -> " + paramType );
			final Type heuristicType = TypeFactory.heuristicType( paramType );
			log.debug( "parameter heuristic type : " + heuristicType );
			def.addParameterType( paramName, heuristicType );
		}
		String condition = element.getTextTrim();
		if ( StringHelper.isEmpty(condition) ) condition = element.attributeValue( "condition" );
		def.setDefaultFilterCondition(condition);
		log.debug( "Parsed filter-def [" + name + "]" );
		mappings.addFilterDefinition( def );
	}

	private static void parseFilter(Element filterElement, Filterable filterable, Mappings model) {
		final String name = filterElement.attributeValue( "name" );
		String condition = filterElement.getTextTrim();
		if ( StringHelper.isEmpty(condition) ) condition = filterElement.attributeValue( "condition" );
		//TODO: bad implementation, cos it depends upon ordering of mapping doc
		if ( StringHelper.isEmpty(condition) ) {
			condition = model.getFilterDefinition(name).getDefaultFilterCondition();
		}
		if ( condition==null) {
			throw new MappingException("no filter condition found for filter: " + name);
		}
		log.debug( "Applying filter [" + name + "] as [" + condition + "]" );
		filterable.addFilter( name, condition );
	}

	private static String getSubselect(Element element) {
		String subselect = element.attributeValue( "subselect" );
		if ( subselect != null ) {
			return subselect;
		}
		else {
			Element subselectElement = element.element( "subselect" );
			return subselectElement == null ? null : subselectElement.getText();
		}
	}

	/**
	 * @param doc
	 * @return
	 */
	public static java.util.List getExtendsNeeded(Document doc, Mappings mappings) {

		java.util.List extendz = new ArrayList();

		Iterator[] subclasses = new Iterator[3];

		final Element hmNode = doc.getRootElement();
		Attribute packNode = hmNode.attribute( "package" );
		if ( packNode != null ) mappings.setDefaultPackage( packNode.getValue() );

		subclasses[0] = hmNode.elementIterator( "subclass" );
		subclasses[1] = hmNode.elementIterator( "joined-subclass" );
		subclasses[2] = hmNode.elementIterator( "union-subclass" );

		Iterator iterator = new JoinedIterator( subclasses );
		while ( iterator.hasNext() ) {
			Element element = (Element) iterator.next();
			String superClass = getClassName( element.attributeValue( "extends" ), mappings );
			if ( mappings.getClass( superClass ) == null ) {
				extendz.add( superClass );
			}
		}

		if ( !extendz.isEmpty() ) {
			java.util.List names = new ArrayList();
			findClassNames( mappings, hmNode, names );

			java.util.Set set = new HashSet( extendz );
			set.removeAll( names );

			extendz.clear();
			extendz.addAll( set );
		}

		return extendz;
	}

	/**
	 * @param mappings
	 * @param startNode
	 * @param names
	 */
	private static void findClassNames(Mappings mappings, final Element startNode,
			final java.util.List names) {
		// if we have some extends we need to check if those classes possibly could be inside the
		// same hbm.xml file...
		Iterator[] classes = new Iterator[4];
		classes[0] = startNode.elementIterator( "class" );
		classes[1] = startNode.elementIterator( "subclass" );
		classes[2] = startNode.elementIterator( "joined-subclass" );
		classes[3] = startNode.elementIterator( "union-subclass" );

		Iterator classIterator = new JoinedIterator( classes );
		while ( classIterator.hasNext() ) {
			Element element = (Element) classIterator.next();
			String entityName = element.attributeValue( "entity-name" );
			if ( entityName == null ) entityName = getClassName( element.attribute("name"), mappings );
			names.add( entityName );
			findClassNames( mappings, element, names );
		}
	}

}
