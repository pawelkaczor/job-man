import sbt._

object Deps {

  object Version {
    val Akka     = "2.5.17"
    val AkkaHttp = "10.1.5"

    val Jackson = "2.9.6"

    val Slick              = "3.2.3"
    val PostgresqlSlickExt = "0.16.2" // Slick 3.2.3
    val H2Driver           = "1.4.189"

    // test
    val ScalaTest      = "3.0.1"
    val ScalaCheck     = "1.13.4"
    val RandomDataGen  = "2.2"
    val LogbackClassic = "1.1.7"
    val nScalaTime     = "2.16.0"
  }

  object Akka {
    val actor                = apply("actor")
    val actorTyped           = apply("actor-typed")
    val clusterTools         = apply("cluster-tools")
    val clusterSharding      = apply("cluster-sharding")
    val clusterShardingTyped = apply("cluster-sharding-typed")
    val multiNodeTestkit     = apply("multi-node-testkit")
    val persistence          = apply("persistence")
    val persistenceTyped     = apply("persistence-typed")
    val persistenceQuery     = apply("persistence-query")
    val slf4j                = apply("slf4j")
    val stream               = apply("stream")
    val streamTyped          = apply("stream-typed")
    val testkit              = apply("testkit")

    private def apply(moduleName: String) = "com.typesafe.akka" %% s"akka-$moduleName" % Version.Akka
  }

  object AkkaHttp {
    val httpCore    = apply("http")
    val jackson     = "de.heikoseeberger" %% "akka-http-jackson" % "1.22.0"
    val httpTestKit = apply("http-testkit")

    private def apply(moduleName: String) = "com.typesafe.akka" %% s"akka-$moduleName" % Version.AkkaHttp
  }

  object Jackson {
    val parameterNames = "com.fasterxml.jackson.module"   % "jackson-module-parameter-names" % Version.Jackson
    val datatypeJdk8   = "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8"          % Version.Jackson
    val datatypeJsr310 = "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310"        % Version.Jackson

    def apply() = Seq(parameterNames, datatypeJdk8, datatypeJsr310)

  }

  object SqlDb {
    val `slick-for-pg` = "com.github.tminglei" %% "slick-pg"       % Version.PostgresqlSlickExt exclude ("org.slf4j", "slf4j-simple")
    val connectionPool = "com.typesafe.slick"  %% "slick-hikaricp" % Version.Slick
    val testDriver     = "com.h2database"      % "h2"              % Version.H2Driver % "test"

    def apply() = Seq(`slick-for-pg`, connectionPool, testDriver)
  }

  object TestFrameworks {
    val scalaTest     = "org.scalatest"       %% "scalatest"             % Version.ScalaTest
    val scalaCheck    = "org.scalacheck"      %% "scalacheck"            % Version.ScalaCheck
    val randomDataGen = "com.danielasfregola" %% "random-data-generator" % Version.RandomDataGen
  }

  val levelDB        = Seq("org.iq80.leveldb" % "leveldb" % "0.7", "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8")
  val commonIO       = "commons-io" % "commons-io" % "2.4"
  val logbackClassic = "ch.qos.logback" % "logback-classic" % Version.LogbackClassic
  val slf4j_log4j    = "org.slf4j" % "slf4j-log4j12" % "1.7.7"
  val nscalaTime     = "com.github.nscala-time" %% "nscala-time" % Version.nScalaTime

}
