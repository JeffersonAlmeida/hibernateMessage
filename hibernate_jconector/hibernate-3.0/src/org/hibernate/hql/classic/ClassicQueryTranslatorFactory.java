//$Id: ClassicQueryTranslatorFactory.java,v 1.3 2005/02/12 18:48:19 pgmjsd Exp $
package org.hibernate.hql.classic;

import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.hql.FilterTranslator;
import org.hibernate.hql.QueryTranslator;
import org.hibernate.hql.QueryTranslatorFactory;

import java.util.Map;

/**
 * @author Gavin King
 */
public class ClassicQueryTranslatorFactory implements QueryTranslatorFactory {

	public QueryTranslator createQueryTranslator(
	        String queryString,
	        Map filters,
	        SessionFactoryImplementor factory) {
		return new QueryTranslatorImpl( queryString, filters, factory );
	}

	public FilterTranslator createFilterTranslator(String queryString,
												   Map filters, SessionFactoryImplementor factory) {
		return new QueryTranslatorImpl( queryString, filters, factory );
	}

}
