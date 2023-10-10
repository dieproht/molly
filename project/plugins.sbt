resolvers += Resolver.sonatypeRepo("snapshots")

addSbtPlugin("ch.epfl.scala"    % "sbt-scalafix"              % "0.11.1")
addSbtPlugin("com.github.cb372" % "sbt-explicit-dependencies" % "0.3.1")
addSbtPlugin("com.github.sbt"   % "sbt-ci-release"            % "1.5.12")
addSbtPlugin("org.scalameta"    % "sbt-scalafmt"              % "2.5.2")
