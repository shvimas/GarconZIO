import sbt._

object TelegramDependencies {
  val all: Seq[ModuleID] = Seq(
    "org.scalaz" %% "scalaz-zio" % Versions.zio,
    "org.json4s" %% "json4s-ext" % Versions.json4s,
    "org.json4s" %% "json4s-native" % Versions.json4s,
    "com.softwaremill.sttp" %% "core" % Versions.sttpCore,
    
    "com.typesafe.scala-logging" %% "scala-logging" % Versions.logging,
    "ch.qos.logback" % "logback-classic" % Versions.logback,
  )
}
