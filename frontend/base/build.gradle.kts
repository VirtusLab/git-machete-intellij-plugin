import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  api(project(":qual"))
  api(project(":backend:api"))
}

jetbrainsAnnotations()
junit()
lombok()
slf4jLambdaApi()
vavr()
