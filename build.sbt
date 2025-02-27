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

ThisBuild / scalaVersion := "3.3.4"

ThisBuild / semanticdbEnabled := true

ThisBuild / Test / fork := true

sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

def commonSettings = Seq(
  sonatypeCredentialHost := "s01.oss.sonatype.org",
  sonatypeRepository := "https://s01.oss.sonatype.org/service/local",
  scalacOptions ++= Seq(
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
)

lazy val molly_core = project
  .in(file("molly-core"))
  .settings(commonSettings: _*)
  .settings(
    name := "molly-core",
    libraryDependencies ++=
      Seq(
        "co.fs2"             %% "fs2-core"                       % "3.11.0",
        "co.fs2"             %% "fs2-reactive-streams"           % "3.11.0",
        "org.mongodb"         % "bson"                           % "5.2.1",
        "org.mongodb"         % "mongodb-driver-core"            % "5.2.1",
        "org.mongodb"         % "mongodb-driver-reactivestreams" % "5.2.1",
        "org.mongodb"         % "mongodb-driver-sync"            % "5.2.1",
        "org.reactivestreams" % "reactive-streams"               % "1.0.4",
        "org.typelevel"      %% "cats-core"                      % "2.12.0",
        "org.typelevel"      %% "cats-effect-kernel"             % "3.5.7",
        //
        "ch.qos.logback"       % "logback-classic"              % "1.5.17" % Test,
        "com.dimafeng"        %% "testcontainers-scala-mongodb" % "0.41.5" % Test,
        "com.disneystreaming" %% "weaver-cats"                  % "0.8.4"  % Test,
        "org.scalatest"       %% "scalatest"                    % "3.2.19" % Test,
        "org.typelevel"       %% "cats-effect"                  % "3.5.7"  % Test
      )
  )

lazy val molly_medeia = project
  .in(file("molly-medeia"))
  .settings(commonSettings: _*)
  .settings(
    name := "molly-medeia",
    libraryDependencies ++=
      Seq(
        "de.megaera"    %% "medeia"             % "0.14.0",
        "org.mongodb"    % "bson"               % "4.11.5",
        "org.typelevel" %% "cats-core"          % "2.12.0",
        "org.typelevel" %% "cats-effect-kernel" % "3.5.7",
        //
        "ch.qos.logback"       % "logback-classic"              % "1.5.17" % Test,
        "com.dimafeng"        %% "testcontainers-scala-mongodb" % "0.41.5" % Test,
        "com.disneystreaming" %% "weaver-cats"                  % "0.8.4"  % Test,
        "org.typelevel"       %% "cats-effect"                  % "3.5.7"  % Test
      )
  )
  .dependsOn(molly_core % "compile->compile;test->test")

lazy val molly = project.in(file(".")).settings(publish / skip := true).aggregate(molly_core, molly_medeia)
