// $Id: UpdateStatementExecutor.java,v 1.2 2005/02/27 03:09:09 steveebersole Exp $
package org.hibernate.hql.ast;

import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.TypedValue;
import org.hibernate.HibernateException;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.type.Type;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Iterator;

/**
 * Performs exeuction of update/delete statements issued through HQL.
 *
 * @author Steve Ebersole
 */
public class UpdateStatementExecutor {

	private static final Log log = LogFactory.getLog( UpdateStatementExecutor.class );

	private String sql;
	private HqlSqlWalker walker;
	private SessionFactoryImplementor factory;

	/**
	 * Constructs a instance of UpdateStatementExecutor.
	 *
	 * @param sql The sql to be built into PreparedStatement.
	 * @param walker The walker containing the semantic analysis of the parsed HQL.
	 * @param factory
	 */
	public UpdateStatementExecutor(String sql, HqlSqlWalker walker, SessionFactoryImplementor factory) {
		this.sql = sql;
		this.walker = walker;
		this.factory = factory;
	}

	/**
	 * Execute the sql managed by this executor using the given parameters.
	 *
	 * @param parameters Essentially bind information for this processing.
	 * @param session The session originating the request.
	 * @return The number of entities updated/deleted.
	 * @throws HibernateException
	 */
	public int execute(
	        QueryParameters parameters,
	        SessionImplementor session) throws HibernateException {

		PreparedStatement st = null;
		try {
			try {
				st = session.getBatcher().prepareStatement( sql );

				int col = 1;

				col += bindPositionalParameters( st, parameters, col, session );
				col += bindNamedParameters( st, parameters.getNamedParameters(), col, session );

				return st.executeUpdate();
			}
			finally {
				if ( st != null ) {
					session.getBatcher().closeStatement( st );
				}
			}
		}
		catch( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
			        factory.getSQLExceptionConverter(),
			        sqle,
			        "could not execute update query",
			        sql
			);
		}
	}

	// TODO : potentially move these two off into a helper,
	//      as the same processing is done here as well as throughout the Loader hierachy

	private int bindPositionalParameters(final PreparedStatement st,
										   final QueryParameters queryParameters,
										   final int start,
										   final SessionImplementor session)
			throws SQLException, HibernateException {

		final Object[] values = queryParameters.getPositionalParameterValues();
		final Type[] types = queryParameters.getPositionalParameterTypes();
		int span = 0;
		for ( int i = 0; i < values.length; i++ ) {
			types[i].nullSafeSet( st, values[i], start + span, session );
			span += types[i].getColumnSpan( factory );
		}
		return span;
	}

	private int bindNamedParameters(final PreparedStatement ps,
									  final Map namedParams,
									  final int start,
									  final SessionImplementor session)
			throws SQLException, HibernateException {

		if ( namedParams != null ) {
			// assumes that types are all of span 1
			Iterator iter = namedParams.entrySet().iterator();
			int result = 0;
			while ( iter.hasNext() ) {
				Map.Entry e = ( Map.Entry ) iter.next();
				String name = ( String ) e.getKey();
				TypedValue typedval = ( TypedValue ) e.getValue();
				int[] locs = walker.getNamedParameterLocs( name );
				for ( int i = 0; i < locs.length; i++ ) {
					if ( log.isDebugEnabled() ) {
						log.debug( "bindNamedParameters() " +
								typedval.getValue() + " -> " + name +
								" [" + ( locs[i] + start ) + "]" );
					}
					typedval.getType().nullSafeSet( ps, typedval.getValue(), locs[i] + start, session );
				}
				result += locs.length;
			}
			return result;
		}
		else {
			return 0;
		}
	}

}
