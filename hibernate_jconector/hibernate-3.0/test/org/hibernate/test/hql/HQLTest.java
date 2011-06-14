//$Id: HQLTest.java,v 1.136 2005/03/31 18:20:04 steveebersole Exp $
package org.hibernate.test.hql;

import antlr.RecognitionException;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.hql.ast.ConstructorNode;
import org.hibernate.hql.ast.DetailedSemanticException;
import org.hibernate.hql.ast.IndexNode;
import org.hibernate.hql.ast.QuerySyntaxError;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;

/**
 * Tests cases where the AST based query translator and the 'classic' query translator generate identical SQL.
 *
 * @author Gavin King
 */
public class HQLTest extends QueryTranslatorTestCase {

	public HQLTest(String x) {
		super( x );
	}

	protected boolean dropAfterFailure() {
		return false;
	}

	//FAILING TESTS:

	public void testVectorSubselect() {
		assertTranslation( "from Animal a where ('foo', 'bar') in (select m.description, m.bodyWeight from a.mother m)" );
	}

	public void testMaxindexHqlFunctionInElementAccessor() {
		//TODO: broken SQL
		assertTranslation( "select c from ContainerX c where c.manyToMany[ maxindex(c.manyToMany) ].count = 2" );
		assertTranslation( "select c from Container c where c.manyToMany[ maxIndex(c.manyToMany) ].count = 2" );
	}

	public void testMultipleElementAccessorOperators() throws Exception {
		//TODO: broken SQL
		assertTranslation( "select c from ContainerX c where c.oneToMany[ c.manyToMany[0].count ].name = 's'" );
		assertTranslation( "select c from ContainerX c where c.manyToMany[ c.oneToMany[0].count ].name = 's'" );
	}

	public void testSelectStandardFunctionsNoParens() throws Exception {
		//TODO: we currently don't support functions w/o parens in the select clause (need to fix this!)
		assertTranslation( "select current_date, current_time, current_timestamp from Animal" );
	}

	/*public void testSelectMaxElements() throws Exception {
		//TODO: this is almost correct, but missing a select-clause column alias!
		assertTranslation("select max( elements(one.manies) ) from org.hibernate.test.legacy.One one");
	}*/

	public void testKeyManyToOneJoin() {
		//TODO: new parser generates unnecessary joins (though the query results are correct)
		assertTranslation( "from Order o left join fetch o.lineItems li left join fetch li.product p" );
		assertTranslation( "from Outer o where o.id.master.id.sup.dudu is not null" );
		assertTranslation( "from Outer o where o.id.master.id.sup.dudu is not null" );
	}

	public void testDuplicateExplicitJoin() throws Exception {
		//very minor issue with select clause:
		assertTranslation( "from Animal a join a.mother m1 join a.mother m2" );
		assertTranslation( "from Zoo zoo join zoo.animals an join zoo.mammals m" );
		assertTranslation( "from Zoo zoo join zoo.mammals an join zoo.mammals m" );
	}

	// TESTS THAT FAIL ONLY ON DIALECTS WITH THETA-STYLE OUTERJOINS:

	public void testIndexWithExplicitJoin() throws Exception {
		//TODO: broken on dialects with theta-style outerjoins:
		assertTranslation( "from Zoo zoo join zoo.animals an where zoo.mammals[ index(an) ] = an" );
		assertTranslation( "from Zoo zoo join zoo.mammals dog where zoo.mammals[ index(dog) ] = dog" );
		assertTranslation( "from Zoo zoo join zoo.mammals dog where dog = zoo.mammals[ index(dog) ]" );
	}

	public void testOneToManyMapIndex() throws Exception {
		//TODO: this breaks on dialects with theta-style outerjoins:
		assertTranslation( "from Zoo zoo where zoo.mammals['dog'].description like '%black%'" );
		assertTranslation( "from Zoo zoo where zoo.mammals['dog'].father.description like '%black%'" );
		assertTranslation( "from Zoo zoo where zoo.mammals['dog'].father.id = 1234" );
		assertTranslation( "from Zoo zoo where zoo.animals['1234'].description like '%black%'" );
	}

	public void testExplicitJoinMapIndex() throws Exception {
		//TODO: this breaks on dialects with theta-style outerjoins:
		assertTranslation( "from Zoo zoo, Dog dog where zoo.mammals['dog'] = dog" );
		assertTranslation( "from Zoo zoo join zoo.mammals dog where zoo.mammals['dog'] = dog" );
	}

	public void testIndexFunction() throws Exception {
		// Instead of doing the pre-processor trick like the existing QueryTranslator, this
		// is handled by MethodNode.
		//TODO: broken on dialects with theta-style outerjoins:
		assertTranslation( "from Zoo zoo join zoo.mammals dog where index(dog) = 'dog'" );
		assertTranslation( "from Zoo zoo join zoo.animals an where index(an) = '1234'" );
	}

	public void testSelectCollectionOfValues() throws Exception {
		//TODO: broken on dialects with theta-style joins
		///old parser had a bug where the collection element was not included in return types!
		assertTranslation( "select baz, date from Baz baz join baz.stringDateMap date where index(date) = 'foo'" );
	}

	public void testCollectionOfValues() throws Exception {
		//old parser had a bug where the collection element was not returned!
		//TODO: broken on dialects with theta-style joins
		assertTranslation( "from Baz baz join baz.stringDateMap date where index(date) = 'foo'" );
	}

	//PASSING TESTS:

	// Do the simplest test first!
	public void testFromOnly() throws Exception {
		// 2004-06-21 [jsd] This test now works with the new AST based QueryTranslatorImpl.
		assertTranslation( "from Animal" );
		assertTranslation( "from Model" );
	}

	public void testJoinPathEndingInValueCollection() {
		assertTranslation( "select h from Human as h join h.nickNames as nn where h.nickName=:nn1 and (nn=:nn2 or nn=:nn3)" );
	}

	public void testSerialJoinPathEndingInValueCollection() {
		// HHH-242
		assertTranslation( "select h from Human as h join h.friends as f join f.nickNames as nn where h.nickName=:nn1 and (nn=:nn2 or nn=:nn3)" );
	}

