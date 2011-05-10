import sbt._

class Project(info: ProjectInfo) extends DefaultProject(info)
with assembly.AssemblyBuilder with IdeaProject
{
  val codaRepo = "Coda Hale's Repository" at "http://repo.codahale.com/"
  val logula = "com.codahale" %% "logula" % "2.1.1" withSources()

  val specs = "org.scala-tools.testing" %% "specs" % "1.6.6" % "test"
}
