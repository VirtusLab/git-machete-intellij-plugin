val jetbrainsAnnotations: Unit by extra
val vavr: Unit by extra

dependencies {
    //api(project(":qual"))
    //api(project(":backend:api"))
}

val addIntellijToCompileClasspath: (params: Map<String, Boolean>) -> Unit by extra
addIntellijToCompileClasspath(mapOf("withGit4Idea" to true))
