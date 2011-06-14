// $Id: SqlGenerator.java,v 1.23 2005/03/04 16:27:34 oneovthafew Exp $
package org.hibernate.hql.ast;

import antlr.RecognitionException;
import antlr.collections.AST;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.hql.antlr.SqlGeneratorBase;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Generates SQL by overriding callback methods in the base class, which does
 * the actual SQL AST walking.
 *
 * @author josh Jun 23, 2004 6:49:55 AM
 */
class SqlGenerator extends SqlGeneratorBase implements ErrorReporter {
	/**
	 * Handles parser errors.
	 */
	private ParseErrorHandler parseErrorHandler;

	/**
	 * all append invocations on the buf should go through this Output instance variable.
	 * The value of this variable may be temporarily substitued by sql function processing code
	 * to catch generated arguments.
	 * This is because sql function templates need arguments as seperate string chunks
	 * that will be assembled into the target dialect-specific function call.
	 */
	private SqlWriter writer = new DefaultWriter();

	private LinkedList outputStack = new LinkedList();

	protected void out(String s) {
		writer.clause( s );
	}

	protected void commaBetweenParameters(String comma) {
		writer.commaBetweenParameters( comma );
	}

	public void reportError(RecognitionException e) {
		parseErrorHandler.reportError( e ); // Use the delegate.
	}

	public void reportError(String s) {
		parseErrorHandler.reportError( s ); // Use the delegate.
	}

	public void reportWarning(String s) {
		parseErrorHandler.reportWarning( s );
	}

	public ParseErrorHandler getParseErrorHandler() {
		return parseErrorHandler;
	}

	public SqlGenerator() {
		super();
		parseErrorHandler = new ErrorCounter();
	}

	public String getSQL() {
		return getStringBuffer().toString();
	}

	protected void optionalSpace() {
		int c = getLastChar();
		switch ( c ) {
			case -1:
				return;
			case ' ':
				return;
			case ')':
				return;
			case '(':
				return;
			default:
				out( " " );
		}
	}

	protected void beginFunctionTemplate(AST m, AST i) {
		MethodNode methodNode = ( MethodNode ) m;
		SQLFunction template = methodNode.getSQLFunction();
		if ( template == null ) {
			// if template is null we just write the function out as it appears in the hql statement
			super.beginFunctionTemplate( m, i );
		}
		else {
			// this function has a template -> redirect output and catch the arguments
			outputStack.addFirst( writer );
			writer = new FunctionArguments();
		}
	}

	protected void endFunctionTemplate(AST m) {
		MethodNode methodNode = ( MethodNode ) m;
		SQLFunction template = methodNode.getSQLFunction();
		if ( template == null ) {
			super.endFunctionTemplate( m );
		}
		else {
			// this function has a template -> restore output, apply the template and write the result out
			FunctionArguments functionArguments = ( FunctionArguments ) writer;   // TODO: Downcast to avoid using an interface?  Yuck.
			writer = ( SqlWriter ) outputStack.removeFirst();
			out( template.render( functionArguments.getArgs() ) );
		}
	}

	// --- Inner classes (moved here from sql-gen.g) ---

	/**
	 * Writes SQL fragments.
	 */
	interface SqlWriter {
		void clause(String clause);

		/**
		 * todo remove this hack
		 * The parameter is either ", " or " , ". This is needed to pass sql generating tests as the old
		 * sql generator uses " , " in the WHERE and ", " in SELECT.
		 *
		 * @param comma either " , " or ", "
		 */
		void commaBetweenParameters(String comma);
	}

	/**
	 * SQL function processing code redirects generated SQL output to an instance of this class
	 * which catches function arguments.
	 */
	class FunctionArguments implements SqlWriter {
		private int argInd;
		private final List args = new ArrayList( 3 );

		public void clause(String clause) {
			if ( argInd == args.size() ) {
				args.add( clause );
			}
			else {
				args.set( argInd, args.get( argInd ) + clause );
			}
		}

		public void commaBetweenParameters(String comma) {
			++argInd;
		}

		public List getArgs() {
			return args;
		}
	}

	/**
	 * The default SQL writer.
	 */
	class DefaultWriter implements SqlWriter {
		public void clause(String clause) {
			getStringBuffer().append( clause );
		}

		public void commaBetweenParameters(String comma) {
			getStringBuffer().append( comma );
		}
	}


}
