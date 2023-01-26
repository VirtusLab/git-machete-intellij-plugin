import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  api(project(":gitCore:api"))

  testImplementation(testFixtures(project(":testCommon")))
}

jgit()
junit()
mockito()
lombok()
slf4jLambdaApi()
slf4jTestImpl()
vavr()

applyAliasingChecker()
