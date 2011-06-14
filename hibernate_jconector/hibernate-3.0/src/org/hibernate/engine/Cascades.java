//$Id: Cascades.java,v 1.35 2005/02/21 14:39:01 oneovthafew Exp $
package org.hibernate.engine;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.ReplicationMode;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.id.IdentifierGeneratorFactory;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.EntityType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;

/**
 * Implements cascaded save / delete / update / lock / evict / replicate / persist / merge
 *
 * @see org.hibernate.type.AssociationType
 * @author Gavin King
 */
public final class Cascades {

	private Cascades() {}

	private static final Log log = LogFactory.getLog(Cascades.class);

	// The available cascade actions:

	/**
	 * A session action that may be cascaded from parent entity to its children
	 */
	public abstract static class CascadingAction {
		protected CascadingAction() {}
		/**
		 * cascade the action to the child object
		 */
		abstract void cascade(SessionImplementor session, Object child, String entityName, Object anything, boolean isCascadeDeleteEnabled) 
		throws HibernateException;
		/**
		 * Should this action be cascaded to the given (possibly uninitialized) collection?
		 */
		abstract Iterator getCascadableChildrenIterator(SessionImplementor session, CollectionType collectionType, Object collection);
		/**
		 * Do we need to handle orphan delete for this action?
		 */
		abstract boolean deleteOrphans();
	}

	/**
	 * @see org.hibernate.Session#delete(Object)
	 */
	public static final CascadingAction ACTION_DELETE = new CascadingAction() {
		void cascade(SessionImplementor session, Object child, String entityName, Object anything, boolean isCascadeDeleteEnabled) 
		throws HibernateException {
			if ( log.isTraceEnabled() ) log.trace("cascading to delete: " + entityName);
			if ( ForeignKeys.isNotTransient(entityName, child, null, session) ) session.delete(entityName, child, isCascadeDeleteEnabled);
		}
		Iterator getCascadableChildrenIterator(SessionImplementor session, CollectionType collectionType, Object collection) {
			// delete does cascade to uninitialized collections
			return getAllElementsIterator(session, collectionType, collection);
		}
		boolean deleteOrphans() {
			// orphans should be deleted during delete
			return true;
		}
		public String toString() {
			return "ACTION_DELETE";
		}
	};

	/**
	 * @see org.hibernate.Session#lock(Object, LockMode)
	 */
	public static final CascadingAction ACTION_LOCK = new CascadingAction() {
		void cascade(SessionImplementor session, Object child, String entityName, Object anything, boolean isCascadeDeleteEnabled) 
		throws HibernateException {
			if ( log.isTraceEnabled() ) log.trace("cascading to lock: " + entityName);
			session.lock( entityName, child, LockMode.NONE/*(LockMode) anything*/ );
		}
		Iterator getCascadableChildrenIterator(SessionImplementor session, CollectionType collectionType, Object collection) {
			// lock doesn't cascade to uninitialized collections
			return getLoadedElementsIterator(session, collectionType, collection);
		}
		boolean deleteOrphans() {
			//TODO: should orphans really be deleted during lock???
			return false;
		}
		public String toString() {
			return "ACTION_LOCK";
		}
	};

	/**
	 * @see org.hibernate.Session#refresh(Object)
	 */
	public static final CascadingAction ACTION_REFRESH = new CascadingAction() {
		void cascade(SessionImplementor session, Object child, String entityName, Object anything, boolean isCascadeDeleteEnabled) 
		throws HibernateException {
			if ( log.isTraceEnabled() ) log.trace("cascading to refresh: " + entityName);
			session.refresh(child);
		}
		Iterator getCascadableChildrenIterator(SessionImplementor session, CollectionType collectionType, Object collection) {
			// refresh doesn't cascade to uninitialized collections
			return getLoadedElementsIterator(session, collectionType, collection);
		}
		boolean deleteOrphans() {
			return false;
		}
		public String toString() {
			return "ACTION_REFRESH";
		}
	};

