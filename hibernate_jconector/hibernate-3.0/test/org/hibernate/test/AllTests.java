//$Id: AllTests.java,v 1.42 2005/03/29 03:07:30 oneovthafew Exp $
package org.hibernate.test;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.hibernate.test.ast.ASTIteratorTest;
import org.hibernate.test.ast.ASTUtilTest;
import org.hibernate.test.batchfetch.BatchFetchTest;
import org.hibernate.test.cache.SecondLevelCacheTest;
import org.hibernate.test.cid.CompositeIdTest;
import org.hibernate.test.collection.CollectionTest;
import org.hibernate.test.component.ComponentTest;
import org.hibernate.test.compositeelement.CompositeElementTest;
import org.hibernate.test.criteria.CriteriaQueryTest;
import org.hibernate.test.cuk.CompositePropertyRefTest;
import org.hibernate.test.cut.CompositeUserTypeTest;
import org.hibernate.test.discriminator.DiscriminatorTest;
import org.hibernate.test.dom4j.Dom4jAccessorTest;
import org.hibernate.test.dom4j.Dom4jTest;
import org.hibernate.test.dynamic.DynamicClassTest;
import org.hibernate.test.entity.MultiRepresentationTest;
import org.hibernate.test.filter.DynamicFilterTest;
import org.hibernate.test.formulajoin.FormulaJoinTest;
import org.hibernate.test.hql.ASTParserLoadingTest;
import org.hibernate.test.hql.EJBQLTest;
import org.hibernate.test.hql.HQLTest;
import org.hibernate.test.hql.HqlParserTest;
import org.hibernate.test.id.MultipleHiLoPerTableGeneratorTest;
import org.hibernate.test.idbag.IdBagTest;
import org.hibernate.test.interfaceproxy.InterfaceProxyTest;
import org.hibernate.test.join.JoinTest;
import org.hibernate.test.joinedsubclass.JoinedSubclassTest;
import org.hibernate.test.legacy.ABCProxyTest;
import org.hibernate.test.legacy.ABCTest;
import org.hibernate.test.legacy.CacheTest;
import org.hibernate.test.legacy.ComponentNotNullTest;
import org.hibernate.test.legacy.ConfigurationPerformanceTest;
import org.hibernate.test.legacy.FooBarTest;
import org.hibernate.test.legacy.FumTest;
import org.hibernate.test.legacy.IJ2Test;
import org.hibernate.test.legacy.IJTest;
import org.hibernate.test.legacy.MapTest;
import org.hibernate.test.legacy.MasterDetailTest;
import org.hibernate.test.legacy.MultiTableTest;
import org.hibernate.test.legacy.NonReflectiveBinderTest;
import org.hibernate.test.legacy.OneToOneCacheTest;
import org.hibernate.test.legacy.ParentChildTest;
import org.hibernate.test.legacy.QueryByExampleTest;
import org.hibernate.test.legacy.SQLFunctionsTest;
import org.hibernate.test.legacy.SQLLoaderTest;
import org.hibernate.test.legacy.StatisticsTest;
import org.hibernate.test.map.MapIndexFormulaTest;
import org.hibernate.test.mapcompelem.MapCompositeElementTest;
import org.hibernate.test.mapelemformula.MapElementFormulaTest;
import org.hibernate.test.mixed.MixedTest;
import org.hibernate.test.onetomany.OneToManyTest;
import org.hibernate.test.onetoone.joined.OneToOneTest;
import org.hibernate.test.onetooneformula.OneToOneFormulaTest;
import org.hibernate.test.ops.CreateTest;
import org.hibernate.test.ops.MergeTest;
import org.hibernate.test.optlock.OptimisticLockTest;
import org.hibernate.test.orphan.OrphanTest;
import org.hibernate.test.propertyref.PropertyRefTest;
import org.hibernate.test.proxy.ProxyTest;
import org.hibernate.test.querycache.QueryCacheTest;
import org.hibernate.test.sql.MSSQLTest;
import org.hibernate.test.sql.SQLTest;
import org.hibernate.test.subclassfilter.DiscrimSubclassFilterTest;
import org.hibernate.test.subclassfilter.JoinedSubclassFilterTest;
import org.hibernate.test.subclassfilter.UnionSubclassFilterTest;
import org.hibernate.test.subclasspropertyref.SubclassPropertyRefTest;
import org.hibernate.test.subselect.SubselectTest;
import org.hibernate.test.subselectfetch.SubselectFetchTest;
import org.hibernate.test.ternary.TernaryTest;
import org.hibernate.test.tm.CMTTest;
import org.hibernate.test.typedonetoone.TypedOneToOneTest;
import org.hibernate.test.typeparameters.TypeParameterTest;
import org.hibernate.test.unidir.BackrefTest;
import org.hibernate.test.unionsubclass.UnionSubclassTest;

/**
 * @author Gavin King
 */
