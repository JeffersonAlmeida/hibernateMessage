//$Id: HibernateIterator.java,v 1.3 2004/11/21 00:11:24 pgmjsd Exp $
package org.hibernate.engine;

import org.hibernate.JDBCException;

import java.util.Iterator;

/**
 * An iterator that may be "closed"
 * @see org.hibernate.Hibernate#close(java.util.Iterator)
 * @author Gavin King
 */
public interface HibernateIterator extends Iterator {
	public void close() throws JDBCException;
}
