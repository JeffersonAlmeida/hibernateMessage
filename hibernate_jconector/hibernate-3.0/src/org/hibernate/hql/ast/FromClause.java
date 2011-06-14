// $Id: FromClause.java,v 1.36 2005/03/31 17:36:05 steveebersole Exp $
package org.hibernate.hql.ast;

import antlr.SemanticException;
import antlr.collections.AST;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.hql.antlr.HqlSqlTokenTypes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents the 'FROM' part of a query or subquery, containing all mapped class references.
 *
 * @author josh
 */
class FromClause extends HqlSqlWalkerNode implements HqlSqlTokenTypes, DisplayableNode {
	private static Log log = LogFactory.getLog( FromClause.class );
	public static final int ROOT_LEVEL = 1;

	private int level = ROOT_LEVEL;
	private Set fromElements = new HashSet();
	private Map fromElementByClassAlias = new HashMap();
	private Map fromElementByTableAlias = new HashMap();
	private Map fromElementsByPath = new HashMap();

	/**
	 * All of the implicit FROM xxx JOIN yyy elements that are the destination of a collection.  These are created from
	 * index operators on collection property references.
	 */
	private Map collectionJoinFromElementsByPath = new HashMap();
	/**
	 * Pointer to the parent FROM clause, if there is one.
	 */
	private FromClause parentFromClause;
	/**
	 * Collection of FROM clauses of which this is the parent.
	 */
	private Set childFromClauses;
	/**
	 * Counts the from elements as they are added.
	 */
	private int fromElementCounter = 0;
	/**
	 * Implied FROM elements to add onto the end of the FROM clause.
	 */
	private List impliedElements = new LinkedList();

	/**
	 * Adds a new from element to the from node.
	 *
	 * @param path  The reference to the class.
	 * @param alias The alias AST.
	 * @return FromElement - The new FROM element.
	 */
	FromElement addFromElement(String path, AST alias) throws SemanticException {
		// The path may be a reference to an alias defined in the parent query.
		String classAlias = ( alias == null ) ? null : alias.getText();
		checkForDuplicateClassAlias( classAlias );
		FromElementFactory factory = new FromElementFactory( this, null, path, classAlias, null, false );
		return factory.addFromElement();
	}

	void registerFromElement(FromElement element) {
		fromElements.add( element );
		String classAlias = element.getClassAlias();
		if ( classAlias != null ) {
			// The HQL class alias refers to the class name.
			fromElementByClassAlias.put( classAlias, element );
		}
		// Associate the table alias with the element.
		String tableAlias = element.getTableAlias();
		if ( tableAlias != null ) {
			fromElementByTableAlias.put( tableAlias, element );
		}
	}

	void addDuplicateAlias(String alias, FromElement element) {
		fromElementByClassAlias.put( alias, element );
	}

	private void checkForDuplicateClassAlias(String classAlias) throws SemanticException {
		if ( classAlias != null && fromElementByClassAlias.containsKey( classAlias ) ) {
			throw new SemanticException( "Duplicate definition of alias '"
					+ classAlias + "'" );
		}
	}

	/**
	 * Retreives the from-element represented by the given alias.
	 *
	 * @param aliasOrClassName The alias by which to locate the from-element.
	 * @return The from-element assigned the given alias, or null if none.
	 */
	public FromElement getFromElement(String aliasOrClassName) {
		FromElement fromElement = ( FromElement ) fromElementByClassAlias.get( aliasOrClassName );
		if ( fromElement == null && parentFromClause != null ) {
			fromElement = parentFromClause.getFromElement( aliasOrClassName );
		}
		return fromElement;
	}

	/**
	 * Convenience method to check whether a given token represents a from-element alias.
	 *
	 * @param possibleAlias The potential from-element alias to check.
	 * @return True if the possibleAlias is an alias to a from-element visible
	 *         from this point in the query graph.
	 */
	public boolean isFromElementAlias(String possibleAlias) {
		boolean isAlias = fromElementByClassAlias.containsKey( possibleAlias );
		if ( !isAlias && parentFromClause != null ) {
			// try the parent FromClause...
			isAlias = parentFromClause.isFromElementAlias( possibleAlias );
		}
		return isAlias;
	}

	/**
	 * Returns the list of from elements in order.
	 *
	 * @return the list of from elements (instances of FromElement).
	 */
	public List getFromElements() {
		return ASTUtil.collectChildren( this, fromElementPredicate );
	}

	/**
	 * Returns the list of from elements that will be part of the result set.
	 *
	 * @return the list of from elements that will be part of the result set.
	 */
	public List getProjectionList() {
		return ASTUtil.collectChildren( this, projectionListPredicate );
	}

	private static ASTUtil.FilterPredicate fromElementPredicate = new ASTUtil.IncludePredicate() {
		public boolean include(AST node) {
			FromElement fromElement = ( FromElement ) node;
			return fromElement.isFromOrJoinFragment();
		}
	};

