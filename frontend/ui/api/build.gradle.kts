import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  implementation(project(":binding"))

  api(project(":branchLayout:api"))
  api(project(":backend:api"))
}

addIntellijToCompileClasspath(withGit4Idea = true)
lombok()
slf4jLambdaApi()
vavr()
