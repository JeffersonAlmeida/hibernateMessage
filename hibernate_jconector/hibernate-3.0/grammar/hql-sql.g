header
{
// $Id: hql-sql.g,v 1.77 2005/03/31 16:29:13 steveebersole Exp $
package org.hibernate.hql.antlr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
}

/**
 * Hibernate Query Language to SQL Tree Transform.<br>
 * This is a tree grammar that transforms an HQL AST into a intermediate SQL AST
 * with bindings to Hibernate interfaces (Queryable, etc.).  The Hibernate specific methods
 * are all implemented in the HqlSqlWalker subclass, allowing the ANTLR-generated class
 * to have only the minimum dependencies on the Hibernate code base.   This will also allow
 * the sub-class to be easily edited using an IDE (most IDE's don't support ANTLR).
 * <br>
 * <i>NOTE:</i> The java class is generated from hql-sql.g by ANTLR.
 * <i>DO NOT EDIT THE GENERATED JAVA SOURCE CODE.</i>
 * @author Joshua Davis (joshua@hibernate.org)
 */
class HqlSqlBaseWalker extends TreeParser;

options
{
	// Note: importVocab and exportVocab cause ANTLR to share the token type numbers between the
	// two grammars.  This means that the token type constants from the source tree are the same
	// as those in the target tree.  If this is not the case, tree translation can result in
	// token types from the *source* tree being present in the target tree.
	importVocab=Hql;        // import definitions from "Hql"
	exportVocab=HqlSql;     // Call the resulting definitions "HqlSql"
	buildAST=true;
}

tokens
{
	FROM_FRAGMENT;	// A fragment of SQL that represents a table reference in a FROM clause.
	IMPLIED_FROM;	// An implied FROM element.
	JOIN_FRAGMENT;	// A JOIN fragment.
	SELECT_CLAUSE;
	LEFT_OUTER;
	RIGHT_OUTER;
	ALIAS_REF;      // An IDENT that is a reference to an entity via it's alias.
	PROPERTY_REF;   // A DOT that is a reference to a property in an entity.
	SQL_TOKEN;      // A chunk of SQL that is 'rendered' already.
	SELECT_COLUMNS; // A chunk of SQL representing a bunch of select columns.
	SELECT_EXPR;    // A select expression, generated from a FROM element.
	THETA_JOINS;	// Root of theta join condition subtree.
	FILTERS;		// Root of the filters condition subtree.
	METHOD_NAME;    // An IDENT that is a method name.
	NAMED_PARAM;    // A named parameter (:foo).
	BOGUS;          // Used for error state detection, etc.
}

// -- Declarations --
{
	private static Log log = LogFactory.getLog(HqlSqlBaseWalker.class);

	// NOTE: The real implementations are in the subclass.

	// Context flags.

	private int level = 0;

	private boolean inSelect = false;
	
	private boolean inFunctionCall = false;

	private boolean inFrom = false;

	private int statementType = SELECT;

	public boolean isSubQuery() { return level > 1; }

	public boolean isInFrom() { return inFrom; }

	public boolean isInFunctionCall() { return inFunctionCall; }
	
	public boolean isInSelect() { return inSelect; }

	public int getStatementType() { return statementType; }

	public boolean isSelectStatement() { return statementType == SELECT; }
	
	/** Pre-process the from clause input tree. **/
	protected void prepareFromClauseInputTree(AST fromClauseInput) {}

	/** Sets the current 'FROM' context. **/
	protected void pushFromClause(AST fromClause,AST inputFromNode) {}

	protected AST createFromElement(String path,AST alias) throws SemanticException {
		return null;
	}

	protected void createFromJoinElement(AST path,AST alias,int joinType,AST fetch) throws SemanticException {}

	protected AST createFromFilterElement(AST filterEntity,AST alias) throws SemanticException	{
		return null;
	}

	protected void processQuery(AST select,AST query) throws SemanticException { }

	protected void postProcessUpdate(AST update) throws SemanticException { }

	protected void postProcessDelete(AST delete) throws SemanticException { }

	protected void beforeSelectClause() throws SemanticException { }

	protected void processIndex(AST indexOp) throws SemanticException { }

	protected void processConstant(AST constant) throws SemanticException { }

	protected void processBoolean(AST constant) throws SemanticException { }

	protected void resolve(AST node) throws SemanticException { }

	protected void resolveSelectExpression(AST dotNode) throws SemanticException { }

	protected void processFunction(AST functionCall,boolean inSelect) throws SemanticException { }

	protected void processConstructor(AST constructor) throws SemanticException { }

	protected void namedParameter(AST namedParameter) throws SemanticException { }

	protected void positionalParameter(AST namedParameter) throws SemanticException { }

	protected void lookupAlias(AST ident) throws SemanticException { }

	protected AST lookupProperty(AST dot,boolean root,boolean inSelect) throws SemanticException {
		return dot;
	}

	protected void setImpliedJoinType(int joinType) { }

	protected void beforeQuery(String ruleName) throws SemanticException {
		inFunctionCall = false;
		level++;
		if (log.isDebugEnabled())
			log.debug(ruleName + "() << begin, level = " + level);
	}

	protected void afterQuery(String ruleName,AST s,AST query) throws SemanticException {
		if (log.isDebugEnabled())
			log.debug(ruleName + "() : finishing up , level = " + level);
		processQuery(s,query);
		if (log.isDebugEnabled())
			log.debug(ruleName + "() >> end, level = " + level);
		level--;
	}
}

