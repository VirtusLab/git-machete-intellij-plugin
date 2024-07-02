import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  implementation(project(":branchLayout:api"))
  implementation(project(":backend:api"))
  implementation(project(":frontend:base"))
  implementation(project(":frontend:file"))
  implementation(project(":frontend:graph:api"))
  implementation(project(":frontend:resourcebundles"))
  implementation(project(":frontend:ui:api"))
}

// TODO (JetBrains/intellij-platform-gradle-plugin#1675): workaround to prevent race condition on .../.intellijPlatform/coroutines-javaagent.jar
tasks.withType<Test> {
  dependsOn(":initializeIntellijPlatformPlugin")
  dependsOn(":frontend:actions:initializeIntellijPlatformPlugin")
  dependsOn(":frontend:graph:initializeIntellijPlatformPlugin")
  dependsOn(":frontend:graph:impl:initializeIntellijPlatformPlugin")
  dependsOn(":frontend:ui:initializeIntellijPlatformPlugin")
  dependsOn(":frontend:ui:impl:initializeIntellijPlatformPlugin")
}

apacheCommonsText()
junit()
junitPlatformLauncher()
lombok()
slf4jLambdaApi()
vavr()

applyI18nFormatterAndTaintingCheckers()
applySubtypingChecker()
