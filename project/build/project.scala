import sbt._
//import com.banksimple.PluginSimple

class Project(info: ProjectInfo) extends DefaultProject(info)
with assembly.AssemblyBuilder with AkkaProject with IdeaProject
//with PluginSimple {
{

  //override val publishTo = BankSimpleRepositories.publishToRepo(version)
// val bsUtil = "com.banksimple" %% "util" % "0.1.1-SNAPSHOT"
  

}
