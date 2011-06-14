//$Id: SubselectCollectionLoader.java,v 1.3 2005/03/21 20:19:56 oneovthafew Exp $
package org.hibernate.loader.collection;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.EntityKey;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.type.Type;
import org.hibernate.util.StringHelper;

/**
 * @author Gavin King
 */
public class SubselectCollectionLoader extends CollectionLoader {
	
	private final Serializable[] keys;
	private final Type[] types;
	private final Object[] values;
	private final Map namedParameters;

	public SubselectCollectionLoader(
			QueryableCollection persister, 
			String subquery,
			Collection entityKeys,
			QueryParameters queryParameters,
			SessionFactoryImplementor factory, 
			Map enabledFilters)
	throws MappingException {
		
		super(persister, 1, subquery, factory, enabledFilters);

		keys = new Serializable[ entityKeys.size() ];
		Iterator iter = entityKeys.iterator();
		int i=0;
		while ( iter.hasNext() ) {
			keys[i++] = ( (EntityKey) iter.next() ).getIdentifier();
		}
		
		this.namedParameters = queryParameters.getNamedParameters();
		this.types = queryParameters.getFilteredPositionalParameterTypes();
		this.values = queryParameters.getFilteredPositionalParameterValues();
		
	}

	public void initialize(Serializable id, SessionImplementor session)
	throws HibernateException {
		loadCollectionSubselect( 
				session, 
				keys, 
				values,
				types,
				namedParameters,
				getKeyType() 
		);
	}

	protected StringBuffer whereString(String alias, String[] columnNames, int batchSize, String subquery) {
		StringBuffer buf = new StringBuffer();
		if (columnNames.length>1) buf.append('(');
		buf.append( StringHelper.join(", ", StringHelper.qualify(alias, columnNames) ) );
		if (columnNames.length>1) buf.append(')');
		buf.append(" in ")
			.append('(')
			.append(subquery) 
			.append(')');
		return buf;
	}
}
