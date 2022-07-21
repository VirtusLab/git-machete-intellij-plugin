//val lombok: Unit by extra
dependencies {
            compileOnly(rootProject.libs.lombok)
            annotationProcessor(rootProject.libs.lombok)
            testCompileOnly(rootProject.libs.lombok)
            testAnnotationProcessor(rootProject.libs.lombok)
        }

//val vavr: Unit by extra
dependencies {
                // Unlike any other current dependency, Vavr classes are very likely to end up in binary interface of the depending subproject,
                // hence it's better to just treat Vavr as an `api` and not `implementation` dependency by default.
                api(rootProject.libs.vavr)
            }


dependencies {
    api(project(":qual"))
}
