import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  implementation(project(":binding"))

  api(project(":branchLayout:api"))
  api(project(":backend:api"))
}

addIntellijToCompileClasspath(withGitPlugin = true)
lombok()
slf4jLambdaApi()
vavr()