	/**
	 * @see org.hibernate.Session#evict(Object)
	 */
	public static final CascadingAction ACTION_EVICT = new CascadingAction() {
		void cascade(SessionImplementor session, Object child, String entityName, Object anything, boolean isCascadeDeleteEnabled) 
		throws HibernateException {
			if ( log.isTraceEnabled() ) log.trace("cascading to evict: " + entityName);
			session.evict(child);
		}
		Iterator getCascadableChildrenIterator(SessionImplementor session, CollectionType collectionType, Object collection) {
			// evicts don't cascade to uninitialized collections
			return getLoadedElementsIterator(session, collectionType, collection);
		}
		boolean deleteOrphans() {
			return false;
		}
		public String toString() {
			return "ACTION_EVICT";
		}
	};

	/**
	 * @see org.hibernate.Session#saveOrUpdate(Object)
	 */
	public static final CascadingAction ACTION_SAVE_UPDATE = new CascadingAction() {
		void cascade(SessionImplementor session, Object child, String entityName, Object anything, boolean isCascadeDeleteEnabled) 
		throws HibernateException {
			if ( log.isTraceEnabled() ) log.trace("cascading to saveOrUpdate: " + entityName);
			session.saveOrUpdate(entityName, child);
		}
		Iterator getCascadableChildrenIterator(SessionImplementor session, CollectionType collectionType, Object collection) {
			// saves / updates don't cascade to uninitialized collections
			return getLoadedElementsIterator(session, collectionType, collection);
		}
		boolean deleteOrphans() {
			// orphans should be deleted during save/update
			return true;
		}
		public String toString() {
			return "ACTION_SAVE_UPDATE";
		}
	};

	/**
	 * @see org.hibernate.Session#merge(Object)
	 */
	public static final CascadingAction ACTION_MERGE = new CascadingAction() {
		void cascade(SessionImplementor session, Object child, String entityName, Object anything, boolean isCascadeDeleteEnabled) 
		throws HibernateException {
			if ( log.isTraceEnabled() ) log.trace("cascading to merge: " + entityName);
			session.merge( entityName, child, (Map) anything );
		}
		Iterator getCascadableChildrenIterator(SessionImplementor session, CollectionType collectionType, Object collection) {
			// saves / updates don't cascade to uninitialized collections
			return getLoadedElementsIterator(session, collectionType, collection);
		}
		boolean deleteOrphans() {
			// orphans should not be deleted during copy??
			return false;
		}
		public String toString() {
			return "ACTION_MERGE";
		}
	};

	/**
	 * @see org.hibernate.classic.Session#saveOrUpdateCopy(Object)
	 */
	public static final CascadingAction ACTION_SAVE_UPDATE_COPY = new CascadingAction() {
		// for deprecated saveOrUpdateCopy()
		void cascade(SessionImplementor session, Object child, String entityName, Object anything, boolean isCascadeDeleteEnabled) 
		throws HibernateException {
			if ( log.isTraceEnabled() ) log.trace("cascading to saveOrUpdateCopy: " + entityName);
			session.saveOrUpdateCopy( entityName, child, (Map) anything );
		}
		Iterator getCascadableChildrenIterator(SessionImplementor session, CollectionType collectionType, Object collection) {
			// saves / updates don't cascade to uninitialized collections
			return getLoadedElementsIterator(session, collectionType, collection);
		}
		boolean deleteOrphans() {
			// orphans should not be deleted during copy??
			return false;
		}
		public String toString() {
			return "ACTION_SAVE_UPDATE_COPY";
		}
	};

