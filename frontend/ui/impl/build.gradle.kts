import org.checkerframework.gradle.plugin.CheckerFrameworkExtension
//val lombok: Unit by extra
dependencies {
            compileOnly(rootProject.libs.lombok)
            annotationProcessor(rootProject.libs.lombok)
            testCompileOnly(rootProject.libs.lombok)
            testAnnotationProcessor(rootProject.libs.lombok)
        }

//val slf4jLambdaApi: Unit by extra
dependencies {
                // It's so useful for us because we are using invocations of methods that potentially consume some time
                // also in debug messages, but this plugin allows us to use lambdas that generate log messages
                // (mainly using string interpolation plugin) and these lambdas are evaluated only when needed
                // (i.e. when the given log level is active)
                implementation(rootProject.libs.slf4j.lambda)
            }

//val vavr: Unit by extra
dependencies {
                // Unlike any other current dependency, Vavr classes are very likely to end up in binary interface of the depending subproject,
                // hence it's better to just treat Vavr as an `api` and not `implementation` dependency by default.
                api(rootProject.libs.vavr)
            }


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

val addIntellijToCompileClasspath: (params: Map<String, Boolean>) -> Unit by extra
addIntellijToCompileClasspath(mapOf("withGit4Idea" to true))
//val applyI18nFormatterAndTaintingCheckers: Unit by extra
configure<CheckerFrameworkExtension> {
            // t0d0
//            checkers.addAll(listOf(
//                    "org.checkerframework.checker.i18nformatter.I18nFormatterChecker",
//                    "org.checkerframework.checker.tainting.TaintingChecker"
//            ))
//            extraJavacArgs.add("-Abundlenames=GitMacheteBundle")
        }

        // Apparently, I18nFormatterChecker doesn't see resource bundles in its classpath unless they're defined in a separate module.
        dependencies {
            add("checkerFramework", project(":frontend:resourcebundles"))
        }

//val applySubtypingChecker: Unit by extra
val shouldRunAllCheckers:Boolean by rootProject.extra
        if (shouldRunAllCheckers) {
            dependencies {
                add("checkerFramework", project(":qual"))
            }
            configure<CheckerFrameworkExtension> {
                checkers.add("org.checkerframework.common.subtyping.SubtypingChecker")
                val qualClassDir = project(":qual").sourceSets["main"].output.classesDirs.asPath
                extraJavacArgs.add("-ASubtypingChecker_qualDirs=${qualClassDir}")
            }
        }
