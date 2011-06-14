//$Id: SessionEventListenerConfig.java,v 1.15 2005/03/23 20:47:25 steveebersole Exp $
package org.hibernate.event;

import java.io.Serializable;

import org.hibernate.event.def.DefaultAutoFlushEventListener;
import org.hibernate.event.def.DefaultPersistEventListener;
import org.hibernate.event.def.DefaultDeleteEventListener;
import org.hibernate.event.def.DefaultDirtyCheckEventListener;
import org.hibernate.event.def.DefaultEvictEventListener;
import org.hibernate.event.def.DefaultFlushEntityEventListener;
import org.hibernate.event.def.DefaultFlushEventListener;
import org.hibernate.event.def.DefaultInitializeCollectionEventListener;
import org.hibernate.event.def.DefaultLoadEventListener;
import org.hibernate.event.def.DefaultLockEventListener;
import org.hibernate.event.def.DefaultMergeEventListener;
import org.hibernate.event.def.DefaultPostDeleteEventListener;
import org.hibernate.event.def.DefaultPostInsertEventListener;
import org.hibernate.event.def.DefaultPostLoadEventListener;
import org.hibernate.event.def.DefaultPostUpdateEventListener;
import org.hibernate.event.def.DefaultPreDeleteEventListener;
import org.hibernate.event.def.DefaultPreInsertEventListener;
import org.hibernate.event.def.DefaultPreLoadEventListener;
import org.hibernate.event.def.DefaultPreUpdateEventListener;
import org.hibernate.event.def.DefaultRefreshEventListener;
import org.hibernate.event.def.DefaultReplicateEventListener;
import org.hibernate.event.def.DefaultSaveEventListener;
import org.hibernate.event.def.DefaultSaveOrUpdateEventListener;
import org.hibernate.event.def.DefaultUpdateEventListener;

/**
 * A convience holder for all defined session event listeners.
 *
 * @author Steve Ebersole
 */
public class SessionEventListenerConfig implements Serializable {

	private LoadEventListener loadEventListener = new DefaultLoadEventListener();
	private SaveOrUpdateEventListener saveOrUpdateEventListener = new DefaultSaveOrUpdateEventListener();
	private MergeEventListener mergeEventListener = new DefaultMergeEventListener();
	private PersistEventListener createEventListener = new DefaultPersistEventListener();
	private ReplicateEventListener replicateEventListener = new DefaultReplicateEventListener();
	private DeleteEventListener deleteEventListener = new DefaultDeleteEventListener();
	private AutoFlushEventListener autoFlushEventListener = new DefaultAutoFlushEventListener();
	private DirtyCheckEventListener dirtyCheckEventListener = new DefaultDirtyCheckEventListener();
	private FlushEventListener flushEventListener = new DefaultFlushEventListener();
	private EvictEventListener evictEventListener = new DefaultEvictEventListener();
	private LockEventListener lockEventListener = new DefaultLockEventListener();
	private RefreshEventListener refreshEventListener = new DefaultRefreshEventListener();
	private FlushEntityEventListener flushEntityEventListener = new DefaultFlushEntityEventListener();
	private InitializeCollectionEventListener initializeCollectionEventListener = new DefaultInitializeCollectionEventListener();
	
	private PostLoadEventListener postLoadEventListener = new DefaultPostLoadEventListener();
	private PreLoadEventListener preLoadEventListener = new DefaultPreLoadEventListener();
	
	private PostDeleteEventListener postDeleteEventListener = new DefaultPostDeleteEventListener();
	private PostUpdateEventListener postUpdateEventListener = new DefaultPostUpdateEventListener();
	private PostInsertEventListener postInsertEventListener = new DefaultPostInsertEventListener();
	private PreDeleteEventListener preDeleteEventListener = new DefaultPreDeleteEventListener();
	private PreUpdateEventListener preUpdateEventListener = new DefaultPreUpdateEventListener();
	private PreInsertEventListener preInsertEventListener = new DefaultPreInsertEventListener();
	
	
	//for deprecated saveOrUpdateCopy()
	private MergeEventListener saveOrUpdateCopyEventListener = new DefaultMergeEventListener(true);

	private SaveOrUpdateEventListener saveEventListener = new DefaultSaveEventListener();
	private SaveOrUpdateEventListener updateEventListener = new DefaultUpdateEventListener();