	public void testImplicitJoinContainedByCollectionFunction() {
		// HHH-281 : Implied joins in a collection function (i.e., indices or elements)
		assertTranslation( "from Human as h where 'shipping' in indices(h.father.addresses)" );
		assertTranslation( "from Human as h where 'shipping' in indices(h.father.father.addresses)" );
		assertTranslation( "from Human as h where 'sparky' in elements(h.father.nickNames)" );
		assertTranslation( "from Human as h where 'sparky' in elements(h.father.father.nickNames)" );
	}


	public void testImpliedJoinInSubselectFrom() {
		// HHH-276 : Implied joins in a from in a subselect.
		assertTranslation( "from Animal a where exists( from a.mother.offspring )" );
	}

	public void testSubselectImplicitJoins() {
		// HHH-276 : Implied joins in a from in a subselect.
		assertTranslation( "from Simple s where s = some( select sim from Simple sim where sim.other.count=s.other.count )" );
	}


	public void testCollectionOfValuesSize() throws Exception {
		//SQL *was* missing a comma
		assertTranslation( "select size(baz.stringDateMap) from org.hibernate.test.legacy.Baz baz" );
	}

	public void testCollectionFunctions() throws Exception {
		//these are both broken, a join that belongs in the subselect finds its way into the main query
		assertTranslation( "from Zoo zoo where size(zoo.animals) > 100" );
		assertTranslation( "from Zoo zoo where maxindex(zoo.mammals) = 'dog'" );
	}

	public void testImplicitJoinInExplicitJoin() throws Exception {
		assertTranslation( "from Animal an inner join an.mother.mother gm" );
		assertTranslation( "from Animal an inner join an.mother.mother.mother ggm" );
		assertTranslation( "from Animal an inner join an.mother.mother.mother.mother gggm" );
	}

	public void testImpliedManyToManyProperty() throws Exception {
		//missing a table join (SQL correct for a one-to-many, not for a many-to-many)
		assertTranslation( "select c from ContainerX c where c.manyToMany[0].name = 's'" );
	}

	public void testCollectionSize() throws Exception {
		//SQL is correct, query spaces *was* missing a table
		assertTranslation( "select size(zoo.animals) from Zoo zoo" );
	}

	/*public void testCollectionIndexFunctionsInSelect() throws Exception {
		assertTranslation("select maxindex(zoo.animals) from Zoo zoo");
		assertTranslation("select minindex(zoo.animals) from Zoo zoo");
		assertTranslation("select indices(zoo.animals) from Zoo zoo");
	}

	public void testCollectionElementFunctionsInSelect() throws Exception {
		assertTranslation("select maxelement(zoo.animals) from Zoo zoo");
		assertTranslation("select minelement(zoo.animals) from Zoo zoo");
		assertTranslation("select elements(zoo.animals) from Zoo zoo");
	}*/
	
	public void testFetchCollectionOfValues() throws Exception {
		assertTranslation( "from Baz baz left join fetch baz.stringSet" );
	}

	public void testFetchList() throws Exception {
		assertTranslation( "from User u join fetch u.permissions" );
	}

	public void testCollectionFetchWithExplicitThetaJoin() {
		assertTranslation( "select m from Master m1, Master m left join fetch m.details where m.name=m1.name" );
	}

	/*public void testListElementFunctionInSelect() throws Exception {
		//wrong pk column in select clause! (easy fix?)
		assertTranslation("select maxelement(u.permissions) from User u");
		assertTranslation("select elements(u.permissions) from User u");
	}*/

	public void testListElementFunctionInWhere() throws Exception {
		assertTranslation( "from User u where 'read' in elements(u.permissions)" );
		assertTranslation( "from User u where 'write' <> all elements(u.permissions)" );
	}
	
	/*public void testManyToManyElementFunctionInSelect() throws Exception {
		assertTranslation("select maxelement(human.friends) from Human human");
		assertTranslation("select elements(human.friends) from Human human");
	}*/

	public void testManyToManyMaxElementFunctionInWhere() throws Exception {
		//completely broken!!
		assertTranslation( "from Human human where 5 = maxelement(human.friends)" );
	}

	public void testCollectionIndexFunctionsInWhere() throws Exception {
		assertTranslation( "from Zoo zoo where 4 = maxindex(zoo.animals)" );
		assertTranslation( "from Zoo zoo where 2 = minindex(zoo.animals)" );
	}

	public void testCollectionIndicesInWhere() throws Exception {
		assertTranslation( "from Zoo zoo where 4 > some indices(zoo.animals)" );
		assertTranslation( "from Zoo zoo where 4 > all indices(zoo.animals)" );
	}

	public void testIndicesInWhere() throws Exception {
		assertTranslation( "from Zoo zoo where 4 in indices(zoo.animals)" );
		assertTranslation( "from Zoo zoo where exists indices(zoo.animals)" );
	}

	public void testCollectionElementInWhere() throws Exception {
		assertTranslation( "from Zoo zoo where 4 > some elements(zoo.animals)" );
		assertTranslation( "from Zoo zoo where 4 > all elements(zoo.animals)" );
	}

	public void testElementsInWhere() throws Exception {
		assertTranslation( "from Zoo zoo where 4 in elements(zoo.animals)" );
		assertTranslation( "from Zoo zoo where exists elements(zoo.animals)" );
	}

	public void testNull() throws Exception {
		assertTranslation( "from Human h where h.nickName is null" );
		assertTranslation( "from Human h where h.nickName is not null" );
	}

	public void testSubstitutions() throws Exception {
		assertTranslation( "from Human h where h.pregnant = true" );
		assertTranslation( "from Human h where h.pregnant = foo" );
	}

	public void testWhere() throws Exception {
		assertTranslation( "from Animal an where an.bodyWeight > 10" );
		// 2004-06-26 [jsd] This one requires NOT GT => LE transform.
		assertTranslation( "from Animal an where not an.bodyWeight > 10" );
		assertTranslation( "from Animal an where an.bodyWeight between 0 and 10" );
		assertTranslation( "from Animal an where an.bodyWeight not between 0 and 10" );
		assertTranslation( "from Animal an where sqrt(an.bodyWeight)/2 > 10" );
		// 2004-06-27 [jsd] Recognize 'is null' properly.  Generate 'and' and 'or' as well.
		assertTranslation( "from Animal an where (an.bodyWeight > 10 and an.bodyWeight < 100) or an.bodyWeight is null" );
	}
	
