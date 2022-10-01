import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  api(project(":qual"))
  api(project(":backend:api"))
}

addIntellijToCompileClasspath(withGitPlugin = true)
jetbrainsAnnotations()
vavr()
