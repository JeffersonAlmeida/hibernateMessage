// $Id: ColumnHelper.java,v 1.3 2005/02/12 07:19:19 steveebersole Exp $
package org.hibernate.hql.ast;

import antlr.ASTFactory;
import antlr.collections.AST;
import org.hibernate.hql.NameGenerator;
import org.hibernate.hql.antlr.SqlTokenTypes;

/**
 * Provides utility methods for dealing with arrays of SQL column names.
 *
 * @author josh Jan 3, 2005 9:08:47 AM
 */
final class ColumnHelper {

	/**
	 * @deprecated (tell clover to filter this out)
	 */
	private ColumnHelper() {
	}

	static void generateSingleScalarColumn(HqlSqlWalkerNode node, int i) {
		ASTFactory factory = node.getASTFactory();
		ASTUtil.createSibling( factory, SqlTokenTypes.SELECT_COLUMNS, " as " + NameGenerator.scalarName( i, 0 ), node );
	}

	/**
	 * Generates the scalar column AST nodes for a given array of SQL columns
	 */
	public static void generateScalarColumns(HqlSqlWalkerNode node, String sqlColumns[], int i) {
		if ( sqlColumns.length == 1 ) {
			generateSingleScalarColumn( node, i );
		}
		else {
			ASTFactory factory = node.getASTFactory();
			AST n = node;
			n.setText( sqlColumns[0] );	// Use the DOT node to emit the first column name.
			// Create the column names, folled by the column aliases.
			for ( int j = 0; j < sqlColumns.length; j++ ) {
				if ( j > 0 ) {
					n = ASTUtil.createSibling( factory, SqlTokenTypes.SQL_TOKEN, sqlColumns[j], n );
				}
				n = ASTUtil.createSibling( factory, SqlTokenTypes.SELECT_COLUMNS, " as " + NameGenerator.scalarName( i, j ), n );
			}
		}
	}
}
