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

scalacOptions ++= Seq(
    "-feature",
    "-Xfatal-warnings",
    "-Ypartial-unification",
    "-deprecation",
)

mainClass in Compile := Some("dev.shvimas.garcon.Main")

PB.protocVersion := "-v300"
PB.targets in Compile := Seq(
    scalapb.gen() -> (sourceManaged in Compile).value
)

enablePlugins(DockerPlugin)
dockerfile in docker := {
  val rootJar: File  = sbt.Keys.`package`.in(Compile, packageBin).value
  val appDir: String = "/app/"
  val rootJarString  = s"$appDir/${rootJar.getName}"
  val mainClassName: String =
    mainClass
      .in(Compile, packageBin)
      .value
      .getOrElse(sys.error("Expected exactly one main class"))

  val fullClasspath: Seq[File] = (root / fullClasspathAsJars in Compile).value.files
  val classpathString: String  = fullClasspath.map(appDir + _.getName).mkString(":")
  val classpath: String        = s"$classpathString:$rootJarString"

  new Dockerfile {
    from("openjdk:jre-alpine")
    add(fullClasspath, appDir)
    add(rootJar, rootJarString)
    entryPoint("java", "-cp", classpath, mainClassName)
  }
}
