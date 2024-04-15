import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  api(project(":branchLayout:api"))
  api(project(":backend:api"))
}

lombok()
slf4jLambdaApi()
vavr()
