package de.itemis.tooling.xturtle.linking

import com.google.inject.Inject
import de.itemis.tooling.xturtle.NoValidationInjectorProvider
import de.itemis.tooling.xturtle.TurtleParseHelper
import de.itemis.tooling.xturtle.resource.TurtleResourceService
import de.itemis.tooling.xturtle.xturtle.DirectiveBlock
import de.itemis.tooling.xturtle.xturtle.PrefixId
import de.itemis.tooling.xturtle.xturtle.QNameRef
import de.itemis.tooling.xturtle.xturtle.Resource
import de.itemis.tooling.xturtle.xturtle.Triples
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.junit4.InjectWith
import org.eclipse.xtext.junit4.XtextRunner
import org.eclipse.xtext.junit4.validation.ValidationTestHelper
import org.eclipse.xtext.naming.IQualifiedNameProvider
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(typeof(XtextRunner))
@InjectWith(typeof(NoValidationInjectorProvider))
class LinkingTest {

	@Inject extension TurtleParseHelper<DirectiveBlock>
	@Inject extension ValidationTestHelper
	@Inject IQualifiedNameProvider namer
	@Inject 
	TurtleResourceService service;

	@Test
	def void resolveUri() {
		val model='''
			@base <http://www.nittka.de/alex/> .
			<name> <is/somethingLike> <http://www.nittka.de/familyName/Nittka> .
		'''.parse
		model.assertNoErrors
		val triple = model.triples.get(0)
		assertName(triple.subject, "http://www.nittka.de/alex/","name")
		assertName(triple.predObjs.get(0).verb, "http://www.nittka.de/alex/is/","somethingLike")
		assertName(triple.predObjs.get(0).objects.get(0), "http://www.nittka.de/familyName/","Nittka")
	}

	@Test
	def void testNameCombinations() {
		val model='''
			@prefix :<tada#> .
			@prefix b:<tidum#> .
			@prefix b.a:<blubbs#> .
			: : :.
			:a :a :a.
			:a.b :a.b :a.b.
			b: b: b:.
			b:a b:a b:a.
			b:a.b b:a.b b:a.b.
			b.a: b.a: b.a:.
			b.a:a b.a:a b.a:a.
			b.a:a.b b.a:a.b b.a:a.b.
			:: :: ::.
			::: ::: :::.
			::.: ::.: ::.:.
			::a: ::a: ::a:.
			:a: :a: :a:.
			::a ::a ::a.
			:::a :::a :::a.
			:a:a.b :a:a.b :a:a.b.
			:a:a.b: :a:a.b: :a:a.b:.
		'''.parse
		model.assertNoIssues
		val triples=model.eAllContents.filter(typeof(Triples)).toList;
		(0..triples.size-1).forEach[
			assertAllLinkToSubject(triples.get(it))
		]
	}


	@Test
	def void testRedefinePrefix() {
		val model='''
			@base <http://example.de/>.
			@prefix a:<tada#> .
			a:a a:a a:a .
			@prefix a:<tidum/> .
			a:b a:b a:b .
		'''.parse
		model.assertNoIssues

		val triples=model.eAllContents.filter(typeof(Triples)).toList
		Assert::assertEquals(2, triples.size)
		assertName(triples.get(0).subject, "http://example.de/tada#","a")
		assertName(triples.get(1).subject, "http://example.de/tidum/","b")

		assertAllLinkToSubject(triples.get(0))
		assertAllLinkToSubject(triples.get(1))

		val prefixDefs=model.eAllContents.filter(typeof(PrefixId)).toList
		Assert::assertEquals(2, prefixDefs.size)
		assertName(prefixDefs.get(0), "http://example.de/tada#")
		assertName(prefixDefs.get(1), "http://example.de/tidum/")
	}

	def void assertName(EObject obj, String... nameElements){
		val name=service.getQualifiedName(obj) 
		if(obj instanceof Resource){
			Assert::assertEquals(name, namer.getFullyQualifiedName(obj))
		}
		Assert::assertEquals(nameElements.size, name.segmentCount)
		(1..nameElements.size).forEach[
			Assert::assertEquals(nameElements.get(it-1), name.segments.get(it-1))
		]
	}

	def assertAllLinkToSubject(Triples triple){
		val subject=triple.subject
		val refs=triple.eAllContents.filter(typeof(QNameRef)).toList
		val subjectName=service.getQualifiedName(subject)
		Assert::assertEquals(2, refs.size)
		refs.forEach[reference|
			Assert::assertEquals(subjectName, service.getQualifiedName(reference.ref))
			Assert::assertSame(subject, reference.ref)
		]
	}

}
