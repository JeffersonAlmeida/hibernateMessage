//$Id: FlushMode.java,v 1.2 2004/08/15 03:13:38 oneovthafew Exp $
package org.hibernate;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a flushing strategy. The flush process synchronizes
 * database state with session state by detecting state changes
 * and executing SQL statements.
 *
 * @see Session#setFlushMode(FlushMode)
 * @author Gavin King
 */
public final class FlushMode implements Serializable {
	private final int level;
	private final String name;
	private static final Map INSTANCES = new HashMap();

	private FlushMode(int level, String name) {
		this.level=level;
		this.name=name;
	}
	public String toString() {
		return name;
	}
	/**
	 * The <tt>Session</tt> is never flushed unless <tt>flush()</tt>
	 * is explicitly called by the application. This mode is very
	 * efficient for read only transactions.
	 */
	public static final FlushMode NEVER = new FlushMode(0, "NEVER");
	/**
	 * The <tt>Session</tt> is flushed when <tt>Transaction.commit()</tt>
	 * is called.
	 */
	public static final FlushMode COMMIT = new FlushMode(5, "COMMIT");
	/**
	 * The <tt>Session</tt> is sometimes flushed before query execution
	 * in order to ensure that queries never return stale state. This
	 * is the default flush mode.
	 */
	public static final FlushMode AUTO = new FlushMode(10, "AUTO");
	/**
	 * The <tt>Session</tt> is flushed before every query. This is
	 * almost always unnecessary and inefficient.
	 */
	public static final FlushMode ALWAYS = new FlushMode(20, "ALWAYS");
	
	public boolean lessThan(FlushMode other) {
		return this.level<other.level;
	}

	static {
		INSTANCES.put( NEVER.name, NEVER );
		INSTANCES.put( AUTO.name, AUTO );
		INSTANCES.put( ALWAYS.name, ALWAYS );
		INSTANCES.put( COMMIT.name, COMMIT );
	}

	private Object readResolve() {
		return INSTANCES.get(name);
	}

}






