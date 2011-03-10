package com.codecommit.antixml

import scala.io.Source
import com.github.dmlap.sizeof.SizeOf.deepsize
import javax.xml.parsers.DocumentBuilderFactory 

object Performance {
  def main(args: Array[String]) {
    println("-- System Information --")
    println("Heap: " + (Runtime.getRuntime.maxMemory / (1024 * 1024)) + "MB")
    println("Java: " + System.getProperty("java.vendor") + " " + System.getProperty("java.version"))
    println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"))
    println("-- Memory Usage --")
    println("anti-xml: " + deepsize(LoadingXml.antiXml.run()))
    println("scala.xml: " + deepsize(LoadingXml.scalaXml.run()))
    println("javax.xml: " + deepsize(LoadingXml.javaXml.run()))
    println("-- Execution Time --")
    
    List(LoadingXml, ShallowSelection, DeepSelection) foreach timeTrial
  }

  def timedMs(f: =>Unit): Long = {
    val start = System.nanoTime
    f
    (System.nanoTime - start) / 1000000
  }

  def timeTrial(trial: Trial) {
    println(trial.description)
    trial.implementations foreach { impl =>
      print(" + " + impl.description + ": ")
      (0 until trial.warmUps) foreach { _ => impl.run() }
      println(TimingResults((0 until trial.runs) map { _ =>
        System.gc()
        timedMs { impl.run() }
      }))
    }
  }

  case class Trial(val description: String) {
    var implementations: Seq[Implementation] = Seq()
    val warmUps = 5
    val runs = 10

    object implemented {
      def by(desc: String) = new {
        def in(impl: =>Any): Implementation = {
          val result = new Implementation  {
            val description = desc
            def run() = {
              impl
            }
          }
          implementations = implementations :+ result
          result
        }
      }
    }
    
    trait Implementation {
      val description: String
      def run(): Any
    }
  }

  object LoadingXml extends Trial("Loading a 7MB XML file") {
    val spendingPath = "/spending.xml"
    val antiXml = implemented by "anti-xml" in {
      XML.fromInputStream(getClass.getResourceAsStream(spendingPath))
    }
    val scalaXml = implemented by "scala.xml" in {
      scala.xml.XML.load(getClass.getResourceAsStream(spendingPath))
    }
    val javaXml = implemented by "javax.xml" in {
      DocumentBuilderFactory.newInstance.newDocumentBuilder.parse(getClass.getResourceAsStream(spendingPath))
    }
  }

  object ShallowSelection extends Trial("Shallow selection in a 7MB tree") {
    val spendingPath = "/spending.xml"
    
    val antiTree = XML.fromInputStream(getClass.getResourceAsStream(spendingPath))
    val scalaTree = scala.xml.XML.load(getClass.getResourceAsStream(spendingPath))
    val domTree = DocumentBuilderFactory.newInstance.newDocumentBuilder.parse(getClass.getResourceAsStream(spendingPath))
    
    val antiXml = implemented by "anti-xml" in {
      antiTree \ "result" \ "doc" \ "MajorFundingAgency2"
      antiTree \ "foo" \ "bar" \ "MajorFundingAgency2"
    }
    val scalaXml = implemented by "scala.xml" in {
      scalaTree \ "result" \ "doc" \ "MajorFundingAgency2"
      scalaTree \ "foo" \ "bar" \ "MajorFundingAgency2"
    }
    val javaXml = implemented by "javax.xml" in {
      val childNodes = domTree.getChildNodes item 0 getChildNodes
      
      for {
        i <- 0 until childNodes.getLength
        val node = childNodes item i
        if node.getNodeName == "result"
        
        val subChildren = node.getChildNodes
        j <- 0 until subChildren.getLength
        val subNode = subChildren item j
        if subNode.getNodeName == "doc"
        
        val subChildren2 = node.getChildNodes
        j <- 0 until subChildren2.getLength
        val subNode2 = subChildren2 item j
        if subNode2.getNodeName == "MajorFundingAgency2"
      } yield subNode
      
      for {
        i <- 0 until childNodes.getLength
        val node = childNodes item i
        if node.getNodeName == "foo"
        
        val subChildren = node.getChildNodes
        j <- 0 until subChildren.getLength
        val subNode = subChildren item j
        if subNode.getNodeName == "bar"
        
        val subChildren2 = node.getChildNodes
        j <- 0 until subChildren2.getLength
        val subNode2 = subChildren2 item j
        if subNode2.getNodeName == "MajorFundingAgency2"
      } yield subNode
    }
  }

  object DeepSelection extends Trial("Deep selection in a 7MB tree") {
    val spendingPath = "/spending.xml"
    
    val antiTree = XML.fromInputStream(getClass.getResourceAsStream(spendingPath))
    val scalaTree = scala.xml.XML.load(getClass.getResourceAsStream(spendingPath))
    val domTree = DocumentBuilderFactory.newInstance.newDocumentBuilder.parse(getClass.getResourceAsStream(spendingPath))
    
    val antiXml = implemented by "anti-xml" in {
      antiTree \\ "MajorFundingAgency2"
      antiTree \\ "fubar"
    }
    val scalaXml = implemented by "scala.xml" in {
      scalaTree \\ "MajorFundingAgency2"
      scalaTree \\ "fubar"
    }
    val javaXml = implemented by "javax.xml" in {
      {
        val result = domTree getElementsByTagName "MajorFundingAgency2"
        (0 until result.getLength) foreach (result item)
      }
      
      {
        val result = domTree getElementsByTagName "fubar"
        (0 until result.getLength) foreach (result item)
      }
    }
  }

  // def loadDiscogs = Test("anti-xml loading a large file") { () =>
  //   XML.fromInputStream(getClass.getResourceAsStream("/discogs_20110201_labels.xml"))
  // }

  class TimingResults private(val data: Seq[Long],
                              val min: Long,
                              val max: Long,
                              val average: Long) {
    override def toString = "min: " + min + " ms, max: " + max +
    " ms, average: " + average + " ms"
  }
  object TimingResults {
    def apply(results: Seq[Long]): TimingResults = {
      val (min, max, sum) = results.foldLeft(java.lang.Long.MAX_VALUE, 0L, 0L) { case ((min, max, sum), result) =>
        (math.min(min, result), math.max(max, result), sum + result)
      }
      new TimingResults(results, min, max, sum / results.size)
    }
  }
}
