import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  api(project(":qual"))
  api(project(":backend:api"))
}

addIntellijToCompileClasspath(withGit4Idea = true)
jetbrainsAnnotations()
lombok()
vavr()