    public LoadEventListener getLoadEventListener() {
        return loadEventListener;
    }

    public void setLoadEventListener(LoadEventListener loadEventListener) {
        this.loadEventListener = loadEventListener;
    }

	public ReplicateEventListener getReplicateEventListener() {
		return replicateEventListener;
	}

	public void setReplicateEventListener(ReplicateEventListener replicateEventListener) {
		this.replicateEventListener = replicateEventListener;
	}

	public DeleteEventListener getDeleteEventListener() {
		return deleteEventListener;
	}

	public void setDeleteEventListener(DeleteEventListener deleteEventListener) {
		this.deleteEventListener = deleteEventListener;
	}

	public AutoFlushEventListener getAutoFlushEventListener() {
		return autoFlushEventListener;
	}

	public void setAutoFlushEventListener(AutoFlushEventListener autoFlushEventListener) {
		this.autoFlushEventListener = autoFlushEventListener;
	}

	public DirtyCheckEventListener getDirtyCheckEventListener() {
		return dirtyCheckEventListener;
	}

	public void setDirtyCheckEventListener(DirtyCheckEventListener dirtyCheckEventListener) {
		this.dirtyCheckEventListener = dirtyCheckEventListener;
	}

	public FlushEventListener getFlushEventListener() {
		return flushEventListener;
	}

	public void setFlushEventListener(FlushEventListener flushEventListener) {
		this.flushEventListener = flushEventListener;
	}

	public EvictEventListener getEvictEventListener() {
		return evictEventListener;
	}

	public void setEvictEventListener(EvictEventListener evictEventListener) {
		this.evictEventListener = evictEventListener;
	}

	public LockEventListener getLockEventListener() {
		return lockEventListener;
	}

	public void setLockEventListener(LockEventListener lockEventListener) {
		this.lockEventListener = lockEventListener;
	}

	public RefreshEventListener getRefreshEventListener() {
		return refreshEventListener;
	}

	public void setRefreshEventListener(RefreshEventListener refreshEventListener) {
		this.refreshEventListener = refreshEventListener;
	}

	public InitializeCollectionEventListener getInitializeCollectionEventListener() {
		return initializeCollectionEventListener;
	}

	public void setInitializeCollectionEventListener(InitializeCollectionEventListener initializeCollectionEventListener) {
		this.initializeCollectionEventListener = initializeCollectionEventListener;
	}
	
	public FlushEntityEventListener getFlushEntityEventListener() {
		return flushEntityEventListener;
	}
	
	public void setFlushEntityEventListener(FlushEntityEventListener flushEntityEventListener) {
		this.flushEntityEventListener = flushEntityEventListener;
	}
	
	public SaveOrUpdateEventListener getSaveOrUpdateEventListener() {
		return saveOrUpdateEventListener;
	}
	
	public void setSaveOrUpdateEventListener(SaveOrUpdateEventListener saveOrUpdateEventListener) {
		this.saveOrUpdateEventListener = saveOrUpdateEventListener;
	}
	
	public MergeEventListener getMergeEventListener() {
		return mergeEventListener;
	}
	
	public void setMergeEventListener(MergeEventListener mergeEventListener) {
		this.mergeEventListener = mergeEventListener;
	}
	
	public PersistEventListener getCreateEventListener() {
		return createEventListener;
	}
	
	public void setCreateEventListener(PersistEventListener createEventListener) {
		this.createEventListener = createEventListener;
	}
	
	public MergeEventListener getSaveOrUpdateCopyEventListener() {
		return saveOrUpdateCopyEventListener;
	}
	
	public void setSaveOrUpdateCopyEventListener(MergeEventListener saveOrUpdateCopyEventListener) {
		this.saveOrUpdateCopyEventListener = saveOrUpdateCopyEventListener;
	}
	
	public SaveOrUpdateEventListener getSaveEventListener() {
		return saveEventListener;
	}
	
	public void setSaveEventListener(SaveOrUpdateEventListener saveEventListener) {
		this.saveEventListener = saveEventListener;
	}
	
	public SaveOrUpdateEventListener getUpdateEventListener() {
		return updateEventListener;
	}
	
