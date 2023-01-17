import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  api(project(":qual"))
  api(project(":branchLayout:api"))
}

junit5()
lombok()
slf4jLambdaApi()
slf4jTestImpl()
vavr()
