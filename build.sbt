val zioVersion = "1.0.7"
val zhttpVersion = "1.0.0.0-RC15"

lazy val root = project
  .in(file("."))
  .settings(
    inThisBuild(
      List(
        name := "zio-slackbot",
        organization := "com.dataspark",
        version := "0.0.1",
        scalaVersion := "3.0.0-RC3"
      )
    ),
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "io.d11" %% "zhttp" % zhttpVersion,
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
      "dev.zio" %% "zio-test-junit" % zioVersion % Test,
      "dev.zio" %% "zio-test-magnolia" % zioVersion % Test
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
