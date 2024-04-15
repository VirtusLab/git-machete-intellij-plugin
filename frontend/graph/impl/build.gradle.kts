import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  api(project(":frontend:graph:api"))

  implementation(project(":backend:api"))
  implementation(project(":frontend:base"))
}

lombok()
slf4jLambdaApi()
vavr()

applySubtypingChecker()