	public void testEscapedQuote() throws Exception {
		assertTranslation( "from Human h where h.nickName='1 ov''tha''few'");
	}

	public void testCaseWhenElse() {
		//new qt has more organized joins
		assertTranslation( "from Human h where case when h.nickName='1ovthafew' then 'Gavin' when h.nickName='turin' then 'Christian' else h.nickName end = h.name.first" );
	}

	public void testInvalidHql() throws Exception {
		Exception newException = compileBadHql( "from Animal foo where an.bodyWeight > 10", false );
		assertTrue( "Wrong exception type!", newException instanceof QuerySyntaxError );
		newException = compileBadHql( "select an.name from Animal foo", false );
		assertTrue( "Wrong exception type!", newException instanceof QuerySyntaxError );
		newException = compileBadHql( "from Animal foo where an.verybogus > 10", false );
		assertTrue( "Wrong exception type!", newException instanceof QuerySyntaxError );
		newException = compileBadHql( "select an.boguspropertyname from Animal foo", false );
		assertTrue( "Wrong exception type!", newException instanceof QuerySyntaxError );
		newException = compileBadHql( "select an.name", false );
		assertTrue( "Wrong exception type!", newException instanceof QuerySyntaxError );
		newException = compileBadHql( "from Animal an where (((an.bodyWeight > 10 and an.bodyWeight < 100)) or an.bodyWeight is null", false );
		assertTrue( "Wrong exception type!", newException instanceof QuerySyntaxError );
		newException = compileBadHql( "from Animal an where an.bodyWeight is null where an.bodyWeight is null", false );
		assertTrue( "Wrong exception type!", newException instanceof QuerySyntaxError );
		newException = compileBadHql( "from where name='foo'", false );
		assertTrue( "Wrong exception type!", newException instanceof QuerySyntaxError );
		newException = compileBadHql( "from NonexistentClass where name='foo'", false );
		assertTrue( "Wrong exception type!", newException instanceof QuerySyntaxError );
		newException = compileBadHql( "select new FOO_BOGUS_Animal(an.description, an.bodyWeight) from Animal an", false );
		assertTrue( "Wrong exception type!", newException instanceof QuerySyntaxError );
		newException = compileBadHql( "select new Animal(an.description, an.bodyWeight, 666) from Animal an", false );
		assertTrue( "Wrong exception type!", newException instanceof QuerySyntaxError );

	}

	public void testWhereBetween() throws Exception {
		// 2004-08-31 [jsd] This "just worked"! Woohoo!
		assertTranslation( "from Animal an where an.bodyWeight between 1 and 10" );
	}

	public void testConcatenation() {
		assertTranslation("from Human h where h.nickName = '1' || 'ov' || 'tha' || 'few'");
	}

	public void testWhereLike() throws Exception {
		assertTranslation( "from Animal a where a.description like '%black%'" );
		assertTranslation( "from Animal an where an.description like '%fat%'" );
		assertTranslation( "from Animal an where lower(an.description) like '%fat%'" );
	}

	public void testWhereIn() throws Exception {
		assertTranslation( "from Animal an where an.description in ('fat', 'skinny')" );
	}

	public void testLiteralInFunction() throws Exception {
		assertTranslation( "from Animal an where an.bodyWeight > abs(5)" );
		assertTranslation( "from Animal an where an.bodyWeight > abs(-5)" );
	}

	public void testExpressionInFunction() throws Exception {
		assertTranslation( "from Animal an where an.bodyWeight > abs(3-5)" );
		assertTranslation( "from Animal an where an.bodyWeight > abs(3/5)" );
		assertTranslation( "from Animal an where an.bodyWeight > abs(3+5)" );
		assertTranslation( "from Animal an where an.bodyWeight > abs(3*5)" );
		//TODO: allow a function template to override the function name!!!
		if ( getDialect() instanceof PostgreSQLDialect ) {
			assertTranslation( "from Animal an where an.description = 'fat'||'skinny'" );
		}
		else {
			assertTranslation( "from Animal an where an.description = concat('fat'||'skinny')" );
		}
	}

	public void testNotOrWhereClause() {
		assertTranslation( "from Simple s where 'foo'='bar' or not 'foo'='foo'" );
		assertTranslation( "from Simple s where 'foo'='bar' or not ('foo'='foo')" );
		assertTranslation( "from Simple s where not ( 'foo'='bar' or 'foo'='foo' )" );
		assertTranslation( "from Simple s where not ( 'foo'='bar' and 'foo'='foo' )" );
		assertTranslation( "from Simple s where not ( 'foo'='bar' and 'foo'='foo' ) or not ('x'='y')" );
		assertTranslation( "from Simple s where not ( 'foo'='bar' or 'foo'='foo' ) and not ('x'='y')" );
		assertTranslation( "from Simple s where not ( 'foo'='bar' or 'foo'='foo' ) and 'x'='y'" );
		assertTranslation( "from Simple s where not ( 'foo'='bar' and 'foo'='foo' ) or 'x'='y'" );
		assertTranslation( "from Simple s where 'foo'='bar' and 'foo'='foo' or not 'x'='y'" );
		assertTranslation( "from Simple s where 'foo'='bar' or 'foo'='foo' and not 'x'='y'" );
		assertTranslation( "from Simple s where ('foo'='bar' and 'foo'='foo') or 'x'='y'" );
		assertTranslation( "from Simple s where ('foo'='bar' or 'foo'='foo') and 'x'='y'" );
		assertTranslation( "from Simple s where not( upper( s.name ) ='yada' or 1=2 or 'foo'='bar' or not('foo'='foo') or 'foo' like 'bar' )" );
	}

	public void testComplexExpressionInFunction() throws Exception {
		assertTranslation( "from Animal an where an.bodyWeight > abs((3-5)/4)" );
	}

