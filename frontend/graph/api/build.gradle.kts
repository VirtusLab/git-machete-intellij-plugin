import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  api(project(":qual"))
  api(project(":backend:api"))
}

addIntellijToCompileClasspath(withGitPlugin = false)
lombok()
vavr()
