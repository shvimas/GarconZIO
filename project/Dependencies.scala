import sbt._

object Dependencies {
  val all: Seq[ModuleID] = Seq(
    "org.scala-lang" % "scala-reflect" % Versions.scala,
    "dev.zio" %% "zio" % Versions.zio,
    "dev.zio" %% "zio-macros" % Versions.zio,
    "org.mongodb.scala" %% "mongo-scala-driver" % Versions.mongoDriver,
    "com.typesafe" % "config" % Versions.config,

//    "com.typesafe.scala-logging" %% "scala-logging" % Versions.logging,
//    "ch.qos.logback" % "logback-classic" % Versions.logback,

    "org.scalatest" %% "scalatest" % Versions.scalaTest % Test,
    "dev.zio" %% "zio-test" % Versions.zio % Test,
    "dev.zio" %% "zio-test-sbt" % Versions.zio % Test,

    // magical import, adds missing javax.annotation contents needed in com.mongodb.lang.Nullable.java
    "com.google.code.findbugs" % "jsr305" % Versions.jsr305
  )
}
