organization  := "org.purang.spray.example"

version       := "0.1"

scalaVersion  := "2.10.2"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers ++= Seq(
  "Mandubian repository snapshots" at "https://github.com/mandubian/mandubian-mvn/raw/master/snapshots/",
  "Mandubian repository releases" at "https://github.com/mandubian/mandubian-mvn/raw/master/releases/",
  "spray repo" at "http://repo.spray.io/",
    "spray nightly repo" at "http://nightlies.spray.io/"
)

libraryDependencies ++= Seq(
  "io.spray"            %   "spray-can"     % "1.2-M8",
  "io.spray"            %   "spray-routing" % "1.2-M8",
  "io.spray"            %   "spray-testkit" % "1.2-M8",
  "io.spray"            %   "spray-caching" % "1.2-M8",
  "com.typesafe.akka" %%  "akka-actor" % "2.2.0-RC1",
  "com.typesafe.akka" %%  "akka-slf4j" % "2.2.0-RC1",
  "ch.qos.logback"% "logback-classic" % "1.0.12" % "runtime",
  "play"        %% "play-json" % "2.2-SNAPSHOT",
  "net.liftweb" %% "lift-json" % "2.5-RC5",
  "org.specs2"          %%  "specs2"        % "1.14" % "test"
)

seq(Revolver.settings: _*)