import sbt._

object ZioTestingDependencies {
  val all: Seq[ModuleID] = Seq(
    "dev.zio" %% "zio" % Versions.zio,
    "org.scalatest" %% "scalatest" % Versions.scalaTest,
  )
}