	/**
	 * @see org.hibernate.Session#persist(Object)
	 */
	public static final CascadingAction ACTION_PERSIST = new CascadingAction() {
		void cascade(SessionImplementor session, Object child, String entityName, Object anything, boolean isCascadeDeleteEnabled) 
		throws HibernateException {
			if ( log.isTraceEnabled() ) log.trace("cascading to persist: " + entityName);
			session.persist( entityName, child, (Map) anything );
		}
		Iterator getCascadableChildrenIterator(SessionImplementor session, CollectionType collectionType, Object collection) {
			// saves / updates don't cascade to uninitialized collections
			return getAllElementsIterator(session, collectionType, collection);
		}
		boolean deleteOrphans() {
			// orphans should not be deleted during create
			return false;
		}
		public String toString() {
			return "ACTION_PERSIST";
		}
	};

	public static final CascadingAction ACTION_REPLICATE = new CascadingAction() {
		void cascade(SessionImplementor session, Object child, String entityName, Object anything, boolean isCascadeDeleteEnabled) 
		throws HibernateException {
			if ( log.isTraceEnabled() ) log.trace("cascading to replicate: " + entityName);
			session.replicate( child, (ReplicationMode) anything );
		}
		Iterator getCascadableChildrenIterator(SessionImplementor session, CollectionType collectionType, Object collection) {
			// replicate does cascade to uninitialized collections
			return getLoadedElementsIterator(session, collectionType, collection);
		}
		boolean deleteOrphans() {
			return false; //I suppose?
		}
		public String toString() {
			return "ACTION_REPLICATE";
		}
	};

	private static boolean collectionIsInitialized(Object collection) {
		return !(collection instanceof PersistentCollection) || ( (PersistentCollection) collection ).wasInitialized();
	}

	// The types of children to cascade to:

	/**
	 * A cascade point that occurs just after the insertion of the parent entity and
	 * just before deletion
	 */
	public static final int CASCADE_AFTER_INSERT_BEFORE_DELETE = 1;
	/**
	 * A cascade point that occurs just before the insertion of the parent entity and
	 * just after deletion
	 */
	public static final int CASCADE_BEFORE_INSERT_AFTER_DELETE = 2;
	/**
	 * A cascade point that occurs just after the insertion of the parent entity and
	 * just before deletion, inside a collection
	 */
	public static final int CASCADE_AFTER_INSERT_BEFORE_DELETE_VIA_COLLECTION = 3;
	/**
	 * A cascade point that occurs just after update of the parent entity
	 */
	public static final int CASCADE_AFTER_UPDATE = 0;
	/**
	 * A cascade point that occurs just before the session is flushed
	 */
	public static final int CASCADE_BEFORE_FLUSH = 0;
	/**
	 * A cascade point that occurs just after eviction of the parent entity from the
	 * session cache
	 */
	public static final int CASCADE_AFTER_EVICT = 0;
	/**
	 * A cascade point that occurs just after locking a transient parent entity into the
	 * session cache
	 */
	public static final int CASCADE_AFTER_REFRESH = 0;
	/**
	 * A cascade point that occurs just after refreshing a parent entity
	 */
	public static final int CASCADE_AFTER_LOCK = 0;
	/**
	 * A cascade point that occurs just before merging from a transient parent entity into
	 * the object in the session cache
	 */
	public static final int CASCADE_BEFORE_MERGE = 0;

	// The allowable cascade styles for a property:

	/**
	 * A style of cascade that can be specified by the mapping for an association.
	 * The style is specified by the <tt>cascade</tt> attribute in the mapping file.
	 */
	public abstract static class CascadeStyle implements Serializable {
		protected CascadeStyle() {}
		/**
		 * Should the given action be cascaded?
		 */
		abstract boolean doCascade(CascadingAction action);
		/**
		 * Should the given action really, really be cascaded?
		 */
		boolean reallyDoCascade(CascadingAction action) {
			return doCascade(action);
		}
		/**
		 * Do we need to delete orphaned collection elements?
		 */
		boolean hasOrphanDelete() {
			return false;
		}
	}
	
