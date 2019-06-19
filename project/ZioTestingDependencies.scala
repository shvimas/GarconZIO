import sbt._

object ZioTestingDependencies {
  val all: Seq[ModuleID] = Seq(
    "org.scalaz" %% "scalaz-zio" % Versions.zio,
    "org.scalatest" %% "scalatest" % Versions.scalaTest,
  )
}
