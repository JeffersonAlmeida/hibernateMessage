// $Id: QueryTranslatorImpl.java,v 1.51 2005/03/27 00:22:14 pgmjsd Exp $
package org.hibernate.hql.ast;

import antlr.ANTLRException;
import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.collections.AST;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.ScrollableResults;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.hql.FilterTranslator;
import org.hibernate.hql.antlr.HqlTokenTypes;
import org.hibernate.hql.antlr.SqlTokenTypes;
import org.hibernate.loader.hql.QueryLoader;
import org.hibernate.type.Type;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A QueryTranslator that uses an AST based parser.
 * <br>User: josh
 * <br>Date: Dec 31, 2003
 * <br>Time: 7:50:35 AM
 *
 * @author Joshua Davis (pgmjsd@sourceforge.net)
 */
public class QueryTranslatorImpl implements FilterTranslator {
	private static final Log log = LogFactory.getLog( QueryTranslatorImpl.class );
	private static final Log AST_LOG = LogFactory.getLog( "org.hibernate.hql.AST" );

	private String hql;
	private boolean shallowQuery;
	private Map tokenReplacements;
	private Map enabledFilters;
	private SessionFactoryImplementor factory;

	private boolean compiled;
	private QueryLoader queryLoader;
	private UpdateStatementExecutor updateStatementExecutor;

	private QueryNode sqlAst;
	private String sql;


	/**
	 * Creates a new AST-based query translator.
	 *
	 * @param query          The HQL query string.
	 * @param enabledFilters Any filters currently enabled for the session.
	 * @param factory        The session factory constructing this translator instance.
	 */
	public QueryTranslatorImpl(String query,
							   Map enabledFilters,
							   SessionFactoryImplementor factory) {
		this.hql = query;
		this.compiled = false;
		this.shallowQuery = false;
		this.enabledFilters = enabledFilters;
		this.factory = factory;
	}

	/**
	 * Compile a "normal" query. This method may be called multiple
	 * times. Subsequent invocations are no-ops.
	 *
	 * @param replacements Defined query substitutions.
	 * @param shallow      Does this represent a shallow (scalar or entity-id) select?
	 * @throws QueryException   There was a problem parsing the query string.
	 * @throws MappingException There was a problem querying defined mappings.
	 */
	public void compile(Map replacements, boolean shallow)
			throws QueryException, MappingException {
		doCompile( replacements, shallow, null );
	}

	/**
	 * Compile a filter. This method may be called multiple
	 * times. Subsequent invocations are no-ops.
	 *
	 * @param collectionRole the role name of the collection used as the basis for the filter.
	 * @param replacements   Defined query substitutions.
	 * @param shallow        Does this represent a shallow (scalar or entity-id) select?
	 * @throws QueryException   There was a problem parsing the query string.
	 * @throws MappingException There was a problem querying defined mappings.
	 */
	public void compile(String collectionRole, Map replacements, boolean shallow)
			throws QueryException, MappingException {
		doCompile( replacements, shallow, collectionRole );
	}

	/**
	 * Performs both filter and non-filter compiling.
	 *
	 * @param replacements   Defined query substitutions.
	 * @param shallow        Does this represent a shallow (scalar or entity-id) select?
	 * @param collectionRole the role name of the collection used as the basis for the filter, NULL if this
	 *                       is not a filter.
	 */
	private synchronized void doCompile(Map replacements, boolean shallow, String collectionRole) {
		// If the query is already compiled, skip the compilation.
		if ( compiled ) {
			if ( log.isDebugEnabled() ) {
				log.debug( "compile() : The query is already compiled, skipping..." );
			}
			return;
		}

		// Remember the parameters for the compilation.
		this.tokenReplacements = replacements;
		if ( tokenReplacements == null ) {
			tokenReplacements = new HashMap();
		}
		this.shallowQuery = shallow;

		try {
			// PHASE 1 : Parse the HQL into an AST.
			HqlParser parser = parse( true );

			// PHASE 2 : Analyze the HQL AST, and produce an SQL AST.
			HqlSqlWalker w = analyze( parser, collectionRole );

			sqlAst = ( QueryNode ) w.getAST();

			// at some point the generate phase needs to be moved out of here,
			// because a single object-level DML might spawn multiple SQL DML
			// command executions.
			//
			// Possible to just move the sql generation for dml stuff, but for
			// consistency-sake probably best to just move responsiblity for
			// the generation phase completely into the delegates
			// (QueryLoader/UpdateStatementExecutor) themselves.  Also, not sure why
			// QueryLoader currently even has a dependency on this at all; does
			// it need it?  Ideally like to see the walker itself given to the delegates directly...

			// PHASE 3 : Generate the SQL.
			generate( sqlAst );

			if ( sqlAst.isDML() ) {
				updateStatementExecutor = new UpdateStatementExecutor( sql, getWalker(), factory );
			}
			else {
				queryLoader = new QueryLoader( this, factory, w.getSelectClause() );
			}

			compiled = true;
		}
		catch ( QueryException qe ) {
			qe.setQueryString( hql );
			throw qe;
		}
		catch ( RecognitionException e ) {
			throw  new QuerySyntaxError( e, hql );
		}
		catch ( ANTLRException e ) {
			QueryException qe = new QueryException( e.getMessage(), e );
			qe.setQueryString( hql );
			throw qe;
		}
	}

