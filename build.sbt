val scala2Version = "2.13.5"

val zioVersion        = "1.0.7"
val zioConfigVersion  = "1.0.4"
val zioLoggingVersion = "0.5.8"
val sttpVersion       = "3.3.0-RC5"
val circeVersion      = "0.13.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "zio-slackbot",
    organization := "com.dataspark",
    version := "0.0.1",
    scalaVersion := scala2Version,
    addCompilerPlugin("org.typelevel" % "kind-projector" % "0.11.3" cross CrossVersion.full),
    scalacOptions ++= Seq(
      "-encoding",
      "utf8", // Option and arguments on same line
      // "-Xfatal-warnings", // New lines for each options
      "-deprecation",
      "-unchecked",
      "-language:implicitConversions",
      "-language:higherKinds",
      "-language:existentials",
      "-language:postfixOps"
    ),
    libraryDependencies ++= Seq(
      "dev.zio"                       %% "zio"                    % zioVersion,
      "dev.zio"                       %% "zio-config"             % zioConfigVersion,
      "dev.zio"                       %% "zio-config-magnolia"    % zioConfigVersion,
      "dev.zio"                       %% "zio-logging"            % zioLoggingVersion,
      "io.github.kitlangton"          %% "zio-magic"              % "0.2.3",
      "com.softwaremill.sttp.client3" %% "core"                   % sttpVersion,
      "com.softwaremill.sttp.client3" %% "circe"                  % sttpVersion,
      "com.softwaremill.sttp.client3" %% "httpclient-backend-zio" % sttpVersion,
      "io.circe"                      %% "circe-generic"          % circeVersion,
      "io.circe"                      %% "circe-generic-extras"   % circeVersion,
      "dev.zio"                       %% "zio-test"               % zioVersion % Test,
      "dev.zio"                       %% "zio-test-sbt"           % zioVersion % Test,
      "dev.zio"                       %% "zio-test-junit"         % zioVersion % Test,
      "dev.zio"                       %% "zio-test-magnolia"      % zioVersion % Test
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
