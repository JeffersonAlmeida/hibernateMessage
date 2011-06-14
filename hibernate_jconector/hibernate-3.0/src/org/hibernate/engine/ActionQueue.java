// $Id: ActionQueue.java,v 1.6 2005/03/07 00:40:41 oneovthafew Exp $
package org.hibernate.engine;

import org.hibernate.action.EntityInsertAction;
import org.hibernate.action.EntityDeleteAction;
import org.hibernate.action.Executable;
import org.hibernate.action.EntityUpdateAction;
import org.hibernate.action.CollectionRecreateAction;
import org.hibernate.action.CollectionRemoveAction;
import org.hibernate.action.CollectionUpdateAction;
import org.hibernate.action.EntityIdentityInsertAction;
import org.hibernate.HibernateException;
import org.hibernate.AssertionFailure;
import org.hibernate.cache.CacheException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * Responsible for maintaing the queue of actions related to events.
 * </p>
 * The ActionQueue holds the DML operations queued as part of a session's
 * transactional-write-behind semantics.  DML operations are queued here
 * until a flush forces them to be executed against the database.
 *
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole</a>
 */
public class ActionQueue implements Serializable {

	// todo : remove dependence on session; move to engine package

	private static final Log log = LogFactory.getLog( ActionQueue.class );
	private static final int INIT_QUEUE_LIST_SIZE = 5;

	private SessionImplementor session;

	// Object insertions, updates, and deletions have list semantics because
	// they must happen in the right order so as to respect referential
	// integrity
	private ArrayList insertions;
	private ArrayList deletions;
	private ArrayList updates;
	// Actually the semantics of the next three are really "Bag"
	// Note that, unlike objects, collection insertions, updates,
	// deletions are not really remembered between flushes. We
	// just re-use the same Lists for convenience.
	private ArrayList collectionCreations;
	private ArrayList collectionUpdates;
	private ArrayList collectionRemovals;

	private transient ArrayList executions;

	/**
	 * Constructs an action queue bound to the given session.
	 *
	 * @param session The session "owning" this queue.
	 */
	public ActionQueue(SessionImplementor session) {
		this.session = session;

		insertions = new ArrayList( INIT_QUEUE_LIST_SIZE );
		deletions = new ArrayList( INIT_QUEUE_LIST_SIZE );
		updates = new ArrayList( INIT_QUEUE_LIST_SIZE );

		collectionCreations = new ArrayList( INIT_QUEUE_LIST_SIZE );
		collectionRemovals = new ArrayList( INIT_QUEUE_LIST_SIZE );
		collectionUpdates = new ArrayList( INIT_QUEUE_LIST_SIZE );

		executions = new ArrayList( INIT_QUEUE_LIST_SIZE * 3 );
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		executions = new ArrayList( INIT_QUEUE_LIST_SIZE * 3 );
	}

	public void clear() {
		updates.clear();
		insertions.clear();
		deletions.clear();

		collectionCreations.clear();
		collectionRemovals.clear();
		collectionUpdates.clear();
	}

	public void addAction(EntityInsertAction action) {
		insertions.add( action );
	}

	public void addAction(EntityDeleteAction action) {
		deletions.add( action );
	}

	public void addAction(EntityUpdateAction action) {
		updates.add( action );
	}

	public void addAction(CollectionRecreateAction action) {
		collectionCreations.add( action );
	}

	public void addAction(CollectionRemoveAction action) {
		collectionRemovals.add( action );
	}

	public void addAction(CollectionUpdateAction action) {
		collectionUpdates.add( action );
	}

	public void addAction(EntityIdentityInsertAction insert) {
		insertions.add( insert );
	}

	/**
	 * Perform all currently queued entity-insertion actions.
	 *
	 * @throws HibernateException error executing queued insertion actions.
	 */
	public void executeInserts() throws HibernateException {
		executeActions( insertions );
	}

	/**
	 * Perform all currently queued actions.
	 *
	 * @throws HibernateException error executing queued actions.
	 */
	public void executeActions() throws HibernateException {
		executeActions( insertions );
		executeActions( updates );
		executeActions( collectionRemovals );
		executeActions( collectionUpdates );
		executeActions( collectionCreations );
		executeActions( deletions );
	}

	/**
	 * Prepares the internal action queues for execution.
	 *
	 * @throws HibernateException error preparing actions.
	 */
	public void prepareActions() throws HibernateException {
		prepareActions( collectionRemovals );
		prepareActions( collectionUpdates );
		prepareActions( collectionCreations );
	}

	/**
	 * Performs cleanup of any held cache softlocks.
	 *
	 * @param success Was the transaction successful.
	 */
	public void afterTransactionCompletion(boolean success) {
		int size = executions.size();
		final boolean invalidateQueryCache = session.getFactory().getSettings().isQueryCacheEnabled();
		for ( int i = 0; i < size; i++ ) {
			try {
				Executable exec = ( Executable ) executions.get(i);
				try {
					exec.afterTransactionCompletion( success );
				}
				finally {
					if ( invalidateQueryCache ) {
						session.getFactory().getUpdateTimestampsCache().invalidate( exec.getPropertySpaces() );
					}
				}
			}
			catch (CacheException ce) {
				log.error( "could not release a cache lock", ce );
				// continue loop
			}
			catch (Exception e) {
				throw new AssertionFailure( "Exception releasing cache locks", e );
			}
		}
		executions.clear();
	}

