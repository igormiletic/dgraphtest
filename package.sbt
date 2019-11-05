import com.typesafe.sbt.packager.MappingsHelper._


// package for publishing: task -> sbt universal:packageZip
mappings in Universal ++= directory("universal")

// a dummy task to hold the result of the universal:packageBin to stop the circular dependency issue
val packageZip = taskKey[File]("package-zip")

// result of "universal:packageBin"
packageZip := (baseDirectory in Compile).value / "target" / "universal" / (name.value + "-" + version.value + ".zip")



// generate version.properties file
resourceGenerators in Compile += Def.task{
  val file = (baseDirectory in Compile).value / "target" / "universal" / "version.properties"
  val contents = "dgraphtest-%s".format(version.value)
  IO.write(file, contents)
  Seq(file)
}


// label the zip artifact as a zip instead of the default jar
artifact in(Universal, packageZip) ~= { (art: Artifact) => art.withType("zip").withExtension("zip") }

// add the artifact so it is included in the publishing tasks
addArtifact(artifact in(Universal, packageZip), packageZip in Universal)



