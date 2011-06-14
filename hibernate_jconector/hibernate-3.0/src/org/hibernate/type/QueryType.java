//$Id: QueryType.java,v 1.16 2005/03/16 04:45:25 oneovthafew Exp $
package org.hibernate.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Node;
import org.hibernate.EntityMode;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.util.ArrayHelper;

/**
 * @author Gavin King
 */
public class QueryType extends AbstractType {
	
	private final String queryName;
	
	private static final Log log = LogFactory.getLog(QueryType.class);
	
	public QueryType(String queryName) {
		this.queryName = queryName;
	}

	public int[] sqlTypes(Mapping mapping) throws MappingException {
		return ArrayHelper.EMPTY_INT_ARRAY;
	}

	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 0;
	}

	public int compare(Object x, Object y, EntityMode entityMode) {
		return 0; //query results cannot be compared
	}

	public Class getReturnedClass() {
		return List.class;
	}

	public boolean isDirty(Object old, Object current, SessionImplementor session) 
	throws HibernateException {
		return false;
	}
	
	public Object nullSafeGet(
			final ResultSet rs, 
			final String[] names, 
			final SessionImplementor session, 
			final Object owner)
	throws HibernateException, SQLException {
		return new ListProxy(session, queryName, owner);
	}

	public Object nullSafeGet(ResultSet rs, String name,
			SessionImplementor session, Object owner)
			throws HibernateException, SQLException {
		throw new UnsupportedOperationException();
	}

	public void nullSafeSet(
			final PreparedStatement st, 
			final Object value, 
			final int index,
			final SessionImplementor session) 
	throws HibernateException, SQLException {
		//do nothing
	}

	public void nullSafeSet(
			final PreparedStatement st, 
			final Object value, 
			final int index,
			final boolean[] settable, 
			final SessionImplementor session) 
	throws HibernateException, SQLException {
		//do nothing
	}

	public Object fromXMLNode(Node xml, Mapping factory) throws HibernateException {
		throw new UnsupportedOperationException("todo");
	}

	public void setToXMLNode(Node node, Object value, SessionFactoryImplementor factory) throws HibernateException {
		throw new UnsupportedOperationException("todo");
	}

	public String toLoggableString(Object value, SessionFactoryImplementor factory)
	throws HibernateException {
		return value==null ? "null" : value.toString();  //TODO: very unsafe!
	}

	public String getName() {
		return queryName;
	}

	public Object deepCopy(Object value, EntityMode entityMode, SessionFactoryImplementor factory) throws HibernateException {
		return value;
	}

	public boolean isMutable() {
		return false;
	}
	
	public Object assemble(Serializable cached, SessionImplementor session, Object owner) 
	throws HibernateException {
		return new ListProxy(session, queryName, owner);
	}
	
	public Serializable disassemble(Object value, SessionImplementor session, Object owner)
	throws HibernateException {
		return null;
	}

	public Object replace(
			Object original, 
			Object target,
			SessionImplementor session, 
			Object owner, 
			Map copyCache)
	throws HibernateException {
		return original;
	}
	public static class ListProxy implements List {
		
		private transient Session session;
		private final String queryName;
		private final Object owner;
		private List list;
		
		ListProxy(Session session, String queryName, Object owner) {
			this.session = session;
			this.queryName = queryName;
			this.owner = owner;
		}
		
		public void setSession(Session session) {
			this.session = session;
		}
		
		public String toString() {
			return getList().toString();
		}
		
		public boolean equals(Object other) {
			return getList().equals(other);
		}
		
		public int hashCode() {
			return getList().hashCode();
		}
		
		private List getList() {
			if (list==null) {
				if ( log.isDebugEnabled() ) {
					log.debug("loading query-list using named query: " + queryName);
				}
				list = session.getNamedQuery(queryName)
					.setProperties(owner)
					.setFlushMode(FlushMode.NEVER) //TODO: should this really override the setting in the query definition?
					.list();
			}
			return list; 
		}
		
		public void add(int index, Object element) {
			getList().add(index, element);
		}
		public boolean add(Object o) {
			return getList().add(o);
		}
		public boolean addAll(Collection c) {
			return getList().addAll(c);
		}
		public boolean addAll(int index, Collection c) {
			return getList().addAll(index, c);
		}
		public void clear() {
			getList().clear();
		}
		public boolean contains(Object o) {
			return getList().contains(o);
		}
		public boolean containsAll(Collection c) {
			return getList().containsAll(c);
		}
		public Object get(int index) {
			return getList().get(index);
		}
		public int indexOf(Object o) {
			return getList().indexOf(o);
		}
		public boolean isEmpty() {
			return getList().isEmpty();
		}
		public Iterator iterator() {
			return getList().iterator();
		}
		public int lastIndexOf(Object o) {
			return getList().lastIndexOf(o);
		}
		public ListIterator listIterator() {
			return getList().listIterator();
		}
		public ListIterator listIterator(int index) {
			return getList().listIterator(index);
		}
		public Object remove(int index) {
			return getList().remove(index);
		}
		public boolean remove(Object o) {
			return getList().remove(o);
		}
		public boolean removeAll(Collection c) {
			return getList().removeAll(c);
		}
		public boolean retainAll(Collection c) {
			return getList().retainAll(c);
		}
		public Object set(int index, Object element) {
			return getList().set(index, element);
		}
		public int size() {
			return getList().size();
		}
		public List subList(int fromIndex, int toIndex) {
			return getList().subList(fromIndex, toIndex);
		}
		public Object[] toArray() {
			return getList().toArray();
		}
		public Object[] toArray(Object[] a) {
			return getList().toArray(a);
		}
	}

}
