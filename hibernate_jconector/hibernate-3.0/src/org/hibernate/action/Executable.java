//$Id: Executable.java,v 1.4 2005/02/12 07:19:07 steveebersole Exp $
package org.hibernate.action;

import org.hibernate.HibernateException;

import java.io.Serializable;

public interface Executable {
	public void beforeExecutions() throws HibernateException;

	public void execute() throws HibernateException;

	public boolean hasAfterTransactionCompletion();

	public void afterTransactionCompletion(boolean success) throws HibernateException;

	public Serializable[] getPropertySpaces();
}
