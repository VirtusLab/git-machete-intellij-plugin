import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  implementation(project(":binding"))
  implementation(project(":gitCore:api"))

  api(project(":backend:api"))
  api(project(":branchLayout:api"))

  testImplementation(testFixtures(project(":testCommon")))
  testRuntimeOnly(project(":branchLayout:impl"))
  testRuntimeOnly(project(":gitCore:jGit"))
}

commonsIO()
jcabiAspects()
junit()
lombok()
powerMock()
slf4jLambdaApi()
slf4jTestImpl()
vavr()

applySubtypingChecker()

tasks.register<JavaExec>("regenerateCliOutputs") {
  group = "Execution"
  description = "Regenerate CLI outputs used for comparison in tests"
  classpath = sourceSets["test"].runtimeClasspath
  mainClass.set("com.virtuslab.gitmachete.backend.integration.RegenerateCliOutputs")
  args = sourceSets["test"].resources.srcDirs.map { it.absolutePath }.toList()
}
