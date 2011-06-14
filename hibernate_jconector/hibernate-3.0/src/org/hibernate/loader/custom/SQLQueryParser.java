//$Id: SQLQueryParser.java,v 1.3 2005/03/29 02:15:27 oneovthafew Exp $
package org.hibernate.loader.custom;

import org.hibernate.QueryException;
import org.hibernate.hql.classic.ParserHelper;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.SQLLoadable;
import org.hibernate.util.StringHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gavin King, Max Andersen
 */
public class SQLQueryParser {

	private final String sqlQuery;
	private final Map alias2Persister;
	private final String[] aliases;
	private final String collectionAlias;
	private final QueryableCollection collectionPersister;
	private final String[] suffixes;	
	
	private int parameterCount = 0;
	private final Map namedParameters = new HashMap();
	//private Map typeAliasesToTypes;
	private final Map alias2Return;
	
	public SQLQueryParser(String sqlQuery,
						  Map alias2Persister,
						  Map alias2Return, String[] aliases,
						  String collectionAlias,
						  //Map typeAliasesToTypes,
						  QueryableCollection collectionPersister,
						  String[] suffixes) {
		this.sqlQuery = sqlQuery;
		this.alias2Persister = alias2Persister;
		this.alias2Return = alias2Return; // TODO: maybe just fieldMaps ?
		this.collectionAlias = collectionAlias;
		this.collectionPersister = collectionPersister;
		this.suffixes = suffixes;
		this.aliases = aliases;
		//this.typeAliasesToTypes = typeAliasesToTypes;
	}

	private SQLLoadable getPersisterByResultAlias(String aliasName) {
		return (SQLLoadable) alias2Persister.get(aliasName);
	}
	
	private Map getPropertyResultByResultAlias(String aliasName) {
		SQLQueryReturn sqr = (SQLQueryReturn) alias2Return.get(aliasName);
		return sqr.getPropertyResultsMap();				
	}
	private boolean isEntityAlias(String aliasName) {
		return alias2Persister.containsKey(aliasName);
	}

	public int getPersisterIndex(String aliasName) {
		for ( int i = 0; i < aliases.length; i++ ) {
			if ( aliasName.equals( aliases[i] ) ) {
				return i;
			}
		}
		return -1;
	}

	public String process() {
		return substituteParams( substituteBrackets() );
	}

	// Inspired by the parsing done in TODO
	// TODO: should "record" how many properties we have reffered to - and if we 
	//       don't get'em'all we throw an exception! Way better than trial and error ;)
	private String substituteBrackets() throws QueryException {

		StringBuffer result = new StringBuffer( sqlQuery.length() + 20 );
		int left, right;

		// replace {....} with corresponding column aliases
		for ( int curr = 0; curr < sqlQuery.length(); curr = right + 1 ) {
			if ( ( left = sqlQuery.indexOf( '{', curr ) ) < 0 ) {
				// No additional open braces found in the string, append the
				// rest of the string in its entirty and quit this loop
				result.append( sqlQuery.substring( curr ) );
				break;
			}

			// apend everything up until the next encountered open brace
			result.append( sqlQuery.substring( curr, left ) );

			if ( ( right = sqlQuery.indexOf( '}', left + 1 ) ) < 0 ) {
				throw new QueryException( "Unmatched braces for alias path", sqlQuery );
			}

			String aliasPath = sqlQuery.substring( left + 1, right );
			int firstDot = aliasPath.indexOf( '.' );
			if ( firstDot == -1 ) {
				if ( isEntityAlias(aliasPath) ) {
					// it is a simple table alias {foo}
					result.append(aliasPath);
				} 
				else {
					// passing through anything we do not know to support jdbc escape sequences HB-898
					result.append( '{' ).append(aliasPath).append( '}' );
				}
			}
			else {
				String aliasName = aliasPath.substring(0, firstDot);
				boolean isCollection = aliasName.equals(collectionAlias);
				boolean isEntity = isEntityAlias(aliasName);
				
				if (isCollection) {
					// The current alias is referencing the collection to be eagerly fetched
					result.append( collectionPersister.selectFragment(aliasName) );
					if (isEntity) result.append(", ");
				}
				
				if (isEntity) {
					// it is a property reference {foo.bar}
					String propertyName = aliasPath.substring( firstDot + 1 );
					resolveProperties( aliasName, propertyName, result,  getPropertyResultByResultAlias(aliasName), getPersisterByResultAlias(aliasName) );
				}
				
				if ( !isEntity && !isCollection ) {
					// passing through anything we do not know to support jdbc escape sequences HB-898
					result.append( '{' ).append(aliasPath).append( '}' );
				}
	
			}
		}

		// Possibly handle :something parameters for the query ?

		return result.toString();
	}	

