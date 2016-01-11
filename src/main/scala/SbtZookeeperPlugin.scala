package com.github.israel.sbt.zookeeper

import sbt._
import sbt.Keys._
import com.github.israel.sbt.zookeeper.{ZookeeperPluginMeta => ZPM}

/**
  * Created by israel on 28/12/2015.
  */
object SbtZookeeperPlugin extends sbt.AutoPlugin{

  object autoImport {
    lazy val helloTask = taskKey[Unit]("prints hello world")

    /** Settings **/
    lazy val zookeeperVersion = settingKey[String]("version of zookeeper")
    lazy val zookeeperServerConfig = settingKey[File]("zookeeper server configuration file")
    lazy val zookeeperServerRunDir = settingKey[File]("Run zookeeper server process from this directory. ")
    lazy val stopAfterTests = settingKey[Boolean]("Stop zookeeper server after tests finish")
    lazy val startBeforeTests = settingKey[Boolean]("Auto start zookeeper server before tests start")
    lazy val cleanAfterTests = settingKey[Boolean]("Clean data after tests finish")
    lazy val cleanBeforeTests = settingKey[Boolean]("Clean data before test starts")



    /** Tasks **/
    lazy val startZookeeper = taskKey[Unit]("start the zookeeper server")
    lazy val stopZookeeper = taskKey[Unit]("stop the zookeeper server")
    lazy val cleanZookeeper = taskKey[Unit]("clean zookeeper run dir")

  }

  import autoImport._

  var zookeeperProcess:java.lang.Process = _

  override def projectSettings = Seq(

    libraryDependencies <++= (zookeeperVersion) { zv =>
      Seq("org.apache.zookeeper" % "zookeeper" % zv,
        ZPM.pluginGroupId % ZPM.resourcesArtifactId % ZPM.pluginVersion
        )
    },

    /** Settings **/
    zookeeperVersion := "3.4.7",
    zookeeperServerConfig := (resourceDirectory in Runtime).value / "zookeeper.server.cfg",
    zookeeperServerRunDir := {
      val f = target.value / "zookeeper-server"
      f.mkdir()
      f
    },
    stopAfterTests := true,
    startBeforeTests := true,
    cleanAfterTests := false,
    cleanBeforeTests := true,

    /** Tasks **/
    startZookeeper := {
      val baseDir = zookeeperServerRunDir.value
      if(!baseDir.isDirectory)
        baseDir.mkdir()
      val depClasspath = (dependencyClasspath in Runtime).value
      val classpath = Attributed.data(depClasspath)
      val serverConfigFile = zookeeperServerConfig.value
      if(!serverConfigFile.exists()){
        val resourcesJar = classpath.find{_.getName.startsWith("sbt-zookeeper-plugin")}
        IO.withTemporaryDirectory{ tmpDir =>
          IO.unzip(resourcesJar.get, tmpDir)
          IO.copyFile(tmpDir / "zookeeper.server.cfg", serverConfigFile)
        }
      }

      val configFile = serverConfigFile.absolutePath
      val cp = classpath.map{_.getAbsolutePath}.mkString(":")
      val javaExec = System.getProperty("java.home") + "/bin/java"
      val mainClass = "org.apache.zookeeper.server.quorum.QuorumPeerMain"
      val pb = new java.lang.ProcessBuilder( javaExec, "-classpath", cp, mainClass, configFile)
      pb.directory(baseDir)
      zookeeperProcess = pb.start()
    },

    stopZookeeper := {
      zookeeperProcess.destroy()
      var triesLeft = 20
      while(zookeeperProcess.isAlive && triesLeft > 0) {
        Thread.sleep(500)
        triesLeft -= 1
      }
    },

    cleanZookeeper := {
      val dir = zookeeperServerRunDir.value
      IO.delete(dir)
    }
  )

}
