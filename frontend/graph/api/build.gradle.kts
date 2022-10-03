import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  api(project(":qual"))
  api(project(":backend:api"))
}

addIntellijToCompileClasspath(withGit4Idea = false)
lombok()
vavr()
