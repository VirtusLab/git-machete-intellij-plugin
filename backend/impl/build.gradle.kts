import org.checkerframework.gradle.plugin.CheckerFrameworkExtension
//val commonsIO: Unit by extra
dependencies {
            implementation(rootProject.libs.commonsIO)
        }

//val jcabiAspects: Unit by extra
apply(plugin = "io.freefair.aspectj.post-compile-weaving")
//        apply<AspectJPlugin>()

        tasks.withType<JavaCompile> {
            // Turn off `adviceDidNotMatch` spam warnings
            //t0d0 gradle build error: invalid flag: -Xlint:ignore
            //options.compilerArgs.add("-Xlint:ignore")
        }

        dependencies {
            add("aspect", rootProject.libs.jcabi.aspects)
        }

//val junit: Unit by extra
dependencies {
            testImplementation(rootProject.libs.junit)
        }

//val lombok: Unit by extra
dependencies {
            compileOnly(rootProject.libs.lombok)
            annotationProcessor(rootProject.libs.lombok)
            testCompileOnly(rootProject.libs.lombok)
            testAnnotationProcessor(rootProject.libs.lombok)
        }

//val powerMock: Unit by extra
dependencies {
            testImplementation(rootProject.libs.bundles.powerMock)
        }

//val slf4jLambdaApi: Unit by extra
dependencies {
                // It's so useful for us because we are using invocations of methods that potentially consume some time
                // also in debug messages, but this plugin allows us to use lambdas that generate log messages
                // (mainly using string interpolation plugin) and these lambdas are evaluated only when needed
                // (i.e. when the given log level is active)
                implementation(rootProject.libs.slf4j.lambda)
            }

//val slf4jTestImpl: Unit by extra
dependencies {
                testRuntimeOnly(rootProject.libs.slf4j.simple)
            }

//val vavr: Unit by extra
dependencies {
                // Unlike any other current dependency, Vavr classes are very likely to end up in binary interface of the depending subproject,
                // hence it's better to just treat Vavr as an `api` and not `implementation` dependency by default.
                api(rootProject.libs.vavr)
            }


dependencies {
    implementation(project(":binding"))
    implementation(project(":gitCore:api"))

    api(project(":backend:api"))
    api(project(":branchLayout:api"))

    // Note that we can't easily use Gradle's `testFixtures` configuration here
    // as it doesn't seem to expose testFixtures resources in test classpath correctly.
    testImplementation(project(":testCommon").dependencyProject.sourceSets["test"].output)
    testRuntimeOnly(project(":branchLayout:impl"))
    testRuntimeOnly(project(":gitCore:jGit"))
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


tasks.withType<JavaExec>() {
    group = "Execution"
    description = "Regenerate CLI outputs used for comparison in tests"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.virtuslab.gitmachete.backend.integration.RegenerateCliOutputs")
    args = sourceSets["test"].resources.srcDirs.map{ it.absolutePath }.toList()
}
