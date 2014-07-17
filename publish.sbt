publishTo <<= (version) apply {
  (v: String) => if (v.trim().endsWith("SNAPSHOT")) Some(Opts.resolver.sonatypeSnapshots) else Some(Opts.resolver.sonatypeStaging)
}

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

homepage := Some(new URL("http://anti-xml.org"))

startYear := Some(2011)

licenses := Seq(("BSD", new URL("https://github.com/arktekk/anti-xml/blob/master/LICENSE.rst")))

pomIncludeRepository := {
  x => false
}

packageOptions <+= (name, version, organization) map { (title, version, vendor) =>
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

pomExtra <<= (pomExtra) { (pom) => pom ++ xml.Group(
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
        <developer>
          <name>Erlend Hamnaberg</name>
          <url>http://twitter.com/hamnis</url>
        </developer>
      </developers>
      <contributors>
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

useGpg := true

aetherPublishSignedSettings
