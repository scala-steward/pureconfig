import Dependencies.Version._

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.3.8",
  "org.scalaz" %% "scalaz-scalacheck-binding" % "7.3.8" % "test"
)

developers := List(
  Developer("ChernikovP", "Pavel Chernikov", "chernikov.pavel92@gmail.com", url("https://github.com/ChernikovP"))
)
