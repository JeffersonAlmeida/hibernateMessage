//$Id: GeneratedEntityAliases.java,v 1.3 2005/02/21 12:55:29 oneovthafew Exp $
package org.hibernate.loader;

import org.hibernate.persister.entity.Loadable;

/**
 * Describes the SQL result set column aliases for a 
 * select clause generated by an entity persister
 * 
 * @author Gavin King
 */
public class GeneratedEntityAliases implements EntityAliases {
	
	private final String[] suffixedKeyColumns;
	private final String[] suffixedVersionColumn;
	private final String[][] suffixedPropertyColumns;
	private final String suffixedDiscriminatorColumn;
	private final String suffix;
	private final String rowIdAlias;

	/**
	 * Calculate and cache select-clause suffixes.
	 */
	public GeneratedEntityAliases(Loadable persister, String suffix) {
		this.suffix = suffix;
		suffixedKeyColumns = persister.getIdentifierAliases(suffix);
		intern(suffixedKeyColumns);
		suffixedPropertyColumns = getSuffixedPropertyAliases(persister);
		suffixedDiscriminatorColumn = persister.getDiscriminatorAlias(suffix);
		if ( persister.isVersioned() ) {
			suffixedVersionColumn = suffixedPropertyColumns[ persister.getVersionProperty() ];
		}
		else {
			suffixedVersionColumn = null;
		}
		rowIdAlias = Loadable.ROWID_ALIAS + suffix;
	}

	public String[][] getSuffixedPropertyAliases(Loadable persister) {
		int size = persister.getPropertyNames().length;
		String[][] suffixedPropertyAliases = new String[size][];
		for ( int j = 0; j < size; j++ ) {
			suffixedPropertyAliases[j] = persister.getPropertyAliases(suffix, j);
			intern( suffixedPropertyAliases[j] );
		}
		return suffixedPropertyAliases;
	}

	public String[] getSuffixedVersionAliases() {
		return suffixedVersionColumn;
	}

	public String[][] getSuffixedPropertyAliases() {
		return suffixedPropertyColumns;
	}

	public String getSuffixedDiscriminatorAlias() {
		return suffixedDiscriminatorColumn;
	}

	public String[] getSuffixedKeyAliases() {
		return suffixedKeyColumns;
	}

	public String getRowIdAlias() {
		return rowIdAlias;
	}
	
	private static void intern(String[] strings) {
		for (int i=0; i<strings.length; i++ ) {
			strings[i] = strings[i].intern();
		}
	}
	
}
