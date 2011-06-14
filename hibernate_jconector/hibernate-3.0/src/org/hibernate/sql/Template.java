//$Id: Template.java,v 1.10 2005/02/12 07:19:46 steveebersole Exp $
package org.hibernate.sql;

import java.util.HashSet;
import java.util.StringTokenizer;

import org.hibernate.dialect.Dialect;

/**
 * Parses SQL fragments specified in mapping documents
 *
 * @author Gavin King
 */
public final class Template {

	private static final java.util.Set KEYWORDS = new HashSet();
	private static final java.util.Set BEFORE_TABLE_KEYWORDS = new HashSet();
	static {
		KEYWORDS.add("and");
		KEYWORDS.add("or");
		KEYWORDS.add("not");
		KEYWORDS.add("like");
		KEYWORDS.add("is");
		KEYWORDS.add("in");
		KEYWORDS.add("between");
		KEYWORDS.add("null");
		KEYWORDS.add("select");
		KEYWORDS.add("distinct");
		KEYWORDS.add("from");
		KEYWORDS.add("join");
		KEYWORDS.add("inner");
		KEYWORDS.add("outer");
		KEYWORDS.add("left");
		KEYWORDS.add("right");
		KEYWORDS.add("on");
		KEYWORDS.add("where");
		KEYWORDS.add("having");
		KEYWORDS.add("group");
		KEYWORDS.add("order");
		KEYWORDS.add("by");
		KEYWORDS.add("desc");
		KEYWORDS.add("asc");
		KEYWORDS.add("limit");
		KEYWORDS.add("case");
		KEYWORDS.add("when");
		KEYWORDS.add("then");
		KEYWORDS.add("else");
		KEYWORDS.add("end");
		KEYWORDS.add("any");
		KEYWORDS.add("some");
		KEYWORDS.add("exists");
		KEYWORDS.add("all");
		BEFORE_TABLE_KEYWORDS.add("from");
		BEFORE_TABLE_KEYWORDS.add("join");
	}

	public static final String TEMPLATE = "$PlaceHolder$";

	private Template() {}

	public static String renderWhereStringTemplate(String sqlWhereString, Dialect dialect) {
		return renderWhereStringTemplate(sqlWhereString, TEMPLATE, dialect);
	}

	public static String renderWhereStringTemplate(String sqlWhereString, String placeholder, Dialect dialect) {
		// takes the where condition provided in the mapping
		// attribute and interpolates the alias
		//TODO: make this a bit nicer
		String symbols = new StringBuffer().append(" =><!+-*/()',")
			.append( dialect.openQuote() )
			.append( dialect.closeQuote() )
			.toString();
		StringTokenizer tokens = new StringTokenizer(sqlWhereString, symbols, true);
		StringBuffer result = new StringBuffer();
		boolean quoted = false;
		boolean quotedIdentifier = false;
		boolean beforeTable = false;
		boolean inFromClause = false;
		boolean afterFromTable = false;
		boolean hasMore = tokens.hasMoreTokens();
		String nextToken = hasMore ? tokens.nextToken() : null;
		while (hasMore) {
			final String token = nextToken;
			final String lcToken = token.toLowerCase();
			hasMore = tokens.hasMoreTokens();
			nextToken = hasMore ? tokens.nextToken() : null;
			if ( !quotedIdentifier && "'".equals(token) ) quoted = !quoted;
			if ( !quoted && !quotedIdentifier && dialect.openQuote()==token.charAt(0) ) quotedIdentifier = true;
			if ( !quoted && quotedIdentifier && dialect.closeQuote()==token.charAt(0) ) quotedIdentifier = false;

			if ( quoted || quotedIdentifier || Character.isWhitespace( token.charAt(0) ) ) {
				result.append(token);
			}
			else if (beforeTable) {
				result.append(token);
				beforeTable = false;
				afterFromTable = true;
			}
			else if (afterFromTable) {
				if ( !"as".equals(lcToken) ) afterFromTable = false;
				result.append(token);
			}
			else if ( isNamedParameter(token) ) {
				result.append(token);
			}
			else if (
				isIdentifier(token, dialect) &&
				!isFunction(lcToken, nextToken, dialect)
			) {
				result.append(placeholder)
					.append('.')
					.append( dialect.quote(token) );
			}
			else {
				if ( BEFORE_TABLE_KEYWORDS.contains(lcToken) ) {
					beforeTable = true;
					inFromClause = true;
				}
				else if ( inFromClause && ",".equals(lcToken) ) {
					beforeTable = true;
				}
				result.append(token);
			}
			
			if ( //Yuck:
					inFromClause && 
					KEYWORDS.contains(lcToken) && //"as" is not in KEYWORDS
					!BEFORE_TABLE_KEYWORDS.contains(lcToken)
			) { 
				inFromClause = false;
			}

		}
		return result.toString();
	}

	private static boolean isNamedParameter(String token) {
		return token.startsWith(":");
	}

	private static boolean isFunction(String lcToken, String nextToken, Dialect dialect) {
		return "(".equals(nextToken) ||
			KEYWORDS.contains(lcToken) ||
			dialect.getFunctions().containsKey(lcToken);
	}

	private static boolean isIdentifier(String token, Dialect dialect) {
		return token.charAt(0)=='`' || ( //allow any identifier quoted with backtick
			Character.isLetter( token.charAt(0) ) && //only recognizes identifiers beginning with a letter
			token.indexOf('.') < 0
		);
	}

	public static String renderOrderByStringTemplate(String sqlOrderByString, Dialect dialect) {
		// takes order by clause provided in the mapping
		// attribute and interpolates the alias
		//TODO: make this a bit nicer
		StringTokenizer tokens = new StringTokenizer(sqlOrderByString, ",");
		StringBuffer result = new StringBuffer();
		while ( tokens.hasMoreTokens() ) {
			String column = dialect.quote( tokens.nextToken().trim() );
			result.append(TEMPLATE)
				.append('.')
				.append(column);
			if ( tokens.hasMoreTokens() ) result.append(", ");
		}
		return result.toString();
	}

}
