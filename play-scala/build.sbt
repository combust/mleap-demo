name := "MLeap with Play"

version := "1.0"

scalaVersion := "2.11.7"

logLevel := Level.Error


//unmanagedJars in Compile += file("mleap-core_2.11-0.6.0-SNAPSHOT.jar")
//unmanagedJars in Compile += file("mleap-runtime_2.11-0.6.0-SNAPSHOT.jar")
//unmanagedJars in Compile += file("mleap-spark_2.11-0.6.0-SNAPSHOT.jar")
//unmanagedJars in Compile += file("mleap-spark-extension_2.11-0.6.0-SNAPSHOT.jar")
//unmanagedJars in Compile += file("mleap-base_2.11-0.6.0-SNAPSHOT.jar")
//unmanagedJars in Compile += file("mleap-tensor_2.11-0.6.0-SNAPSHOT.jar")
//unmanagedJars in Compile += file("bundle-ml_2.11-0.6.0-SNAPSHOT.jar")
//unmanagedJars in Compile += file("mleap-spark-base_2.11-0.6.0-SNAPSHOT.jar")

lazy val root = (project in file(".")).enablePlugins(PlayScala)


//Since SBT fails to detect deafult "src" with Pay plugin
unmanagedSourceDirectories in Compile += baseDirectory.value / "src" / "main" / "java"
unmanagedSourceDirectories in Compile += baseDirectory.value / "src" / "main" / "scala"
unmanagedResourceDirectories in Compile  += baseDirectory.value / "src" / "main"  / "resources"

resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies ++= Seq(

  //Include below packages if Spark is not part of your project
  "io.spray" %% "spray-json" % "1.3.2",
  "com.jsuereth" %% "scala-arm" % "2.0-RC1",
  "org.scalanlp" %% "breeze" % "0.13",
  "commons-io" % "commons-io" % "2.4",


  "com.trueaccord.scalapb" % "scalapb-runtime_2.11" % "0.5.47" ,//exclude("com.google.protobuf", "protobuf-java"),


  "org.apache.spark" % "spark-core_2.11" % "2.1.0",
  "org.apache.spark" % "spark-sql_2.11" % "2.1.0",
  "org.apache.spark" % "spark-mllib_2.11" % "2.1.0" withSources(),


  "ml.combust.mleap" %% "mleap-spark" % "0.5.0",
  "ml.combust.mleap" %% "mleap-core" % "0.5.0",
  "ml.combust.mleap" %% "mleap-runtime" % "0.5.0",

  //Log
  //--------------------------------
  "log4j" % "log4j" % "1.2.16",

  //Web Framework
  //--------------------------------
  jdbc,
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,

  "org.webjars" 			%% 	"webjars-play" 		% "2.4.0-2",
  "org.webjars" 			%	  "bootstrap" 		% "3.1.1-2",
  "org.webjars" 			% 	"bootswatch-darkly" % "3.3.1+2",
  "org.webjars" 			% 	"html5shiv" 		% "3.7.0",
  "org.webjars" 			% 	"respond" 			% "1.4.2",

  "org.freemarker" % "freemarker" % "2.3.23"
)

//Some tweaks for Spark + Play
//Jackson 2.7.8 not working  due Spark + Play
dependencyOverrides ++= Set("com.fasterxml.jackson.core" % "jackson-databind" % "2.6.1")


libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api"       % "1.7.7",
  "org.slf4j" % "jcl-over-slf4j"  % "1.7.7"
).map(_.force())

libraryDependencies ~= { _.map(_.exclude("org.slf4j", "slf4j-jdk14")) }

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

//For fast compiling of Play framework
JsEngineKeys.engineType := JsEngineKeys.EngineType.Node