public class AllTests {
	
	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTest( newTests() );
		suite.addTest( oldTests() );
		return suite;
	}

	public static TestSuite newTests() {
		TestSuite suite = new TestSuite("New tests suite");
		suite.addTest( CreateTest.suite() );
		suite.addTest( MergeTest.suite() );
		suite.addTest( ComponentTest.suite() );
		suite.addTest( ProxyTest.suite() );
		suite.addTest( TernaryTest.suite() );
		suite.addTest( CollectionTest.suite() );
		suite.addTest( IdBagTest.suite() );
		suite.addTest( MapCompositeElementTest.suite() );
		suite.addTest( MapIndexFormulaTest.suite() );
		suite.addTest( MapElementFormulaTest.suite() );
		suite.addTest( BackrefTest.suite() );
		suite.addTest( BatchFetchTest.suite() );
		suite.addTest( CompositeIdTest.suite() );
		suite.addTest( CompositeElementTest.suite() );
		suite.addTest( CompositePropertyRefTest.suite() );
		suite.addTest( FormulaJoinTest.suite() );
		suite.addTest( DiscriminatorTest.suite() );
		suite.addTest( DynamicClassTest.suite() );
		suite.addTest( DynamicFilterTest.suite() );
		suite.addTest( InterfaceProxyTest.suite() );
		suite.addTest( OrphanTest.suite() );
		suite.addTest( JoinTest.suite() );
		suite.addTest( JoinedSubclassTest.suite() );
		suite.addTest( MixedTest.suite() );
		suite.addTest( OneToManyTest.suite() );
		suite.addTest( OneToOneFormulaTest.suite() );
		suite.addTest( OneToOneTest.suite() );
		suite.addTest( org.hibernate.test.onetoone.singletable.OneToOneTest.suite() );
		suite.addTest( org.hibernate.test.onetoonelink.OneToOneTest.suite() );
		suite.addTest( OptimisticLockTest.suite() );
		suite.addTest( PropertyRefTest.suite() );
		suite.addTest( SubclassPropertyRefTest.suite() );
		suite.addTest( SQLTest.suite() );
		suite.addTest( MSSQLTest.suite() );
		suite.addTest( CriteriaQueryTest.suite() );
		suite.addTest( SubselectTest.suite() );
		suite.addTest( SubselectFetchTest.suite() );
		suite.addTest( UnionSubclassTest.suite() );
		suite.addTest( ASTIteratorTest.suite() );
		suite.addTest( ASTParserLoadingTest.suite() );
		suite.addTest( ASTUtilTest.suite() );
		suite.addTest( SecondLevelCacheTest.suite() );
		suite.addTest( QueryCacheTest.suite() );
		suite.addTest( HqlParserTest.suite() );
		suite.addTest( CompositeUserTypeTest.suite() );
		suite.addTest( TypeParameterTest.suite() );
		suite.addTest( TypedOneToOneTest.suite() );
		suite.addTest( CMTTest.suite() );
		suite.addTest( HQLTest.suite() );
		suite.addTest( EJBQLTest.suite() );
		suite.addTest( MultipleHiLoPerTableGeneratorTest.suite() );
		suite.addTest( MultiRepresentationTest.suite() );
		suite.addTest( Dom4jAccessorTest.suite() );
		suite.addTest( Dom4jTest.suite() );
		suite.addTest( UnionSubclassFilterTest.suite() );
		suite.addTest( JoinedSubclassFilterTest.suite() );
		suite.addTest( DiscrimSubclassFilterTest.suite() );
		return suite;
	}

	public static TestSuite oldTests() {
		TestSuite suite = new TestSuite("Legacy tests suite");
		suite.addTest( FumTest.suite() );
		suite.addTest( MasterDetailTest.suite() );
		suite.addTest( ParentChildTest.suite() );
		suite.addTest( ABCTest.suite() );
		suite.addTest( ABCProxyTest.suite() );
		suite.addTest( SQLFunctionsTest.suite() );
		suite.addTest( SQLLoaderTest.suite() );
		suite.addTest( MultiTableTest.suite() );
		suite.addTest( MapTest.suite() );
		suite.addTest( QueryByExampleTest.suite() );
		suite.addTest( ComponentNotNullTest.suite() );
		suite.addTest( IJTest.suite() );
		suite.addTest( IJ2Test.suite() );
		suite.addTest( FooBarTest.suite() );
		suite.addTest( StatisticsTest.suite() );
		suite.addTest( CacheTest.suite() );
		suite.addTest( OneToOneCacheTest.suite() );
		suite.addTest( NonReflectiveBinderTest.suite() );
		suite.addTest( ConfigurationPerformanceTest.suite() ); // Added to ensure we can utilize the recommended performance tips ;)
		
		return suite;
	}

	public static void main(String args[]) {
		TestRunner.run( suite() );
	}

}