// $Id: FromElement.java,v 1.66 2005/03/28 01:00:31 pgmjsd Exp $
package org.hibernate.hql.ast;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.QueryException;
import org.hibernate.engine.JoinSequence;
import org.hibernate.hql.QueryTranslator;
import org.hibernate.hql.antlr.SqlTokenTypes;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents a single mapped class mentioned in an HQL FROM clause.  Each
 * class reference will have the following symbols:
 * <ul>
 * <li>A class name - This is the name of the Java class that is mapped by Hibernate.</li>
 * <li>[optional] an HQL alias for the mapped class.</li>
 * <li>A table name - The name of the table that is mapped to the Java class.</li>
 * <li>A table alias - The alias for the table that will be used in the resulting SQL.</li>
 * </ul>
 * <br>
 * User: josh<br>
 * Date: Dec 6, 2003<br>
 * Time: 10:28:17 AM<br>
 */
public class FromElement extends HqlSqlWalkerNode implements DisplayableNode {
	private static final Log log = LogFactory.getLog( FromElement.class );

	private String className;
	private String classAlias;
	private String tableAlias;
	private String collectionTableAlias;
	private FromClause fromClause;
	private String selectFragment;
	private boolean includeSubclasses = true;
	private boolean collectionJoin = false;
	private FromElement origin;
	private String[] columns;
	private String role;
	private boolean fetch;
	private boolean filter = false;
	private int sequence = -1;
	private boolean useFromFragment = false;
	private boolean initialized = false;
	private FromElementType elementType;
	private boolean useWhereFragment = true;
	private List destinations = new LinkedList();
	private boolean manyToMany = false;

	public FromElement() {
	}

	void initializeCollection(FromClause fromClause, String classAlias, String tableAlias) {
		doInitialize( fromClause, tableAlias, null, classAlias, null, null );
		initialized = true;
	}

	private void doInitialize(FromClause fromClause, String tableAlias, String className, String classAlias,
							  EntityPersister persister, EntityType type) {
		if ( initialized ) {
			throw new IllegalStateException( "Already initialized!!" );
		}
		this.fromClause = fromClause;
		this.tableAlias = tableAlias;
		this.className = className;
		this.classAlias = classAlias;
		this.elementType = new FromElementType( this, persister, type );
		// Register the FromElement with the FROM clause, now that we have the names and aliases.
		fromClause.registerFromElement( this );
		if ( log.isDebugEnabled() ) {
			log.debug( fromClause + " :  " + className + " ("
					+ ( classAlias == null ? "no alias" : classAlias ) + ") -> " + tableAlias );
		}
	}

	void initializeEntity(FromClause fromClause,
						  String className,
						  EntityPersister persister,
						  EntityType type,
						  String classAlias,
						  String tableAlias) {
		doInitialize( fromClause, tableAlias, className, classAlias, persister, type );
		this.sequence = fromClause.nextFromElementCounter();
		initialized = true;
	}

	public EntityPersister getEntityPersister() {
		return elementType.getEntityPersister();
	}

	public Type getDataType() {
		return elementType.getDataType();
	}

	public Type getSelectType() {
		return elementType.getSelectType();
	}

	public Queryable getQueryable() {
		return elementType.getQueryable();
	}

	public String getClassName() {
		return className;
	}

	public String getClassAlias() {
		return classAlias;
		//return classAlias == null ? className : classAlias;
	}

	private String getTableName() {
		Queryable queryable = getQueryable();
		return ( queryable != null ) ? queryable.getTableName() : "{none}";
	}

	public String getTableAlias() {
		return tableAlias;
	}

	/**
	 * Render the identifier select, but in a 'scalar' context (i.e. generate the column alias).
	 *
	 * @param i the sequence of the returned type
	 * @return the identifier select with the column alias.
	 */
	String renderScalarIdentifierSelect(int i) {
		return elementType.renderScalarIdentifierSelect( i );
	}

	void checkInitialized() {
		if ( !initialized ) {
			throw new IllegalStateException( "FromElement has not been initialized!" );
		}
	}

