import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  api(project(":qual"))
  api(project(":branchLayout:api"))
}

commonsIO()
lombok()
vavr()
slf4jLambdaApi()