	public static final class MultipleCascadeStyle extends CascadeStyle {
		private final CascadeStyle[] styles;
		public MultipleCascadeStyle(CascadeStyle[] styles) {
			this.styles = styles;
		}
		boolean doCascade(CascadingAction action) {
			for (int i=0; i<styles.length; i++) {
				if ( styles[i].doCascade(action) ) return true;
			}
			return false;
		}
		boolean reallyDoCascade(CascadingAction action) {
			for (int i=0; i<styles.length; i++) {
				if ( styles[i].reallyDoCascade(action) ) return true;
			}
			return false;
		}
		boolean hasOrphanDelete() {
			for (int i=0; i<styles.length; i++) {
				if ( styles[i].hasOrphanDelete() ) return true;
			}
			return false;
		}
		public String toString() {
			return ArrayHelper.toString(styles);
		}
	}
	
	/**
	 * save / delete / update / evict / lock / replicate / merge / persist + delete orphans
	 */
	public static final CascadeStyle STYLE_ALL_DELETE_ORPHAN = new CascadeStyle() {
		boolean doCascade(CascadingAction action) {
			return true;
		}
		boolean hasOrphanDelete() {
			return true;
		}
		public String toString() {
			return "STYLE_ALL_DELETE_ORPHAN";
		}
	};
	/**
	 * save / delete / update / evict / lock / replicate / merge / persist
	 */
	public static final CascadeStyle STYLE_ALL = new CascadeStyle() {
		boolean doCascade(CascadingAction action) {
			return true;
		}
		public String toString() {
			return "STYLE_ALL";
		}
	};
	/**
	 * save / update
	 */
	public static final CascadeStyle STYLE_SAVE_UPDATE = new CascadeStyle() {
		boolean doCascade(CascadingAction action) {
			return action==ACTION_SAVE_UPDATE || action==ACTION_SAVE_UPDATE_COPY;
		}
		public String toString() {
			return "STYLE_SAVE_UPDATE";
		}
	};
	/**
	 * lock
	 */
	public static final CascadeStyle STYLE_LOCK = new CascadeStyle() {
		boolean doCascade(CascadingAction action) {
			return action==ACTION_LOCK;
		}
		public String toString() {
			return "STYLE_LOCK";
		}
	};
	/**
	 * refresh
	 */
	public static final CascadeStyle STYLE_REFRESH = new CascadeStyle() {
		boolean doCascade(CascadingAction action) {
			return action==ACTION_REFRESH;
		}
		public String toString() {
			return "STYLE_REFRESH";
		}
	};
	/**
	 * evict
	 */
	public static final CascadeStyle STYLE_EVICT = new CascadeStyle() {
		boolean doCascade(CascadingAction action) {
			return action==ACTION_EVICT;
		}
		public String toString() {
			return "STYLE_EVICT";
		}
	};
	/**
	 * replicate
	 */
	public static final CascadeStyle STYLE_REPLICATE = new CascadeStyle() {
		boolean doCascade(CascadingAction action) {
			return action==ACTION_REPLICATE;
		}
		public String toString() {
			return "STYLE_REPLICATE";
		}
	};
	/**
	 * merge
	 */
	public static final CascadeStyle STYLE_MERGE = new CascadeStyle() {
		boolean doCascade(CascadingAction action) {
			return action==ACTION_MERGE;
		}
		public String toString() {
			return "STYLE_MERGE";
		}
	};
	/**
	 * create
	 */
	public static final CascadeStyle STYLE_PERSIST = new CascadeStyle() {
		boolean doCascade(CascadingAction action) {
			return action==ACTION_PERSIST;
		}
		public String toString() {
			return "STYLE_PERSIST";
		}
	};
	/**
	 * delete
	 */
	public static final CascadeStyle STYLE_DELETE = new CascadeStyle() {
		boolean doCascade(CascadingAction action) {
			return action==ACTION_DELETE;
		}
		public String toString() {
			return "STYLE_DELETE";
		}
	};
	/**
	 * delete + delete orphans
	 */
	public static final CascadeStyle STYLE_DELETE_ORPHAN = new CascadeStyle() {
		boolean doCascade(CascadingAction action) {
			return action==ACTION_DELETE || action==ACTION_SAVE_UPDATE;
		}
		boolean reallyDoCascade(CascadingAction action) {
			return action==ACTION_DELETE;
		}
		boolean hasOrphanDelete() {
			return true;
		}
		public String toString() {
			return "STYLE_DELETE_ORPHAN";
		}
	};
	/**
	 * no cascades
	 */
	public static final CascadeStyle STYLE_NONE = new CascadeStyle() {
		boolean doCascade(CascadingAction action) {
			return action==ACTION_REPLICATE;
		}
		public String toString() {
			return "STYLE_NONE";
		}
	};