	public void testStandardFunctions() throws Exception {
		assertTranslation( "from Animal where current_date = current_time" );
		assertTranslation( "from Animal a where upper(a.description) = 'FAT'" );
		assertTranslation( "select lower(a.description) from Animal a" );
	}

	public void testOrderBy() throws Exception {
		assertTranslation( "from Animal an order by an.bodyWeight" );
		assertTranslation( "from Animal an order by an.bodyWeight asc" );
		assertTranslation( "from Animal an order by an.bodyWeight desc" );
		assertTranslation( "from Human h order by sqrt(h.bodyWeight), year(h.birthdate)" );
		assertTranslation( "from Animal an order by sqrt(an.bodyWeight*4)/2" );
		assertTranslation( "from Animal an order by an.mother.bodyWeight" );
		assertTranslation( "from Animal an order by an.bodyWeight, an.description" );
		assertTranslation( "from Animal an order by an.bodyWeight asc, an.description desc" );
	}

	public void testGroupByFunction() {
		assertTranslation( "select count(*) from Human h group by year(h.birthdate)" );
		assertTranslation( "select count(*) from Human h group by trunc( sqrt(h.bodyWeight*4)/2 )" );
		assertTranslation( "select count(*) from Human h group by year(sysdate)" );
	}

	public void testPolymorphism() throws Exception {
		HashMap replacements = new HashMap();
		replacements.put( "false", "0" );
		replacements.put( "true", "1" );
		assertTranslation( "from Mammal" );
		assertTranslation( "from Dog" );
		assertTranslation( "from Mammal m where m.pregnant = false and m.bodyWeight > 10", replacements );
		assertTranslation( "from Dog d where d.pregnant = false and d.bodyWeight > 10", replacements );
	}

	public void testTokenReplacement() throws Exception {
		HashMap replacements = new HashMap();
		replacements.put( "false", "0" );
		replacements.put( "true", "1" );
		assertTranslation( "from Mammal m where m.pregnant = false and m.bodyWeight > 10",
				replacements );
	}

	public void testProduct() throws Exception {
		HashMap replacements = new HashMap();
		replacements.put( "false", "0" );
		replacements.put( "true", "1" );
		assertTranslation( "from Animal, Animal" );
		assertTranslation( "from Animal x, Animal y where x.bodyWeight = y.bodyWeight" );
		assertTranslation( "from Animal x, Mammal y where x.bodyWeight = y.bodyWeight and not y.pregnant = true", replacements );
		assertTranslation( "from Mammal, Mammal" );
	}

	public void testJoinedSubclassProduct() throws Exception {
		assertTranslation( "from PettingZoo, PettingZoo" ); //product of two subclasses
	}

	public void testProjectProduct() throws Exception {
		assertTranslation( "select x from Human x, Human y where x.nickName = y.nickName" );
		assertTranslation( "select x, y from Human x, Human y where x.nickName = y.nickName" );
	}

	public void testExplicitEntityJoins() throws Exception {
		assertTranslation( "from Animal an inner join an.mother mo" );
		assertTranslation( "from Animal an left outer join an.mother mo" );
		assertTranslation( "from Animal an left outer join fetch an.mother" );
	}

	public void testMultipleExplicitEntityJoins() throws Exception {
		assertTranslation( "from Animal an inner join an.mother mo inner join mo.mother gm" );
		assertTranslation( "from Animal an left outer join an.mother mo left outer join mo.mother gm" );
		assertTranslation( "from Animal an inner join an.mother m inner join an.father f" );
		assertTranslation( "from Animal an left join fetch an.mother m left join fetch an.father f" );
	}

	public void testMultipleExplicitJoins() throws Exception {
		assertTranslation( "from Animal an inner join an.mother mo inner join an.offspring os" );
		assertTranslation( "from Animal an left outer join an.mother mo left outer join an.offspring os" );
	}

	public void testExplicitEntityJoinsWithRestriction() throws Exception {
		assertTranslation( "from Animal an inner join an.mother mo where an.bodyWeight < mo.bodyWeight" );
	}

	public void testIdProperty() throws Exception {
		assertTranslation( "from Animal a where a.mother.id = 12" );
	}

	public void testSubclassAssociation() throws Exception {
		assertTranslation( "from DomesticAnimal da join da.owner o where o.nickName = 'Gavin'" );
		assertTranslation( "from DomesticAnimal da left join fetch da.owner" );
		assertTranslation( "from Human h join h.pets p where p.pregnant = 1" );
		assertTranslation( "from Human h join h.pets p where p.bodyWeight > 100" );
		assertTranslation( "from Human h left join fetch h.pets" );
	}

	public void testExplicitCollectionJoins() throws Exception {
		assertTranslation( "from Animal an inner join an.offspring os" );
		assertTranslation( "from Animal an left outer join an.offspring os" );
	}

	public void testExplicitOuterJoinFetch() throws Exception {
		assertTranslation( "from Animal an left outer join fetch an.offspring" );
	}

	public void testExplicitOuterJoinFetchWithSelect() throws Exception {
		assertTranslation( "select an from Animal an left outer join fetch an.offspring" );
	}

	public void testExplicitJoins() throws Exception {
		HashMap replacements = new HashMap();
		replacements.put( "false", "0" );
		replacements.put( "true", "1" );
		assertTranslation( "from Zoo zoo join zoo.mammals mam where mam.pregnant = true and mam.description like '%white%'", replacements );
		assertTranslation( "from Zoo zoo join zoo.animals an where an.description like '%white%'" );
	}

	public void testImplicitJoins() throws Exception {
		// Two dots...
		assertTranslation( "from Animal an where an.mother.bodyWeight > ?" );
		assertTranslation( "from Animal an where an.mother.bodyWeight > 10" );
		assertTranslation( "from Dog dog where dog.mother.bodyWeight > 10" );
		// Three dots...
		assertTranslation( "from Animal an where an.mother.mother.bodyWeight > 10" );
		// The new QT doesn't throw an exception here, so this belongs in ASTQueryTranslator test. [jsd]
//		assertTranslation( "from Animal an where an.offspring.mother.bodyWeight > 10" );
		// Is not null (unary postfix operator)
		assertTranslation( "from Animal an where an.mother is not null" );
		// ID property shortut (no implicit join)
		assertTranslation( "from Animal an where an.mother.id = 123" );
	}

