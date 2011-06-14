//$Id: ASTQueryTranslatorFactory.java,v 1.5 2005/02/20 17:08:00 pgmjsd Exp $
package org.hibernate.hql.ast;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.hql.FilterTranslator;
import org.hibernate.hql.QueryTranslator;
import org.hibernate.hql.QueryTranslatorFactory;

import java.util.Map;

/**
 * @author Gavin King
 */
public class ASTQueryTranslatorFactory implements QueryTranslatorFactory {

	private static final Log log = LogFactory.getLog( ASTQueryTranslatorFactory.class );

	public ASTQueryTranslatorFactory() {
		log.info( "Using ASTQueryTranslatorFactory" );
	}

	public QueryTranslator createQueryTranslator(String queryString,
												 Map filters,
												 SessionFactoryImplementor factory) {
		return new QueryTranslatorImpl( queryString, filters, factory );
	}

	public FilterTranslator createFilterTranslator(String queryString,
												   Map filters,
												   SessionFactoryImplementor factory) {
		return new QueryTranslatorImpl( queryString, filters, factory );
	}

}
