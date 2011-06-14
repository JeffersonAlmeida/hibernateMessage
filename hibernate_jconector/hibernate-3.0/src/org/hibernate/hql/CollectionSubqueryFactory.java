// $Id: CollectionSubqueryFactory.java,v 1.3 2005/02/23 02:31:40 oneovthafew Exp $
package org.hibernate.hql;

import org.hibernate.engine.JoinSequence;
import org.hibernate.sql.JoinFragment;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.util.StringHelper;

import java.util.Map;

/**
 * Provides the SQL for collection subqueries.
 * <br>
 * Moved here from PathExpressionParser to make it re-useable.
 * @author josh Dec 23, 2004 7:12:55 AM
 */
public final class CollectionSubqueryFactory {

	private CollectionSubqueryFactory() {
	}

	public static String createCollectionSubquery(JoinSequence joinSequence, Map enabledFilters, String[] columns) {
		//TODO: refactor to .sql package
		JoinFragment join;
		try {
			join = joinSequence.toJoinFragment( enabledFilters, true );
		}
		catch ( MappingException me ) {
			throw new QueryException( me );
		}
		return new StringBuffer( "select " )
				.append( StringHelper.join( ", ", columns ) )
				.append( " from " )
				/*.append(collectionTable)
				.append(' ')
				.append(collectionName)*/
				.append( join.toFromFragmentString().substring( 2 ) )// remove initial ", "
				.append( " where " )
				.append( join.toWhereFragmentString().substring( 5 ) )// remove initial " and "
				.toString();
	}
}
