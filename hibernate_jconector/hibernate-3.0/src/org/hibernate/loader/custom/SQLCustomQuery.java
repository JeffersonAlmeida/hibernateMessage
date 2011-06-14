//$Id: SQLCustomQuery.java,v 1.4 2005/03/19 00:29:13 maxcsaucdk Exp $
package org.hibernate.loader.custom;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.loader.BasicLoader;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.GeneratedEntityAliases;
import org.hibernate.loader.SQLGeneratedEntityAliases;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.SQLLoadable;
import org.hibernate.type.Type;
import org.hibernate.util.ArrayHelper;
import org.hibernate.util.StringHelper;

/**
 * Implements Hibernate's built-in support for
 * native SQL queries.
 * 
 * @author Gavin King, Max Andersen
 */
public class SQLCustomQuery implements CustomQuery {
	
	private final String[] entityNames;
	private final String collectionRole;
	private int collectionOwner;
	private final int[] entityOwners;
	private final LockMode[] lockModes;
	private final String sql;
	private final Set querySpaces = new HashSet();
	private final Map namedParameters;
	private final Type[] scalarTypes;
	private final String[] scalarColumnAliases;
	private final EntityAliases[] entityDescriptors;

	public String getSQL() {
		return sql;
	}
	
	public Map getNamedParameterBindPoints() {
		return namedParameters;
	}
	
	public String getCollectionRole() {
		return collectionRole;
	}

	public String[] getEntityNames() {
		return entityNames;
	}

	public LockMode[] getLockModes() {
		return lockModes;
	}

	public EntityAliases[] getEntityAliases() {
		return entityDescriptors;
	}

	public Set getQuerySpaces() {
		return querySpaces;
	}

	public int getCollectionOwner() {
		return collectionOwner;
	}

	public int[] getEntityOwners() {
		return entityOwners;
	}
	
	public String[] getScalarColumnAliases() {
		return scalarColumnAliases;
	}
	
	public Type[] getScalarTypes() {
		return scalarTypes;
	}
	
	public SQLCustomQuery(
			final SQLQueryReturn[] queryReturns,
			final SQLQueryScalarReturn[] scalarQueryReturns,
			final String sqlQuery,
			final Collection additionalQuerySpaces,
			final SessionFactoryImplementor factory)
	throws HibernateException {

		SQLQueryReturnProcessor processor = new SQLQueryReturnProcessor(queryReturns, scalarQueryReturns, factory);
		processor.process();
		
		collectionOwner = processor.getCollectionOwner();
		collectionRole = processor.getCollectionPersister()==null ? 
				null : 
				processor.getCollectionPersister().getRole();
		QueryableCollection collectionPersister = processor.getCollectionPersister();
		
		String[] aliases = ArrayHelper.toStringArray( processor.getAliases() );
		Map[] propertyResultMaps =  (Map[]) processor.getPropertyResults().toArray( new Map[0] );
		SQLLoadable[] entityPersisters = (SQLLoadable[]) processor.getPersisters().toArray( new SQLLoadable[0] );
        lockModes = (LockMode[]) processor.getLockModes().toArray( new LockMode[0] );
		
        scalarColumnAliases = ArrayHelper.toStringArray( processor.getScalarColumnAliases() );
		scalarTypes = ArrayHelper.toTypeArray( processor.getScalarTypes() );

		String[] suffixes = BasicLoader.generateSuffixes(entityPersisters.length);

		SQLQueryParser parser = new SQLQueryParser(
				sqlQuery, 
				processor.getAlias2Persister(),
				processor.getAlias2Return(),				
				aliases, 
				processor.getCollectionAlias(),
				collectionPersister,
				suffixes
		);

		//Very, very ugly!
		if ( ! processor.isCollectionInitializer() && StringHelper.isNotEmpty( processor.getCollectionOwnerAlias() ) ) {
			collectionOwner = parser.getPersisterIndex( processor.getCollectionOwnerAlias() );
		}

		sql = parser.process();
		
		namedParameters = parser.getNamedParameters();
		
		// Populate entityNames, entityDescrptors and querySpaces
		entityNames = new String[entityPersisters.length];
		entityDescriptors = new EntityAliases[entityPersisters.length];		
		for (int i = 0; i < entityPersisters.length; i++) {
			SQLLoadable persister = entityPersisters[i];
			//alias2Persister.put( aliases[i], persister );
			//TODO: Does not consider any other tables referenced in the query
			ArrayHelper.addAll( querySpaces, persister.getQuerySpaces() ); 
			entityNames[i] = persister.getEntityName();
			if(propertyResultMaps[i].isEmpty()) {
				entityDescriptors[i] = new GeneratedEntityAliases( entityPersisters[i], suffixes[i] );
			} else {
				entityDescriptors[i] = new SQLGeneratedEntityAliases( propertyResultMaps[i], entityPersisters[i], suffixes[i] );
			}
		}
		if (additionalQuerySpaces!=null) {
			querySpaces.addAll(additionalQuerySpaces);
		}
		
		// Resolve owners
		Map alias2OwnerAlias = processor.getAlias2OwnerAlias();
		int[] ownersArray = new int[entityPersisters.length];
		for ( int j=0; j < aliases.length; j++ ) {
			String ownerAlias = (String) alias2OwnerAlias.get( aliases[j] );
			if ( StringHelper.isNotEmpty(ownerAlias) ) {
				ownersArray[j] = parser.getPersisterIndex(ownerAlias);
			}
			else {
				ownersArray[j] = -1;
			}
		}
		if ( ArrayHelper.isAllNegative(ownersArray) ) {
			ownersArray = null;
		}
		this.entityOwners = ownersArray;
		
	}

}