// The main statement rule.
statement
	: selectStatement
	| u:updateStatement { postProcessUpdate(#u); }
	| d:deleteStatement { postProcessDelete(#d); }
	;

selectStatement
	: query
	;

// Cannot use just the fromElement rule here in the update and delete queries
// because fromElement essentially relies on a FromClause already having been
// built...
updateStatement
	: #( UPDATE { statementType = UPDATE; } fromClause setClause (whereClause)? )
	;

deleteStatement
	: #( DELETE { statementType = DELETE; } fromClause (whereClause)? )
	;

setClause
	: #( SET (assignment)* )
	;

assignment
	: #( EQ (nonNestedPropertyRef) (newValue) )
	;

nonNestedPropertyRef
	: ae:addrExpr [ true ] { resolve(#ae); }
	;

// For now, just use expr.  Revisit after ejb3 solidifies this.
newValue
	: expr
	;

// The query / subquery rule.  Pops the current 'from node' context (list
// of aliases.
query!	{
		beforeQuery("query");
	}
	: #( QUERY
			// The first phase places the FROM first to make processing the SELECT simpler.
			#(SELECT_FROM
				f:fromClause
				(s:selectClause)?
			)
			(w:whereClause)?
			(g:groupClause)?
			(o:orderClause)?
		) {
		// Antlr note: #x_in refers to the input AST, #x refers to the output AST
		#query = #([SELECT,"SELECT"], #s , #f, #w, #g, #o);
		afterQuery("query",#s,#query);
	}
	;

orderClause
	: #(ORDER orderExprs)
	;

orderExprs
	: expr ( ASCENDING | DESCENDING )? (orderExprs)?
	;

groupClause
	: #(GROUP (expr)+ ( #(HAVING logicalExpr) )? )
	;

selectClause! {
		beforeSelectClause();
	}
	: #(SELECT (d:DISTINCT)? x:selectExprList ) {
		#selectClause = #([SELECT_CLAUSE,"{select clause}"], #d, #x);
	}
	;

selectExprList {
		boolean oldInSelect = inSelect;
		inSelect = true;
	}
	: (selectExpr)+ {
		inSelect = oldInSelect;
	}
	;

selectExpr
	: p:propertyRef					{ resolveSelectExpression(#p); }
	| #(ALL ar2:aliasRef) 			{ resolveSelectExpression(#ar2); #selectExpr = #ar2; }
	| #(OBJECT ar3:aliasRef)		{ resolveSelectExpression(#ar3); #selectExpr = #ar3; }
	| con:constructor 				{ processConstructor(#con); }
	// ANTLR Note: The <rule> [ expr ] syntax passes parameters to <rule>
	| functionCall
	| count
	| collectionFunction			// elements() or indices()
	| literal
	;

count
	: #(COUNT ( DISTINCT | ALL )? ( aggregateExpr | STAR ) )
	;

constructor
	{ String className = null; }
	: #(CONSTRUCTOR className=path (selectExpr)* )
	;

aggregateExpr
	: p:propertyRef { resolve(#p); }
	| collectionFunction
	;

// Establishes the list of aliases being used by this query.
fromClause {
		// NOTE: This references the INPUT AST! (see http://www.antlr.org/doc/trees.html#Action%20Translation)  the
		// ouput AST (#fromClause) has not been built yet.
		prepareFromClauseInputTree(#fromClause_in);
	}
	: #(f:FROM { pushFromClause(#fromClause,f); } fromElementList )
	;

fromElementList {
		boolean oldInFrom = inFrom;
		inFrom = true;
		}
	: (fromElement)+ {
		inFrom = oldInFrom;
		}
	;

fromElement! {
	String p = null;
	}
	// A simple class name, alias element.
	: p=path (a:ALIAS)? {
		#fromElement = createFromElement(p,a);
	}
	| je:joinElement {
		#fromElement = #je;
	}
	// A from element created due to filter compilation
	| fe:FILTER_ENTITY a3:ALIAS {
		#fromElement = createFromFilterElement(fe,a3);
	}
	;

joinElement! {
		int j = INNER;
	}
	// A from element with a join.  This time, the 'path' should be treated as an AST
	// and resolved (like any path in a WHERE clause).   Make sure all implied joins
	// generated by the property ref use the join type, if it was specified.
	: #(JOIN (j=joinType { setImpliedJoinType(j); } )? (f:FETCH)? ref:propertyRef (a:ALIAS)? ) {
		createFromJoinElement(#ref,a,j,f);
		setImpliedJoinType(INNER);	// Reset the implied join type.
	}
	;

// Returns an node type integer that represents the join type
// tokens.
joinType returns [int j] {
	j = INNER;
	}
	: ( (left:LEFT | right:RIGHT) (outer:OUTER)? ) {
		if (left != null)       j = LEFT_OUTER;
		else if (right != null) j = RIGHT_OUTER;
		else if (outer != null) j = RIGHT_OUTER;
	}
	| FULL {
		j = FULL;
	}
	| INNER {
		j = INNER;
	}
	;

// Matches a path and returns the normalized string for the path (usually
// fully qualified a class name).
path returns [String p] {
	p = "???";
	String x = "?x?";
	}
	: a:identifier { p = a.getText(); }
	| #(DOT x=path y:identifier) {
			StringBuffer buf = new StringBuffer();
			buf.append(x).append(".").append(y.getText());
			p = buf.toString();
		}
	;

whereClause
	: #(w:WHERE b:logicalExpr ) {
		// Use the *output* AST for the boolean expression!
		#whereClause = #(w , #b);
	}
	;

logicalExpr
	: #(AND logicalExpr logicalExpr)
	| #(OR logicalExpr logicalExpr)
	| #(NOT logicalExpr)
	| comparisonExpr
	;

// TODO: Add any other comparison operators here.
comparisonExpr
	: #(EQ exprOrSubquery exprOrSubquery)
	| #(NE exprOrSubquery exprOrSubquery)
	| #(LT exprOrSubquery exprOrSubquery)
	| #(GT exprOrSubquery exprOrSubquery)
	| #(LE exprOrSubquery exprOrSubquery)
	| #(GE exprOrSubquery exprOrSubquery)
	| #(LIKE expr expr ( #(ESCAPE expr) )? )
	| #(NOT_LIKE expr expr ( #(ESCAPE expr) )? )
	| #(BETWEEN expr expr expr)
	| #(NOT_BETWEEN expr expr expr)
	| #(IN expr inList )
	| #(NOT_IN expr inList )
	| #(IS_NULL expr)
//	| #(IS_TRUE expr)
//	| #(IS_FALSE expr)
	| #(IS_NOT_NULL expr)
	| #(EXISTS ( expr | collectionFunctionOrSubselect ) )
	;

inList
	: #(IN_LIST ( collectionFunctionOrSubselect | ( (expr)* ) ) )
	;
	
exprOrSubquery
	: expr
	| query
	| #(ANY collectionFunctionOrSubselect)
	| #(ALL collectionFunctionOrSubselect)
	| #(SOME collectionFunctionOrSubselect)
	;
	
collectionFunctionOrSubselect
	: collectionFunction
	| query
	;
	
expr
	: ae:addrExpr [ true ] { resolve(#ae); }	// Resolve the top level 'address expression'
	| constant
	| arithmeticExpr
	| functionCall							// Function call, not in the SELECT clause.
	| parameter
	| count										// Count, not in the SELECT clause.
	;

arithmeticExpr
	: #(PLUS expr expr)
	| #(MINUS expr expr)
	| #(DIV expr expr)
	| #(STAR expr expr)
	| #(CONCAT expr (expr)+ )
	| #(UNARY_MINUS expr)
	| caseExpr
	;

caseExpr
	: #(CASE (#(WHEN logicalExpr expr))+ (#(ELSE expr))?)
	;

//TODO: I don't think we need this anymore .. how is it different to 
//      maxelements, etc, which are handled by functionCall
collectionFunction
	: #(e:ELEMENTS {inFunctionCall=true;} p1:propertyRef { resolve(#p1); } ) { processFunction(#e,inSelect); } {inFunctionCall=false;}
	| #(i:INDICES {inFunctionCall=true;} p2:propertyRef { resolve(#p2); } ) { processFunction(#i,inSelect); } {inFunctionCall=false;}
	;

functionCall
	: #(METHOD_CALL  {inFunctionCall=true;} identifier ( #(EXPR_LIST (expr)* ) )? ) { processFunction(#functionCall,inSelect); } {inFunctionCall=false;}
	| #(AGGREGATE aggregateExpr )
	;

constant
	: literal
	| NULL
	| TRUE { processBoolean(#constant); } 
	| FALSE { processBoolean(#constant); }
	;

literal
	: NUM_INT
	| NUM_FLOAT
	| NUM_LONG
	| NUM_DOUBLE
	| QUOTED_STRING
	;

identifier
	: (IDENT | WEIRD_IDENT)
	;

addrExpr! [ boolean root ]
	: #(d:DOT lhs:addrExprLhs rhs:propertyName )	{
		// This gives lookupProperty() a chance to transform the tree to process collection properties (.elements, etc).
		#addrExpr = #(#d, #lhs, #rhs);
		#addrExpr = lookupProperty(#addrExpr,root,false);
	}
	| #(i:INDEX_OP lhs2:addrExprLhs rhs2:expr)	{
		#addrExpr = #(#i, #lhs2, #rhs2);
		processIndex(#addrExpr);
	}
	| p:identifier {
		#addrExpr = #p;
		resolve(#addrExpr);
	}
	;

addrExprLhs
	: addrExpr [ false ]
	;

propertyName
	: identifier
	| CLASS
	| ELEMENTS
	| INDICES
	;

propertyRef!
	: #(d:DOT lhs:propertyRefLhs rhs:propertyName )	{
		// This gives lookupProperty() a chance to transform the tree to process collection properties (.elements, etc).
		#propertyRef = #(#d, #lhs, #rhs);
		#propertyRef = lookupProperty(#propertyRef,false,true);
	}
	|
	p:identifier {
		resolve(#p);
		#propertyRef = #p;
	}
	;

propertyRefLhs
	: propertyRef
	;

aliasRef!
	: i:identifier {
		#aliasRef = #([ALIAS_REF,i.getText()]);	// Create an ALIAS_REF node instead of an IDENT node.
		lookupAlias(#aliasRef);
		}
	;

parameter!
	: #(COLON a:identifier) {
			#parameter = #([NAMED_PARAM,a.getText()]);  // Create a NAMED_PARAM node instead of (COLON IDENT).
			namedParameter(#parameter);
		}
	| p:PARAM {
			#parameter = #([PARAM,"?"]);		// Must create a node here, since the rule doesn't work.
			positionalParameter(#parameter);
		}
	;