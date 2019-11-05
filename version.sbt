git.baseVersion := "0.1.0"

git.useGitDescribe := false

git.formattedShaVersion := {
  val base = git.baseVersion.?.value
  val suffix = "-SNAPSHOT"
  git.gitHeadCommit.value map { sha =>
    git.defaultFormatShaVersion(base, sha, suffix)
  }
}

val VersionRegex = "v([0-9]+.[0-9]+.[0-9]+)-?(.*)?".r
git.gitTagToVersionNumber := {
  case VersionRegex(v, "") => Some(v)
  case VersionRegex(v, "SNAPSHOT") => Some(s"$v-SNAPSHOT")
  case VersionRegex(v, s) => Some(s"$v-$s-SNAPSHOT")
  case _ => None
}


