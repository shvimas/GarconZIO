import sbt._

object TranslatorDependencies {
  val all: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio" % Versions.zio,
    "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % Versions.jsonIter,
    "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % Versions.jsonIter % Provided,
    "com.softwaremill.sttp" %% "core" % Versions.sttpCore,
    "com.typesafe" % "config" % Versions.config,

    "org.scalatest" %% "scalatest" % Versions.scalaTest % Test,
    "dev.zio" %% "zio-test" % Versions.zio % Test,
    "dev.zio" %% "zio-test-sbt" % Versions.zio % Test
  )
}
