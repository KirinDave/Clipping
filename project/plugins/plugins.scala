import sbt._

class Plugins(info: ProjectInfo) extends PluginDefinition(info) {

  val bankSimpleRepo = "BankSimple Repo" at "http://nexus.banksimple.com/content/groups/public"

  val sbtIdeaRepo = "sbt-idea-repo" at "http://mpeltonen.github.com/maven/"
  val sbtIdeaPlugin = "com.github.mpeltonen" % "sbt-idea-plugin" % "0.2.0"

  val assemblySBT = "com.codahale" % "assembly-sbt" % "0.1"

  val lessis = "less is repo" at "http://repo.lessis.me"
  val ghIssues = "me.lessis" % "sbt-gh-issues" % "0.0.1"
}
