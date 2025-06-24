import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  implementation(project(":backend:api"))
  implementation(project(":branchLayout:api"))
  implementation(project(":frontend:base"))
  implementation(project(":frontend:file"))
  implementation(project(":frontend:resourcebundles"))
  implementation(project(":frontend:ui:api"))
}

apacheCommonsText()
applyKotlinConfig()
junit()
lombok()
mockito()
slf4jLambdaApi()
vavr()

applyI18nFormatterAndTaintingCheckers()
applySubtypingChecker()
