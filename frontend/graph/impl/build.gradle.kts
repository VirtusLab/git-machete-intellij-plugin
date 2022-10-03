import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  api(project(":frontend:graph:api"))

  implementation(project(":backend:api"))
  implementation(project(":frontend:base"))
}

addIntellijToCompileClasspath(withGit4Idea = false)
lombok()
slf4jLambdaApi()
vavr()

applySubtypingChecker()
