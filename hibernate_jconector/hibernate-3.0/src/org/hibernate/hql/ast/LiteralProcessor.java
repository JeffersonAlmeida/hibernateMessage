// $Id: LiteralProcessor.java,v 1.21 2005/03/30 16:51:17 oneovthafew Exp $
package org.hibernate.hql.ast;

import antlr.SemanticException;
import antlr.collections.AST;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.hql.QueryTranslator;
import org.hibernate.hql.antlr.HqlSqlTokenTypes;
import org.hibernate.hql.antlr.SqlTokenTypes;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.InFragment;
import org.hibernate.type.LiteralType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;
import org.hibernate.util.ReflectHelper;

/**
 * A delegate that handles literals and constants for HqlSqlWalker, performing the token replacement functions and
 * classifying literals.
 *
 * @author josh Sep 2, 2004 7:15:30 AM
 */
class LiteralProcessor implements HqlSqlTokenTypes {
	/**
	 * A logger for this class.
	 */
	private static final Log log = LogFactory.getLog( LiteralProcessor.class );

	private HqlSqlWalker walker;

	public LiteralProcessor(HqlSqlWalker hqlSqlWalker) {
		this.walker = hqlSqlWalker;
	}
	
	boolean isAlias(String alias) {
		FromClause from = walker.getCurrentFromClause();
		while ( from.isSubQuery() ) {
			if ( from.containsClassAlias(alias) ) {
				return true;
			}
			from = from.getParentFromClause();
		}
		return from.containsClassAlias(alias);
	}

	void processConstant(AST constant) throws SemanticException {
		// If the constant is an IDENT, figure out what it means...
		boolean isIdent = ( constant.getType() == IDENT || constant.getType() == WEIRD_IDENT );
		if ( isIdent && isAlias( constant.getText() ) ) { // IDENT is a class alias in the FROM.
			IdentNode ident = ( IdentNode ) constant;
			// Resolve to an identity column.
			ident.resolve(false, true);
		}
		else {	// IDENT might be the name of a class.
			Queryable queryable = walker.getSessionFactoryHelper().findQueryableUsingImports( constant.getText() );
			if ( isIdent && queryable != null ) {
				constant.setText( queryable.getDiscriminatorSQLValue().toString() );
			}
			// Otherwise, it's a literal.
			else {
				processLiteral( constant );
			}
		}
	}

	public void lookupConstant(DotNode node) throws SemanticException {
		String text = getText( node );
		Queryable persister = walker.getSessionFactoryHelper().findQueryableUsingImports( text );
		if ( persister != null ) {
			// the name of an entity class
			final String discrim = persister.getDiscriminatorSQLValue();
			if ( InFragment.NULL.equals(discrim) || InFragment.NOT_NULL.equals(discrim) ) {
				throw new InvalidPathException( "subclass test not allowed for null or not null discriminator: '" + text + "'" );
			}
			else {
				setSQLValue( node, text, discrim ); //the class discriminator value
			}
		}
		else {
			Object value = ReflectHelper.getConstantValue( text );
			if ( value == null ) {
				throw new InvalidPathException( "Invalid path: '" + text + "'" );
			}
			else {
				setConstantValue( node, text, value );
			}
		}
	}
	
	private void setSQLValue(DotNode node, String text, String value) {
		if ( log.isDebugEnabled() ) {
			log.debug( "setSQLValue() " + text + " -> " + value );
		}
		node.setFirstChild( null );	// Chop off the rest of the tree.
		node.setType( SqlTokenTypes.SQL_TOKEN );
		node.setText(value);
		node.setResolvedConstant( text );
	}

	private void setConstantValue(DotNode node, String text, Object value) {
		if ( log.isDebugEnabled() ) {
			log.debug( "setConstantValue() " + text + " -> " + value + " " + value.getClass().getName() );
		}
		node.setFirstChild( null );	// Chop off the rest of the tree.
		if ( value instanceof String ) {
			node.setType( SqlTokenTypes.QUOTED_STRING );
		}
		else if ( value instanceof Integer ) {
			node.setType( SqlTokenTypes.NUM_INT );
		}
		else if ( value instanceof Long ) {
			node.setType( SqlTokenTypes.NUM_LONG );
		}
		else if ( value instanceof Double ) {
			node.setType( SqlTokenTypes.NUM_DOUBLE );
		}
		else if ( value instanceof Float ) {
			node.setType( SqlTokenTypes.NUM_FLOAT );
		}
		else {
			node.setType( SqlTokenTypes.CONSTANT );
		}
		Type type;
		try {
			type = TypeFactory.heuristicType( value.getClass().getName() );
		}
		catch ( MappingException me ) {
			throw new QueryException( me );
		}
		if ( type == null ) throw new QueryException( QueryTranslator.ERROR_CANNOT_DETERMINE_TYPE + node.getText() );
		try {
			LiteralType literalType = ( LiteralType ) type;
			node.setText( literalType.objectToSQLString( value ) );
		}
		catch ( Exception e ) {
			throw new QueryException( QueryTranslator.ERROR_CANNOT_FORMAT_LITERAL + node.getText(), e );
		}
		node.setDataType( type );
		node.setResolvedConstant( text );
	}

	private String getText(AST node) {
		return ASTUtil.getPathText( node );
	}

	public void processBoolean(AST constant) {
		// TODO: something much better - look at the type of the other expression!
		// TODO: Have comparisonExpression and/or arithmeticExpression rules complete the resolution of boolean nodes.
		String replacement = ( String ) walker.getTokenReplacements().get( constant.getText() );
		if ( replacement != null ) constant.setText( replacement );
	}

	private void processLiteral(AST constant) {
		String replacement = ( String ) walker.getTokenReplacements().get( constant.getText() );
		if ( replacement != null ) {
			if ( log.isDebugEnabled() ) {
				log.debug( "processConstant() : Replacing '" + constant.getText() + "' with '" + replacement + "'" );
			}
			constant.setText( replacement );
		}
	}

}
