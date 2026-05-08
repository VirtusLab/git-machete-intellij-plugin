import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  implementation(project(":branchLayout:api"))
  implementation(project(":backend:api"))
  implementation(project(":frontend:base"))
  implementation(project(":frontend:file"))
  implementation(project(":frontend:graph:api"))
  implementation(project(":frontend:ui:api"))
}

apacheCommonsText()
junit()
lombok()
mockito()
slf4jLambdaApi()
vavr()

applyI18nFormatterAndTaintingCheckers()
applySubtypingChecker()
