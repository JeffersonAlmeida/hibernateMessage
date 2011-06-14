//$Id: IteratorImpl.java,v 1.8 2004/12/24 03:52:00 pgmjsd Exp $
package org.hibernate.impl;

import java.lang.reflect.Constructor;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.QueryException;
import org.hibernate.engine.HibernateIterator;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.type.Type;
import org.hibernate.util.ReflectHelper;

/**
 * An implementation of <tt>java.util.Iterator</tt> that is
 * returned by <tt>iterate()</tt> query execution methods.
 * @author Gavin King
 */
public final class IteratorImpl implements HibernateIterator {

	private static final Log log = LogFactory.getLog(IteratorImpl.class);

	private ResultSet rs;
	private final SessionImplementor sess;
	private final Type[] types;
	private final boolean single;
	private Object currentResult;
	private boolean hasNext;
	private final String[][] names;
	private PreparedStatement ps;
	private Object nextResult;
	private Constructor holderConstructor;

	public IteratorImpl(
	        ResultSet rs,
	        PreparedStatement ps,
	        SessionImplementor sess,
	        Type[] types,
	        String[][] columnNames,
	        Class holderClass)
	throws HibernateException, SQLException {

		this.rs=rs;
		this.ps=ps;
		this.sess = sess;
		this.types = types;
		this.names = columnNames;

		if (holderClass != null) {
			holderConstructor = ReflectHelper.getConstructor(holderClass, types);
		}

		single = types.length==1;

		postNext();
	}

	public void close() throws JDBCException {
		if (ps!=null) {
			try {
				log.debug("closing iterator");
				nextResult = null;
				sess.getBatcher().closeQueryStatement(ps, rs);
				ps = null;
				rs = null;
				hasNext = false;
			}
			catch (SQLException e) {
				log.info( "Unable to close iterator", e );
				throw JDBCExceptionHelper.convert(
				        sess.getFactory().getSQLExceptionConverter(),
				        e,
				        "Unable to close iterator"
				);
			}
		}
	}

	private void postNext() throws HibernateException, SQLException {
		this.hasNext = rs.next();
		if (!hasNext) {
			log.debug("exhausted results");
			close();
		}
		else {
			log.debug("retrieving next results");
			if (single) {
				nextResult = types[0].nullSafeGet( rs, names[0], sess, null );
			}
			else {
				Object[] nextResults = new Object[types.length];
				for (int i=0; i<types.length; i++) {
					nextResults[i] = types[i].nullSafeGet( rs, names[i], sess, null );
				}
				nextResult = nextResults;
			}

			if (holderConstructor != null) {
				try {
					if ( nextResult == null || !nextResult.getClass().isArray() ) {
						nextResult = holderConstructor.newInstance( new Object[] {nextResult} );
					}
					else {
						// NOTE: This doesn't compile under JDK1.4 [jsd]
						// nextResult = holderConstructor.newInstance(nextResult);
						nextResult = holderConstructor.newInstance( new Object[] {nextResult} );
					}
				}
				catch (Exception e) {
					throw new QueryException(
							"Could not instantiate: " + 
							holderConstructor.getDeclaringClass(), 
							e
					);
				}
			}
		}
	}

	public boolean hasNext() {
		return hasNext;
	}

	public Object next() {
		if ( !hasNext ) throw new NoSuchElementException("No more results");
		try {
			currentResult = nextResult;
			postNext();
			log.debug("returning current results");
			return currentResult;
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					sess.getFactory().getSQLExceptionConverter(),
					sqle,
					"could not get next iterator result"
			);
		}
	}

	public void remove() {
		if (!single) throw new UnsupportedOperationException("Not a single column hibernate query result set");
		if (currentResult==null) throw new IllegalStateException("Called Iterator.remove() before next()");
		sess.delete(currentResult);
	}

}
