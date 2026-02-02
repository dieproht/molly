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

ThisBuild / scalaVersion := "3.3.7"

ThisBuild / semanticdbEnabled := true

ThisBuild / Test / fork := true

def commonSettings = Seq(
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
            "co.fs2"             %% "fs2-core"                       % "3.12.2",
            "co.fs2"             %% "fs2-reactive-streams"           % "3.12.2",
            "org.mongodb"         % "bson"                           % "5.6.2",
            "org.mongodb"         % "mongodb-driver-core"            % "5.6.2",
            "org.mongodb"         % "mongodb-driver-reactivestreams" % "5.6.2",
            "org.mongodb"         % "mongodb-driver-sync"            % "5.6.2",
            "org.reactivestreams" % "reactive-streams"               % "1.0.4",
            "org.typelevel"      %% "cats-core"                      % "2.13.0",
            "org.typelevel"      %% "cats-effect-kernel"             % "3.6.3",
            //
            "ch.qos.logback" % "logback-classic"              % "1.5.26" % Test,
            "com.dimafeng"  %% "testcontainers-scala-mongodb" % "0.44.1" % Test,
            "org.typelevel" %% "weaver-cats"                  % "0.11.3" % Test,
            "org.scalatest" %% "scalatest"                    % "3.2.19" % Test,
            "org.typelevel" %% "cats-effect"                  % "3.6.3"  % Test
          )
    )

lazy val molly_medeia = project
    .in(file("molly-medeia"))
    .settings(commonSettings: _*)
    .settings(
      name := "molly-medeia",
      libraryDependencies ++=
          Seq(
            "de.megaera"    %% "medeia"             % "1.0.6",
            "org.mongodb"    % "bson"               % "5.6.2",
            "org.typelevel" %% "cats-core"          % "2.13.0",
            "org.typelevel" %% "cats-effect-kernel" % "3.6.3",
            //
            "ch.qos.logback" % "logback-classic"              % "1.5.26" % Test,
            "com.dimafeng"  %% "testcontainers-scala-mongodb" % "0.44.1" % Test,
            "org.typelevel" %% "weaver-cats"                  % "0.11.3" % Test,
            "org.typelevel" %% "cats-effect"                  % "3.6.3"  % Test
          )
    )
    .dependsOn(molly_core % "compile->compile;test->test")

lazy val molly = project.in(file(".")).settings(publish / skip := true).aggregate(molly_core, molly_medeia)
