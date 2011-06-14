// $Id: DotNode.java,v 1.90 2005/03/31 15:00:01 steveebersole Exp $
package org.hibernate.hql.ast;

import antlr.SemanticException;
import antlr.collections.AST;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.QueryException;
import org.hibernate.engine.JoinSequence;
import org.hibernate.hql.antlr.SqlTokenTypes;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.JoinFragment;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.util.StringHelper;

/**
 * Represents a reference to a property or alias expression.  This should duplicate the relevant behaviors in
 * PathExpressionParser.
 * <hr>
 * User: josh<br>
 * Date: Dec 16, 2003<br>
 * Time: 8:03:09 AM
 */
class DotNode extends FromReferenceNode implements DisplayableNode, SelectExpression {

	/**
	 * A logger for this class.
	 */
	private static final Log log = LogFactory.getLog( DotNode.class );

	private static final int DEREF_UNKNOWN = 0;
	private static final int DEREF_ENTITY = 1;
	private static final int DEREF_COMPONENT = 2;
	private static final int DEREF_COLLECTION = 3;
	private static final int DEREF_PRIMITIVE = 4;
	private static final int DEREF_IDENTIFIER = 5;
	private static final int DEREF_JAVA_CONSTANT = 6;

	/**
	 * The identifier that is the name of the property.
	 */
	private String propertyName;
	/**
	 * The full path, to the root alias of this dot node.
	 */
	private String path;
	/**
	 * The unresolved property path relative to this dot node.
	 */
	private String propertyPath;

	/**
	 * The column names that this resolves to.
	 */
	private String[] columns;

	/**
	 * The type of join to create.   Default is an inner join.
	 */
	private int joinType = JoinFragment.INNER_JOIN;

	/**
	 * Fetch join or not.
	 */
	private boolean fetch = false;

	/**
	 * The type of dereference that hapened (DEREF_xxx).
	 */
	private int dereferenceType = DEREF_UNKNOWN;

	private FromElement impliedJoin;

	/**
	 * Sets the join type for the '.' node (JoinFragment.XXX).
	 *
	 * @param joinType
	 * @see JoinFragment
	 */
	public void setJoinType(int joinType) {
		this.joinType = joinType;
	}

	private String[] getColumns() throws QueryException {
		if ( columns == null ) {
			// Use the table fromElement and the property name to get the array of column names.
			String tableAlias = getLhs().getFromElement().getTableAlias();
			columns = getFromElement().toColumns( tableAlias, propertyPath, false );
		}
		return columns;
	}

	public String getDisplayText() {
		StringBuffer buf = new StringBuffer();
		FromElement fromElement = getFromElement();
		buf.append( "{propertyName=" ).append( propertyName );
		buf.append( ",dereferenceType=" ).append( ASTPrinter.getConstantName( getClass(), dereferenceType ) );
		buf.append( ",propertyPath=" ).append( propertyPath );
		buf.append( ",path=" ).append( getPath() );
		if ( fromElement != null ) {
			buf.append( ",tableAlias=" ).append( fromElement.getTableAlias() );
			buf.append( ",className=" ).append( fromElement.getClassName() );
			buf.append( ",classAlias=" ).append( fromElement.getClassAlias() );
		}
		else {
			buf.append( ",no from element" );
		}
		buf.append( '}' );
		return buf.toString();
	}

	/**
	 * Resolves the left hand side of the DOT.
	 *
	 * @throws SemanticException
	 */
	public void resolveFirstChild() throws SemanticException {
		FromReferenceNode lhs = ( FromReferenceNode ) getFirstChild();
		SqlNode property = ( SqlNode ) lhs.getNextSibling();

		// Set the attributes of the property reference expression.
		String propName = property.getText();
		propertyName = propName;
		// If the uresolved property path isn't set yet, just use the property name.
		if ( propertyPath == null ) {
			propertyPath = propName;
		}
		// Resolve the LHS fully, generate implicit joins.  Pass in the property name so that the resolver can
		// discover foreign key (id) properties.
		lhs.resolve( true, true, null, this );
		setFromElement( lhs.getFromElement() );			// The 'from element' that the property is in.
	}
	
	public void resolveInFunctionCall(boolean generateJoin, boolean implicitJoin) throws SemanticException {
		if ( isResolved() ) {
			return;
		}
		Type propertyType = prepareLhs();			// Prepare the left hand side and get the data type.
		if ( propertyType!=null && propertyType.isCollectionType() ) {
			resolveIndex(null);
		}
		else {
			resolveFirstChild();
			super.resolve(generateJoin, implicitJoin);
		}
	}


	public void resolveIndex(AST parent) throws SemanticException {
		if ( isResolved() ) {
			return;
		}
		Type propertyType = prepareLhs();			// Prepare the left hand side and get the data type.
		dereferenceCollection( ( CollectionType ) propertyType, true, true, null, parent );
	}

