//$Id: SQLQueryReturnProcessor.java,v 1.4 2005/03/19 00:29:13 maxcsaucdk Exp $
package org.hibernate.loader.custom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.MappingException;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.SQLLoadable;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import org.hibernate.util.CollectionHelper;

/**
 * @author Gavin King, Max Andersen
 */
public class SQLQueryReturnProcessor {
	
	private SQLQueryReturn[] queryReturns;
	private SQLQueryScalarReturn[] scalarQueryReturns;

	private final List aliases = new ArrayList();
	private final List persisters = new ArrayList();
	private final List propertyResults = new ArrayList();
	private final List lockModes = new ArrayList();
	private final Map alias2Persister = new HashMap();
	private final Map alias2Return = new HashMap();
	private final Map alias2OwnerAlias = new HashMap();
	
	private final List scalarTypes = new ArrayList();
	private final List scalarColumnAliases = new ArrayList();

	private final SessionFactoryImplementor factory;

	private int collectionOwner;
	private String collectionOwnerAlias;
	private String collectionAlias;
	private QueryableCollection collectionPersister;
	private boolean isCollectionInitializer = false;
	
	
	private SessionFactoryImplementor getFactory() {
		return factory;
	}

	private SQLLoadable getSQLLoadable(String entityName) throws MappingException {
		EntityPersister persister = getFactory().getEntityPersister( entityName );
		if ( !(persister instanceof SQLLoadable) ) {
			throw new MappingException( "class persister is not SQLLoadable: " + entityName );
		}
		return (SQLLoadable) persister;
	}
	
	public SQLQueryReturnProcessor(
			SQLQueryReturn[] queryReturns, 
			SQLQueryScalarReturn[] scalarQueryReturns, 
			SessionFactoryImplementor factory
	) {
		this.queryReturns = queryReturns;
		this.scalarQueryReturns = scalarQueryReturns;
		this.factory = factory;
	}
	
	public void process() {
		
		// first, break down the returns into maps keyed by alias
		// so that role returns can be more easily resolved to their owners
		for (int i=0; i<queryReturns.length; i++) {
			alias2Return.put( queryReturns[i].getAlias(), queryReturns[i] );
			if (queryReturns[i] instanceof SQLQueryJoinReturn) {
				SQLQueryJoinReturn roleReturn = (SQLQueryJoinReturn) queryReturns[i];
				alias2OwnerAlias.put( roleReturn.getAlias(), roleReturn.getOwnerAlias() );
			}
			else if (queryReturns[i] instanceof SQLQueryCollectionReturn) {
				isCollectionInitializer = true;
			}
		}
		
		// Now, process the returns
		for (int i=0; i<queryReturns.length; i++) {
			processReturn( queryReturns[i] );
		}
		if (scalarQueryReturns!=null) {
			for (int i=0; i<scalarQueryReturns.length; i++) {
				processScalarReturn( scalarQueryReturns[i] );
			}
		}
	}

	private void processReturn(SQLQueryReturn rtn) {
		if (rtn instanceof SQLQueryRootReturn) {
			processRootReturn( (SQLQueryRootReturn) rtn );
		}
		else if (rtn instanceof SQLQueryCollectionReturn) {
			processCollectionReturn( (SQLQueryCollectionReturn) rtn );
		}
		else {
			processRoleReturn( (SQLQueryJoinReturn) rtn );
		}
	}
	
	private void processScalarReturn(SQLQueryScalarReturn typeReturn) {
		scalarColumnAliases.add( typeReturn.getColumnAlias() );
		scalarTypes.add( typeReturn.getType() );
	}

	private void processRootReturn(SQLQueryRootReturn rootReturn) {
		if ( alias2Persister.containsKey( rootReturn.getAlias() ) ) {
			// already been processed...
			return;
		}

		SQLLoadable persister = getSQLLoadable( rootReturn.getReturnEntityName() );
		aliases.add( rootReturn.getAlias() );
		addPersister(rootReturn.getPropertyResultsMap(), persister);
		alias2Persister.put( rootReturn.getAlias(), persister );
		lockModes.add( rootReturn.getLockMode() );
	}

