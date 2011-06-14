// $Id: ErrorReporter.java,v 1.2 2004/06/29 02:40:43 pgmjsd Exp $
package org.hibernate.hql.ast;

import antlr.RecognitionException;

/**
 * Implementations will report or handle errors invoked by an ANTLR base parser.
 *
 * @author josh Jun 27, 2004 9:49:55 PM
 */
public interface ErrorReporter {
	void reportError(RecognitionException e);

	void reportError(String s);

	void reportWarning(String s);
}
