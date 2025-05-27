import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  api(project(":qual"))
  api(project(":backend:api"))
  implementation(project(":frontend:resourcebundles"))
}

jetbrainsAnnotations()
junit()
lombok()
slf4jLambdaApi()
vavr()