	/**
	 * @param map
	 * @param persister
	 */
	private void addPersister(Map map, SQLLoadable persister) {
		persisters.add(persister);
		propertyResults.add(map);
	}
	
	private void addCollection(String role, String alias, Map propertyResults, LockMode lockMode) {
		collectionPersister = (QueryableCollection) getFactory().getCollectionPersister(role);
		collectionAlias = alias;
		
		if ( collectionPersister.isOneToMany() ) {
			SQLLoadable persister = (SQLLoadable) collectionPersister.getElementPersister();
			aliases.add(alias);
			addPersister(propertyResults, persister);
			lockModes.add(lockMode);
			alias2Persister.put(alias, persister);
		}
	}

	private void processCollectionReturn(SQLQueryCollectionReturn collectionReturn) {
		// we are initializing an owned collection
		collectionOwner = -1;
		String role = collectionReturn.getOwnerEntityName() + '.' + collectionReturn.getOwnerProperty();
		addCollection( role, collectionReturn.getAlias(), collectionReturn.getPropertyResultsMap(), collectionReturn.getLockMode() );
	}

	private void processRoleReturn(SQLQueryJoinReturn roleReturn) {
		if ( alias2Persister.containsKey( roleReturn.getAlias() ) || roleReturn.getAlias().equals(collectionAlias) ) {
			// already been processed...
			return;
		}

		String ownerAlias = roleReturn.getOwnerAlias();

		// Make sure the owner alias is known...
		if ( !alias2Return.containsKey(ownerAlias) ) {
			throw new HibernateException(
			        "Owner alias [" + ownerAlias + "] is unknown for alias [" +
			        roleReturn.getAlias() + "]"
			);
		}

		// If this return's alias has not been processed yet, do so b4 further processing of this return
		if ( !alias2Persister.containsKey(ownerAlias) ) {
			SQLQueryReturn ownerReturn = (SQLQueryReturn) alias2Return.get(ownerAlias);
			processReturn(ownerReturn);
		}

		SQLLoadable ownerPersister = (SQLLoadable) alias2Persister.get(ownerAlias);
		Type returnType = ownerPersister.getPropertyType( roleReturn.getOwnerProperty() );
		
		if ( returnType.isCollectionType() ) {
			if (isCollectionInitializer) {
				throw new HibernateException("A sql query cannot name both a collection to be initialized and a collection to be fetched");
			}
			if (collectionAlias != null) {
				throw new HibernateException("Only one colection role return can be specified per sql-query");
			}
			String role = ownerPersister.getEntityName() + '.' + roleReturn.getOwnerProperty();
			addCollection( role, roleReturn.getAlias(), roleReturn.getPropertyResultsMap(), roleReturn.getLockMode() );
			collectionOwnerAlias = ownerAlias;
		}
		else if ( returnType.isEntityType() ) {
			EntityType eType = (EntityType) returnType;
			String returnEntityName = eType.getAssociatedEntityName();
			SQLLoadable persister = getSQLLoadable(returnEntityName);
			aliases.add( roleReturn.getAlias() );
			addPersister(roleReturn.getPropertyResultsMap(), persister);
			lockModes.add( roleReturn.getLockMode() );
			alias2Persister.put( roleReturn.getAlias(), persister );
		}

	}
	
	public String getCollectionAlias() {
		return collectionAlias;
	}
	
	public int getCollectionOwner() {
		return collectionOwner;
	}
	
	public String getCollectionOwnerAlias() {
		return collectionOwnerAlias;
	}
	
	public QueryableCollection getCollectionPersister() {
		return collectionPersister;
	}
	
	public Map getAlias2Persister() {
		return alias2Persister;
	}
	
	public List getAliases() {
		return aliases;
	}
	
	public boolean isCollectionInitializer() {
		return isCollectionInitializer;
	}
	
	public List getLockModes() {
		return lockModes;
	}
	
	public List getPersisters() {
		return persisters;
	}
	
	public Map getAlias2OwnerAlias() {
		return alias2OwnerAlias;
	}
	
	public List getScalarTypes() {
		return scalarTypes;
	}
	public List getScalarColumnAliases() {
		return scalarColumnAliases;
	}

	public List getPropertyResults() {
		return propertyResults;
	}

	public Map getAlias2Return() {
		return alias2Return;
	}
}
