// $Id: ErrorCounter.java,v 1.7 2005/03/11 11:29:17 pgmjsd Exp $
package org.hibernate.hql.ast;

import antlr.RecognitionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.QueryException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An error handler that counts parsing errors and warnings.
 */
public class ErrorCounter implements ParseErrorHandler {
	private Log log = LogFactory.getLog( ErrorCounter.class );
	private Log hqlLog = LogFactory.getLog( "org.hibernate.hql.PARSER" );

	private List errorList = new ArrayList();
	private List warningList = new ArrayList();
	private List recognitionExceptions = new ArrayList();

	public void reportError(RecognitionException e) {
		reportError( e.toString() );
		recognitionExceptions.add( e );
		if ( log.isDebugEnabled() ) {
			log.debug( e, e );
		}
	}

	public void reportError(String s) {
//		String message = ( getFilename() == null ) ? "*** ERROR: " + s : getFilename() + ": *** ERROR: " + s;
		String message = "*** ERROR: " + s;
		hqlLog.error( message );
		errorList.add( message );
	}

	public int getErrorCount() {
		return errorList.size();
	}

	public void reportWarning(String s) {
//		String message = ( getFilename() == null ) ? "*** WARNING: " + s : getFilename() + ": *** WARNING: " + s;
		String message = "*** WARNING: " + s;
		hqlLog.warn( message );
		warningList.add( message );
	}

	private String getErrorString() {
		StringBuffer buf = new StringBuffer();
		for ( Iterator iterator = errorList.iterator(); iterator.hasNext(); ) {
			buf.append( ( String ) iterator.next() );
			if ( iterator.hasNext() ) buf.append( "\n" );

		}
		return buf.toString();
	}

	public void throwQueryException() throws QueryException {
		if ( getErrorCount() > 0 ) {
			if ( recognitionExceptions.size() > 0 ) {
				throw new QuerySyntaxError( ( RecognitionException ) recognitionExceptions.get( 0 ) );
			}
			else {
				throw new QueryException( getErrorString() );
			}
		}
		else {
			// all clear
			if ( log.isDebugEnabled() ) {
				log.debug( "throwQueryException() : no errors" );
			}
		}
	}
}