	public void testImplicitJoinInSelect() {
		assertTranslation( "select foo, foo.long from Foo foo" );
		assertTranslation( "select foo.foo from Foo foo" );
		assertTranslation( "select foo, foo.foo from Foo foo" );
		assertTranslation( "select foo.foo from Foo foo where foo.foo is not null" );
	}

	public void testSelectExpressions() {
		assertTranslation( "select an.mother.mother from Animal an" );
		assertTranslation( "select an.mother.mother.mother from Animal an" );
		assertTranslation( "select an.mother.mother.bodyWeight from Animal an" );
		assertTranslation( "select an.mother.zoo.id from Animal an" );
		assertTranslation( "select user.human.zoo.id from User user" );
		assertTranslation( "select u.userName, u.human.name.first from User u" );
		assertTranslation( "select u.human.name.last, u.human.name.first from User u" );
		assertTranslation( "select bar.baz.name from Bar bar" );
		assertTranslation( "select bar.baz.name, bar.baz.count from Bar bar" );
	}

	public void testMapIndex() throws Exception {
		assertTranslation( "from User u where u.permissions['hibernate']='read'" );
	}

	/*public void testCollectionFunctionsInSelect() {
		//sql is correct, just different order in select clause
		assertTranslation("select baz, size(baz.stringSet), count( distinct elements(baz.stringSet) ), max( elements(baz.stringSet) ) from Baz baz group by baz");
	}
	
	public void testSelectElements() throws Exception {
		assertTranslation( "select elements(fum1.friends) from org.hibernate.test.legacy.Fum fum1" );
		assertTranslation( "select elements(one.manies) from org.hibernate.test.legacy.One one" );
	}*/

	public void testNamedParameters() throws Exception {
		assertTranslation( "from Animal an where an.mother.bodyWeight > :weight" );
	}

	// Second set of examples....

	public void testClassProperty() throws Exception {
		assertTranslation( "from Animal a where a.mother.class = Reptile" );
	}

	public void testComponent() throws Exception {
		assertTranslation( "from Human h where h.name.first = 'Gavin'" );
	}

	public void testSelectEntity() throws Exception {
		assertTranslation( "select an from Animal an inner join an.mother mo where an.bodyWeight < mo.bodyWeight" );
		assertTranslation( "select mo, an from Animal an inner join an.mother mo where an.bodyWeight < mo.bodyWeight" );
	}

	public void testValueAggregate() {
		assertTranslation( "select max(p), min(p) from User u join u.permissions p" );
	}

	public void testAggregation() throws Exception {
		assertTranslation( "select count(an) from Animal an" );
		assertTranslation( "select count(*) from Animal an" );
		assertTranslation( "select count(distinct an) from Animal an" );
		assertTranslation( "select count(distinct an.id) from Animal an" );
		assertTranslation( "select count(all an.id) from Animal an" );
	}

	public void testSelectProperty() throws Exception {
		assertTranslation( "select an.bodyWeight, mo.bodyWeight from Animal an inner join an.mother mo where an.bodyWeight < mo.bodyWeight" );
	}

	public void testSelectEntityProperty() throws Exception {
		assertTranslation( "select an.mother from Animal an" );
		assertTranslation( "select an, an.mother from Animal an" );
	}

	public void testSelectDistinctAll() throws Exception {
		assertTranslation( "select distinct an.description, an.bodyWeight from Animal an" );
		assertTranslation( "select all an from Animal an" );
	}

	public void testSelectAssociatedEntityId() throws Exception {
		assertTranslation( "select an.mother.id from Animal an" );
	}

	public void testGroupBy() throws Exception {
		assertTranslation( "select an.mother.id, max(an.bodyWeight) from Animal an group by an.mother.id" );
		assertTranslation( "select an.mother.id, max(an.bodyWeight) from Animal an group by an.mother.id having max(an.bodyWeight)>1.0" );
	}

	public void testGroupByMultiple() throws Exception {
		assertTranslation( "select s.id, s.count, count(t), max(t.date) from org.hibernate.test.legacy.Simple s, org.hibernate.test.legacy.Simple t where s.count = t.count group by s.id, s.count order by s.count" );
	}

	public void testManyToMany() throws Exception {
		assertTranslation( "from Human h join h.friends f where f.nickName = 'Gavin'" );
		assertTranslation( "from Human h join h.friends f where f.bodyWeight > 100" );
	}

	public void testManyToManyElementFunctionInWhere() throws Exception {
		assertTranslation( "from Human human where human in elements(human.friends)" );
		assertTranslation( "from Human human where human = some elements(human.friends)" );
	}

	public void testManyToManyElementFunctionInWhere2() throws Exception {
		assertTranslation( "from Human h1, Human h2 where h2 in elements(h1.family)" );
		assertTranslation( "from Human h1, Human h2 where 'father' in indices(h1.family)" );
	}

	public void testManyToManyFetch() throws Exception {
		assertTranslation( "from Human h left join fetch h.friends" );
	}

	public void testManyToManyIndexAccessor() throws Exception {
		// From ParentChildTest.testCollectionQuery()
		assertTranslation( "select c from ContainerX c, Simple s where c.manyToMany[2] = s" );
		assertTranslation( "select s from ContainerX c, Simple s where c.manyToMany[2] = s" );
		assertTranslation( "from ContainerX c, Simple s where c.manyToMany[2] = s" );
		//would be nice to have:
		//assertTranslation( "select c.manyToMany[2] from ContainerX c" );
	}

	public void testSelectNew() throws Exception {
		assertTranslation( "select new Animal(an.description, an.bodyWeight) from Animal an" );
		assertTranslation( "select new org.hibernate.test.hql.Animal(an.description, an.bodyWeight) from Animal an" );
	}

	public void testSimpleCorrelatedSubselect() throws Exception {
		assertTranslation( "from Animal a where a.bodyWeight = (select o.bodyWeight from a.offspring o)" );
		assertTranslation( "from Animal a where a = (from a.offspring o)" );
	}

