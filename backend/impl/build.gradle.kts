val commonsIO: Unit by extra
val jcabiAspects: Unit by extra
val junit: Unit by extra
val lombok: Unit by extra
val powerMock: Unit by extra
val slf4jLambdaApi: Unit by extra
val slf4jTestImpl: Unit by extra
val vavr: Unit by extra

dependencies {
    implementation(project(":binding"))
    implementation(project(":gitCore:api"))

    api(project(":backend:api"))
    api(project(":branchLayout:api"))

    // Note that we can't easily use Gradle's `testFixtures` configuration here
    // as it doesn't seem to expose testFixtures resources in test classpath correctly.
    testImplementation(project(":testCommon").dependencyProject.sourceSets["test"].output)
    testRuntimeOnly(project(":branchLayout:impl"))
    testRuntimeOnly(project(":gitCore:jGit"))
}

val applySubtypingChecker: Unit by extra

tasks.withType<JavaExec>() {
    group = "Execution"
    description = "Regenerate CLI outputs used for comparison in tests"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.virtuslab.gitmachete.backend.integration.RegenerateCliOutputs")
    args = sourceSets["test"].resources.srcDirs.map{ it.absolutePath }.toList()
}
