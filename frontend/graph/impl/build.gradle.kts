val lombok: Unit by extra
val slf4jLambdaApi: Unit by extra
val vavr: Unit by extra

dependencies {
    api(project(":frontend:graph:api"))

    implementation(project(":backend:api"))
    implementation(project(":frontend:base"))
}

val addIntellijToCompileClasspath: (params: Map<String, Boolean>) -> Unit by extra
addIntellijToCompileClasspath(mapOf("withGit4Idea" to false))
val applySubtypingChecker: Unit by extra
