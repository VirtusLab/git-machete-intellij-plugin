import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  api(project(":gitCore:api"))

  testImplementation(testFixtures(project(":testCommon")))
}

jgit()
junit()
lombok()
slf4jLambdaApi()
slf4jMock()
vavr()

applyAliasingChecker()
