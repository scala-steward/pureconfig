import Dependencies.Version._

name := "pureconfig-spark"

crossScalaVersions := Seq(scala212) //Spark does not support Scala 2.13 yet

libraryDependencies ++= Seq("org.apache.spark" %% "spark-sql" % "3.2.0" % "provided")
mdocLibraryDependencies ++= Seq("org.apache.spark" %% "spark-sql" % "3.2.0")

osgiSettings

OsgiKeys.exportPackage := Seq("pureconfig.module.spark.*")
OsgiKeys.privatePackage := Seq()
OsgiKeys.importPackage := Seq(
  s"""scala.*;version="[${scalaBinaryVersion.value}.0,${scalaBinaryVersion.value}.50)"""",
  "*"
)
