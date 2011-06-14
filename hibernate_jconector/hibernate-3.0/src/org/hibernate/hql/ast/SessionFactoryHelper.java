// $Id: SessionFactoryHelper.java,v 1.31 2005/03/31 18:23:25 steveebersole Exp $
package org.hibernate.hql.ast;

import antlr.SemanticException;
import antlr.collections.AST;
import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.JoinSequence;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.hql.NameGenerator;
import org.hibernate.persister.collection.CollectionPropertyMapping;
import org.hibernate.persister.collection.CollectionPropertyNames;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import java.util.HashMap;
import java.util.Map;

/**
 * Wraps SessionFactoryImpl, adding more lookup behaviors and encapsulating some of the error handling.
 *
 * @author josh Jul 24, 2004 6:44:00 PM
 */
class SessionFactoryHelper {
	private SessionFactoryImplementor sfi;
	private Map collectionPropertyMappingByRole;

	SessionFactoryHelper(SessionFactoryImplementor sfi) {
		this.sfi = sfi;
		collectionPropertyMappingByRole = new HashMap();
	}

	String getImportedClassName(String className) {
		return sfi.getImportedClassName( className );
	}

	Queryable findQueryableUsingImports(String className) {
		final String importedClassName = sfi.getImportedClassName( className );
		if ( importedClassName == null ) {
			return null;
		}
		try {
			return ( Queryable ) sfi.getEntityPersister( importedClassName );
		}
		catch ( MappingException me ) {
			return null;
		}
	}

	private EntityPersister findEntityPersisterByName(String className) throws MappingException {
		// First, try to get the persister using the class name directly.
		EntityPersister persister = null;
		try {
			persister = sfi.getEntityPersister( className );
		}
		catch ( MappingException ignore ) {
		}
		if ( persister == null ) {
			// If that didn't work, try using the 'import' name.
			String importedClassName = sfi.getImportedClassName( className );
			if ( importedClassName == null ) {
				return null;
			}
			persister = sfi.getEntityPersister( importedClassName );
		}
		return persister;
	}

	EntityPersister requireClassPersister(String className) throws SemanticException {
		EntityPersister cp;
		try {
			cp = findEntityPersisterByName( className );
			if ( cp == null ) {
				throw new SemanticException( className + " is not mapped." );
			}
		}
		catch ( MappingException e ) {
			throw new DetailedSemanticException( e.getMessage(), e );
		}
		return cp;
	}

	QueryableCollection requireQueryableCollection(String role) {
		try {
			QueryableCollection queryableCollection = ( QueryableCollection ) sfi.getCollectionPersister( role );
			if ( queryableCollection != null ) {
				collectionPropertyMappingByRole.put( role, new CollectionPropertyMapping( queryableCollection ) );
			}
			return queryableCollection;
		}
		catch ( ClassCastException cce ) {
			throw new QueryException( "collection role is not queryable: " + role );
		}
		catch ( Exception e ) {
			throw new QueryException( "collection role not found: " + role );
		}
	}

	private PropertyMapping getCollectionPropertyMapping(String role) {
		return ( PropertyMapping ) collectionPropertyMappingByRole.get( role );
	}

	String[] getCollectionElementColumns(String role, String roleAlias) {
		return getCollectionPropertyMapping( role ).toColumns( roleAlias, CollectionPropertyNames.COLLECTION_ELEMENTS );
	}

	JoinSequence createJoinSequence() {
		return new JoinSequence( sfi );
	}

	JoinSequence createJoinSequence(boolean implicit, AssociationType associationType, String tableAlias, int joinType, String[] columns) {
		JoinSequence joinSequence = createJoinSequence();
		joinSequence.setUseThetaStyle( implicit );	// Implicit joins use theta style (WHERE pk = fk), explicit joins use JOIN (after from)
		joinSequence.addJoin( associationType, tableAlias, joinType, columns );
		return joinSequence;
	}

	JoinSequence createCollectionJoinSequence(QueryableCollection collPersister, String collectionName) {
		JoinSequence joinSequence = createJoinSequence();
		joinSequence.setRoot( collPersister, collectionName );
		joinSequence.setUseThetaStyle( true );		// TODO: figure out how this should be set.
///////////////////////////////////////////////////////////////////////////////
// This was the reason for failures regarding INDEX_OP and subclass joins on
// theta-join dialects; not sure what behaviour we were trying to emulate ;)
//		joinSequence = joinSequence.getFromPart();	// Emulate the old addFromOnly behavior.
		return joinSequence;
	}

	String getIdentifierOrUniqueKeyPropertyName(EntityType entityType) {
		try {
			return entityType.getIdentifierOrUniqueKeyPropertyName( sfi );
		}
		catch ( MappingException me ) {
			throw new QueryException( me );
		}
	}

	int getColumnSpan(Type type) {
		return type.getColumnSpan( sfi );
	}

	String getAssociatedEntityName(CollectionType collectionType) {
		return collectionType.getAssociatedEntityName( sfi );
	}

	private Type getElementType(CollectionType collectionType) {
		return collectionType.getElementType( sfi );
	}

	public AssociationType getElementAssociationType(CollectionType collectionType) {
		return ( AssociationType ) getElementType( collectionType );
	}

	SQLFunction findSQLFunction(String functionName) {
		return ( SQLFunction ) sfi.getDialect().getFunctions().get( functionName );
	}

	private SQLFunction requireSQLFunction(String functionName) {
		SQLFunction f = findSQLFunction( functionName );
		if ( f == null ) {
			throw new QueryException( "Unable to find SQL function: " + functionName );
		}
		return f;
	}

	/**
	 * Find the function return type given the function name and the first argument expression node.
	 *
	 * @param functionName The function name.
	 * @param first        The first argument expression.
	 * @return the function return type given the function name and the first argument expression node.
	 */
	Type findFunctionReturnType(String functionName, AST first) {
		Type argumentType = null;
		if ( first != null && first instanceof SqlNode ) {
			SqlNode sqlNode = ( SqlNode ) first;
			argumentType = sqlNode.getDataType();
		}
		// This implementation is a bit strange, but then that's why this helper exists.
		Type functionReturnType = requireSQLFunction( functionName ).getReturnType( argumentType, sfi );
		return functionReturnType;
	}

	public QueryableCollection getCollectionPersister(String collectionRole) {
		try {
			return ( QueryableCollection ) sfi.getCollectionPersister( collectionRole );
		}
		catch ( ClassCastException cce ) {
			throw new QueryException( "collection collectionRole is not queryable: " + collectionRole );
		}
		catch ( Exception e ) {
			throw new QueryException( "collection collectionRole not found: " + collectionRole );
		}
	}

	public String[][] generateColumnNames(Type[] sqlResultTypes) {
		return NameGenerator.generateColumnNames( sqlResultTypes, sfi );
	}
}
