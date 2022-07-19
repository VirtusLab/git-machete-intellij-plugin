val lombok: Unit by extra
val vavr: Unit by extra

dependencies {
    api(project(":qual"))
    api(project(":branchLayout:api"))
}