	// The allowable unsaved-value settings:

	/**
	 * A strategy for determining if an identifier value is an identifier of
	 * a new transient instance or a previously persistent transient instance.
	 * The strategy is determined by the <tt>unsaved-value</tt> attribute in
	 * the mapping file.
	 */
	public static class IdentifierValue {
		private final Serializable value;
		
		protected IdentifierValue() {
			this.value = null;
		}
		
		/**
		 * Assume the transient instance is newly instantiated if
		 * its identifier is null or equal to <tt>value</tt>
		 */
		public IdentifierValue(Serializable value) {
			this.value = value;
		}
		
		/**
		 * Does the given identifier belong to a new instance?
		 */
		public Boolean isUnsaved(Serializable id) {
			if ( log.isTraceEnabled() ) log.trace("id unsaved-value: " + value);
			return id==null || id.equals(value) ? Boolean.TRUE : Boolean.FALSE;
		}
		
		public Serializable getDefaultValue(Serializable currentValue) {
			return value;
		}
		
		public String toString() {
			return "identifier unsaved-value: " + value;
		}
	}

	/**
	 * Always assume the transient instance is newly instantiated
	 */
	public static final IdentifierValue SAVE_ANY = new IdentifierValue() {
		public final Boolean isUnsaved(Serializable id) {
			log.trace("id unsaved-value strategy ANY");
			return Boolean.TRUE;
		}
		public Serializable getDefaultValue(Serializable currentValue) {
			return currentValue;
		}
		public String toString() {
			return "SAVE_ANY";
		}
	};
	/**
	 * Never assume the transient instance is newly instantiated
	 */
	public static final IdentifierValue SAVE_NONE = new IdentifierValue() {
		public final Boolean isUnsaved(Serializable id) {
			log.trace("id unsaved-value strategy NONE");
			return Boolean.FALSE;
		}
		public Serializable getDefaultValue(Serializable currentValue) {
			return currentValue;
		}
		public String toString() {
			return "SAVE_NONE";
		}
	};
	/**
	 * Assume the transient instance is newly instantiated if the identifier
	 * is null.
	 */
	public static final IdentifierValue SAVE_NULL = new IdentifierValue() {
		public final Boolean isUnsaved(Serializable id) {
			log.trace("id unsaved-value strategy NULL");
			return id==null ? Boolean.TRUE : Boolean.FALSE;
		}
		public Serializable getDefaultValue(Serializable currentValue) {
			return null;
		}
		public String toString() {
			return "SAVE_NULL";
		}
	};

	public static final IdentifierValue UNDEFINED = new IdentifierValue() {
		public final Boolean isUnsaved(Serializable id) {
			log.trace("id unsaved-value strategy UNDEFINED");
			return null;
		}
		public Serializable getDefaultValue(Serializable currentValue) {
			return null;
		}
		public String toString() {
			return "UNDEFINED";
		}
	};

	/**
	 * A strategy for determining if a version value is an version of
	 * a new transient instance or a previously persistent transient instance.
	 * The strategy is determined by the <tt>unsaved-value</tt> attribute in
	 * the mapping file.
	 */
	public static class VersionValue {

