// Your profile name of the sonatype account. The default is the same with the organization value
sonatypeProfileName := "com.rallyhealth"

// publish to Maven Central
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / publishTo := sonatypePublishToBundle.value

// add SNAPSHOT to non-release versions so they are not published to the main repo
ThisBuild / dynverSonatypeSnapshots := true
// Use '-' instead of '+' for simpler snapshot URLs
ThisBuild / dynverSeparator := "-"

import xerial.sbt.Sonatype.GitHubHosting
sonatypeProjectHosting := Some(GitHubHosting("rallyhealth", "weepickle", "roperdj+weepickle@gmail.com"))
