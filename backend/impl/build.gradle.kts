import com.virtuslab.gitmachete.buildsrc.BuildUtils
import com.virtuslab.gitmachete.buildsrc.CheckerFrameworkConfigurator

dependencies {
  implementation(project(":binding"))
  implementation(project(":gitCore:api"))

  api(project(":backend:api"))
  api(project(":branchLayout:api"))

  testImplementation(testFixtures(project(":testCommon")))
  testRuntimeOnly(project(":branchLayout:impl"))
  testRuntimeOnly(project(":gitCore:jGit"))
}

BuildUtils.commonsIO(project)

BuildUtils.jcabiAspects(project)

BuildUtils.junit(project)

BuildUtils.lombok(project)

BuildUtils.powerMock(project)

BuildUtils.slf4jLambdaApi(project)

BuildUtils.slf4jTestImpl(project)

BuildUtils.vavr(project)

CheckerFrameworkConfigurator.applySubtypingChecker(project)

tasks.withType<JavaExec>() {
  group = "Execution"
  description = "Regenerate CLI outputs used for comparison in tests"
  classpath = sourceSets["test"].runtimeClasspath
  mainClass.set("com.virtuslab.gitmachete.backend.integration.RegenerateCliOutputs")
  args = sourceSets["test"].resources.srcDirs.map { it.absolutePath }.toList()
}
