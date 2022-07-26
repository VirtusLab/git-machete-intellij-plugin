import com.virtuslab.gitmachete.buildsrc.BuildUtils
import com.virtuslab.gitmachete.buildsrc.CheckerFrameworkConfigurator

dependencies {
    implementation(project(":binding"))
    implementation(project(":backend:api"))
    implementation(project(":branchLayout:api"))
    implementation(project(":frontend:ui:api"))
    implementation(project(":frontend:file"))
    implementation(project(":frontend:base"))
    implementation(project(":frontend:resourcebundles"))
}

BuildUtils.lombok(project)

BuildUtils.slf4jLambdaApi(project)

BuildUtils.vavr(project)

BuildUtils.addIntellijToCompileClasspath(project, mapOf("withGit4Idea" to true))

apply(plugin = "org.jetbrains.kotlin.jvm")
BuildUtils.applyKotlinConfig(project)

CheckerFrameworkConfigurator.applyI18nFormatterAndTaintingCheckers(project)

CheckerFrameworkConfigurator.applySubtypingChecker(project)
