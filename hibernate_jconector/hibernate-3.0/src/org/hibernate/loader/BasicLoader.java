//$Id: BasicLoader.java,v 1.6 2005/02/13 11:50:04 oneovthafew Exp $
package org.hibernate.loader;

import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.persister.entity.Loadable;

/**
 * Uses the default mapping from property to result set column 
 * alias defined by the entities' persisters. Used when Hibernate
 * is generating result set column aliases.
 * 
 * @author Gavin King
 */
public abstract class BasicLoader extends Loader {

	protected static final String[] NO_SUFFIX = {""};

	private EntityAliases[] descriptors;

	public BasicLoader(SessionFactoryImplementor factory) {
		super(factory);
	}
	
	protected EntityAliases[] getEntityAliases() {
		return descriptors;
	}
	
	protected abstract String[] getSuffixes();

	protected void postInstantiate() {
		Loadable[] persisters = getEntityPersisters();
		String[] suffixes = getSuffixes();
		descriptors = new EntityAliases[persisters.length];
		for ( int i=0; i<descriptors.length; i++ ) {
			descriptors[i] = new GeneratedEntityAliases( persisters[i], suffixes[i] );
		}
	}
	
	/**
	 * Utility method that generates 0_, 1_ suffixes. Subclasses don't
	 * necessarily need to use this algorithm, but it is intended that
	 * they will in most cases.
	 */
	public static String[] generateSuffixes(int length) {

		if ( length == 0 ) return NO_SUFFIX;

		String[] suffixes = new String[length];
		for ( int i = 0; i < length; i++ ) {
			suffixes[i] = /*StringHelper.UNDERSCORE +*/ Integer.toString( i ) + '_';
		}
		return suffixes;
	}

}
