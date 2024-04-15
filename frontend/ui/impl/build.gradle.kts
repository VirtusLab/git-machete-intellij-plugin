import com.virtuslab.gitmachete.buildsrc.*

plugins {
  // With org.jetbrains.intellij.platform.base, JUnit won't see IntelliJ classes for some reason
  id("org.jetbrains.intellij.platform.module")
}

dependencies {
  implementation(project(":branchLayout:api"))
  implementation(project(":backend:api"))
  implementation(project(":frontend:base"))
  implementation(project(":frontend:file"))
  implementation(project(":frontend:graph:api"))
  implementation(project(":frontend:resourcebundles"))
  implementation(project(":frontend:ui:api"))
}

// To prevent race condition on <project-root>/.intellijPlatform/coroutines-javaagent.jar
tasks.withType<Test> {
  dependsOn(":initializeIntellijPlatformPlugin")
}

apacheCommonsText()
junit()
junitPlatformLauncher()
lombok()
slf4jLambdaApi()
vavr()

applyI18nFormatterAndTaintingCheckers()
applySubtypingChecker()
