import com.virtuslab.gitmachete.buildsrc.BuildUtils
import com.virtuslab.gitmachete.buildsrc.CheckerFrameworkConfigurator

dependencies {
  implementation(project(":binding"))
  implementation(project(":backend:api"))
  implementation(project(":branchLayout:api"))
  implementation(project(":frontend:base"))
  implementation(project(":frontend:resourcebundles"))
  implementation(project(":frontend:ui:api"))
}

BuildUtils.applyKotlinConfig(project)
BuildUtils.lombok(project)
BuildUtils.slf4jLambdaApi(project)
BuildUtils.vavr(project)
BuildUtils.addIntellijToCompileClasspath(project, withGit4Idea = true)

CheckerFrameworkConfigurator.applyI18nFormatterAndTaintingCheckers(project)
CheckerFrameworkConfigurator.applySubtypingChecker(project)
