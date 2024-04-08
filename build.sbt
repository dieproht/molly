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

ThisBuild / scalaVersion := "3.3.3"
ThisBuild / crossScalaVersions := Seq("2.13.13", "3.3.3")

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
               "-feature",
               "-unchecked",
               "-source:3.3",
               "-release:17",
               "-Wunused:all"
            )
         case _ =>
            Seq(
               "-Xsource:3",
               "-Wconf:msg=Seq in package scala has changed semantics:s," + //
                  "msg=constructor modifiers are assumed:s," + //
                  "msg=access modifiers for `copy` method are copied from the case class constructor:s," + //
                  "msg=access modifiers for `apply` method are copied from the case class constructor:s",
               "-release:17"
            )
      })
)

lazy val molly_core = project
   .in(file("molly-core"))
   .settings(commonSettings: _*)
   .settings(
      name := "molly-core",
      libraryDependencies ++=
         Seq(
            "co.fs2"             %% "fs2-core"                       % "3.9.4",
            "co.fs2"             %% "fs2-reactive-streams"           % "3.9.4",
            "org.mongodb"         % "bson"                           % "5.0.0",
            "org.mongodb"         % "mongodb-driver-core"            % "5.0.0",
            "org.mongodb"         % "mongodb-driver-reactivestreams" % "5.0.0",
            "org.reactivestreams" % "reactive-streams"               % "1.0.4",
            "org.typelevel"      %% "cats-core"                      % "2.10.0",
            "org.typelevel"      %% "cats-effect-kernel"             % "3.5.3",
            //
            "ch.qos.logback"       % "logback-classic"              % "1.5.3"  % Test,
            "com.dimafeng"        %% "testcontainers-scala-mongodb" % "0.41.3" % Test,
            "com.disneystreaming" %% "weaver-cats"                  % "0.8.4"  % Test,
            "org.scalatest"       %% "scalatest"                    % "3.2.18" % Test,
            "org.typelevel"       %% "cats-effect"                  % "3.5.3"  % Test
         )
   )

lazy val molly_medeia = project
   .in(file("molly-medeia"))
   .settings(commonSettings: _*)
   .settings(
      name := "molly-medeia",
      libraryDependencies ++=
         Seq(
            "de.megaera"    %% "medeia"             % "0.12.2",
            "org.mongodb"    % "bson"               % "4.11.2",
            "org.typelevel" %% "cats-core"          % "2.10.0",
            "org.typelevel" %% "cats-effect-kernel" % "3.5.3",
            //
            "ch.qos.logback"       % "logback-classic"              % "1.5.3"  % Test,
            "com.dimafeng"        %% "testcontainers-scala-mongodb" % "0.41.3" % Test,
            "com.disneystreaming" %% "weaver-cats"                  % "0.8.4"  % Test,
            "org.typelevel"       %% "cats-effect"                  % "3.5.3"  % Test
         )
   )
   .dependsOn(molly_core % "compile->compile;test->test")

lazy val molly = project.in(file(".")).settings(publish / skip := true).aggregate(molly_core, molly_medeia)
