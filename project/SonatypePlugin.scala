import sbt.Keys.publishTo
import sbt.{AutoPlugin, Def, Plugins}
import sbtdynver.DynVerPlugin.autoImport.{dynverSeparator, dynverSonatypeSnapshots}
import xerial.sbt.Sonatype
import xerial.sbt.Sonatype.GitHubHosting
import xerial.sbt.Sonatype.autoImport.{sonatypeCredentialHost, sonatypeProfileName, sonatypeProjectHosting, sonatypePublishToBundle}

/**
  * Publishing requires setting up auth, e.g.:
  * - ~/.sbt/1.0/sonatype.sbt
  * - ~/.sbt/sonatype_credentials
  *
  * @see https://github.com/xerial/sbt-sonatype#homesbtsbt-version-013-or-10sonatypesbt
  */
object SonatypePlugin extends AutoPlugin {

  override def trigger = allRequirements

  override def requires: Plugins = Sonatype

  override def buildSettings: Seq[Def.Setting[_]] = Seq(
    // Use '-' instead of '+' for simpler snapshot URLs
    dynverSeparator := "-",
    // add SNAPSHOT to non-release versions so they are not published to the main repo
    dynverSonatypeSnapshots := true,
  )

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    publishTo := sonatypePublishToBundle.value,
    sonatypeProfileName := "com.rallyhealth",
    sonatypeProjectHosting := Some(GitHubHosting("rallyhealth", "weepickle", "roperdj+weepickle@gmail.com")),
  )
}
