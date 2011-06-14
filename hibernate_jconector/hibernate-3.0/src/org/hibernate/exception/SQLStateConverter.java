// $Id: SQLStateConverter.java,v 1.4 2005/03/16 15:43:46 oneovthafew Exp $
package org.hibernate.exception;

import org.hibernate.JDBCException;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * A SQLExceptionConverter implementation which performs converion based on
 * the underlying SQLState.  Interpretation of a SQL error based on SQLState
 * is not nearly as accurate as using the ErrorCode (which is, however, vendor-
 * specific).  Use of a ErrorCcode-based converter should be preferred approach
 * for converting/interpreting SQLExceptions.
 *
 * @author Steve Ebersole
 */
public class SQLStateConverter implements SQLExceptionConverter {

	private ViolatedConstraintNameExtracter extracter;

	private static final Set SQL_GRAMMAR_CATEGORIES = new HashSet();
	private static final Set INTEGRITY_VIOLATION_CATEGORIES = new HashSet();
	private static final Set CONNECTION_CATEGORIES = new HashSet();

	static {
		SQL_GRAMMAR_CATEGORIES.add( "07" );
		SQL_GRAMMAR_CATEGORIES.add( "37" );
		SQL_GRAMMAR_CATEGORIES.add( "42" );
		SQL_GRAMMAR_CATEGORIES.add( "65" );
		SQL_GRAMMAR_CATEGORIES.add( "S0" );

		INTEGRITY_VIOLATION_CATEGORIES.add( "23" );
		INTEGRITY_VIOLATION_CATEGORIES.add( "27" );
		INTEGRITY_VIOLATION_CATEGORIES.add( "44" );

		CONNECTION_CATEGORIES.add( "08" );
	}

	public SQLStateConverter(ViolatedConstraintNameExtracter extracter) {
		this.extracter = extracter;
	}

	/**
	 * Convert the given SQLException into Hibernate's JDBCException hierarchy.
	 *
	 * @param sqlException The SQLException to be converted.
	 * @param message      An optional error message.
	 * @param sql          Optionally, the sql being performed when the exception occurred.
	 * @return The resulting JDBCException.
	 */
	public JDBCException convert(SQLException sqlException, String message, String sql) {

		String sqlStateClassCode = JDBCExceptionHelper.extractSqlStateClassCode( sqlException );

		if ( sqlStateClassCode != null ) {
			if ( SQL_GRAMMAR_CATEGORIES.contains( sqlStateClassCode ) ) {
				return new SQLGrammarException( message, sqlException, sql );
			}
			else if ( INTEGRITY_VIOLATION_CATEGORIES.contains( sqlStateClassCode ) ) {
				String constraintName = extracter.extractConstraintName( sqlException );
				return new ConstraintViolationException( message, sqlException, sql, constraintName );
			}
			else if ( CONNECTION_CATEGORIES.contains( sqlStateClassCode ) ) {
				return new JDBCConnectionException( message, sqlException, sql );
			}
		}

		return handledNonSpecificException( sqlException, message, sql );
	}

	/**
	 * Handle an exception not converted to a specific type based on the SQLState.
	 *
	 * @param sqlException The exception to be handled.
	 * @param message      An optional message
	 * @param sql          Optionally, the sql being performed when the exception occurred.
	 * @return The converted exception; should <b>never</b> be null.
	 */
	protected JDBCException handledNonSpecificException(SQLException sqlException, String message, String sql) {
		return new GenericJDBCException( message, sqlException, sql );
	}
}