	public void setUpdateEventListener(SaveOrUpdateEventListener updateEventListener) {
		this.updateEventListener = updateEventListener;
	}

	public PostLoadEventListener getPostLoadEventListener() {
		return postLoadEventListener;
	}

	public void setPostLoadEventListener(PostLoadEventListener postLoadEventListener) {
		this.postLoadEventListener = postLoadEventListener;
	}

	public PreLoadEventListener getPreLoadEventListener() {
		return preLoadEventListener;
	}

	public void setPreLoadEventListener(PreLoadEventListener preLoadEventListener) {
		this.preLoadEventListener = preLoadEventListener;
	}

	public PostDeleteEventListener getPostDeleteEventListener() {
		return postDeleteEventListener;
	}
	public PostInsertEventListener getPostInsertEventListener() {
		return postInsertEventListener;
	}
	public PostUpdateEventListener getPostUpdateEventListener() {
		return postUpdateEventListener;
	}
	
	public void setPostDeleteEventListener(PostDeleteEventListener postDeleteEventListener) {
		this.postDeleteEventListener = postDeleteEventListener;
	}
	public void setPostInsertEventListener(PostInsertEventListener postInsertEventListener) {
		this.postInsertEventListener = postInsertEventListener;
	}
	public void setPostUpdateEventListener(PostUpdateEventListener postUpdateEventListener) {
		this.postUpdateEventListener = postUpdateEventListener;
	}
	
	public PreDeleteEventListener getPreDeleteEventListener() {
		return preDeleteEventListener;
	}
	public void setPreDeleteEventListener(
			PreDeleteEventListener preDeleteEventListener) {
		this.preDeleteEventListener = preDeleteEventListener;
	}
	public PreInsertEventListener getPreInsertEventListener() {
		return preInsertEventListener;
	}
	
	public void setPreInsertEventListener(PreInsertEventListener preInsertEventListener) {
		this.preInsertEventListener = preInsertEventListener;
	}
	public PreUpdateEventListener getPreUpdateEventListener() {
		return preUpdateEventListener;
	}
	public void setPreUpdateEventListener(PreUpdateEventListener preUpdateEventListener) {
		this.preUpdateEventListener = preUpdateEventListener;
	}

	/**
	 * Essentially performs a shallow copy of this SessionEventListenerConfig
	 * instance; meaning the SessionEventListenerConfig itself is cloned, but
	 * the individual listeners are <b>not</b> cloned.
	 *
	 * @return The SessionEventListenerConfig shallow copy.
	 */
	public SessionEventListenerConfig shallowCopy() {
		SessionEventListenerConfig clone = new SessionEventListenerConfig();
		clone.loadEventListener = this.loadEventListener;
		clone.saveOrUpdateEventListener = this.saveOrUpdateEventListener;
		clone.mergeEventListener = this.mergeEventListener;
		clone.createEventListener = this.createEventListener;
		clone.replicateEventListener = this.replicateEventListener;
		clone.deleteEventListener = this.deleteEventListener;
		clone.autoFlushEventListener = this.autoFlushEventListener;
		clone.dirtyCheckEventListener = this.dirtyCheckEventListener;
		clone.flushEventListener = this.flushEventListener;
		clone.evictEventListener = this.evictEventListener;
		clone.lockEventListener = this.lockEventListener;
		clone.refreshEventListener = this.refreshEventListener;
		clone.flushEntityEventListener = this.flushEntityEventListener;
		clone.initializeCollectionEventListener = this.initializeCollectionEventListener;
		clone.postLoadEventListener = this.postLoadEventListener;
		clone.preLoadEventListener = this.preLoadEventListener;
		clone.postDeleteEventListener = this.postDeleteEventListener;
		clone.postUpdateEventListener = this.postUpdateEventListener;
		clone.postInsertEventListener = this.postInsertEventListener;
		clone.preDeleteEventListener = this.preDeleteEventListener;
		clone.preUpdateEventListener = this.preUpdateEventListener;
		clone.preInsertEventListener = this.preInsertEventListener;

		clone.saveOrUpdateCopyEventListener = this.saveOrUpdateCopyEventListener;
		clone.saveEventListener = this.saveEventListener;
		clone.updateEventListener = this.updateEventListener;

		return clone;
	}
}
