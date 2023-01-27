import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  implementation(project(":gitCore:api"))

  api(project(":backend:api"))
  api(project(":branchLayout:api"))

  testImplementation(testFixtures(project(":testCommon")))
  testImplementation(project(":branchLayout:impl"))
  testImplementation(project(":gitCore:jGit"))
}

addIntellijToCompileClasspath(withGit4Idea = false)
commonsIO()
jcabiAspects()
junit()
junitParams()
junitPlatformLauncher()
lombok()
mockito()
slf4jLambdaApi()
vavr()

applySubtypingChecker()

tasks.register<JavaExec>("regenerateCliOutputs") {
  group = "Execution"
  description = "Regenerate CLI outputs used for comparison in tests"
  classpath = sourceSets["test"].runtimeClasspath
  mainClass.set("com.virtuslab.gitmachete.backend.integration.RegenerateCliOutputs")
  args = sourceSets["test"].resources.srcDirs.map { it.absolutePath }.toList()
}
