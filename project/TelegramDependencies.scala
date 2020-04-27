import sbt._

object TelegramDependencies {
  val all: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio" % Versions.zio,
    "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % Versions.jsonIter,
    "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % Versions.jsonIter % Provided,
    "com.softwaremill.sttp" %% "core" % Versions.sttpCore,
    
    "com.typesafe.scala-logging" %% "scala-logging" % Versions.logging,
    "ch.qos.logback" % "logback-classic" % Versions.logback,
  )
}