	private void resolveProperties(String aliasName,
								   String propertyName,
								   StringBuffer result,
								   Map fieldResults, SQLLoadable currentPersister) {
		int currentPersisterIndex = getPersisterIndex( aliasName );

		if ( !aliasName.equals( aliases[currentPersisterIndex] ) ) {
			throw new QueryException( "Alias [" +
					aliasName +
					"] does not correspond to return alias " +
					aliases[currentPersisterIndex],
					sqlQuery );
		}

		if ( "*".equals( propertyName ) ) {
			if(!fieldResults.isEmpty()) {
				throw new QueryException("Using return-propertys together with * syntax is not supported.");
			}
			result.append( currentPersister.selectFragment( aliasName, suffixes[currentPersisterIndex] ) );
		}
		else {

			// here it would be nice just to be able to do;
			//result.append( getAliasFor(currentPersister, propertyName) );
			// but that requires more exposure of the internal maps of the persister...
			// but it should be possible as propertyname should be unique for all persisters

			String[] columnAliases;

			/*if ( AbstractEntityPersister.ENTITY_CLASS.equals(propertyName) ) {
				columnAliases = new String[1];
				columnAliases[0] = currentPersister.getDiscriminatorAlias(suffixes[currentPersisterIndex]);
			}
			else {*/
			// Let return-propertys override whatever the persister has for aliases.
			columnAliases = (String[]) fieldResults.get(propertyName);
			if(columnAliases==null) {
				columnAliases = currentPersister.getSubclassPropertyColumnAliases( propertyName, suffixes[currentPersisterIndex] );
			}
			//}

			if ( columnAliases == null || columnAliases.length == 0 ) {
				throw new QueryException( "No column name found for property [" +
						propertyName +
						"]",
						sqlQuery );
			}
			if ( columnAliases.length != 1 ) {
				throw new QueryException( "SQL queries only support properties mapped to a single column - property [" +
						propertyName +
						"] is mapped to " +
						columnAliases.length +
						" columns.",
						sqlQuery );
			}
			// here we need to find the field of the aliasName
			// Find by alias first
			// Find by class second ?
			//result.append("$" + aliasName + "/" + propertyName + "$");
			result.append( columnAliases[0] );
		}
	}


	private String substituteParams(String sqlString) {

		StringBuffer result = new StringBuffer( sqlString.length() );
		int left, right;

		// Replace :... with ? and record the parameter. Naively just replaces ALL occurences of :... 
		// including whatever is BEFORE FROM. Do not "fast-forward" to the first or last FROM as 
		// "weird" sql might have parameters in places we do not know of, right ? ;)
		for ( int curr = 0; curr < sqlString.length(); curr = right + 1 ) {
			if ( ( left = sqlString.indexOf( ParserHelper.HQL_VARIABLE_PREFIX, curr ) ) < 0 ) {
				result.append( sqlString.substring( curr ) );
				break;
			}

			result.append( sqlString.substring( curr, left ) );

			// Find first place of a HQL_SEPERATOR char
			right = StringHelper.firstIndexOfChar( sqlString, ParserHelper.HQL_SEPARATORS, left + 1 );

			// did we find a HQL_SEPERATOR ?
			boolean foundSeperator = right > 0;
			int chopLocation = -1;
			if ( right < 0 ) {
				chopLocation = sqlString.length();
			}
			else {
				chopLocation = right;
			}

			String param = sqlString.substring( left + 1, chopLocation );
			addNamedParameter( param );
			result.append( "?" );
			if ( foundSeperator ) {
				result.append( sqlString.charAt( right ) );
			}
			else {
				break;
			}
		}
		return result.toString();
	}

	// NAMED PARAMETERS SUPPORT, copy/pasted from QueryTranslator!
	private void addNamedParameter(String name) {
		Integer loc = new Integer( parameterCount++ );
		Object o = namedParameters.get( name );
		if ( o == null ) {
			namedParameters.put( name, loc );
		}
		else if ( o instanceof Integer ) {
			ArrayList list = new ArrayList( 4 );
			list.add( o );
			list.add( loc );
			namedParameters.put( name, list );
		}
		else {
			( ( List ) o ).add( loc );
		}
	}

	public Map getNamedParameters() {
		return namedParameters;
	}

}
