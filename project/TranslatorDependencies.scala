import sbt._

object TranslatorDependencies {
  val all: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio" % Versions.zio,
    "org.typelevel" %% "cats-core" % Versions.catsCore,
    "org.json4s" %% "json4s-native" % Versions.json4s,
    "org.json4s" %% "json4s-ext" % Versions.json4s,
    "com.softwaremill.sttp" %% "core" % Versions.sttpCore,
    "com.typesafe" % "config" % Versions.config,

    "org.scalatest" %% "scalatest" % Versions.scalaTest % Test,
    "com.lihaoyi" %% "pprint" % Versions.pprint % Test,
  )
}