		private final Object value;
		
		protected VersionValue() {
			this.value = null;
		}

		/**
		 * Assume the transient instance is newly instantiated if
		 * its version is null or equal to <tt>value</tt>
		 * @param value value to compare to
		 */
		public VersionValue(Object value) {
			this.value = value;
		}
		
		/**
		 * Does the given version belong to a new instance?
		 *
		 * @param version version to check
		 * @return true is unsaved, false is saved, null is undefined
		 */
		public Boolean isUnsaved(Object version) throws MappingException  {
			if ( log.isTraceEnabled() ) log.trace("version unsaved-value: " + value);
			return version==null || version.equals(value) ? Boolean.TRUE : Boolean.FALSE;
		}
		
		public Object getDefaultValue(Object currentValue) {
			return value;
		}
		
		public String toString() {
			return "version unsaved-value: " + value;
		}
	}

	/**
	 * Assume the transient instance is newly instantiated if the version
	 * is null, otherwise assume it is a detached instance.
	 */
	public static final VersionValue VERSION_SAVE_NULL = new VersionValue() {
		public final Boolean isUnsaved(Object version) {
			log.trace("version unsaved-value strategy NULL");
			return version==null ? Boolean.TRUE : Boolean.FALSE;
		}
		public Object getDefaultValue(Object currentValue) {
			return null;
		}
		public String toString() {
			return "VERSION_SAVE_NULL";
		}
	};

	/**
	 * Assume the transient instance is newly instantiated if the version
	 * is null, otherwise defer to the identifier unsaved-value.
	 */
	public static final VersionValue VERSION_UNDEFINED = new VersionValue() {
		public final Boolean isUnsaved(Object version) {
			log.trace("version unsaved-value strategy UNDEFINED");
			return version==null ? Boolean.TRUE : null;
		}
		public Object getDefaultValue(Object currentValue) {
			return currentValue;
		}
		public String toString() {
			return "VERSION_UNDEFINED";
		}
	};

	/**
	 * Assume the transient instance is newly instantiated if the version
	 * is negative, otherwise assume it is a detached instance.
	 */
	public static final VersionValue VERSION_NEGATIVE = new VersionValue() {

		public final Boolean isUnsaved(Object version) throws MappingException {
			log.trace("version unsaved-value strategy NEGATIVE");
			if (version==null) return Boolean.TRUE;
			if (version instanceof Number) {
				return ( (Number) version ).longValue() < 0l ? Boolean.TRUE : Boolean.FALSE;
			}
			else {
				throw new MappingException("unsaved-value NEGATIVE may only be used with short, int and long types");
			}
		}
		public Object getDefaultValue(Object currentValue) {
			return IdentifierGeneratorFactory.createNumber( -1l, currentValue.getClass() );
		}
		public String toString() {
			return "VERSION_NEGATIVE";
		}
	};

