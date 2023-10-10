ThisBuild / organization := "io.github.dieproht"
ThisBuild / homepage := Some(url("https://github.com/dieproht/molly"))
ThisBuild / licenses +=
   ("Apache-2.0", url("http://opensource.org/licenses/Apache-2.0"))
ThisBuild / developers :=
   List(
      Developer(
         "dieproht",
         "Karl F Walkow",
         "opensource@walkow.de",
         url("https://github.com/dieproht")
      )
   )

ThisBuild / scalaVersion := "3.3.1"
ThisBuild / crossScalaVersions := Seq("2.13.11", "3.3.1")

ThisBuild / semanticdbEnabled := true

ThisBuild / Test / fork := true

sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

def commonSettings = Seq(
   sonatypeCredentialHost := "s01.oss.sonatype.org",
   sonatypeRepository := "https://s01.oss.sonatype.org/service/local",
   scalacOptions ++=
      (CrossVersion.partialVersion(scalaVersion.value) match {
         case Some((3, _)) =>
            Seq(
               "-encoding",
               "UTF-8",
               "-explain",
               "-explain-types",
               "-deprecation",
               "-unchecked",
               "-source:3.3",
               "-release:17",
               "-Wunused:all"
            )
         case _ =>
            Seq("-Xsource:3", "-release:17")
      })
)

lazy val molly_core = project
   .in(file("molly-core"))
   .settings(commonSettings: _*)
   .settings(
      name := "molly-core",
      libraryDependencies ++=
         Seq(
            "co.fs2"             %% "fs2-core"                       % "3.9.2",
            "co.fs2"             %% "fs2-reactive-streams"           % "3.9.2",
            "org.mongodb"         % "bson"                           % "4.10.2",
            "org.mongodb"         % "mongodb-driver-core"            % "4.10.2",
            "org.mongodb"         % "mongodb-driver-reactivestreams" % "4.10.2",
            "org.reactivestreams" % "reactive-streams"               % "1.0.4",
            "org.typelevel"      %% "cats-core"                      % "2.10.0",
            "org.typelevel"      %% "cats-effect-kernel"             % "3.5.2",
            //
            "com.dimafeng"        %% "testcontainers-scala-mongodb" % "0.41.0" % Test,
            "com.disneystreaming" %% "weaver-cats"                  % "0.8.3"  % Test,
            "org.slf4j"            % "slf4j-simple"                 % "2.0.9"  % Test,
            "org.typelevel"       %% "cats-effect"                  % "3.5.2"  % Test
         ),
      testFrameworks += new TestFramework("weaver.framework.CatsEffect")
   )

lazy val molly = project.in(file(".")).settings(publish / skip := true).aggregate(molly_core)
