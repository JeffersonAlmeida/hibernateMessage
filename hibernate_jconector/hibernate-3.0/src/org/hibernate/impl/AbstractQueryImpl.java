//$Id: AbstractQueryImpl.java,v 1.30 2005/03/22 07:35:27 maxcsaucdk Exp $
package org.hibernate.impl;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.NonUniqueResultException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.Query;
import org.hibernate.QueryException;
import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.RowSelection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.TypedValue;
import org.hibernate.hql.classic.ParserHelper;
import org.hibernate.property.Getter;
import org.hibernate.proxy.HibernateProxyHelper;
import org.hibernate.type.SerializableType;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.MarkerObject;
import org.hibernate.util.ReflectHelper;
import org.hibernate.util.StringHelper;

/**
 * Abstract implementation of the Query interface
 * @author Gavin King, Max Andersen
 */
public abstract class AbstractQueryImpl implements Query {

	private static final Object UNSET_PARAMETER = new MarkerObject("<unset parameter>");
	private static final Object UNSET_TYPE = new MarkerObject("<unset type>");

	private String queryString;
	private final SessionImplementor session;

	private RowSelection selection;
	private List values = new ArrayList(4);
	private List types = new ArrayList(4);
	private int positionalParameterCount = 0;
	private Set actualNamedParameters = new HashSet(4);
	private Map namedParameters = new HashMap(4);
	private Map namedParameterLists = new HashMap(4);
	private boolean cacheable;
	private String cacheRegion;
	private String comment;
	private FlushMode flushMode;
	private CacheMode cacheMode;
	private FlushMode sessionFlushMode;
	private CacheMode sessionCacheMode;
	private Serializable collectionKey;
	private boolean readOnly;

	public AbstractQueryImpl(String queryString, FlushMode flushMode, SessionImplementor session) {
		this.session = session;
		this.queryString = queryString;
		this.selection = new RowSelection();
		this.flushMode = flushMode;
		this.cacheMode = null;
		initParameterBookKeeping();
	}

	public final String getQueryString() {
		return queryString;
	}
	
	public Query setFlushMode(FlushMode flushMode) {
		this.flushMode = flushMode;
		return this;
	}
	
	public Query setCacheMode(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
		return this;
	}
	
	protected Map getNamedParams() {
		return new HashMap(namedParameters);
	}

	protected void verifyParameters() throws QueryException {
		verifyParameters(false);
	}
	
	/**
	 * @param reserveFirstParameter if true, the first ? will not be verified since its needed for e.g. callable statements returning a out parameter
	 * @throws HibernateException
	 */
	protected void verifyParameters(boolean reserveFirstParameter) throws HibernateException {

		if ( actualNamedParameters.size() != namedParameters.size() + namedParameterLists.size() ) {
			Set missingParams = new HashSet(actualNamedParameters);
			missingParams.removeAll( namedParameterLists.keySet() );
			missingParams.removeAll( namedParameters.keySet() );
			throw new QueryException( "Not all named parameters have been set: " + missingParams, getQueryString() );
		}
		
		int positionalValueSpan = 0;
		for ( int i=0; i<values.size(); i++ ) {
			Object object = types.get(i);
			if( values.get(i)==UNSET_PARAMETER || object==UNSET_TYPE ) {
				if(reserveFirstParameter && i==0) {
					continue;
				} else {
					throw new QueryException( "Unset positional parameter at position: " + i, getQueryString() );
				}
			}
			positionalValueSpan += ( (Type) object ).getColumnSpan( session.getFactory() );
		}

		if ( positionalParameterCount!=positionalValueSpan ) {
			if(reserveFirstParameter && positionalParameterCount-1!=positionalValueSpan) {
				throw new QueryException( 
				 		"Expected positional parameter count: " + 
				 		(positionalParameterCount-1) + 
				 		", actual parameters: " + 
				 		values, 
				 		getQueryString() 
				 );
			} else if (!reserveFirstParameter) {
				throw new QueryException( 
				 		"Expected positional parameter count: " + 
				 		positionalParameterCount + 
				 		", actual parameters: " + 
				 		values, 
				 		getQueryString() 
				 );	
			}			
		}
	}

