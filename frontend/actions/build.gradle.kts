import com.virtuslab.gitmachete.buildsrc.*

dependencies {
  implementation(project(":binding"))
  implementation(project(":backend:api"))
  implementation(project(":branchLayout:api"))
  implementation(project(":frontend:base"))
  implementation(project(":frontend:resourcebundles"))
  implementation(project(":frontend:ui:api"))
}

addIntellijToCompileClasspath(withGit4Idea = true)
apacheCommonsText()
applyKotlinConfig()
apacheCommonsText()
lombok()
slf4jLambdaApi()
vavr()

applyI18nFormatterAndTaintingCheckers()
applySubtypingChecker()
