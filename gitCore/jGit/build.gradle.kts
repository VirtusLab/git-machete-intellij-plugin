import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  api(project(":gitCore:api"))

  testImplementation(project(":backend:impl"))
  testImplementation(project(":branchLayout:impl"))
  testImplementation(testFixtures(project(":testCommon")))
}

addIntellijToCompileClasspath(withGit4Idea = true)
jgit()
junit()
mockito()
lombok()
slf4jLambdaApi()
slf4jTestImpl()
vavr()

applyAliasingChecker()