	protected Map getNamedParameterLists() {
		return namedParameterLists;
	}

	protected List getValues() {
		return values;
	}

	protected List getTypes() {
		return types;
	}

	//TODO: maybe call it getRowSelection() ?
	public RowSelection getSelection() {
		return selection;
	}

	public Query setMaxResults(int maxResults) {
		selection.setMaxRows( new Integer(maxResults) );
		return this;
	}

	public Query setTimeout(int timeout) {
		selection.setTimeout( new Integer(timeout) );
		return this;
	}
	public Query setFetchSize(int fetchSize) {
		selection.setFetchSize( new Integer(fetchSize) );
		return this;
	}


	public Query setFirstResult(int firstResult) {
		selection.setFirstRow( new Integer(firstResult) );
		return this;
	}

	public Query setParameter(int position, Object val, Type type) {
		if ( positionalParameterCount==0 ) {
			throw new IllegalArgumentException("No positional parameters in query: " + getQueryString() );
		}
		if ( position<0 || position>positionalParameterCount-1 ) {
			throw new IllegalArgumentException("Positional parameter does not exist: " + position + " in query: " + getQueryString() );
		}
		int size = values.size();
		if ( position<size ) {
			values.set(position, val);
			types.set(position, type);
		}
		else {
			// prepend value and type list with null for any positions before the wanted position.
			for ( int i=0; i<position-size; i++ ) {
				values.add(UNSET_PARAMETER);
				types.add(UNSET_TYPE);
			}
			values.add(val);
			types.add(type);
		}
		return this;
	}

	public Query setString(int position, String val) {
		setParameter(position, val, Hibernate.STRING);
		return this;
	}

	public Query setCharacter(int position, char val) {
		setParameter(position, new Character(val), Hibernate.CHARACTER);
		return this;
	}

	public Query setBoolean(int position, boolean val) {
		setParameter(position, val ? Boolean.TRUE : Boolean.FALSE, Hibernate.BOOLEAN);
		return this;
	}

	public Query setByte(int position, byte val) {
		setParameter(position, new Byte(val), Hibernate.BYTE);
		return this;
	}

	public Query setShort(int position, short val) {
		setParameter(position, new Short(val), Hibernate.SHORT);
		return this;
	}

	public Query setInteger(int position, int val) {
		setParameter(position, new Integer(val), Hibernate.INTEGER);
		return this;
	}

	public Query setLong(int position, long val) {
		setParameter(position, new Long(val), Hibernate.LONG);
		return this;
	}

	public Query setFloat(int position, float val) {
		setParameter(position, new Float(val), Hibernate.FLOAT);
		return this;
	}

	public Query setDouble(int position, double val) {
		setParameter(position, new Double(val), Hibernate.DOUBLE);
		return this;
	}

	public Query setBinary(int position, byte[] val) {
		setParameter(position, val, Hibernate.BINARY);
		return this;
	}

	public Query setText(int position, String val) {
		setParameter(position, val, Hibernate.TEXT);
		return this;
	}

	public Query setSerializable(int position, Serializable val) {
		setParameter(position, val, Hibernate.SERIALIZABLE);
		return this;
	}

	public Query setDate(int position, Date date) {
		setParameter(position, date, Hibernate.DATE);
		return this;
	}

	public Query setTime(int position, Date date) {
		setParameter(position, date, Hibernate.TIME);
		return this;
	}

	public Query setTimestamp(int position, Date date) {
		setParameter(position, date, Hibernate.TIMESTAMP);
		return this;
	}

	public Query setEntity(int position, Object val) {
		setParameter( position, val, Hibernate.entity( session.bestGuessEntityName(val) ) );
		return this;
	}

