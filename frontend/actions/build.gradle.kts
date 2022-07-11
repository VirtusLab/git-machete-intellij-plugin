val lombok: Unit by extra
val slf4jLambdaApi: Unit by extra
val vavr: Unit by extra

dependencies {
    implementation(project(":binding"))
    implementation(project(":backend:api"))
    implementation(project(":branchLayout:api"))
    implementation(project(":frontend:ui:api"))
    implementation(project(":frontend:file"))
    implementation(project(":frontend:base"))
}

val addIntellijToCompileClasspath: (params: Map<String, Boolean>) -> Unit by extra
addIntellijToCompileClasspath(mapOf("withGit4Idea" to true))
val applyI18nFormatterAndTaintingCheckers: Unit by extra
val applySubtypingChecker: Unit by extra

apply(plugin = "org.jetbrains.kotlin.jvm")
val applyKotlinConfig: Unit by extra
