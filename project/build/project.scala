import sbt._

class Project(info: ProjectInfo) extends DefaultProject(info)
with assembly.AssemblyBuilder with IdeaProject
{
  

}
