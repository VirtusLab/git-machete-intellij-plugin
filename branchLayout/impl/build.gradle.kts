import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  api(project(":qual"))
  api(project(":branchLayout:api"))
}

junit()
lombok()
powerMock()
slf4jLambdaApi()
slf4jTestImpl()
vavr()