	/**
	 * Check whether the given tables/query-spaces are to be executed against
	 * given the currently queued actions.
	 *
	 * @param tables The table/query-spaces to check.
	 * @return
	 */
	public boolean areTablesToBeUpdated(Set tables) {
		return areTablesToUpdated( updates, tables ) ||
				areTablesToUpdated( insertions, tables ) ||
				areTablesToUpdated( deletions, tables ) ||
				areTablesToUpdated( collectionUpdates, tables ) ||
				areTablesToUpdated( collectionCreations, tables ) ||
				areTablesToUpdated( collectionRemovals, tables );
	}

	public boolean areInsertionsOrDeletionsQueued() {
		return ( insertions.size() > 0 || deletions.size() > 0 );
	}

	private static boolean areTablesToUpdated(List executables, Set set) {
		int size = executables.size();
		for ( int j = 0; j < size; j++ ) {
			Serializable[] spaces = ( (Executable) executables.get(j) ).getPropertySpaces();
			for ( int i = 0; i < spaces.length; i++ ) {
				if ( set.contains( spaces[i] ) ) {
					if ( log.isDebugEnabled() ) log.debug( "changes must be flushed to space: " + spaces[i] );
					return true;
				}
			}
		}
		return false;
	}

	private void executeActions(List list) throws HibernateException {
		int size = list.size();
		for ( int i = 0; i < size; i++ ) {
			execute( (Executable) list.get(i) );
		}
		list.clear();
		session.getBatcher().executeBatch();
	}
	
	public void execute(Executable executable) {
		final boolean lockQueryCache = session.getFactory().getSettings().isQueryCacheEnabled();
		if ( executable.hasAfterTransactionCompletion() || lockQueryCache ) {
			executions.add( executable );
		}
		if (lockQueryCache) {
			session.getFactory()
				.getUpdateTimestampsCache()
				.preinvalidate( executable.getPropertySpaces() );
		}
		executable.execute();
	}

	private void prepareActions(List queue) throws HibernateException {
		int size = queue.size();
		for ( int i=0; i<size; i++ ) {
			Executable executable = ( Executable ) queue.get(i);
			executable.beforeExecutions();
		}
	}

	/**
	 * Returns a string representation of the object.
	 *
	 * @return a string representation of the object.
	 */
	public String toString() {
		return new StringBuffer()
				.append("ActionQueue[insertions=").append(insertions)
				.append(" updates=").append(updates)
		        .append(" deletions=").append(deletions)
				.append(" collectionCreations=").append(collectionCreations)
				.append(" collectionRemovals=").append(collectionRemovals)
				.append(" collectionUpdates=").append(collectionUpdates)
		        .append("]")
				.toString();
	}

	public int numberOfCollectionRemovals() {
		return collectionRemovals.size();
	}

	public int numberOfCollectionUpdates() {
		return collectionUpdates.size();
	}

	public int numberOfCollectionCreations() {
		return collectionCreations.size();
	}

	public int numberOfDeletions() {
		return deletions.size();
	}

	public int numberOfUpdates() {
		return updates.size();
	}

	public int numberOfInsertions() {
		return insertions.size();
	}

	public void sortCollectionActions() {
		if ( session.getFactory().getSettings().isOrderUpdatesEnabled() ) {
			//sort the updates by fk
			java.util.Collections.sort( collectionCreations );
			java.util.Collections.sort( collectionUpdates );
			java.util.Collections.sort( collectionRemovals );
		}
	}

	public void sortUpdateActions() {
		if ( session.getFactory().getSettings().isOrderUpdatesEnabled() ) {
			//sort the updates by pk
			java.util.Collections.sort( updates );
		}
	}

	public ArrayList cloneDeletions() {
		return (ArrayList) deletions.clone();
	}

	public void clearFromFlushNeededCheck(int previousCollectionRemovalSize) {
		collectionCreations.clear();
		collectionUpdates.clear();
		updates.clear();
		// collection deletions are a special case since update() can add
		// deletions of collections not loaded by the session.
		for ( int i = collectionRemovals.size()-1; i >= previousCollectionRemovalSize; i-- ) {
			collectionRemovals.remove(i);
		}
	}

	public List suspendNewDeletions(ArrayList oldDeletions) {
		List suspendedDeletions = deletions.subList( oldDeletions.size(), deletions.size() );
		deletions = oldDeletions;
		return suspendedDeletions;
	}

	public void resumeSuspendedDeletions(List suspendedDeletions) {
		deletions.addAll( suspendedDeletions );
	}

	public boolean hasAnyQueuedActions() {
		return updates.size() > 0 ||
		        insertions.size() > 0 ||
		        deletions.size() > 0 ||
		        collectionUpdates.size() > 0 ||
		        collectionRemovals.size() > 0 ||
		        collectionCreations.size() > 0;
	}
}
