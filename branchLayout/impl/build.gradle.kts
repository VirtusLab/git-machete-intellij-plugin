val junit: Unit by extra
val lombok: Unit by extra
val powerMock: Unit by extra
val slf4jLambdaApi: Unit by extra
val slf4jTestImpl: Unit by extra
val vavr: Unit by extra

dependencies {
    api(project(":branchLayout:api"))
}
