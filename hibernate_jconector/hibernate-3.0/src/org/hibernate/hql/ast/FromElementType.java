// $Id: FromElementType.java,v 1.4 2005/02/27 18:28:56 oneovthafew Exp $
package org.hibernate.hql.ast;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.MappingException;
import org.hibernate.engine.JoinSequence;
import org.hibernate.hql.CollectionProperties;
import org.hibernate.hql.CollectionSubqueryFactory;
import org.hibernate.hql.NameGenerator;
import org.hibernate.persister.collection.CollectionPropertyMapping;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import java.util.Map;

/**
 * Delegate that handles the type and join sequence information for a FromElement.
 *
 * @author josh Feb 12, 2005 10:17:34 AM
 */
class FromElementType {
	private static final Log log = LogFactory.getLog( FromElementType.class );

	private FromElement fromElement;
	private EntityType entityType;
	private EntityPersister persister;
	private QueryableCollection queryableCollection;
	private CollectionPropertyMapping collectionPropertyMapping;
	private JoinSequence joinSequence;

	public FromElementType(FromElement fromElement, EntityPersister persister, EntityType entityType) {
		this.fromElement = fromElement;
		this.persister = persister;
		this.entityType = entityType;
		if ( persister != null ) {
			fromElement.setText( ( ( Queryable ) persister ).getTableName() + " " + getTableAlias() );
		}
	}

	private String getTableAlias() {
		return fromElement.getTableAlias();
	}

	public EntityPersister getEntityPersister() {
		return persister;
	}

	public Type getDataType() {
		if ( persister == null ) {
			if ( queryableCollection == null ) {
				return null;
			}
			return queryableCollection.getType();
		}
		else {
			return entityType;
		}
	}

	public Type getSelectType() {
		if ( persister == null ) {
			if ( queryableCollection == null ) {
				return null;
			}
			return queryableCollection.getType();
		}
		else {
			return ( ( Queryable ) persister ).getType();
		}
	}

	/**
	 * Returns the Hibernate queryable implementation for the HQL class.
	 *
	 * @return the Hibernate queryable implementation for the HQL class.
	 */
	public Queryable getQueryable() {
		return ( persister instanceof Queryable ) ? ( Queryable ) persister : null;
	}

	/**
	 * Render the identifier select, but in a 'scalar' context (i.e. generate the column alias).
	 *
	 * @param i the sequence of the returned type
	 * @return the identifier select with the column alias.
	 */
	String renderScalarIdentifierSelect(int i) {
		checkInitialized();
		String[] cols = getPropertyMapping( EntityPersister.ENTITY_ID ).toColumns( getTableAlias(), EntityPersister.ENTITY_ID );
		StringBuffer buf = new StringBuffer();
		// For property references generate <tablealias>.<columnname> as <projectionalias>
		for ( int j = 0; j < cols.length; j++ ) {
			String column = cols[j];
			if ( j > 0 ) {
				buf.append( ", " );
			}
			buf.append( column ).append( " as " ).append( NameGenerator.scalarName( i, j ) );
		}
		return buf.toString();
	}

	/**
	 * Returns the identifier select SQL fragment.
	 *
	 * @param size The total number of returned types.
	 * @param k    The sequence of the current returned type.
	 * @return the identifier select SQL fragment.
	 */
	String renderIdentifierSelect(int size, int k) {
		checkInitialized();
		// Render the identifier select fragment using the table alias.
		if ( fromElement.getFromClause().isSubQuery() ) {
			// TODO: Replace this with a more elegant solution.
			String[] idColumnNames = ( persister != null ) ?
					( ( Queryable ) persister ).getIdentifierColumnNames() : new String[0];
			StringBuffer buf = new StringBuffer();
			for ( int i = 0; i < idColumnNames.length; i++ ) {
				buf.append( fromElement.getTableAlias() ).append( '.' ).append( idColumnNames[i] );
				if ( i != idColumnNames.length - 1 ) buf.append( ", " );
			}
			return buf.toString();
		}
		else {
			String fragment = ( ( Queryable ) persister ).identifierSelectFragment( getTableAlias(), getSuffix( size, k ) );
			return trimLeadingCommaAndSpaces( fragment );
		}
	}

	private static String getSuffix(int size, int k) {
		String suffix = size == 1 ? "" : Integer.toString( k ) + '_';
		return suffix;
	}

	private void checkInitialized() {
		fromElement.checkInitialized();
	}

	/**
	 * Returns the property select SQL fragment.
	 *
	 * @param size The total number of returned types.
	 * @param k    The sequence of the current returned type.
	 * @return the property select SQL fragment.
	 */
	String renderPropertySelect(int size, int k) {
		checkInitialized();
		String fragment = ( persister != null ) ?
				( ( Queryable ) persister ).propertySelectFragment( getTableAlias(), getSuffix( size, k ) ) : "";
		return trimLeadingCommaAndSpaces( fragment );
	}

