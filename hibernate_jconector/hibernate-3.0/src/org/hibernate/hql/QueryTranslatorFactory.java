//$Id: QueryTranslatorFactory.java,v 1.6 2005/02/12 07:19:22 steveebersole Exp $
package org.hibernate.hql;

import org.hibernate.engine.SessionFactoryImplementor;

import java.util.Map;

/**
 * @author Gavin King
 */
public interface QueryTranslatorFactory {
	public QueryTranslator createQueryTranslator(String queryString, Map filters, SessionFactoryImplementor factory);

	public FilterTranslator createFilterTranslator(String queryString, Map filters, SessionFactoryImplementor factory);
}
