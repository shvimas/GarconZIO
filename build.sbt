name := "GarconZIO"
version := "0.1-SNAPSHOT"
scalaVersion := "2.12.8"

lazy val zioTestFramework = new TestFramework("zio.test.sbt.ZTestFramework")

lazy val translator = project in file("translator")
translator / libraryDependencies ++= TranslatorDependencies.all
translator / testFrameworks += zioTestFramework

lazy val telegram = project in file("telegram")
telegram / libraryDependencies ++= TelegramDependencies.all
telegram / testFrameworks += zioTestFramework

lazy val root = (project in file("."))
  .dependsOn(translator, telegram)
  .aggregate(translator, telegram)
root / libraryDependencies ++= Dependencies.all
root / testFrameworks += zioTestFramework
root / mainClass in Compile := Some("dev.shvimas.garcon.Main")
root / PB.protocVersion := "-v300"
root / PB.targets in Compile := Seq(scalapb.gen() -> (root / sourceManaged in Compile).value)
root / scalacOptions ++= Seq(
    "-feature",
    "-Xfatal-warnings",
    "-Ypartial-unification",
    "-deprecation",
)

enablePlugins(DockerPlugin)
dockerfile in (root / docker) := {
  val rootJar: File  = sbt.Keys.`package`.in(Compile, packageBin).value
  val appDir: String = "/app/"
  val rootJarDestination  = s"$appDir/${rootJar.getName}"
  val mainClassName: String =
    (root / mainClass)
      .in(Compile, packageBin)
      .value
      .getOrElse(sys.error("Expected exactly one main class"))

  val fullClasspath: Seq[File] = (root / fullClasspathAsJars in Compile).value.files
  val classpathString: String  = fullClasspath.map(appDir + _.getName).mkString(":")
  val classpath: String        = s"$classpathString:$rootJarDestination"

  new Dockerfile {
    from("openjdk:jre-alpine")
    add(fullClasspath, appDir)
    add(rootJar, rootJarDestination)
    entryPoint("java", "-cp", classpath, mainClassName)
  }
}