	public void resolve(boolean generateJoin, boolean implicitJoin, String classAlias, AST parent) 
	throws SemanticException {
		// If this dot has already been resolved, stop now.
		if ( isResolved() ) {
			return;
		}
		Type propertyType = prepareLhs(); // Prepare the left hand side and get the data type.

		// If there is no data type for this node, and we're at the end of the path (top most dot node), then
		// this might be a Java constant.
		if ( propertyType == null ) {
			if ( parent == null ) {
				getWalker().getLiteralProcessor().lookupConstant( this );
			}
			// If the propertyType is null and there isn't a parent, just 
			// stop now... there was a problem resolving the node anyway.
			return;
		}
		// The property is a component...
		if ( propertyType.isComponentType() ) {
			dereferenceComponent( parent );
			initText();
		}
		// The property is another class..
		else if ( propertyType.isEntityType() ) {
			dereferenceEntity( ( EntityType ) propertyType, implicitJoin, classAlias, generateJoin, parent );
			initText();
		}
		// The property is a collection...
		else if ( propertyType.isCollectionType() ) {
			dereferenceCollection( ( CollectionType ) propertyType, implicitJoin, false, classAlias, parent );
		}
		else {	// Otherwise, this is a primitive type.
			dereferenceType = DEREF_PRIMITIVE;
			initText();
		}
		setResolved();
	}
	
	private void initText() {
		setText( StringHelper.join( ", ", getColumns() ) );		
	}

	private Type prepareLhs() throws SemanticException {
		FromReferenceNode lhs = getLhs();
		lhs.prepareForDot( propertyName );
		Type propertyType = getDataType();
		return propertyType;
	}

	private void dereferenceCollection(CollectionType collectionType, boolean implicitJoin, boolean indexed, String classAlias, AST parent) 
	throws SemanticException {
		dereferenceType = DEREF_COLLECTION;
		String role = collectionType.getRole();

		QueryableCollection queryableCollection = getSessionFactoryHelper().requireQueryableCollection( role );
		String propName = getPath();
		FromClause currentFromClause = getWalker().getCurrentFromClause();

		//We do not look for an existing join on the same path, because
		//it makes sense to join twice on the same collection role
		FromElementFactory factory = new FromElementFactory(
		        currentFromClause,
		        getLhs().getFromElement(),
		        propName,
				classAlias,
		        getColumns(),
		        implicitJoin
		);
		FromElement elem = factory.createCollection( queryableCollection, role, joinType, fetch, indexed );
		
		if ( log.isDebugEnabled() ) {
			log.debug( "dereferenceCollection() : Created new FROM element for " + propName + " : " + elem );
		}
		
		setImpliedJoin( elem );
		setFromElement( elem );	// This 'dot' expression now refers to the resulting from element.
		
		if ( !implicitJoin ) {
			EntityPersister entityPersister = elem.getEntityPersister();
			if ( entityPersister != null ) {
				getWalker().addQuerySpaces( entityPersister.getQuerySpaces() );
			}
		}
		getWalker().addQuerySpaces( queryableCollection.getCollectionSpaces() );	// Always add the collection's query spaces.
	}

	private void dereferenceEntity(EntityType entityType, boolean implicitJoin, String classAlias, boolean generateJoin, AST parent) throws SemanticException {
		checkForCorrelatedSubquery( "dereferenceEntity" );
		// If this is an entity inside a component reference, then generate the join.
		if ( unresolvedComponent( generateJoin ) ) {
			if ( log.isDebugEnabled() ) {
				log.debug( "dereferenceEntity() : resolving unresolved component '" + propertyPath + "' ... " );
			}
			dereferenceEntityJoin( classAlias, entityType, implicitJoin, parent );
			return;
		}

		//NOTE: we avoid joining to the next table if the named property is just the foreign key value
		String property = propertyName;
		DotNode dotParent = null;

		// Use the parent property name, if a parent was specified to look ahead.
		if ( isDotNode( parent ) ) {
			dotParent = ( DotNode ) parent;
			property = dotParent.propertyName;
		}

		boolean joinIsNotNeeded = isPrimaryKeyReference( property, entityType ) ||
				isNamedIdPropertyShortcut( entityType, property ) ||
				!generateJoin;

		if ( joinIsNotNeeded ) {
			dereferenceEntityIdentifier( property, dotParent );
		}
		else {
			dereferenceEntityJoin( classAlias, entityType, implicitJoin, parent );
		}
	}

