organization := "no.arktekk"

scalaVersion := "2.10.4"

//crossScalaVersions := Seq("2.10.4", "2.11.1")

scalacOptions := Seq("-deprecation", "-language:_")

description := "anti-xml"

name := "anti-xml"

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.10.0" % "test", //todo: upgrade to 1.11.3 (needed for 2.11)
  "org.specs2" %% "specs2" % "1.12.3" % "test" //todo: upgrade to 2.3.13 (needed for 2.11)
)

libraryDependencies <++= (scalaBinaryVersion) { (sv) => sv match {
    case "2.11" => Seq("org.scala-lang.modules" %% "scala-xml" % "1.0.2")
    case _ => Nil
  }
}
      
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
