import com.virtuslab.gitmachete.buildsrc.BuildUtils
import com.virtuslab.gitmachete.buildsrc.CheckerFrameworkConfigurator

dependencies {
    implementation(project(":binding"))
    implementation(project(":branchLayout:api"))
    implementation(project(":backend:api"))
    implementation(project(":frontend:graph:api"))
    implementation(project(":frontend:file"))
    implementation(project(":frontend:base"))
    implementation(project(":frontend:resourcebundles"))
    implementation(project(":frontend:ui:api"))
}

BuildUtils.lombok(project)

BuildUtils.slf4jLambdaApi(project)

BuildUtils.vavr(project)

BuildUtils.addIntellijToCompileClasspath(project, mapOf("withGit4Idea" to true))

CheckerFrameworkConfigurator.applyI18nFormatterAndTaintingCheckers(project)

CheckerFrameworkConfigurator.applySubtypingChecker(project)
