import java.io.File
import com.scalapenos.sbt.prompt.SbtPrompt.autoImport._

addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7")

name := "kafka-backwards"

organization := "backwards"

scalaVersion := "2.12.6"

sbtVersion := "1.2.0"

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  //"-language:_",
  //"-target:jvm-10",
  "-encoding", "UTF-8",
  "-Ypartial-unification",
  "-Ywarn-unused-import"
)

promptTheme := com.scalapenos.sbt.prompt.PromptThemes.ScalapenosTheme

resolvers ++= Seq[Resolver](
  "Typesafe" at "http://repo.typesafe.com/typesafe/releases",
  "Artima Maven Repository" at "http://repo.artima.com/releases",
  Resolver.jcenterRepo,
  Resolver.sbtPluginRepo("releases"),
  Resolver.sonatypeRepo("releases"),
  Classpaths.sbtPluginReleases,
  "Nexus snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  "Nexus releases" at "https://oss.sonatype.org/content/repositories/releases"
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % "test, it, acceptance",
  "org.scalamock" %% "scalamock" % "4.1.0" % "test, it, acceptance",
  "org.mockito" % "mockito-core" % "2.20.1" % "test, it, acceptance",
  "org.http4s" %% "http4s-testing" % "0.18.15" % "test, it, acceptance",
  "com.github.agourlay" %% "cornichon-scalatest" % "0.16.1" % "test, it, acceptance"
) ++ Seq(
  "io.gatling.highcharts" % "gatling-charts-highcharts",
  "io.gatling" % "gatling-test-framework"
).map(_ % "2.3.1" % "test, it, acceptance")

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "com.typesafe" % "config" % "1.3.3",
  "com.github.pureconfig" %% "pureconfig" % "0.9.1",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.25",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "de.siegmar" % "logback-gelf" % "1.1.0",
  "org.apache.commons" % "commons-lang3" % "3.7",
  "com.github.nscala-time" %% "nscala-time" % "2.20.0",
  "com.chuusai" %% "shapeless" % "2.3.3",
  "com.github.pathikrit" %% "better-files" % "3.5.0",
  "com.lightbend" %% "kafka-streams-scala" % "0.2.1",
  "com.sksamuel.avro4s" %% "avro4s-core" % "1.9.0",
  "com.twitter" %% "bijection-avro" % "0.9.6",
) ++ Seq(
  "org.typelevel" %% "cats-core",
  "org.typelevel" %% "cats-macros",
  "org.typelevel" %% "cats-kernel"
).map(_ % "1.2.0") ++ Seq(
  "org.typelevel" %% "cats-effect" % "0.10.1"
) ++ Seq(
  "com.github.julien-truffaut" %% "monocle-core",
  "com.github.julien-truffaut" %% "monocle-macro"
).map(_ % "1.5.0") ++ Seq(
  "org.http4s" %% "http4s-core",
  "org.http4s" %% "http4s-dsl",
  "org.http4s" %% "http4s-blaze-server",
  "org.http4s" %% "http4s-blaze-client",
  "org.http4s" %% "http4s-client",
  "org.http4s" %% "http4s-circe"
).map(_ % "0.18.15") ++ Seq(
  "com.github.pureconfig" %% "pureconfig-http4s" % "0.9.1",
  "org.http4s" %% "jawn-fs2" % "0.12.2",
  "com.github.gvolpe" %% "http4s-tracer" % "0.1",
  "com.github.barambani" %% "http4s-extend" % "0.0.38"
) ++ Seq(
  "io.monix" %% "monix",
  "io.monix" %% "monix-eval",
  "io.monix" %% "monix-execution"
).map(_ % "3.0.0-RC1") ++ Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-jawn",
  "io.circe" %% "circe-optics"
).map(_ % "0.9.3")

assemblyOption in assembly := (assemblyOption in assembly).value.copy(cacheUnzip = false)

assemblyMergeStrategy in assembly := {
  case "application.conf" => MergeStrategy.concat
  case PathList(ps @ _*) if ps.last endsWith ".class" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".properties" => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".jar" => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

unmanagedResourceDirectories in Compile += baseDirectory(_ / "src" / "it" / "resources").value

imageNames in docker := Seq(
  ImageName(s"${organization.value.toLowerCase}/${name.value.toLowerCase}:latest"),

  ImageName(
    namespace = Some(organization.value.toLowerCase),
    repository = name.value.toLowerCase,
    tag = Some(version.value)
  )
)

// To use 'dockerComposeTest' to run tests in the 'IntegrationTest' scope instead of the default 'Test' scope:
// 1) Package the tests that exist in the IntegrationTest scope
testCasesPackageTask := (sbt.Keys.packageBin in IntegrationTest).value

// 2) Specify the path to the IntegrationTest jar produced in Step 1
testCasesJar := artifactPath.in(IntegrationTest, packageBin).value.getAbsolutePath

// 3) Include any IntegrationTest scoped resources on the classpath if they are used in the tests
testDependenciesClasspath := {
  val fullClasspathCompile = (fullClasspath in Compile).value
  val classpathTestManaged = (managedClasspath in IntegrationTest).value
  val classpathTestUnmanaged = (unmanagedClasspath in IntegrationTest).value
  val testResources = (resources in IntegrationTest).value
  (fullClasspathCompile.files ++ classpathTestManaged.files ++ classpathTestUnmanaged.files ++ testResources).map(_.getAbsoluteFile).mkString(File.pathSeparator)
}

testExecutionExtraConfigTask := Map("config.file" -> s"${sourceDirectory.value}/it/resources/application.it.conf",
  "logback.configurationFile" -> s"${sourceDirectory.value}/it/resources/logback-test.xml")

dockerImageCreationTask := docker.value

dockerfile in docker := {
  // The assembly task generates a fat JAR file
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  new Dockerfile {
    from("hseeberger/scala-sbt")
    env("SCALA_VERSION", scalaVersion.value)
    env("SBT_VERSION", sbtVersion.value)
    add(artifact, artifactTargetPath)
    entryPoint("java", "-Xms1024m", "-Xmx2048m", "-jar", artifactTargetPath)
  }
}

buildOptions in docker := BuildOptions(cache = false)

fork in console := true

fork in run := true

fork in Test := true

val IT = config("it") extend IntegrationTest extend Test
fork in IT := true

lazy val itSettings =
  inConfig(IT)(Defaults.testSettings) ++
    Seq(
      javaOptions in IntegrationTest ++= Seq("-Dconfig.resource=application.it.conf", "-Dlogback.configurationFile=./src/it/resources/logback-test.xml"),
      fork in IT := true,
      parallelExecution in IT := false,
      scalaSource in IT := baseDirectory.value / "src/it/scala"
    )

val Acceptance = config("acceptance") extend Test
fork in Acceptance := true

lazy val acceptanceSettings =
  inConfig(Acceptance)(Defaults.testSettings) ++
    Seq(
      javaOptions in Acceptance ++= Seq("-Dconfig.resource=application.acceptance.conf", "-Dlogback.configurationFile=./src/it/resources/logback-test.xml"),
      fork in Acceptance := true,
      parallelExecution in Acceptance := false,
      scalaSource in Acceptance := baseDirectory.value / "src/acceptance/scala"
    )

lazy val root = Project("kafka-backwards", file(".")).enablePlugins(ClassDiagramPlugin, GatlingPlugin, DockerPlugin, DockerComposePlugin)
  .configs(IT, IntegrationTest, GatlingIt, Acceptance)
  .settings(itSettings)
  .settings(acceptanceSettings)
  .settings(javaOptions in Test ++= Seq("-Dconfig.resource=application.test.conf"))