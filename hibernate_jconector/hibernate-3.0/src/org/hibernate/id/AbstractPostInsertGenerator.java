//$Id: AbstractPostInsertGenerator.java,v 1.4 2005/02/12 07:19:22 steveebersole Exp $
package org.hibernate.id;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.pretty.MessageHelper;

/**
 * @author Gavin King
 */
public abstract class AbstractPostInsertGenerator implements PostInsertIdentifierGenerator{

	public Serializable generate(SessionImplementor s, Object obj) {
		return IdentifierGeneratorFactory.POST_INSERT_INDICATOR;
	}
	
	protected abstract String getSQL(PostInsertIdentityPersister persister);
	
	protected void bindParameters(SessionImplementor session, PreparedStatement ps, Object object, PostInsertIdentityPersister persister)
	throws SQLException {}
	
	protected abstract Serializable getResult(SessionImplementor session, ResultSet rs, Object object, PostInsertIdentityPersister persister)
	throws SQLException;

	public Serializable getGenerated(SessionImplementor session, Object object, PostInsertIdentityPersister persister) 
	throws HibernateException {
		
		final String sql = getSQL(persister);

		try {
	
			//fetch the generated id in a separate query
			PreparedStatement idSelect = session.getBatcher().prepareStatement(sql);
			try {
				bindParameters(session, idSelect, object, persister);
				ResultSet rs = idSelect.executeQuery();
				try {
					return getResult(session, rs, object, persister);
				}
				finally {
					rs.close();
				}
			}
			finally {
				session.getBatcher().closeStatement(idSelect);
			}
	
		}
		catch ( SQLException sqle ) {
			throw JDBCExceptionHelper.convert(
					session.getFactory().getSQLExceptionConverter(),
					sqle,
					"could not insert: " + MessageHelper.infoString( persister ),
			        sql
			);
		}

	}

}