	public void testSimpleUncorrelatedSubselect() throws Exception {
		assertTranslation( "from Animal a where a.bodyWeight = (select an.bodyWeight from Animal an)" );
		assertTranslation( "from Animal a where a = (from Animal an)" );
	}

	public void testSimpleCorrelatedSubselect2() throws Exception {
		assertTranslation( "from Animal a where a = (select o from a.offspring o)" );
		assertTranslation( "from Animal a where a in (select o from a.offspring o)" );
	}

	public void testSimpleUncorrelatedSubselect2() throws Exception {
		assertTranslation( "from Animal a where a = (select an from Animal an)" );
		assertTranslation( "from Animal a where a in (select an from Animal an)" );
	}

	public void testUncorrelatedSubselect2() throws Exception {
		assertTranslation( "from Animal a where a.bodyWeight = (select max(an.bodyWeight) from Animal an)" );
	}

	public void testCorrelatedSubselect2() throws Exception {
		assertTranslation( "from Animal a where a.bodyWeight > (select max(o.bodyWeight) from a.offspring o)" );
	}

	public void testManyToManyJoinInSubselect() throws Exception {
		assertTranslation( "select foo from Foo foo where foo in (select elt from Baz baz join baz.fooArray elt)" );
	}

	public void testImplicitJoinInSubselect() throws Exception {
		assertTranslation( "from Animal a where a = (select an.mother from Animal an)" );
		assertTranslation( "from Animal a where a.id = (select an.mother.id from Animal an)" );
	}

	public void testManyToOneSubselect() {
		//TODO: the join in the subselect also shows up in the outer query!
		assertTranslation( "from Animal a where 'foo' in (select m.description from a.mother m)" );
	}

	public void testPositionalParameters() throws Exception {
		assertTranslation( "from Animal an where an.bodyWeight > ?" );
	}

	public void testKeywordPropertyName() throws Exception {
		assertTranslation( "from Glarch g order by g.order asc" );
		assertTranslation( "select g.order from Glarch g where g.order = 3" );
	}

	public void testJavaConstant() throws Exception {
		assertTranslation( "from org.hibernate.test.legacy.Category c where c.name = org.hibernate.test.legacy.Category.ROOT_CATEGORY" );
		assertTranslation( "from org.hibernate.test.legacy.Category c where c.id = org.hibernate.test.legacy.Category.ROOT_ID" );
		// todo : additional desired functionality
		//assertTranslation( "from Category c where c.name = Category.ROOT_CATEGORY" );
		//assertTranslation( "select c.name, Category.ROOT_ID from Category as c");
	}

	public void testClassName() throws Exception {
		assertTranslation( "from Zoo zoo where zoo.class = PettingZoo" );
		assertTranslation( "from DomesticAnimal an where an.class = Dog" );
		assertTranslation( "from Animal an where an.class = Dog" );
	}

	public void testSelectDialectFunction() throws Exception {
		// From SQLFunctionsTest.testDialectSQLFunctions...
		if ( getDialect() instanceof HSQLDialect ) {
			assertTranslation( "select mod(s.count, 2) from org.hibernate.test.legacy.Simple as s where s.id = 10" );
			//assertTranslation( "from org.hibernate.test.legacy.Simple as s where mod(s.count, 2) = 0" );
		}
		assertTranslation( "select upper(human.name.first) from Human human" );
		assertTranslation( "from Human human where lower(human.name.first) like 'gav%'" );
		assertTranslation( "select upper(a.description) from Animal a" );
		assertTranslation( "select max(a.bodyWeight) from Animal a" );
	}

	public void testTwoJoins() throws Exception {
		assertTranslation( "from Human human join human.friends, Human h join h.mother" );
		assertTranslation( "from Human human join human.friends f, Animal an join an.mother m where f=m" );
		assertTranslation( "from Baz baz left join baz.fooToGlarch, Bar bar join bar.foo" );
	}

	public void testToOneToManyManyJoinSequence() throws Exception {
		assertTranslation( "from Dog d join d.owner h join h.friends f where f.name.first like 'joe%'" );
	}

	public void testToOneToManyJoinSequence() throws Exception {
		assertTranslation( "from Animal a join a.mother m join m.offspring" );
		assertTranslation( "from Dog d join d.owner m join m.offspring" );
		assertTranslation( "from Animal a join a.mother m join m.offspring o where o.bodyWeight > a.bodyWeight" );
	}

	public void testSubclassExplicitJoin() throws Exception {
		assertTranslation( "from DomesticAnimal da join da.owner o where o.nickName = 'gavin'" );
		assertTranslation( "from DomesticAnimal da join da.owner o where o.bodyWeight > 0" );
	}

	public void testMultipleExplicitCollectionJoins() throws Exception {
		assertTranslation( "from Animal an inner join an.offspring os join os.offspring gc" );
		assertTranslation( "from Animal an left outer join an.offspring os left outer join os.offspring gc" );
	}

	public void testSelectDistinctComposite() throws Exception {
		// This is from CompositeElementTest.testHandSQL.
		assertTranslation( "select distinct p from org.hibernate.test.compositeelement.Parent p join p.children c where c.name like 'Child%'" );
	}

	public void testDotComponent() throws Exception {
		// from FumTest.testListIdentifiers()
		assertTranslation( "select fum.id from org.hibernate.test.legacy.Fum as fum where not fum.fum='FRIEND'" );
	}

	public void testOrderByCount() throws Exception {
		assertTranslation( "from Animal an group by an.zoo.id order by an.zoo.id, count(*)" );
	}

	public void testHavingCount() throws Exception {
		assertTranslation( "from Animal an group by an.zoo.id having count(an.zoo.id) > 1" );
	}

	public void selectWhereElements() throws Exception {
		assertTranslation( "select foo from Foo foo, Baz baz where foo in elements(baz.fooArray)" );
	}

	public void testCollectionOfComponents() throws Exception {
		assertTranslation( "from Baz baz inner join baz.components comp where comp.name='foo'" );
	}

	public void testNestedComponentIsNull() {
		// From MapTest...
		assertTranslation( "from Commento c where c.marelo.commento.mcompr is null" );
	}