	/**
	 * Cascade an action to the child or children
	 */
	private static void cascade(
		final SessionImplementor session,
		final Object child,
		final Type type,
		final CascadingAction action,
		final CascadeStyle style,
		final int cascadeTo,
		final Object anything,
		final boolean isCascadeDeleteEnabled) 
	throws HibernateException {

		if (child!=null) {
			if ( type.isAssociationType() ) {
				if ( ( (AssociationType) type ).getForeignKeyDirection().cascadeNow(cascadeTo) ) {
					if ( type.isEntityType() || type.isAnyType() ) {
						final String entityName = type.isEntityType() ?
								( (EntityType) type ).getAssociatedEntityName() :
								null;
						if ( style.reallyDoCascade(action) ) { //not really necessary, but good for consistency...
							action.cascade(session, child, entityName, anything, isCascadeDeleteEnabled);
						}
					}
					else if ( type.isCollectionType() ) {
						final int cascadeVia;
						if ( cascadeTo==CASCADE_AFTER_INSERT_BEFORE_DELETE) {
							cascadeVia = CASCADE_AFTER_INSERT_BEFORE_DELETE_VIA_COLLECTION;
						}
						else {
							cascadeVia = cascadeTo;
						}
						CollectionType pctype = (CollectionType) type;
						CollectionPersister persister = session.getFactory()
								.getCollectionPersister( pctype.getRole() );
						Type elemType = persister.getElementType();

						//cascade to current collection elements
						if ( elemType.isEntityType() || elemType.isAnyType() || elemType.isComponentType() ) {
							cascadeCollection(
								action, 
								style, 
								pctype, 
								elemType, 
								child, 
								cascadeVia, 
								session, 
								anything, 
								persister.isCascadeDeleteEnabled() 
							);
						}
					}

				}
			}
			else if ( type.isComponentType() ) {
				AbstractComponentType componentType = (AbstractComponentType) type;
				Object[] children = componentType.getPropertyValues(child, session);
				Type[] types = componentType.getSubtypes();
				for ( int i=0; i<types.length; i++ ) {
					CascadeStyle componentPropertyStyle = componentType.getCascadeStyle(i);
					if ( componentPropertyStyle.doCascade(action) ) {
						cascade( 
								session, 
								children[i], 
								types[i], 
								action, 
								componentPropertyStyle, 
								cascadeTo, 
								anything, 
								false
						);
					}
				}
			}
		}
	}

	/**
	 * Cascade an action from the parent object to all its children
	 */
	public static void cascade(
		final SessionImplementor session,
		final EntityPersister persister,
		final Object parent,
		final Cascades.CascadingAction action,
		final int cascadeTo) 
	throws HibernateException {

		cascade(session, persister, parent, action, cascadeTo, null);
	}

	/**
	 * Cascade an action from the parent object to all its children
	 */
	public static void cascade(
		final SessionImplementor session,
		final EntityPersister persister,
		final Object parent,
		final Cascades.CascadingAction action,
		final int cascadeTo,
		final Object anything) 
	throws HibernateException {

		if ( persister.hasCascades() ) { // performance opt
			if ( log.isTraceEnabled() ) {
				log.trace( "processing cascade " + action + " for: " + persister.getEntityName() );
			}
			
			Type[] types = persister.getPropertyTypes();
			Cascades.CascadeStyle[] cascadeStyles = persister.getPropertyCascadeStyles();
			for ( int i=0; i<types.length; i++) {
				CascadeStyle style = cascadeStyles[i];
				if ( style.doCascade(action) ) { 
					// associations cannot be field-level lazy="true", so don't 
					// need to check that the field is fetched (laziness for
					// associations is always done by proxying currently)
					cascade(
					        session,
					        persister.getPropertyValue( parent, i, session.getEntityMode() ),
					        types[i],
					        action,
					        style,
					        cascadeTo,
					        anything,
					        false
					);
				}
			}
			
			if ( log.isTraceEnabled() ) {
				log.trace( "done processing cascade " + action + " for: " + persister.getEntityName() );
			}
		}
	}

