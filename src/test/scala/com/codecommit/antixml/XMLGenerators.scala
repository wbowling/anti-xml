/*
 * Copyright (c) 2011, Daniel Spiewak
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer. 
 * - Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 * - Neither the name of "Anti-XML" nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.codecommit.antixml

import org.scalacheck._
import scala.io.Source
import java.lang.String

trait XMLGenerators {
  import Arbitrary.arbitrary
  import Gen._
  
  val MaxGroupDepth = 3
  
  lazy val identifiers = (Source fromURL (getClass getResource ("/identifiers.txt")) getLines).toList
  
  implicit val arbSelector: Arbitrary[Selector[Node]] =
    Arbitrary(oneOf(nodeSelectorGenerator, elemSelectorGenerator))
  
  implicit def arbGroup[A <: Node](implicit arb: Arbitrary[A]): Arbitrary[Group[A]] =
    Arbitrary(groupGenerator[A])
  
  implicit val arbNode: Arbitrary[Node] = Arbitrary(nodeGenerator(0))
  
  implicit val arbProcInstr: Arbitrary[ProcInstr] = Arbitrary(procInstrGenerator)
  implicit val arbElem: Arbitrary[Elem] = Arbitrary(elemGenerator(0))
  implicit val arbText: Arbitrary[Text] = Arbitrary(textGenerator)
  implicit val arbEntityRef: Arbitrary[EntityRef] = Arbitrary(entityRefGenerator)
  
  implicit val arbAttributes: Arbitrary[Attributes] = Arbitrary(genAttributes)
  implicit val arbQName: Arbitrary[QName] = Arbitrary(genQName)
  
  lazy val elemSelectorGenerator = oneOf(identifiers) map Selector.stringToSelector
  
  lazy val nodeSelectorGenerator: Gen[Selector[Node]] = for {
    flag <- arbitrary[Boolean]
    xform <- arbitrary[Node => Node]
  } yield Selector({
    case n if flag => xform(n)
  })
  
  def groupGenerator[A <: Node](implicit arb: Arbitrary[A]): Gen[Group[A]] =
    listOf(arb.arbitrary) map Group.fromSeq
  
  def nodeGenerator(depth: Int = 0): Gen[Node] = frequency(
    3 -> procInstrGenerator, 
    30 -> elemGenerator(depth), 
    50 -> textGenerator, 
    17 -> entityRefGenerator)
  
  lazy val procInstrGenerator: Gen[ProcInstr] = for {
    target <- genSaneString
    data <- genSaneString
  } yield ProcInstr(target, data)
  
  def elemGenerator(depth: Int = 0): Gen[Elem] = for {
    ns <- genSaneOptionString
    name <- genSaneString
    attrs <- genAttributes
    bindings <- genBindings
    children <- if (depth > MaxGroupDepth) const(Group()) else (listOf(nodeGenerator(depth + 1)) map Group.fromSeq)
  } yield {
    val namespace = attrs.foldLeft(bindings){
      case (nb, (QName(Some(p), _), _)) => nb.findByPrefix(p).map(_ => nb).getOrElse(nb.append(p, "urn:foo:bar"))
      case (nb, _) => nb
    }
    Elem(ns, name, attrs, namespace, children)
  }
  
  lazy val textGenerator: Gen[Text] = genSaneString map Text
  
  lazy val entityRefGenerator: Gen[EntityRef] = genSaneString map EntityRef
  
  lazy val genSaneString: Gen[String] = oneOf(identifiers)
  
  private lazy val genSaneOptionString: Gen[Option[String]] =
    frequency(5 -> (genSaneString map { Some(_) }), 1 -> None)
  
  private lazy val genAttributes: Gen[Attributes] = genAttributeList map { Attributes(_: _*) }
  
  lazy val genAttributeList: Gen[List[(QName,String)]] = {
    val genTuple = for {
      qname <- genQName
      value <- genSaneString
    } yield (qname, value)
    
    listOf(genTuple)
  }

  private lazy val genBindings: Gen[NamespaceBinding] = {
    val genTuple = for {
      prefix <- genSaneString
      uri <- genSaneString
    } yield (prefix, uri)

    listOf(genTuple).map(_.foldLeft(NamespaceBinding.empty)((parent, child) => parent.append(child._1, child._2)))
  }

  private lazy val genQName: Gen[QName] = for {
    ns <- genSaneOptionString
    name <- genSaneString
  } yield QName(ns, name)
}
