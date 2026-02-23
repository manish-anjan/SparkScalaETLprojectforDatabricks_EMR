name := "SparkScalaETLprojectforDatabricks_EMR"
version := "0.1.0-SNAPSHOT"
scalaVersion := "2.12.18"
organization := "com.etl.spark"

resolvers ++= Seq(
  "Central Repository" at "https://repo1.maven.org/maven2",
  "Spark Repository" at "https://repository.apache.org/content/repositories/releases",
  "Databricks" at "https://databricks.com/maven"
)

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.5.0" % "provided",
  "org.apache.spark" %% "spark-sql" % "3.5.0" % "provided",
  "org.apache.spark" %% "spark-hive" % "3.5.0" % "provided",
  "org.apache.hadoop" % "hadoop-aws" % "3.3.6",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.12.565",
  "com.amazonaws" % "aws-java-sdk-glue" % "1.12.565",
  "io.delta" %% "delta-core" % "3.0.0",
  "com.typesafe" % "config" % "1.4.3",
  "org.slf4j" % "slf4j-api" % "2.0.9",
  "org.slf4j" % "slf4j-simple" % "2.0.9",
  "org.scalatest" %% "scalatest" % "3.2.17" % Test
)

scalacOptions ++= Seq(
  "-encoding", "utf-8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:implicitConversions"
)

javacOptions ++= Seq("-source", "11", "-target", "11")

assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "services", xs @ _*) => MergeStrategy.filterDistinctLines
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

assembly / assemblyShadeRules := Seq(
  ShadeRule.rename("com.google.common.**" -> "shaded.google.common.@1").inAll,
  ShadeRule.rename("com.google.protobuf.**" -> "shaded.google.protobuf.@1").inAll
)

assembly / test := {}
assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)
