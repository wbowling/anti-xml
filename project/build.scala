import aether.Aether
import sbt._
import Keys._


object build extends Build {


  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "no.arktekk",
    scalaVersion := "2.10.0",
    crossScalaVersions := Seq("2.9.2", "2.9.1", "2.10.0"),
    scalacOptions := Seq("-deprecation", "-language:_"),
    publishTo <<= (version) apply {
      (v: String) => if (v.trim().endsWith("SNAPSHOT")) Some(Resolvers.sonatypeNexusSnapshots) else Some(Resolvers.sonatypeNexusStaging)
    },
    pomIncludeRepository := {
      x => false
    },
    credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
  ) ++ Aether.aetherPublishSettings

  lazy val testPerf = test in Performance <<=
    (streams, classDirectory in Performance, classDirectory in Compile, managedClasspath in Performance,
      compile in Performance, compile in Compile) map {
      (s, cdt, cdc, deps, ct, cc) =>
        val log = s.log
        log.info("Running performance tests...")
        val depFiles = deps map {
          _.data
        }
        val sizeOfJar = depFiles ** ("sizeof" + "*.jar")
        val cp = depFiles ++ Seq(cdt, cdc)
        val vmArgs = Seq(
          "-javaagent:" + sizeOfJar.absString,
          "-Xmx2g",
          "-XX:+UseSerialGC") //Seems to make System.gc() synchronous
        //To find bottlenecks, add "-agentlib:hprof=cpu=samples,depth=6" to vmArgs
        new Fork.ForkScala("com.codecommit.antixml.Performance")(None, vmArgs, cp, Nil, log) match {
          case 0 =>
          case x => throw new IllegalArgumentException("failed with error code " + x)
        }
    } dependsOn(copyResources in Performance)


  lazy val root = Project(
    id = "anti-xml",
    base = file("."),
    settings = buildSettings ++ Seq(
      description := "anti-xml",
      name := "anti-xml",
      libraryDependencies ++= Seq(
        "org.scalacheck" %% "scalacheck" % "1.10.0" % "test" withSources,
        "org.specs2" %% "specs2" % "1.12.3" % "test" withSources,
        "com.github.dmlap" % "sizeof" % "0.1" % "perf" from "http://cloud.github.com/downloads/dmlap/jvm-sizeof/jvm-sizeof-0.1.jar"
      ),
      /*publishArtifact in(Compile, packageBin) := true,
      publishArtifact in(Test, packageBin) := false,
      publishArtifact in(Compile, packageDoc) := true,
      publishArtifact in(Test, packageDoc) := false,
      publishArtifact in(Compile, packageSrc) := true,
      publishArtifact in(Test, packageSrc) := false,*/
      manifestSetting,
      initialCommands in console := """import com.codecommit.antixml._
                                      |val bookstore = <bookstore><book><title>For Whom the Bell Tolls</title><author>Hemmingway</author></book><book><title>I, Robot</title><author>Isaac Asimov</author></book><book><title>Programming Scala</title><author>Dean Wampler</author><author>Alex Payne</author></book></bookstore>.convert
                                      |val books = bookstore \ "book" """.stripMargin,
      doc in Compile <<= (clean in Compile, doc in Compile) map {
        (c, d) => d
      },

      scalacOptions in Compile in doc <++= (unmanagedSourceDirectories in Compile) map {
        (usd) =>
          val scalaSrc: File = (usd filter {
            _.toString endsWith "scala"
          }).head
          Seq(
            "-sourcepath", scalaSrc.toString,
            "-doc-source-url", "https://github.com/arktekk/anti-xml/tree/master/src/main/scala€{FILE_PATH}.scala"
          )
      },
      testPerf
    ) ++ inConfig(Performance)(Defaults.configSettings) ++ mavenCentralFrouFrou
  ).configs(Performance)

  object Resolvers {
    val sonatypeNexusSnapshots = "Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    val sonatypeNexusStaging = "Sonatype Nexus Staging" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  }

  lazy val manifestSetting = packageOptions <+= (name, version, organization) map {
    (title, version, vendor) =>
      Package.ManifestAttributes(
        "Created-By" -> "Simple Build Tool",
        "Built-By" -> System.getProperty("user.name"),
        "Build-Jdk" -> System.getProperty("java.version"),
        "Specification-Title" -> title,
        "Specification-Version" -> version,
        "Specification-Vendor" -> vendor,
        "Implementation-Title" -> title,
        "Implementation-Version" -> version,
        "Implementation-Vendor-Id" -> vendor,
        "Implementation-Vendor" -> vendor
      )
  }

  // Things we care about primarily because Maven Central demands them
  lazy val mavenCentralFrouFrou = Seq(
    homepage := Some(new URL("http://anti-xml.org")),
    startYear := Some(2011),
    licenses := Seq(("BSD", new URL("https://github.com/arktekk/anti-xml/blob/master/LICENSE.rst"))),
    pomExtra <<= (pomExtra, name, description) {
      (pom, name, desc) => pom ++ xml.Group(
        <scm>
          <url>http://github.com/arktekk/anti-xml</url>
          <connection>scm:git:git://github.com/arktekk/anti-xml.git</connection>
          <developerConnection>scm:git:git@github.com:arktekk/anti-xml.git</developerConnection>
        </scm>
          <developers>
            <developer>
              <id>djspiewak</id>
              <name>Daniel Spiewak</name>
              <url>http://twitter.com/djspiewak</url>
            </developer>
          </developers>
          <contributors>
            <contributor>
              <name>Erlend Hamnaberg</name>
              <url>http://twitter.com/hamnis</url>
            </contributor>
            <contributor>
              <name>Trygve Laugstøl</name>
              <url>http://twitter.com/trygvis</url>
            </contributor>
            <contributor>
              <name>Daniel Beskin</name>
            </contributor>
            <contributor>
              <name>Joshua Arnold</name>
            </contributor>
            <contributor>
              <name>Martin Kneissl</name>
            </contributor>
            <contributor>
              <name>Erik Engbrecht</name>
            </contributor>
            <contributor>
              <name>Heikki Vesalainen</name>
            </contributor>
          </contributors>
      )
    }
  )

  lazy val Performance = config("perf") extend(Compile) describedAs("Performance tests")

}