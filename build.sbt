organization := "no.arktekk"

scalaVersion := "2.10.4"

crossScalaVersions := Seq("2.10.4", "2.11.1")

scalacOptions := Seq("-deprecation", "-language:_")

description := "anti-xml"

name := "anti-xml"

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.10.0" % "test" withSources,
  "org.specs2" %% "specs2" % "1.12.3" % "test" withSources
)
      
initialCommands in console := """import com.codecommit.antixml._
    |val bookstore = <bookstore><book><title>For Whom the Bell Tolls</title><author>Hemmingway</author></book><book><title>I, Robot</title><author>Isaac Asimov</author></book><book><title>Programming Scala</title><author>Dean Wampler</author><author>Alex Payne</author></book></bookstore>.convert
    |val books = bookstore \ "book" """.stripMargin

doc in Compile <<= (clean in Compile, doc in Compile) map {
  (c, d) => d
}

scalacOptions in Compile in doc <++= (unmanagedSourceDirectories in Compile) map {
  (usd) =>
    val scalaSrc: File = (usd filter {
      _.toString endsWith "scala"
    }).head
    Seq(
      "-sourcepath", scalaSrc.toString,
      "-doc-source-url", "https://github.com/arktekk/anti-xml/tree/master/src/main/scala€{FILE_PATH}.scala"
    )
}
