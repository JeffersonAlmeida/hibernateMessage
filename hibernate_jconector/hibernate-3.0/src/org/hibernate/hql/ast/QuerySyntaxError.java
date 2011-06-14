// $Id: QuerySyntaxError.java,v 1.5 2005/02/12 07:19:20 steveebersole Exp $
package org.hibernate.hql.ast;

import antlr.RecognitionException;
import org.hibernate.QueryException;

/**
 * Exception thrown when there is a syntax error in the HQL.
 *
 * @author josh Dec 5, 2004 7:22:54 PM
 */
public class QuerySyntaxError extends QueryException {
	public QuerySyntaxError(RecognitionException e) {
		super( e.getMessage() + (
				( e.getLine() > 0 && e.getColumn() > 0 ) ?
				( " near line " + e.getLine() + ", column " + e.getColumn() ) : ""
				), e );
	}

	public QuerySyntaxError(RecognitionException e, String hql) {
		super( e.getMessage() + (
				( e.getLine() > 0 && e.getColumn() > 0 ) ?
				( " near line " + e.getLine() + ", column " + e.getColumn() ) : ""
				), e );
		setQueryString( hql );
	}
}
