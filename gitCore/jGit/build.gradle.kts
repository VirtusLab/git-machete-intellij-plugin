val jgit: Unit by extra
val lombok: Unit by extra
val slf4jLambdaApi: Unit by extra
val vavr: Unit by extra

dependencies {
    api(project(":gitCore:api"))
}

val applyAliasingChecker: Unit by extra
