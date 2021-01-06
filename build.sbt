//Core deps
val catsV = "2.3.1"
val catsEffectV = "2.3.1"
val refinedV = "0.9.20"
//Test/build deps
val scalaTestV = "3.2.3"
val scalaCheckV = "1.15.2"
val scalaTestScalacheckV = "3.2.3.0"
val betterMonadicForV = "0.3.1"
val kindProjectorV = "0.11.2"
val silencerV = "1.7.1"
val flexmarkV = "0.35.10" // scala-steward:off

val scala213 = "2.13.4"
val scala212 = "2.12.12"

inThisBuild(
  List(
    organization := "com.rewardsnetwork",
    developers := List(
      Developer("sloshy", "Ryan Peters", "me@rpeters.dev", url("https://github.com/sloshy"))
    ),
    homepage := Some(url("https://github.com/rewards-network/combos")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    githubWorkflowJavaVersions := Seq("adopt@1.11"),
    githubWorkflowTargetTags ++= Seq("v*"),
    githubWorkflowPublishTargetBranches := Seq(RefPredicate.StartsWith(Ref.Tag("v"))),
    githubWorkflowPublish := Seq(
      WorkflowStep.Sbt(
        List("ci-release"),
        env = Map(
          "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
          "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
          "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
          "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
        )
      )
    )
  )
)

val commonSettings = Seq(
  scalaVersion := "2.13.4",
  crossScalaVersions := Seq(scala212, scala213),
  organization := "com.rewardsnetwork",
  name := "combos",
  testOptions in Test ++= Seq(
    Tests.Argument(TestFrameworks.ScalaTest, "-o"),
    Tests.Argument(TestFrameworks.ScalaTest, "-h", "target/test/test-reports")
  ),
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % betterMonadicForV),
  addCompilerPlugin("org.typelevel" %% "kind-projector" % kindProjectorV cross CrossVersion.full),
  addCompilerPlugin("com.github.ghik" % "silencer-plugin" % silencerV cross CrossVersion.full),
  scalacOptions += "-P:silencer:pathFilters=.*[/]src_managed[/].*" //Filter compiler warnings from generated source
)

lazy val root = (project in file("."))
  .aggregate(core, refined)
  .settings(
    commonSettings,
    publish / skip := true
  )

lazy val core = (project in file("core"))
  .settings(
    commonSettings,
    name := "combos",
    libraryDependencies ++= Seq(
      //Core deps
      "org.typelevel" %% "cats-core" % catsV,
      //Test deps
      "org.typelevel" %% "cats-effect" % catsEffectV,
      "org.typelevel" %% "cats-effect-laws" % catsEffectV % "test",
      "org.scalatest" %% "scalatest" % scalaTestV % "test",
      "org.scalacheck" %% "scalacheck" % scalaCheckV % "test",
      "org.scalatestplus" %% "scalacheck-1-15" % scalaTestScalacheckV % "test",
      "com.vladsch.flexmark" % "flexmark-all" % flexmarkV % "test"
    )
  )

lazy val refined = (project in file("refined"))
  .settings(
    commonSettings,
    name := "combos-refined",
    libraryDependencies ++= Seq(
      "eu.timepit" %% "refined" % refinedV
    )
  )
  .dependsOn(core % "compile->compile;test->test")
