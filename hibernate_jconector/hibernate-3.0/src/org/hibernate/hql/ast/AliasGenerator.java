// $Id: AliasGenerator.java,v 1.10 2005/02/12 07:19:19 steveebersole Exp $
package org.hibernate.hql.ast;

import org.hibernate.util.StringHelper;

/**
 * Generates class/table/column aliases during semantic analysis and SQL rendering.
 */
class AliasGenerator {
	private int next = 0;

	AliasGenerator() {
	}

	private int nextCount() {
		return next++;
	}

	String createName(String name) {
		return StringHelper.generateAlias( name, nextCount() );
	}

}
