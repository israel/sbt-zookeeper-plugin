

lazy val commonSettings = Seq(
  shellPrompt := { s => Project.extract(s).currentProject.id + " > " },
  version := "0.1",
  organization := "com.github.israel"
)

val resourcesProjectName = "sbt-zookeeper-plugin-resources"

lazy val root = (project in file(".")).
  settings(commonSettings : _*).
  settings(
    name := "sbt-zookeeper-plugin",
    sbtPlugin := true,
    sourceGenerators in Compile += task[Seq[File]] {
      val file =  (sourceManaged in Compile).value / "com" / "github" / "israel" / "sbt" / "zookeeper" / "BuildUtils.scala"
      IO.write(file,
        s"""
           |package com.github.israel.sbt.zookeeper
           |
        |object ZookeeperPluginMeta {
           |  val pluginVersion = "${version.value}"
           |  val pluginSbtVersion = "${sbtVersion.value}"
           |  val pluginArtifactId = "${name.value}"
           |  val resourcesArtifactId = "${resourcesProjectName}_${scalaBinaryVersion.value}"
           |  val pluginGroupId = "${organization.value}"
           |}
      """.stripMargin)
      Seq(file)
    },
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
      else                  Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },

    publishMavenStyle := true,

    publishArtifact in Test := false,
    publishArtifact in (Compile, packageSrc) :=  true,

    pomIncludeRepository := { x => false },

    homepage := Some(url("http://github.com/israel/sbt-kafka-plugin")),

    licenses := Seq("The MIT License (MIT)" -> url("http://opensource.org/licenses/MIT")),

    pomExtra := (
      <scm>
        <connection>scm:git:git@github.com:israel/sbt-zookeeper-plugin.git</connection>
        <developerConnection>scm:git:git@github.com:israel/sbt-zookeeper-plugin.git</developerConnection>
        <url>https://github.com/israel/sbt-zookeeper-plugin</url>
      </scm>
        <developers>
          <developer>
            <name>Israel Klein</name>
            <email>israel.klein@gmail.com</email>
          </developer>
        </developers>
      )
  ).aggregate(resources)

lazy val resources = (project in file("./resources")).
  settings(commonSettings : _*).
  settings(
    name := resourcesProjectName,
    // disable publishing the main API jar
    publishArtifact in (Compile, packageDoc) := false,
    // disable publishing the main sources jar
    publishArtifact in (Compile, packageSrc) := false
  )