	/**
	 * Cascade to the collection elements
	 */
	private static void cascadeCollection(
		final CascadingAction action,
		final CascadeStyle style,
		final CollectionType collectionType,
		final Type elemType,
		final Object child,
		final int cascadeVia,
		final SessionImplementor session,
		final Object anything,
		final boolean isCascadeDeleteEnabled) 
	throws HibernateException {
		
		if (child==CollectionType.UNFETCHED_COLLECTION) return; //EARLY EXIT
		
		if ( style.reallyDoCascade(action) ) {
			if ( log.isTraceEnabled() ) {
				log.trace( "cascade " + action + " for collection: " + collectionType.getRole() );
			}
			
			Iterator iter = action.getCascadableChildrenIterator(session, collectionType, child);
			while ( iter.hasNext() ) {
				cascade( 
						session, 
						iter.next(), 
						elemType, 
						action, 
						style, 
						cascadeVia, 
						anything, 
						isCascadeDeleteEnabled 
				);
			}
			
			if ( log.isTraceEnabled() ) {
				log.trace( "done cascade " + action + " for collection: " + collectionType.getRole() );
			}
		}
		
		final boolean deleteOrphans = style.hasOrphanDelete() && 
				action.deleteOrphans() && 
				elemType.isEntityType() && 
				child instanceof PersistentCollection; //a newly instantiated collection can't have orphans
		
		if ( deleteOrphans ) { // handle orphaned entities!!
			if ( log.isTraceEnabled() ) {
				log.trace( "deleting orphans for collection: " + collectionType.getRole() );
			}
			
			// we can do the cast since orphan-delete does not apply to:
			// 1. newly instantiated collections
			// 2. arrays (we can't track orphans for detached arrays)
			final String entityName = collectionType.getAssociatedEntityName( session.getFactory() );
			deleteOrphans( entityName, (PersistentCollection) child, session );
			
			if ( log.isTraceEnabled() ) {
				log.trace( "done deleting orphans for collection: " + collectionType.getRole() );
			}
		}
	}

	/**
	 * Delete any entities that were removed from the collection
	 */
	private static void deleteOrphans(String entityName, PersistentCollection pc, SessionImplementor session) 
			throws HibernateException {
		if ( pc.wasInitialized() ) { //can't be any orphans if it was not initialized!
			CollectionEntry ce = session.getPersistenceContext().getCollectionEntry(pc);
			if (ce!=null) {
				Iterator orphanIter = ce.getOrphans(entityName, pc).iterator();
				while ( orphanIter.hasNext() ) {
					Object orphan = orphanIter.next();
					if (orphan!=null) {
						if ( log.isTraceEnabled() ) log.trace("deleting orphaned: " + entityName);
						session.delete(entityName, orphan, false);
					}
				}
			}
		}
	}

	/**
	 * Iterate just the elements of the collection that are already there. Don't load
	 * any new elements from the database.
	 */
	public static Iterator getLoadedElementsIterator(SessionImplementor session, CollectionType collectionType, Object collection) {
		if ( collectionIsInitialized(collection) ) {
			// handles arrays and newly instantiated collections
			return collectionType.getElementsIterator(collection, session);
		}
		else {
			// does not handle arrays (thats ok, cos they can't be lazy)
			// or newly instantiated collections, so we can do the cast
			return ( (PersistentCollection) collection ).queuedAdditionIterator();
		}
	}

	/**
	 * Iterate all the collection elements, loading them from the database if necessary.
	 */
	private static Iterator getAllElementsIterator(SessionImplementor session, CollectionType collectionType, Object collection) {
		return collectionType.getElementsIterator(collection, session);
	}
	
	private static final Map STYLES = new HashMap();
	static {
		STYLES.put("all", STYLE_ALL);
		STYLES.put("all-delete-orphan", STYLE_ALL_DELETE_ORPHAN);
		STYLES.put("save-update", STYLE_SAVE_UPDATE);
		STYLES.put("persist", STYLE_PERSIST);
		STYLES.put("merge", STYLE_MERGE);
		STYLES.put("lock", STYLE_LOCK);
		STYLES.put("refresh", STYLE_REFRESH);
		STYLES.put("replicate", STYLE_REPLICATE);
		STYLES.put("evict", STYLE_EVICT);
		STYLES.put("delete", STYLE_DELETE);
		STYLES.put("delete-orphan", STYLE_DELETE_ORPHAN);
		STYLES.put("none", STYLE_NONE);
	}
	
	public static CascadeStyle getCascadeStyle(String cascade) {
		CascadeStyle style = (CascadeStyle) STYLES.get(cascade);
		if (style==null) {
			throw new MappingException("Unsupported cascade style: " + cascade);
		}
		else {
			return style;
		}	
	}

}