	/**
	 * This accounts for a quirk in Queryable, where it sometimes generates ',  ' in front of the
	 * SQL fragment.  :-P
	 *
	 * @param fragment An SQL fragment.
	 * @return The fragment, without the leading comma and spaces.
	 */
	private static String trimLeadingCommaAndSpaces(String fragment) {
		if ( fragment.length() > 0 && fragment.charAt( 0 ) == ',' ) {
			fragment = fragment.substring( 1 );
		}
		fragment = fragment.trim();
		return fragment.trim();
	}

	public void setJoinSequence(JoinSequence joinSequence) {
		this.joinSequence = joinSequence;
	}

	public JoinSequence getJoinSequence() {
		if ( joinSequence != null ) {
			return joinSequence;
		}

		// Class names in the FROM clause result in a JoinSequence (the old FromParser does this).
		if ( persister instanceof Joinable ) {
			Joinable joinable = ( Joinable ) persister;
			return fromElement.getSessionFactoryHelper().createJoinSequence().setRoot( joinable, getTableAlias() );
		}
		else {
			return null;	// TODO: Should this really return null?  If not, figure out something better to do here.
		}
	}

	public void setQueryableCollection(QueryableCollection queryableCollection) {
		if ( this.queryableCollection != null ) {
			throw new IllegalStateException( "QueryableCollection is already defined for " + this + "!" );
		}
		this.queryableCollection = queryableCollection;
		if ( !queryableCollection.isOneToMany() ) {
			// For many-to-many joins, use the tablename from the queryable collection for the default text.
			fromElement.setText( queryableCollection.getTableName() + " " + getTableAlias() );
		}
	}

	public QueryableCollection getQueryableCollection() {
		return queryableCollection;
	}

	/**
	 * Returns the type of a property, given it's name (the last part) and the full path.
	 *
	 * @param propertyName The last part of the full path to the property.
	 * @return The type.
	 * @0param propertyPath The full property path.
	 */
	public Type getPropertyType(String propertyName, String propertyPath) {
		checkInitialized();
		Type type = null;
		// If this is an entity and the property is the identifier property, then use getIdentifierType().
		if ( persister != null && propertyName.equals( persister.getIdentifierPropertyName() ) ) {
			type = persister.getIdentifierType();
		}
		else {	// Otherwise, use the property mapping.
			PropertyMapping mapping = getPropertyMapping( propertyName );
			type = mapping.toType( propertyPath );
		}
		if ( type == null ) {
			throw new MappingException( "Property " + propertyName + " does not exist in " +
					( ( queryableCollection == null ) ? "class" : "collection" ) + " "
					+ ( ( queryableCollection == null ) ? fromElement.getClassName() : queryableCollection.getRole() ) );
		}
		return type;
	}

	String[] toColumns(String tableAlias, String path, boolean inSelect) {
		checkInitialized();
		PropertyMapping propertyMapping = getPropertyMapping( path );
		// If this from element is a collection and the path is a collection property (maxIndex, etc.) then
		// generate a sub-query.
		if ( !inSelect && queryableCollection != null && CollectionProperties.isCollectionProperty( path ) ) {
			Map enabledFilters = fromElement.getWalker().getEnabledFilters();
			String subquery = CollectionSubqueryFactory.createCollectionSubquery( joinSequence, enabledFilters,
					propertyMapping.toColumns( tableAlias, path ) );
			if ( log.isDebugEnabled() ) {
				log.debug( "toColumns(" + tableAlias + "," + path + ") : subquery = " + subquery );
			}
			return new String[]{"(" + subquery + ")"};
		}
		else {
			return propertyMapping.toColumns( tableAlias, path );
		}
	}

	PropertyMapping getPropertyMapping(String propertyName) {
		checkInitialized();
		if ( queryableCollection == null ) {		// Not a collection?
			return ( PropertyMapping ) persister;	// Return the entity property mapping.
		}
		// If the property is a special collection property name, return a CollectionPropertyMapping.
		if ( CollectionProperties.isCollectionProperty( propertyName ) ) {
			if ( collectionPropertyMapping == null ) {
				collectionPropertyMapping = new CollectionPropertyMapping( queryableCollection );
			}
			return collectionPropertyMapping;
		}
		if ( queryableCollection.getElementType().isComponentType() ) {	// Collection of components.
			if ( propertyName.equals( EntityPersister.ENTITY_ID ) ) {
				return ( PropertyMapping ) queryableCollection.getOwnerEntityPersister();
			}
		}
		return queryableCollection;
	}

	public boolean isCollectionOfValuesOrComponents() {
		if ( persister == null ) {
			if ( queryableCollection == null ) {
				return false;
			}
			else {
				return !queryableCollection.getElementType().isEntityType();
			}
		}
		else {
			return false;
		}
	}

	public boolean isEntity() {
		return persister != null;
	}
}
