import com.virtuslab.gitmachete.buildsrc.BuildUtils
import com.virtuslab.gitmachete.buildsrc.CheckerFrameworkConfigurator

dependencies {
  api(project(":frontend:graph:api"))

  implementation(project(":backend:api"))
  implementation(project(":frontend:base"))
}

BuildUtils.lombok(project)

BuildUtils.slf4jLambdaApi(project)

BuildUtils.vavr(project)

BuildUtils.addIntellijToCompileClasspath(project, withGit4Idea = false)

CheckerFrameworkConfigurator.applySubtypingChecker(project)