	private void generate(AST sqlAst) throws QueryException, RecognitionException {
		if ( sql == null ) {
			SqlGenerator gen = new SqlGenerator();
			gen.statement( sqlAst );
			sql = gen.getSQL();
			if ( log.isDebugEnabled() ) {
				log.debug( "HQL: " + hql );
				log.debug( "SQL: " + sql );
			}
			gen.getParseErrorHandler().throwQueryException();
		}
	}

	private HqlSqlWalker analyze(HqlParser parser, String collectionRole) throws QueryException, RecognitionException {
		HqlSqlWalker w = new HqlSqlWalker( this, factory, parser, tokenReplacements, collectionRole );
		AST hqlAst = parser.getAST();

		// Transform the tree.
		w.statement( hqlAst );

		if ( AST_LOG.isDebugEnabled() ) {
			ASTPrinter printer = new ASTPrinter( SqlTokenTypes.class );
			AST_LOG.debug( printer.showAsString( w.getAST(), "--- SQL AST ---" ) );
		}

		w.getParseErrorHandler().throwQueryException();

		return w;
	}

	private HqlParser parse(boolean filter) throws TokenStreamException, RecognitionException {
		// Parse the query string into an HQL AST.
		HqlParser parser = HqlParser.getInstance( hql );
		parser.setFilter( filter );

		if ( log.isDebugEnabled() ) {
			log.debug( "parse() - HQL: " + hql );
		}
		parser.statement();

		AST hqlAst = parser.getAST();

		showHqlAst( hqlAst );

		parser.getParseErrorHandler().throwQueryException();
		return parser;
	}

	void showHqlAst(AST hqlAst) {
		if ( AST_LOG.isDebugEnabled() ) {
			ASTPrinter printer = new ASTPrinter( HqlTokenTypes.class );
			printer.setShowClassNames( false ); // The class names aren't interesting in the first tree.
			AST_LOG.debug( printer.showAsString( hqlAst, "--- HQL AST ---" ) );
		}
	}

	private void errorIfDML() throws HibernateException {
		if ( sqlAst.isDML() ) {
			throw new HibernateException( "Not supported for DML operations" );
		}
	}

	private void errorIfSelect() throws HibernateException {
		if ( !sqlAst.isDML() ) {
			throw new HibernateException( "Not supported for select queries" );
		}
	}

	private HqlSqlWalker getWalker() {
		return sqlAst.getWalker();
	}

	/**
	 * Types of the return values of an <tt>iterate()</tt> style query.
	 *
	 * @return an array of <tt>Type</tt>s.
	 */
	public Type[] getReturnTypes() {
		errorIfDML();
		return getWalker().getReturnTypes();
	}

	public String[][] getColumnNames() {
		errorIfDML();
		return getWalker().getSelectClause().getColumnNames();
	}

	public Set getQuerySpaces() {
		return getWalker().getQuerySpaces();
	}

	public List list(SessionImplementor session, QueryParameters queryParameters)
			throws HibernateException {
		// Delegate to the QueryLoader...
		errorIfDML();
		return queryLoader.list( session, queryParameters );
	}

	/**
	 * Return the query results as an iterator
	 */
	public Iterator iterate(QueryParameters queryParameters, SessionImplementor session)
			throws HibernateException {
		// Delegate to the QueryLoader...
		errorIfDML();
		return queryLoader.iterate( queryParameters, session );
	}

	/**
	 * Return the query results, as an instance of <tt>ScrollableResults</tt>
	 */
	public ScrollableResults scroll(QueryParameters queryParameters, SessionImplementor session)
			throws HibernateException {
		// Delegate to the QueryLoader...
		errorIfDML();
		return queryLoader.scroll( queryParameters, session );
	}

	public int executeUpdate(QueryParameters queryParameters, SessionImplementor session)
			throws HibernateException {
		errorIfSelect();
		return updateStatementExecutor.execute( queryParameters, session );
	}

	/**
	 * The SQL query string to be called; implemented by all subclasses
	 */
	public String getSQLString() {
		return sql;
	}

	// -- Package local methods for the QueryLoader delegate --

	boolean isShallowQuery() {
		return shallowQuery;
	}

	public String getQueryString() {
		return hql;
	}

	public Map getEnabledFilters() {
		return enabledFilters;
	}

	public int[] getNamedParameterLocs(String name) {
		return getWalker().getNamedParameterLocs( name );
	}
}
