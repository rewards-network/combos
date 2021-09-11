//Core deps
val catsV = "2.6.1"
val catsEffectV = "3.2.8"
val refinedV = "0.9.27"
//Test/build deps
val munitV = "0.7.29"
val munitCatsEffectV = "1.0.5"
val scalaCheckV = "1.15.4"
val scalaCheckEffectV = "1.0.2"
val betterMonadicForV = "0.3.1"
val kindProjectorV = "0.11.2"

val catsCore = "org.typelevel" %% "cats-core" % catsV
val refinedCore = "eu.timepit" %% "refined" % refinedV

val munitCatsEffect = "org.typelevel" %% "munit-cats-effect-3" % munitCatsEffectV % "test"
val munitScalacheck = "org.scalameta" %% "munit-scalacheck" % munitV
val scalaCheck = "org.scalacheck" %% "scalacheck" % scalaCheckV % "test"
val scalaCheckEffect = "org.typelevel" %% "scalacheck-effect-munit" % scalaCheckEffectV % "test"

val scala213 = "2.13.4"
val scala212 = "2.12.12"
val scala3 = "3.0.2"

inThisBuild(
  List(
    developers := List(
      Developer("sloshy", "Ryan Peters", "me@rpeters.dev", url("https://github.com/sloshy"))
    ),
    homepage := Some(url("https://github.com/rewards-network/combos")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    githubWorkflowJavaVersions := Seq("adopt@1.8"),
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
  scalaVersion := scala213,
  crossScalaVersions := Seq(scala212, scala213, scala3),
  organization := "com.rewardsnetwork",
  name := "combos",
  libraryDependencies ++= {
    if (scalaVersion.value.startsWith("2")) {
      Seq(
        compilerPlugin("com.olegpy" %% "better-monadic-for" % betterMonadicForV),
        compilerPlugin("org.typelevel" %% "kind-projector" % kindProjectorV cross CrossVersion.full)
      )
    } else {
      Seq.empty
    }
  }
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
      catsCore,
      //Test deps
      munitCatsEffect,
      munitScalacheck,
      scalaCheck,
      scalaCheckEffect
    )
  )

lazy val refined = (project in file("refined"))
  .settings(
    commonSettings,
    name := "combos-refined",
    libraryDependencies ++= Seq(
      refinedCore
    )
  )
  .dependsOn(core % "compile->compile;test->test")