	public void testOneToOneJoinedFetch() throws Exception {
		// From OneToOneTest.testOneToOneOnSubclass
		assertTranslation( "from org.hibernate.test.onetoone.joined.Person p join fetch p.address left join fetch p.mailingAddress" );
	}

	public void testSubclassImplicitJoin() throws Exception {
		assertTranslation( "from DomesticAnimal da where da.owner.nickName like 'Gavin%'" );
		assertTranslation( "from DomesticAnimal da where da.owner.nickName = 'gavin'" );
		assertTranslation( "from DomesticAnimal da where da.owner.bodyWeight > 0" );
	}

	public void testComponent2() throws Exception {
		assertTranslation( "from Dog dog where dog.owner.name.first = 'Gavin'" );
	}

	public void testOneToOne() throws Exception {
		assertTranslation( "from User u where u.human.nickName='Steve'" );
		assertTranslation( "from User u where u.human.name.first='Steve'" );
	}

	public void testSelectClauseImplicitJoin() throws Exception {
		//assertTranslation( "select d.owner.mother from Dog d" ); //bug in old qt
		assertTranslation( "select d.owner.mother.description from Dog d" );
	}

	public void testFromClauseImplicitJoin() throws Exception {
		assertTranslation( "from DomesticAnimal da join da.owner.mother m where m.bodyWeight > 10" );
	}

	public void testJoinedSubclassWithOrCondition() {
		assertTranslation( "from Animal an where (an.bodyWeight > 10 and an.bodyWeight < 100) or an.bodyWeight is null" );
	}

	public void testImplicitJoinInFrom() {
		assertTranslation( "from Human h join h.mother.mother.offspring o" );
	}

	public void testDuplicateImplicitJoinInSelect() {
		// This test causes failures on theta-join dialects because the SQL is different.  The old parser
		// duplicates the condition, whereas the new parser does not.  The queries are semantically the
		// same however.
		assertTranslation( "select an.mother.bodyWeight from Animal an join an.mother m where an.mother.bodyWeight > 10" );
		assertTranslation( "select an.mother.bodyWeight from Animal an where an.mother.bodyWeight > 10" );
		//assertTranslation("select an.mother from Animal an where an.mother.bodyWeight is not null");
		assertTranslation( "select an.mother.bodyWeight from Animal an order by an.mother.bodyWeight" );
	}

	public void testConstructorNode() throws Exception {
		ConstructorNode n = new ConstructorNode();
		assertNull( n.getFromElement() );
		assertFalse( n.isReturnableEntity() );
	}

	public void testIndexNode() throws Exception {
		IndexNode n = new IndexNode();
		Exception ex = null;
		try {
			n.setScalarColumnText( 0 );
		}
		catch ( UnsupportedOperationException e ) {
			ex = e;
		}
		assertNotNull( ex );
	}

	public void testExceptions() throws Exception {
		DetailedSemanticException dse = new DetailedSemanticException( "test" );
		dse.printStackTrace();
		dse.printStackTrace( new PrintWriter( new StringWriter() ) );
		QuerySyntaxError qse = new QuerySyntaxError( new RecognitionException( "test" ), "from bozo b where b.clown = true" );
		assertNotNull( qse.getMessage() );
	}

	public static Test suite() {
		return new TestSuite( HQLTest.class );
	}


	public void testSelectProperty2() throws Exception {
		assertTranslation( "select an, mo.bodyWeight from Animal an inner join an.mother mo where an.bodyWeight < mo.bodyWeight" );
		assertTranslation( "select an, mo, an.bodyWeight, mo.bodyWeight from Animal an inner join an.mother mo where an.bodyWeight < mo.bodyWeight" );
	}

	public void testSubclassWhere() throws Exception {
		// TODO: The classic QT generates lots of extra parens, etc.
		assertTranslation( "from PettingZoo pz1, PettingZoo pz2 where pz1.id = pz2.id" );
		assertTranslation( "from PettingZoo pz1, PettingZoo pz2 where pz1.id = pz2" );
		assertTranslation( "from PettingZoo pz where pz.id > 0 " );
	}

	public void testNestedImplicitJoinsInSelect() throws Exception {
		// NOTE: This test is not likely to generate the exact SQL because of the where clause.  The synthetic
		// theta style joins come out differently in the new QT.
		// From FooBarTest.testQuery()
		// Missing the foo2_ join, and foo3_ should include subclasses, but it doesn't.
//		assertTranslation("select foo.foo.foo.foo.string from org.hibernate.test.legacy.Foo foo where foo.foo.foo = 'bar'");
		assertTranslation( "select foo.foo.foo.foo.string from org.hibernate.test.legacy.Foo foo" );
	}

	public void testNestedComponent() throws Exception {
		// From FooBarTest.testQuery()
		//an extra set of parens in new SQL
		assertTranslation( "from org.hibernate.test.legacy.Foo foo where foo.component.subcomponent.name='bar'" );
	}

	public void testNull2() throws Exception {
		//old parser generates useless extra parens
		assertTranslation( "from Human h where not( h.nickName is null )" );
		assertTranslation( "from Human h where not( h.nickName is not null )" );
	}

	public void testUnknownFailureFromMultiTableTest() {
		assertTranslation( "from Lower s where s.yetanother.name='name'" );
	}

	public void testJoinInSubselect() throws Exception {
		//new parser uses ANSI-style inner join syntax
		assertTranslation( "from Animal a where a in (select m from Animal an join an.mother m)" );
		assertTranslation( "from Animal a where a in (select o from Animal an join an.offspring o)" );
	}
	
	public void testJoinedSubclassImplicitJoin() throws Exception {
		// From MultiTableTest.testQueries()
		// TODO: This produces the proper from clause now, but the parens in the where clause are different.
		assertTranslation( "from org.hibernate.test.legacy.Lower s where s.yetanother.name='name'" );
	}

	public void testProjectProductJoinedSubclass() throws Exception {
		// TODO: The old QT generates the discriminator and the theta join in a strange order, and with two extra sets of parens, this is okay, right?
		assertTranslation( "select zoo from Zoo zoo, PettingZoo pz where zoo=pz" );
		assertTranslation( "select zoo, pz from Zoo zoo, PettingZoo pz where zoo=pz" );
	}

