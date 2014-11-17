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

import org.specs2.mutable._

class StAXSpecs extends Specification {
  object StAXParser extends StAXParser

  "StAXParser" should {
    "parse a simple element" in {
      StAXParser.fromString("<a/>") mustEqual Elem(None, "a")
    }

    "parse an element with namespace" in {
      StAXParser.fromString("<a xmlns='urn:a'/>") mustEqual Elem(None, "a", Attributes(), NamespaceBinding("urn:a"), Group())
    }

    "parse an element with default namespace prefix" in {
      StAXParser.fromString("<a xmlns='urn:a'><b/></a>") mustEqual Elem(None, "a", Attributes(), NamespaceBinding("urn:a"), Group(Elem(None, "b", Attributes(), NamespaceBinding("urn:a"), Group())))
    }

    "parse an element with default namespace prefix" in {
      StAXParser.fromString("<a xmlns='urn:a' xmlns:x='urn:x'><b/></a>") mustEqual Elem(None, "a", Attributes(), NamespaceBinding("x", "urn:x", NamespaceBinding("urn:a")), Group(Elem(None, "b", Attributes(), NamespaceBinding("x", "urn:x", NamespaceBinding("urn:a")), Group())))
    }

    "parse an element with default namespace prefix and child element with a re-defined default namespace" in {
      StAXParser.fromString("<a xmlns='urn:a'><b xmlns='urn:b'/></a>") mustEqual Elem(None, "a", Attributes(), NamespaceBinding("urn:a"), Group(Elem(None, "b", Attributes(), NamespaceBinding("urn:b", NamespaceBinding("urn:a")), Group())))
    }

    "parse a simpleString and generate an Elem" in {
      StAXParser.fromString("<a:a xmlns:a='urn:a'>hi<b attr='value' /> there</a:a>") mustEqual Elem(Some("a"), "a", Attributes(), NamespaceBinding("a", "urn:a"), Group(Text("hi"), Elem(None, "b", Attributes(QName("attr") -> "value"), NamespaceBinding("a", "urn:a"), Group()), Text(" there")))
    }

    "parse a simpleString with an non-prefixed namespace" in {
      StAXParser.fromString("<a xmlns='urn:a'/>") mustEqual Elem(None, "a", Attributes(), NamespaceBinding("urn:a"), Group())
    }

    "parse a simpleString with both a namespace and an attribute" in {
      StAXParser.fromString("<a xmlns='urn:a' key='val' />") mustEqual Elem(None, "a", Attributes("key"->"val"), NamespaceBinding("urn:a"), Group())
    }

    "parse a simpleString with both a namespace and an attribute" in {
      StAXParser.fromString("<a xmlns='urn:a' xmlns:b='urn:b-ns' key='val'><b:foo/></a>") mustEqual Elem(None, "a", Attributes("key"->"val"), NamespaceBinding("b", "urn:b-ns", NamespaceBinding("urn:a")), Group(Elem(Some("b"), "foo", Attributes(), NamespaceBinding("b", "urn:b-ns", NamespaceBinding("urn:a")), Group())))
    }

    "reuse namespace binding objects, 1" in {
      val e = StAXParser.fromString("<a xmlns='urn:a'><b/></a>")
      e.namespaces must beTheSameAs(e.children.head.asInstanceOf[Elem].namespaces)
    }

    "reuse namespace binding objects even if it's re-declared" in {
      val e = StAXParser.fromString("<a xmlns='urn:a'><b xmlns='urn:a'/></a>")
      e.namespaces must beTheSameAs(e.children.head.asInstanceOf[Elem].namespaces)
    }

    "reuse namespace but treat as a separate scope because of added prefix" in {
      // example EPUB OPF tag with an attribute using a namespace prefix for a namespace that's also global
      StAXParser.fromString("<package xmlns='http://www.idpf.org/2007/opf' version='2.0'><metadata xmlns:dc='http://purl.org/dc/elements/1.1/' xmlns:opf='http://www.idpf.org/2007/opf'><dc:creator opf:role='aut'>Author Name Here</dc:creator></metadata></package>")
      true // doubt it will get this far. will probably get "Attribute with name 'role' with prefix 'opf' is not defined on element"
    }

    "parse a complex document with namespaces" in {
      val feed = StAXParser.fromInputStream(getClass.getResourceAsStream("/feed.xml"))
      feed.canonicalize.children.length mustEqual(17)
    }
  }
}