	private static ASTUtil.FilterPredicate projectionListPredicate = new ASTUtil.IncludePredicate() {
		public boolean include(AST node) {
			FromElement fromElement = ( FromElement ) node;
			return fromElement.inProjectionList();
		}
	};

	FromElement findCollectionJoin(String path) {
		return ( FromElement ) collectionJoinFromElementsByPath.get( path );
	}

	/**
	 * Look for an existing implicit or explicit join by the
	 * given path.
	 */
	FromElement findJoinByPath(String path) {
		FromElement elem = findJoinByPathLocal( path );
		if ( elem == null && parentFromClause != null ) {
			elem = parentFromClause.findJoinByPath( path );
		}
		return elem;
	}

	FromElement findJoinByPathLocal(String path) {
		Map joinsByPath = fromElementsByPath;
		return ( FromElement ) joinsByPath.get( path );
	}

	void addJoinByPathMap(String path, FromElement destination) {
		if ( log.isDebugEnabled() ) {
			log.debug( "addJoinByPathMap() : " + path + " -> " + destination );
		}
		fromElementsByPath.put( path, destination );
	}

	/**
	 * Returns true if the from node contains the class alias name.
	 *
	 * @param alias The HQL class alias name.
	 * @return true if the from node contains the class alias name.
	 */
	public boolean containsClassAlias(String alias) {
		return fromElementByClassAlias.keySet().contains( alias );
	}

	/**
	 * Returns true if the from node contains the table alias name.
	 *
	 * @param alias The SQL table alias name.
	 * @return true if the from node contains the table alias name.
	 */
	public boolean containsTableAlias(String alias) {
		return fromElementByTableAlias.keySet().contains( alias );
	}

	public String getDisplayText() {
		return "FromClause{" +
				"level=" + level +
				", fromElementCounter=" + fromElementCounter +
				", fromElements=" + fromElements.size() +
				", fromElementByClassAlias=" + fromElementByClassAlias.keySet() +
				", fromElementByTableAlias=" + fromElementByTableAlias.keySet() +
				", fromElementsByPath=" + fromElementsByPath.keySet() +
				", collectionJoinFromElementsByPath=" + collectionJoinFromElementsByPath.keySet() +
				", impliedElements=" + impliedElements +
				"}";
	}

	public void setParentFromClause(FromClause parentFromClause) {
		this.parentFromClause = parentFromClause;
		if ( parentFromClause != null ) {
			level = parentFromClause.getLevel() + 1;
			parentFromClause.addChild( this );
		}
	}

	private void addChild(FromClause fromClause) {
		if ( childFromClauses == null ) {
			childFromClauses = new HashSet();
		}
		childFromClauses.add( fromClause );
	}

	public FromClause locateChildFromClauseWithJoinByPath(String path) {
		if ( childFromClauses != null && !childFromClauses.isEmpty() ) {
			Iterator children = childFromClauses.iterator();
			while ( children.hasNext() ) {
				FromClause child = ( FromClause ) children.next();
				if ( child.findJoinByPathLocal( path ) != null ) {
					return child;
				}
			}
		}
		return null;
	}

	public void promoteJoin(FromElement elem) {
		if ( log.isDebugEnabled() ) {
			log.debug( "Promoting [" + elem + "] to [" + this + "]" );
		}
		//TODO: implement functionality
		//  this might be painful to do here, as the "join post processing" for
		//  the subquery has already been performed (meaning that for
		//  theta-join dialects, the join conditions have already been moved
		//  over to the where clause).  A "simple" solution here might to
		//  perform "join post processing" once for the entire query (including
		//  any subqueries) at one fell swoop
	}

	public boolean isSubQuery() {
		return parentFromClause != null;
	}

	void addCollectionJoinFromElementByPath(String path, FromElement destination) {
		if ( log.isDebugEnabled() ) {
			log.debug( "addCollectionJoinFromElementByPath() : " + path + " -> " + destination );
		}
		collectionJoinFromElementsByPath.put( path, destination );	// Add the new node to the map so that we don't create it twice.
	}

	public FromClause getParentFromClause() {
		return parentFromClause;
	}

	public int getLevel() {
		return level;
	}

	public int nextFromElementCounter() {
		return fromElementCounter++;
	}

	void resolve() {
		// Make sure that all from elements registered with this FROM clause are actually in the AST.
		ASTIterator iter = new ASTIterator( this.getFirstChild() );
		Set childrenInTree = new HashSet();
		while ( iter.hasNext() ) {
			childrenInTree.add( iter.next() );
		}
		for ( Iterator iterator = fromElements.iterator(); iterator.hasNext(); ) {
			FromElement fromElement = ( FromElement ) iterator.next();
			if ( !childrenInTree.contains( fromElement ) ) {
				throw new IllegalStateException( "Element not in AST: " + fromElement );
			}
		}
	}

	public void addImpliedFromElement(FromElement element) {
		impliedElements.add( element );
	}

	public String toString() {
		return "FromClause{" +
				"level=" + level +
				"}";
	}
}