	public Query setLocale(int position, Locale locale) {
		setParameter(position, locale, Hibernate.LOCALE);
		return this;
	}

	public Query setCalendar(int position, Calendar calendar) {
		setParameter(position, calendar, Hibernate.CALENDAR);
		return this;
	}

	public Query setCalendarDate(int position, Calendar calendar) {
		setParameter(position, calendar, Hibernate.CALENDAR_DATE);
		return this;
	}

	public Query setBinary(String name, byte[] val) {
		setParameter(name, val, Hibernate.BINARY);
		return this;
	}

	public Query setText(String name, String val) {
		setParameter(name, val, Hibernate.TEXT);
		return this;
	}

	public Query setBoolean(String name, boolean val) {
		setParameter(name, val ? Boolean.TRUE : Boolean.FALSE, Hibernate.BOOLEAN);
		return this;
	}

	public Query setByte(String name, byte val) {
		setParameter(name, new Byte(val), Hibernate.BYTE);
		return this;
	}

	public Query setCharacter(String name, char val) {
		setParameter(name, new Character(val), Hibernate.CHARACTER);
		return this;
	}

	public Query setDate(String name, Date date) {
		setParameter(name, date, Hibernate.DATE);
		return this;
	}

	public Query setDouble(String name, double val) {
		setParameter(name, new Double(val), Hibernate.DOUBLE);
		return this;
	}

	public Query setEntity(String name, Object val) {
		setParameter( name, val, Hibernate.entity( session.bestGuessEntityName(val) ) );
		return this;
	}

	public Query setFloat(String name, float val) {
		setParameter(name, new Float(val), Hibernate.FLOAT);
		return this;
	}

	public Query setInteger(String name, int val) {
		setParameter(name, new Integer(val), Hibernate.INTEGER);
		return this;
	}

	public Query setLocale(String name, Locale locale) {
		setParameter(name, locale, Hibernate.LOCALE);
		return this;
	}

	public Query setCalendar(String name, Calendar calendar) {
		setParameter(name, calendar, Hibernate.CALENDAR);
		return this;
	}

	public Query setCalendarDate(String name, Calendar calendar) {
		setParameter(name, calendar, Hibernate.CALENDAR_DATE);
		return this;
	}

	public Query setLong(String name, long val) {
		setParameter(name, new Long(val), Hibernate.LONG);
		return this;
	}

	public Query setParameter(String name, Object val, Type type) {
		if( !actualNamedParameters.contains(name) ) {
			 throw new IllegalArgumentException("Parameter " + name + " does not exist as a named parameter in [" + getQueryString() + "]");
		}
		else {
			 namedParameters.put( name, new TypedValue( type, val, session.getEntityMode() ) );
			 return this;
		}
	}

	public Query setSerializable(String name, Serializable val) {
		setParameter(name, val, Hibernate.SERIALIZABLE);
		return this;
	}

	public Query setShort(String name, short val) {
		setParameter(name, new Short(val), Hibernate.SHORT);
		return this;
	}

	public Query setString(String name, String val) {
		setParameter(name, val, Hibernate.STRING);
		return this;
	}

	public Query setTime(String name, Date date) {
		setParameter(name, date, Hibernate.TIME);
		return this;
	}

	public Query setTimestamp(String name, Date date) {
		setParameter(name, date, Hibernate.TIMESTAMP);
		return this;
	}

	public Query setBigDecimal(int position, BigDecimal number) {
		setParameter(position, number, Hibernate.BIG_DECIMAL);
		return this;
	}

	public Query setBigDecimal(String name, BigDecimal number) {
		setParameter(name, number, Hibernate.BIG_DECIMAL);
		return this;
	}

	public Query setParameter(int position, Object val) throws HibernateException {
		if (val == null) {
			setParameter( position, val, Hibernate.SERIALIZABLE );
		} 
		else {
			setParameter( position, val, guessType(val) );
		}
		return this;
	}