	private boolean unresolvedComponent(boolean generateJoin) {
		AST c = getFirstChild();
		if ( generateJoin && isDotNode( c ) ) {
			DotNode dot = ( DotNode ) c;
			if ( dot.dereferenceType == DEREF_COMPONENT || dot.dereferenceType == DEREF_IDENTIFIER ) {
				if ( StringHelper.isNotEmpty( propertyPath ) ) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isDotNode(AST n) {
		return n != null && n.getType() == SqlTokenTypes.DOT;
	}

	private void dereferenceEntityJoin(String classAlias, EntityType propertyType, boolean impliedJoin, AST parent) 
	throws SemanticException {
		dereferenceType = DEREF_ENTITY;
		if ( log.isDebugEnabled() ) {
			log.debug( "dereferenceEntityJoin() : generating join for " + propertyName + " in "
					+ getFromElement().getClassName() + " "
					+ ( ( classAlias == null ) ? "{no alias}" : "(" + classAlias + ")" )
					+ " parent = " + ASTUtil.getDebugString( parent )
			);
		}
		// Create a new FROM node for the referenced class.
		String associatedEntityName = propertyType.getAssociatedEntityName();
		String tableAlias = getAliasGenerator().createName( associatedEntityName );

		String[] joinColumns = getColumns();
		String joinPath = getPath();

		if ( impliedJoin && getWalker().isInFrom() ) {
			int impliedJoinType = getWalker().getImpliedJoinType();
			joinType = impliedJoinType;
		}

		FromClause currentFromClause = getWalker().getCurrentFromClause();
		FromElement elem = null;
		elem = currentFromClause.findJoinByPath( joinPath );

///////////////////////////////////////////////////////////////////////////////
//
// This is the piece which recognizes the condition where an implicit join path
// resolved earlier in a correlated subquery is now being referenced in the
// outer query.  For 3.0final, we just let this generate a second join (which
// is exactly how the old parser handles this).  Eventually we need to add this
// logic back in and complete the logic in FromClause.promoteJoin; however,
// FromClause.promoteJoin has its own difficulties (see the comments in
// FromClause.promoteJoin).
//
//		if ( elem == null ) {
//			// see if this joinPath has been used in a "child" FromClause, and if so
//			// promote that element to the outer query
//			FromClause currentNodeOwner = getFromElement().getFromClause();
//			FromClause currentJoinOwner = currentNodeOwner.locateChildFromClauseWithJoinByPath( joinPath );
//			if ( currentJoinOwner != null && currentNodeOwner != currentJoinOwner ) {
//				elem = currentJoinOwner.findJoinByPathLocal( joinPath );
//				if ( elem != null ) {
//					currentFromClause.promoteJoin( elem );
//					// EARLY EXIT!!!
//					return;
//				}
//			}
//		}
//
///////////////////////////////////////////////////////////////////////////////

		if ( elem == null ) {
			// If this is an implied join in a from element, then use the impled join type which is part of the
			// tree parser's state (set by the gramamar actions).
			JoinSequence joinSequence = getSessionFactoryHelper()
				.createJoinSequence( impliedJoin, propertyType, tableAlias, joinType, joinColumns );

			FromElementFactory factory = new FromElementFactory(
			        currentFromClause,
					getLhs().getFromElement(),
					joinPath, 
					classAlias, 
					joinColumns, 
					impliedJoin );
			elem = factory.createEntityJoin( 
					associatedEntityName, 
					tableAlias, 
					joinSequence, 
					fetch, 
					getWalker().isInFrom(), 
					propertyType
			);
		}
		else {
			currentFromClause.addDuplicateAlias(classAlias, elem);
		}
		setImpliedJoin( elem );
		getWalker().addQuerySpaces( elem.getEntityPersister().getQuerySpaces() );
		setFromElement( elem );	// This 'dot' expression now refers to the resulting from element.
	}

	private void setImpliedJoin(FromElement elem) {
		this.impliedJoin = elem;
		if ( getFirstChild().getType() == SqlTokenTypes.DOT ) {
			DotNode dotLhs = ( DotNode ) getFirstChild();
			if ( dotLhs.getImpliedJoin() != null ) {
				this.impliedJoin = dotLhs.getImpliedJoin();
			}
		}
	}

	FromElement getImpliedJoin() {
		return impliedJoin;
	}

	private boolean isPrimaryKeyReference(String property, EntityType propertyType) {
		boolean isIdShortcut = EntityPersister.ENTITY_ID.equals( property ) &&
				propertyType.isReferenceToPrimaryKey();
		return isIdShortcut;
	}

	private boolean isNamedIdPropertyShortcut(EntityType propertyType, String property) {
		final String idPropertyName = getSessionFactoryHelper()
				.getIdentifierOrUniqueKeyPropertyName( propertyType );
		boolean isNamedIdPropertyShortcut = idPropertyName != null && 
				idPropertyName.equals( property );
		return isNamedIdPropertyShortcut;
	}

	private void checkForCorrelatedSubquery(String methodName) {
		if ( isCorrelatedSubselect() ) {
			if ( log.isDebugEnabled() ) {
				log.debug( methodName + "() : correlated subquery" );
			}
		}
	}

	private boolean isCorrelatedSubselect() {
		return getWalker().isSubQuery() && 
			getFromElement().getFromClause() != getWalker().getCurrentFromClause();
	}

	private void dereferenceComponent(AST parent) {
		dereferenceType = DEREF_COMPONENT;
		setPropertyNameAndPath( parent );
	}

	private void dereferenceEntityIdentifier(String propertyName, DotNode dotParent) {
		// special shortcut for id properties, skip the join!
		// this must only occur at the _end_ of a path expression
		if ( log.isDebugEnabled() ) {
			log.debug( "dereferenceShortcut() : property " + 
				propertyName + " in " + getFromElement().getClassName() + 
				" does not require a join." );
		}

		initText();
		setPropertyNameAndPath( dotParent ); // Set the unresolved path in this node and the parent.
		// Set the text for the parent.
		if ( dotParent != null ) {
			dotParent.dereferenceType = DEREF_IDENTIFIER;
			dotParent.setText( getText() );
			dotParent.columns = getColumns();
		}
	}

	private void setPropertyNameAndPath(AST parent) {
		if ( isDotNode( parent ) ) {
			DotNode dotNode = ( DotNode ) parent;
			AST lhs = dotNode.getFirstChild();
			AST rhs = lhs.getNextSibling();
			propertyName = rhs.getText();
			propertyPath = propertyPath + "." + propertyName; // Append the new property name onto the unresolved path.
			dotNode.propertyPath = propertyPath;
			if ( log.isDebugEnabled() ) {
				log.debug( "Unresolved property path is now '" + dotNode.propertyPath + "'" );
			}
		}
		else {
			// Handle "select foo.component from Foo foo", or even "where foo.component = bar.component"
			AST lhs = getFirstChild();
			AST rhs = lhs.getNextSibling();
			propertyPath = rhs.getText();
		}
	}

	public Type getDataType() {
		if ( super.getDataType() == null ) {
			FromElement fromElement = getLhs().getFromElement();
			if ( fromElement == null ) {
				return null;
			}
			// If the lhs is a collection, use CollectionPropertyMapping
			Type propertyType = fromElement.getPropertyType( propertyName, propertyPath );
			if ( log.isDebugEnabled() ) {
				log.debug( "getDataType() : " + propertyPath + " -> " + propertyType );
			}
			super.setDataType( propertyType );
		}
		return super.getDataType();
	}

	void setPropertyPath(String propertyPath) {
		this.propertyPath = propertyPath;
	}

	FromReferenceNode getLhs() {
		FromReferenceNode lhs = ( ( FromReferenceNode ) getFirstChild() );
		if ( lhs == null ) {
			throw new IllegalStateException( "DOT node with no left-hand-side!" );
		}
		return lhs;
	}

	/**
	 * Returns the full path of the node.
	 *
	 * @return the full path of the node.
	 */
	public String getPath() {
		if ( path == null ) {
			FromReferenceNode lhs = getLhs();
			if ( lhs == null ) {
				path = getText();
			}
			else {
				SqlNode rhs = ( SqlNode ) lhs.getNextSibling();
				path = lhs.getPath() + "." + rhs.getOriginalText();
			}
		}
		return path;
	}

	public void setFetch(boolean fetch) {
		this.fetch = fetch;
	}

	public void setScalarColumnText(int i) throws SemanticException {
		String[] sqlColumns = getColumns();
		ColumnHelper.generateScalarColumns( this, sqlColumns, i );
	}

	/**
	 * Special method to resolve expressions in the SELECT list.
	 *
	 * @param useThetaStyleImplicitJoins True for classic-style theta joins.
	 * @throws SemanticException if this cannot be resolved.
	 */
	public void resolveSelectExpression(boolean useThetaStyleImplicitJoins) throws SemanticException {
		if ( getWalker().isShallowQuery() || getWalker().getCurrentFromClause().isSubQuery() ) {
			resolve(false, true);
		}
		else {
			resolve(true, false);
			Type type = getDataType();
			if ( type.isEntityType() ) {
				FromElement fromElement = getFromElement();
				fromElement.setIncludeSubclasses( true ); // Tell the destination fromElement to 'includeSubclasses'.
				if ( useThetaStyleImplicitJoins ) {
					fromElement.getJoinSequence().setUseThetaStyle( true );	// Use theta style (for regression)
					// Move the node up, after the origin node.
					FromElement origin = fromElement.getOrigin();
					if ( origin != null ) {
						ASTUtil.makeSiblingOfParent( origin, fromElement );
					}
				}
			}
		}
	}

	public void setResolvedConstant(String text) {
		path = text;
		dereferenceType = DEREF_JAVA_CONSTANT;
		setResolved(); // Don't resolve the node again.
	}
}
