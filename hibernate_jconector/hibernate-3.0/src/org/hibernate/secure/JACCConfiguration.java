//$Id: JACCConfiguration.java,v 1.3 2005/02/12 07:19:45 steveebersole Exp $
package org.hibernate.secure;

import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyConfigurationFactory;
import javax.security.jacc.PolicyContextException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;

/**
 * Adds Hibernate permissions to roles via JACC
 * 
 * @author Gavin King
 */
public class JACCConfiguration {
	
	private static final Log log = LogFactory.getLog(JACCConfiguration.class);

	private final PolicyConfiguration policyConfiguration;

	public JACCConfiguration(String contextId) throws HibernateException {
		try {
			policyConfiguration = PolicyConfigurationFactory.getPolicyConfigurationFactory()
				.getPolicyConfiguration(contextId, false);
		}
		catch (ClassNotFoundException cnfe) {
			throw new HibernateException("JACC provider class not found", cnfe);
		}
		catch (PolicyContextException pce) {
			throw new HibernateException("policy context exception occurred", pce);
		}
	}
	
	public void addPermission(String role, String entityName, String action) {
		HibernatePermission permission = new HibernatePermission(entityName, action);
		if ( log.isDebugEnabled() ) {
			log.debug("adding permission to role " + role + ": " + permission);
		}
		try {
			policyConfiguration.addToRole(role, permission);
		}
		catch (PolicyContextException pce) {
			throw new HibernateException("policy context exception occurred", pce);
		}
	}
	

}