	public Query setParameter(String name, Object val) throws HibernateException {
		if (val == null) {
			setParameter( name, val, Hibernate.SERIALIZABLE );
		} 
		else {
			setParameter( name, val, guessType(val) );
		}
		return this;
	}

	private Type guessType(Object param) throws HibernateException {
		Class clazz = HibernateProxyHelper.getClassWithoutInitializingProxy(param);
		return guessType(clazz);
	}

	private Type guessType(Class clazz) throws HibernateException {
		String typename = clazz.getName();
		Type type = TypeFactory.heuristicType(typename);
		boolean serializable = type!=null && type instanceof SerializableType;
		if (type==null || serializable) {
			try {
				session.getFactory().getEntityPersister( clazz.getName() );
			}
			catch (MappingException me) {
				if (serializable) {
					return type;
				}
				else {
					throw new HibernateException("Could not determine a type for class: " + typename);
				}
			}
			return Hibernate.entity(clazz);
		}
		else {
			return type;
		}
	}


	public Type[] getReturnTypes() throws HibernateException {
		return session.getFactory().getReturnTypes( queryString );
	}

	public Query setParameterList(String name, Collection vals, Type type) throws HibernateException {
		if( !actualNamedParameters.contains(name) ) {
			   throw new IllegalArgumentException("Parameter " + name + " does not exist as a named parameter in [" + getQueryString() + "]");
		}
		namedParameterLists.put( name, new TypedValue( type, vals, session.getEntityMode() ) );
		return this;
	}
	
	/**
	 * Warning: adds new parameters to the argument by side-effect!
	 */
	protected String bindParameterLists(Map namedParamsCopy) {
		String query = this.queryString;
		Iterator iter = namedParameterLists.entrySet().iterator();
		while ( iter.hasNext() ) {
			Map.Entry me = (Map.Entry) iter.next();
			query = bindParameterList( query, (String) me.getKey(), (TypedValue) me.getValue(), namedParamsCopy );
		}
		return query;
	}

	/**
	 * Warning: adds new parameters to the argument by side-effect!
	 */
	private String bindParameterList(String query, String name, TypedValue typedList, Map namedParamsCopy) {
		Collection vals = (Collection) typedList.getValue();
		Type type = typedList.getType();
		StringBuffer list = new StringBuffer(16);
		Iterator iter = vals.iterator();
		int i=0;
		while ( iter.hasNext() ) {
			String alias = name + i++ + '_';
			namedParamsCopy.put(alias, new TypedValue( type, iter.next(), session.getEntityMode() ) );
			list.append( ParserHelper.HQL_VARIABLE_PREFIX + alias );
			if ( iter.hasNext() ) list.append(", ");
		}
		return StringHelper.replace( query, ParserHelper.HQL_VARIABLE_PREFIX + name, list.toString(), true );
	}

	public Query setParameterList(String name, Collection vals) throws HibernateException {
		if (vals==null) {
			throw new QueryException("Collection must be not null!");
		}

		if(vals.size()==0) {
			setParameterList(name, vals, null);
		}
		else {
			setParameterList(name, vals, guessType( vals.iterator().next() ) );
		}

		return this;
	}

	public String[] getNamedParameters() throws HibernateException {
		return (String[]) actualNamedParameters.toArray(new String[actualNamedParameters.size()]);
	}

	private void initParameterBookKeeping() {
		StringTokenizer st = new StringTokenizer(queryString, ParserHelper.HQL_SEPARATORS);
		Set result = new HashSet();

		while ( st.hasMoreTokens() ) {
			String string = st.nextToken();
			if( string.startsWith(ParserHelper.HQL_VARIABLE_PREFIX) ) {
				result.add( string.substring(1) );
			}
		}

		actualNamedParameters = result;
		positionalParameterCount = StringHelper.countUnquoted(queryString, '?');
	}

