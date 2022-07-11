val vavr: Unit by extra
val slf4jLambdaApi: Unit by extra
val lombok: Unit by extra

val addIntellijToCompileClasspath: (params: Map<String, Boolean>) -> Unit by extra
addIntellijToCompileClasspath(mapOf("withGit4Idea" to true))

dependencies {
    implementation(project(":binding"))

    api(project(":branchLayout:api"))
    api(project(":backend:api"))
}
