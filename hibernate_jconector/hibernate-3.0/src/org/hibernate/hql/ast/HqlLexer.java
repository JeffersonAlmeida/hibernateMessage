// $Id: HqlLexer.java,v 1.11 2005/01/03 14:43:07 pgmjsd Exp $
package org.hibernate.hql.ast;

import antlr.Token;
import org.hibernate.hql.antlr.HqlBaseLexer;

import java.io.InputStream;

/**
 * Custom lexer for the HQL grammar.  Extends the base lexer generated by ANTLR
 * in order to keep the grammar source file clean.
 */
class HqlLexer extends HqlBaseLexer {
	/**
	 * A logger for this class. *
	 */
	private boolean possibleID = false;

	public HqlLexer(InputStream in) {
		super( in );
	}

	public void setTokenObjectClass(String cl) {
		// Ignore the token class name parameter, and use a specific token class.
		super.setTokenObjectClass( HqlToken.class.getName() );
	}

	protected void setPossibleID(boolean possibleID) {
		this.possibleID = possibleID;
	}

	protected Token makeToken(int i) {
		HqlToken token = ( HqlToken ) super.makeToken( i );
		token.setPossibleID( possibleID );
		possibleID = false;
		return token;
	}

	public int testLiteralsTable(int i) {
		int ttype = super.testLiteralsTable( i );
		return ttype;
	}

}
