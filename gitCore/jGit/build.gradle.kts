import com.virtuslab.gitmachete.buildsrc.BuildUtils
import com.virtuslab.gitmachete.buildsrc.CheckerFrameworkConfigurator

dependencies {
    api(project(":gitCore:api"))
}

BuildUtils.jgit(project)

BuildUtils.lombok(project)

BuildUtils.slf4jLambdaApi(project)

BuildUtils.vavr(project)

CheckerFrameworkConfigurator.applyAliasingChecker(project)
