//$Id: LoadEventListener.java,v 1.2 2004/08/08 11:24:54 oneovthafew Exp $
package org.hibernate.event;

import org.hibernate.HibernateException;

import java.io.Serializable;

/**
 * Defines the contract for handling of load events generated from a session.
 *
 * @author Steve Ebersole
 */
public interface LoadEventListener extends Serializable {

	public static final LoadType GET = new LoadType("get");
	public static final LoadType LOAD = new LoadType("load");
	public static final LoadType IMMEDIATE_LOAD = new LoadType("immediate load");
	public static final LoadType INTERNAL_LOAD = new LoadType("internal load");
	public static final LoadType INTERNAL_LOAD_ONE_TO_ONE = new LoadType("internal load (one to one)");

    /** Handle the given load event.
     *
     * @param event The load event to be handled.
     * @return The result (i.e., the loaded entity).
     * @throws HibernateException
     */
	public Object onLoad(LoadEvent event, LoadType loadType) throws HibernateException;

	public static final class LoadType {
		private String name;

        private LoadType(String name) {
	        this.name = name;
        }

		public String getName() {
			return name;
		}
	}
}