	public Query setProperties(Object bean) throws HibernateException {
		Class clazz = bean.getClass();
		String[] params = getNamedParameters();
		for (int i = 0; i < params.length; i++) {
			String namedParam = params[i];
			try {
				Getter getter = ReflectHelper.getGetter(clazz, namedParam);
				Class retType = getter.getReturnType();
				final Object object = getter.get( bean );
				if ( Collection.class.isAssignableFrom(retType) ) {
					setParameterList( namedParam, (Collection) object );
				}
				else if ( retType.isArray() ) {
				 	setParameterList( namedParam, (Object[]) object );
				}
				else {
					setParameter( namedParam, object, guessType( getter.getReturnType() ) );
				}
			}
			catch (PropertyNotFoundException pnfe) {}
		}
		return this;
	}


	public Query setParameterList(String name, Object[] vals, Type type)
		throws HibernateException {
		return setParameterList(name, Arrays.asList(vals), type);
	}

	public Query setParameterList(String name, Object[] vals)
		throws HibernateException {
		return setParameterList( name, Arrays.asList(vals) );
	}

	SessionImplementor getSession() {
		return session;
	}

	public Object uniqueResult() throws HibernateException {
		return uniqueElement( list() );
	}

	public int executeUpdate() throws HibernateException {
		throw new UnsupportedOperationException( "Update queries only supported through HQL" );
	}

	static Object uniqueElement(List list) throws NonUniqueResultException {
		int size = list.size();
		if (size==0) return null;
		Object first = list.get(0);
		for ( int i=1; i<size; i++ ) {
			if ( list.get(i)!=first ) {
				throw new NonUniqueResultException( list.size() );
			}
		}
		return first;
	}

	protected RowSelection getRowSelection() {
		return selection;
	}

	public Type[] typeArray() {
		return ArrayHelper.toTypeArray( getTypes() );
	}
	
	public Object[] valueArray() {
		return getValues().toArray();
	}

	public QueryParameters getQueryParameters(Map namedParams) {
		return new QueryParameters(
				typeArray(),
				valueArray(),
				namedParams,
				getLockModes(),
				getSelection(),
				readOnly,
				cacheable,
				cacheRegion,
				//forceCacheRefresh,
				comment,
				collectionKey==null ?
						null :
						new Serializable[] { collectionKey }
		);
	}
	
	protected abstract Map getLockModes();

	public Query setCacheable(boolean cacheable) {
		this.cacheable = cacheable;
		return this;
	}

	public Query setCacheRegion(String cacheRegion) {
		if (cacheRegion != null)
			this.cacheRegion = cacheRegion.trim();
		return this;
	}

	public Query setComment(String comment) {
		this.comment = comment;
		return this;
	}
	
	public String toString() {
		return StringHelper.unqualify( getClass().getName() ) + '(' + queryString + ')';
	}
	
	protected void before() {
		if ( flushMode!=null ) {
			sessionFlushMode = getSession().getFlushMode();
			getSession().setFlushMode(flushMode);
		}
		if ( cacheMode!=null ) {
			sessionCacheMode = getSession().getCacheMode();
			getSession().setCacheMode(cacheMode);
		}
	}
	
	protected void after() {
		if (sessionFlushMode!=null) {
			getSession().setFlushMode(sessionFlushMode);
			sessionFlushMode = null;
		}
		if (sessionCacheMode!=null) {
			getSession().setCacheMode(sessionCacheMode);
			sessionCacheMode = null;
		}
	}
	
	public Query setCollectionKey(Serializable collectionKey) {
		this.collectionKey = collectionKey;
		return this;
	}
	
	public Query setParameters(Object[] values, Type[] types) {
		this.values = Arrays.asList(values);
		this.types = Arrays.asList(types);
		return this;
	}
	
	public boolean isReadOnly() {
		return readOnly;
	}
	
	public Query setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		return this;
	}
}
