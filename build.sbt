name := "recaptcha-play"

description := "Google reCAPTCHA v2 integration for Play Framework 2.4+"

organization := "com.sandinh"

version := "0.1"

scalaVersion := "2.11.7"

libraryDependencies += ws

// fix sbt resolve warn when import IDEA project:
// Multiple dependencies with the same organization/name but different versions. To avoid conflict, pick one version:
dependencyOverrides ++= Set(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.4"
)