	public void testCorrelatedSubselect1() throws Exception {
		// The old translator generates the theta join before the condition in the sub query.
		// TODO: Decide if we want to bother generating the theta join in the same order (non simple).
		assertTranslation( "from Animal a where exists (from a.offspring o where o.bodyWeight>10)" );
	}

	public void testOuterAliasInSubselect() {
		assertTranslation( "from Human h where h = (from Animal an where an = h)" );
	}

	public void testFetch() throws Exception {
		assertTranslation( "from Zoo zoo left join zoo.mammals" );
		assertTranslation( "from Zoo zoo left join fetch zoo.mammals" );
	}

	public void testOneToManyElementFunctionInWhere() throws Exception {
		assertTranslation( "from Zoo zoo where 'dog' in indices(zoo.mammals)" );
		assertTranslation( "from Zoo zoo, Dog dog where dog in elements(zoo.mammals)" );
	}	

	/*public void testManyToManyElementFunctionInSelect() throws Exception {
		assertTranslation("select elements(zoo.mammals) from Zoo zoo");
		assertTranslation("select indices(zoo.mammals) from Zoo zoo");
	}*/
	
	public void testManyToManyInJoin() throws Exception {
		assertTranslation( "select x.id from Human h1 join h1.family x" );
		//assertTranslation("select index(h2) from Human h1 join h1.family h2");
	}

	public void testManyToManyInSubselect() throws Exception {
		assertTranslation( "from Human h1, Human h2 where h2 in (select x.id from h1.family x)" );
		assertTranslation( "from Human h1, Human h2 where 'father' in indices(h1.family)" );
	}

	public void testOneToManyIndexAccess() throws Exception {
		assertTranslation( "from Zoo zoo where zoo.mammals['dog'] is not null" );
	}

	public void testImpliedSelect() throws Exception {
		assertTranslation( "select zoo from Zoo zoo" );
		assertTranslation( "from Zoo zoo" );
		assertTranslation( "from Zoo zoo join zoo.mammals m" );
		assertTranslation( "from Zoo" );
		assertTranslation( "from Zoo zoo join zoo.mammals" );
	}	
	
	/*public void testSelectElementsOfCollectionOfValues() throws Exception {
		// From FooBarTest.testQuery()
		// TODO: This produces the where clause in a different order, but it seems okay.
		assertTranslation("select foo.component.name, elements(foo.component.importantDates) from org.hibernate.test.legacy.Foo foo where foo.foo.id=?");
	}*/

	//public void testMultiTableElements() throws Exception {
	/*
	HQL    : select elements(ls.bag), elements(ls.set) from org.hibernate.test.legacy.Lower ls
	OLD SQL:
	select top2_.id1_ as col_0_0_, top4_.id1_ as col_1_0_
	from leafsubclass lower0_ inner join rootclass lower0_1_ on lower0_.id__=lower0_1_.id1_, simple_simple bag1_, rootclass top2_, rootclass set3_, rootclass top4_
	where lower0_1_.id1_ is not null and lower0_.id__=bag1_.simple1 and bag1_.simple2=top2_.id1_ and lower0_.id__=set3_.parent and set3_.id1_=top4_.id1_
	*/

	//assertTranslation("select elements(ls.bag), elements(ls.set) from org.hibernate.test.legacy.Lower ls");
	//}

	public void testCollectionsInSelect2() throws Exception {
		// This one looks okay now, it just generates extra parens in the where clause.
		assertTranslation( "select foo.string from Bar bar left join bar.baz.fooArray foo where bar.string = foo.string" );
	}


	//public void testCollectionsInSelect() throws Exception {
	// From FooBarTest.testCollectionsInSelect
	/*
	HQL    : select baz, baz.stringSet.size, count( distinct elements(baz.stringSet) ), max( elements(baz.stringSet) ) from org.hibernate.test.legacy.Baz baz group by baz
	OLD SQL:
	select
		baz0_.baz_id_column_ as baz_id_c1_, baz0_.count_count as count_co2_37_, baz0_.name_b as name_b37_, baz0_.foo as foo37_, baz0_.superBaz as superBaz37_, baz0_.str as str37_, baz0_.baz_id_column_ as col_0_0_,
		count(*) as col_1_0_,
		count(distinct stringset2_.element) as col_2_0_, max(stringset3_.element) as col_3_0_
	from baz baz0_, stringSet stringset1_, stringSet stringset2_, stringSet stringset3_
	where baz0_.baz_id_column_=stringset1_.id_ and baz0_.baz_id_column_=stringset2_.id_ and baz0_.baz_id_column_=stringset3_.id_
	group by  baz0_.baz_id_column_

	NEW SQL:
	select
		// TODO: Remove the extra 'id' column select.
		baz0_.baz_id_column_ as col_0_0_,
		// TODO: Figure out how the classic translator knows to use count(*)
		(select count(*) from stringSet stringset1_ where baz0_.baz_id_column_=stringset1_.id_) as col_1_0_,
		// This is also correct.
		count(distinct stringset2_.element) as col_2_0_, max(stringset3_.element) as col_3_0_,
		// The properties of baz are correct, they're just in the wrong place.
		baz0_.baz_id_column_ as baz_id_c1_, baz0_.count_count as count_co2_37_, baz0_.name_b as name_b37_, baz0_.foo as foo37_, baz0_.superBaz as superBaz37_, baz0_.str as str37_
//		 FROM is okay.
	from baz baz0_ stringSet stringset1_, stringSet stringset3_, stringSet stringset2_
//		 WHERE is okay.
	where (baz0_.baz_id_column_=stringset1_.id_ and baz0_.baz_id_column_=stringset2_.id_ baz0_.baz_id_column_=stringset3_.id_)
//		 GROUP BY is okay.
	group by  baz0_.baz_id_column_
	*/
	//assertTranslation( "select baz, size(baz.stringSet), count( distinct elements(baz.stringSet) ), max( elements(baz.stringSet) ) from org.hibernate.test.legacy.Baz baz group by baz");

	//}

}
