import sbt._

object Dependencies {
  val all: Seq[ModuleID] = Seq(
//    "org.scala-lang" % "scala-reflect" % Versions.scala,
    "org.scalaz" %% "scalaz-zio" % Versions.zio,
    "org.typelevel" %% "cats-core" % Versions.catsCore,
    "org.mongodb.scala" %% "mongo-scala-driver" % Versions.mongoDriver,
    "com.typesafe" % "config" % Versions.config,

    "com.typesafe.scala-logging" %% "scala-logging" % Versions.logging,
    "ch.qos.logback" % "logback-classic" % Versions.logback,

    "org.scalatest" %% "scalatest" % Versions.scalaTest % Test,
    "com.lihaoyi" %% "pprint" % Versions.pprint % Test,

    // magical import, adds missing javax.annotation contents needed in com.mongodb.lang.Nullable.java
    "com.google.code.findbugs" % "jsr305" % Versions.jsr305,
  )
}
