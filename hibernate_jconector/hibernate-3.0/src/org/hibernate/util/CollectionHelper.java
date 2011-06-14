//$Id: CollectionHelper.java,v 1.1 2005/02/20 10:07:42 oneovthafew Exp $
package org.hibernate.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gavin King
 */
public final class CollectionHelper {

	public static final List EMPTY_LIST = Collections.unmodifiableList( new ArrayList(0) );
	public static final Collection EMPTY_COLLECTION = Collections.unmodifiableCollection( new ArrayList(0) );
	public static final Map EMPTY_MAP = Collections.unmodifiableMap( new HashMap(0) );

	private CollectionHelper() {}

}
