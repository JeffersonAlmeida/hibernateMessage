//$Id: RootClass.java,v 1.12 2005/02/19 07:06:05 oneovthafew Exp $
package org.hibernate.mapping;

import java.util.Collections;
import java.util.Iterator;

import org.hibernate.MappingException;
import org.hibernate.engine.Mapping;

/**
 * The root class of an inheritance hierarchy
 * @author Gavin King
 */
public class RootClass extends PersistentClass implements TableOwner {

	public static final String DEFAULT_IDENTIFIER_COLUMN_NAME = "id";
	public static final String DEFAULT_DISCRIMINATOR_COLUMN_NAME = "class";

	private Property identifierProperty; //may be final
	private KeyValue identifier; //may be final
	private Property version; //may be final
	private boolean polymorphic;
	private String cacheConcurrencyStrategy;
	private String cacheRegionName;
	private Value discriminator; //may be final
	private boolean mutable = true;
	private boolean embeddedIdentifier = false; // may be final
	private boolean explicitPolymorphism;
	private Class entityPersisterClass;
	private boolean forceDiscriminator = false;
	private String where;
	private Table table;
	private boolean discriminatorInsertable = true;
	private int nextSubclassId = 0;
	
	int nextSubclassId() {
		return ++nextSubclassId;
	}

	public int getSubclassId() {
		return 0;
	}
	
	public void setTable(Table table) {
		this.table=table;
	}
	public Table getTable() {
		return table;
	}

	public Property getIdentifierProperty() {
		return identifierProperty;
	}
	public KeyValue getIdentifier() {
		return identifier;
	}
	public boolean hasIdentifierProperty() {
		return identifierProperty!=null;
	}

	public Value getDiscriminator() {
		return discriminator;
	}

	public boolean isInherited() {
		return false;
	}
	public boolean isPolymorphic() {
		return polymorphic;
	}

	public void setPolymorphic(boolean polymorphic) {
		this.polymorphic = polymorphic;
	}

	public RootClass getRootClass() {
		return this;
	}

	public Iterator getPropertyClosureIterator() {
		return getPropertyIterator();
	}
	public Iterator getTableClosureIterator() {
		return Collections.singleton( getTable() ).iterator();
	}
	public Iterator getKeyClosureIterator() {
		return Collections.singleton( getKey() ).iterator();
	}

	public void addSubclass(Subclass subclass) throws MappingException {
		super.addSubclass(subclass);
		setPolymorphic(true);
	}

	public boolean isExplicitPolymorphism() {
		return explicitPolymorphism;
	}

	public Property getVersion() {
		return version;
	}
	public void setVersion(Property version) {
		this.version = version;
	}
	public boolean isVersioned() {
		return version!=null;
	}

	public boolean isMutable() {
		return mutable;
	}
	public boolean hasEmbeddedIdentifier() {
		return embeddedIdentifier;
	}

	public Class getEntityPersisterClass() {
		return entityPersisterClass;
	}

	public Table getRootTable() {
		return getTable();
	}

	public void setEntityPersisterClass(Class persister) {
		this.entityPersisterClass = persister;
	}

	public PersistentClass getSuperclass() {
		return null;
	}

	public KeyValue getKey() {
		return getIdentifier();
	}

	public void setDiscriminator(Value discriminator) {
		this.discriminator = discriminator;
	}

	public void setEmbeddedIdentifier(boolean embeddedIdentifier) {
		this.embeddedIdentifier = embeddedIdentifier;
	}

	public void setExplicitPolymorphism(boolean explicitPolymorphism) {
		this.explicitPolymorphism = explicitPolymorphism;
	}

	public void setIdentifier(KeyValue identifier) {
		this.identifier = identifier;
	}

	public void setIdentifierProperty(Property identifierProperty) {
		this.identifierProperty = identifierProperty;
		identifierProperty.setPersistentClass(this);
	}

	public void setMutable(boolean mutable) {
		this.mutable = mutable;
	}

	public boolean isDiscriminatorInsertable() {
		return discriminatorInsertable;
	}
	
	public void setDiscriminatorInsertable(boolean insertable) {
		this.discriminatorInsertable = insertable;
	}

	public boolean isForceDiscriminator() {
		return forceDiscriminator;
	}

	public void setForceDiscriminator(boolean forceDiscriminator) {
		this.forceDiscriminator = forceDiscriminator;
	}

	public String getWhere() {
		return where;
	}

	public void setWhere(String string) {
		where = string;
	}

	public void validate(Mapping mapping) throws MappingException {
		super.validate(mapping);
		if ( !getIdentifier().isValid(mapping) ) {
			throw new MappingException(
				"identifier mapping has wrong number of columns: " +
				getEntityName() +
				" type: " +
				getIdentifier().getType().getName()
			);
		}
	}

	public String getCacheConcurrencyStrategy() {
		return cacheConcurrencyStrategy;
	}

	public void setCacheConcurrencyStrategy(String cacheConcurrencyStrategy) {
		this.cacheConcurrencyStrategy = cacheConcurrencyStrategy;
	}

	public String getCacheRegionName() {
		return cacheRegionName==null ? getEntityName() : cacheRegionName;
	}
	public void setCacheRegionName(String cacheRegionName) {
		this.cacheRegionName = cacheRegionName;
	}

	public boolean isJoinedSubclass() {
		return false;
	}

	public java.util.Set getSynchronizedTables() {
		return synchronizedTables;
	}
	
	public Object accept(PersistentClassVisitor mv) {
		return mv.accept(this);
	}
}