	/**
	 * Returns the identifier select SQL fragment.
	 *
	 * @param size The total number of returned types.
	 * @param k    The sequence of the current returned type.
	 * @return the identifier select SQL fragment.
	 */
	String renderIdentifierSelect(int size, int k) {
		return elementType.renderIdentifierSelect( size, k );
	}

	/**
	 * Returns the property select SQL fragment.
	 *
	 * @param size The total number of returned types.
	 * @param k    The sequence of the current returned type.
	 * @return the property select SQL fragment.
	 */
	String renderPropertySelect(int size, int k) {
		return elementType.renderPropertySelect( size, k );
	}

	public FromClause getFromClause() {
		return fromClause;
	}

	/**
	 * Returns true if this FromElement was implied by a path, or false if this FROM element is explicitly declared in
	 * the FROM clause.
	 *
	 * @return true if this FromElement was implied by a path, or false if this FROM element is explicitly declared
	 */
	boolean isImplied() {
		return false;	// This is an explicit FROM element.
	}

	/**
	 * Returns additional display text for the AST node.
	 *
	 * @return String - The additional display text.
	 */
	public String getDisplayText() {
		StringBuffer buf = new StringBuffer();
		buf.append( "FromElement{" );
		appendDisplayText( buf );
		buf.append( "}" );
		return buf.toString();
	}

	protected void appendDisplayText(StringBuffer buf) {
		buf.append( isImplied() ? (
				isImpliedInFromClause() ? "implied in FROM clause" : "implied" )
				: "explicit" );
		buf.append( "," ).append( isCollectionJoin() ? "collection join" : "not a collection join" );
		buf.append( ",classAlias=" ).append( getClassAlias() );
		buf.append( ",role=" ).append( role );
		buf.append( ",tableName=" ).append( getTableName() );
		buf.append( ",tableAlias=" ).append( getTableAlias() );
		buf.append( ",colums={" );
		if ( columns != null ) {
			for ( int i = 0; i < columns.length; i++ ) {
				buf.append( columns[i] );
				if ( i < columns.length ) {
					buf.append( " " );
				}
			}
		}
		buf.append( ",className=" ).append( className );
		buf.append( "}" );
	}

	public int hashCode() {
		return super.hashCode();
	}

	public boolean equals(Object obj) {
		return super.equals( obj );
	}


	public void setJoinSequence(JoinSequence joinSequence) {
		elementType.setJoinSequence( joinSequence );
	}

	public JoinSequence getJoinSequence() {
		return elementType.getJoinSequence();
	}

	public String renderCollectionSelectFragment() {
		return selectFragment;
	}

	public void setSelectFragment(String selectFragment) {
		this.selectFragment = selectFragment;
	}

	public void setIncludeSubclasses(boolean includeSubclasses) {
		this.includeSubclasses = includeSubclasses;
	}

	public boolean isIncludeSubclasses() {
		return includeSubclasses;
	}

	public String getIdentityColumn() {
		checkInitialized();
		String table = getTableAlias();
		if ( table == null ) {
			throw new IllegalStateException( "No table alias for node " + this );
		}
		String[] cols = getPropertyMapping( EntityPersister.ENTITY_ID ).toColumns( table, EntityPersister.ENTITY_ID );
		if ( cols.length > 1 ) {
			StringBuffer buf = new StringBuffer();
			buf.append( "{composite id for " ).append( classAlias ).append( '}' );
			return buf.toString();
		}
		return cols[0];
	}

	public void setCollectionJoin(boolean collectionJoin) {
		this.collectionJoin = collectionJoin;
	}

