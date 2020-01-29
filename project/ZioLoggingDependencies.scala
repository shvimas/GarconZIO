import sbt._

object ZioLoggingDependencies {

  val all: Seq[ModuleID] = Seq(
      "dev.zio"                    %% "zio"            % Versions.zio,
      "com.typesafe.scala-logging" %% "scala-logging"  % Versions.logging,
      "ch.qos.logback"             % "logback-classic" % Versions.logback,
  )
}