	public boolean isCollectionJoin() {
		return collectionJoin;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public void setQueryableCollection(QueryableCollection queryableCollection) {
		elementType.setQueryableCollection( queryableCollection );
	}

	public QueryableCollection getQueryableCollection() {
		return elementType.getQueryableCollection();
	}

	public void setColumns(String[] columns) {
		this.columns = columns;
	}

	public void setOrigin(FromElement origin, boolean manyToMany) {
		this.origin = origin;
		this.manyToMany = manyToMany;
		origin.addDestination( this );
		if ( origin.getFromClause() == this.getFromClause() ) {
			// TODO: Figure out a better way to get the FROM elements in a proper tree structure.
			// If this is not the destination of a many-to-many, add it as a child of the origin.
			if ( manyToMany ) {
				ASTUtil.appendSibling( origin, this );
			}
			else {
				if ( !getWalker().isInFrom() && !getWalker().isInSelect() ) {
					getFromClause().addChild( this );
				}
				else {
					origin.addChild( this );
				}
			}
		}
		else if ( !getWalker().isInFrom() ) {
			// HHH-276 : implied joins in a subselect where clause - The destination needs to be added
			// to the destination's from clause.
			getFromClause().addChild( this );	// Not sure if this is will fix everything, but it works.
		}
		else {
			// Otherwise, the destination node was implied by the FROM clause and the FROM clause processor
			// will automatically add it in the right place.
		}
	}

	boolean isManyToMany() {
		return manyToMany;
	}

	private void addDestination(FromElement fromElement) {
		destinations.add( fromElement );
	}

	List getDestinations() {
		return destinations;
	}

	public FromElement getOrigin() {
		return origin;
	}

	/**
	 * Returns the type of a property, given it's name (the last part) and the full path.
	 *
	 * @param propertyName The last part of the full path to the property.
	 * @return The type.
	 * @0param propertyPath The full property path.
	 */
	public Type getPropertyType(String propertyName, String propertyPath) {
		return elementType.getPropertyType( propertyName, propertyPath );
	}

	String[] toColumns(String tableAlias, String path, boolean inSelect) {
		return elementType.toColumns( tableAlias, path, inSelect );
	}

	PropertyMapping getPropertyMapping(String propertyName) {
		return elementType.getPropertyMapping( propertyName );
	}

	public void setFetch(boolean fetch) {
		this.fetch = fetch;
		// Fetch can't be used with scroll() or iterate().
		if ( fetch && getWalker().isShallowQuery() ) {
			throw new QueryException( QueryTranslator.ERROR_CANNOT_FETCH_WITH_ITERATE );
		}
	}

	public boolean isFetch() {
		return fetch;
	}

	public int getSequence() {
		return sequence;
	}

	void setFilter(boolean b) {
		filter = b;
	}

	boolean isFilter() {
		return filter;
	}

	/**
	 * Returns true if the from fragment should be included in the from clause.
	 */
	boolean useFromFragment() {
		checkInitialized();
		// If it's not implied or it is implied and it's a many to many join where the target wasn't found.
		return !isImplied() || this.useFromFragment;
	}

	void setUseFromFragment(boolean useFromFragment) {
		this.useFromFragment = useFromFragment;
	}

	boolean useWhereFragment() {
		return useWhereFragment;
	}

	void setUseWhereFragment(boolean b) {
		useWhereFragment = b;
	}


	public void setCollectionTableAlias(String collectionTableAlias) {
		this.collectionTableAlias = collectionTableAlias;
	}

	public String getCollectionTableAlias() {
		return collectionTableAlias;
	}

	public boolean isCollectionOfValuesOrComponents() {
		return elementType.isCollectionOfValuesOrComponents();
	}

	public boolean isEntity() {
		return elementType.isEntity();
	}

	void setImpliedInFromClause(boolean flag) {
		throw new UnsupportedOperationException( "Explicit FROM elements can't be implied in the FROM clause!" );
	}

	boolean isImpliedInFromClause() {
		return false;	// Since this is an explicit FROM element, it can't be implied in the FROM clause.
	}

	public void setInProjectionList(boolean inProjectionList) {
		// Do nothing, eplicit from elements are *always* in the projection list.
	}

	/**
	 * Returns true if this element should be in the projection list.
	 *
	 * @return
	 */
	boolean inProjectionList() {
		return !isImplied() && isFromOrJoinFragment();
	}

	boolean isFromOrJoinFragment() {
		return getType() == SqlTokenTypes.FROM_FRAGMENT || getType() == SqlTokenTypes.JOIN_FRAGMENT;
	}
}
