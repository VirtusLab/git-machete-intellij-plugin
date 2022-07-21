import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.virtuslab.gitmachete.buildsrc.IntellijVersionHelper
import com.virtuslab.gitmachete.buildsrc.UpdateEapBuildNumber
import org.ajoberstar.grgit.gradle.GrgitPlugin
import org.checkerframework.gradle.plugin.CheckerFrameworkExtension
import org.checkerframework.gradle.plugin.CheckerFrameworkPlugin
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.intellij.IntelliJPlugin
import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.intellij.tasks.*
import org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath(libs.jsoup)
    }
}

plugins {
    checkstyle
    `java-library`
    scala
    groovy

    alias(libs.plugins.taskTree)
    alias(libs.plugins.versions)
    alias(libs.plugins.versionCatalogUpdate)

    alias(libs.plugins.aspectj.postCompileWeaving) apply false
    alias(libs.plugins.checkerFramework) apply false
    alias(libs.plugins.grgit) apply false
    alias(libs.plugins.jetbrains.grammarkit) apply false
    alias(libs.plugins.jetbrains.intellij) apply false
    alias(libs.plugins.jetbrains.kotlin) apply false
    alias(libs.plugins.spotless) apply false
}

if (JavaVersion.current() != JavaVersion.VERSION_11) {
    throw GradleException("Project must be built with Java version 11")
}

val javaMajorVersion by extra(JavaVersion.VERSION_11)

val intellijSnapshotsUrl by extra("https://www.jetbrains.com/intellij-repository/snapshots/")
val intellijVersionsProp by extra(IntellijVersionHelper.getProperties())

// See https://www.jetbrains.com/intellij-repository/releases/ -> Ctrl+F .idea
val intellijVersions by extra(mutableMapOf<String, Any>(
        // When this value is updated, remember to update the minimum required IDEA version in README.md.
        "earliestSupportedMajor" to intellijVersionsProp.getProperty("earliestSupportedMajor"),
        // Most recent minor versions of all major releases
        // between earliest supported (incl.) and latest stable (excl.), used for binary compatibility checks and UI tests
        "latestMinorsOfOldSupportedMajors" to intellijVersionsProp.getProperty("latestMinorsOfOldSupportedMajors").split(","), "latestStable" to intellijVersionsProp.getProperty("latestStable"),
        // Note that we have to use a "fixed snapshot" version X.Y.Z-EAP-SNAPSHOT (e.g. 211.4961.33-EAP-SNAPSHOT)
        // rather than a "rolling snapshot" X-EAP-SNAPSHOT (e.g. 211-EAP-SNAPSHOT)
        // to ensure that the builds are reproducible.
        // EAP-CANDIDATE-SNAPSHOTs can be used for binary compatibility checks,
        // but for some reason aren't resolved in UI tests.
        // Generally, see https://www.jetbrains.com/intellij-repository/snapshots/ -> Ctrl+F .idea
        // Use `null` if the latest supported major has a stable release (and not just EAPs).
        "eapOfLatestSupportedMajor" to intellijVersionsProp.getProperty("eapOfLatestSupportedMajor")))


intellijVersions["latestSupportedMajor"] = if (intellijVersions["eapOfLatestSupportedMajor"] != null) IntellijVersionHelper.getFromBuildNumber(intellijVersions["eapOfLatestSupportedMajor"]!! as String)
else IntellijVersionHelper.getMajorPart(intellijVersions["latestStable"]!! as String)

intellijVersions["buildTarget"] = intellijVersions["eapOfLatestSupportedMajor"] ?: intellijVersions["latestStable"]!!

val ciBranch by extra(System.getenv("CIRCLE_BRANCH"))
val isCI by extra(System.getenv("CI") == "true")
val jetbrainsMarketplaceToken by extra(System.getenv("JETBRAINS_MARKETPLACE_TOKEN"))

// This values can't be named in the same way as their corresponding properties to avoid a name clash.
val jvmArgsForJavaCompilation by extra((project.properties["compileJavaJvmArgs"] as String?)?.split(" "))
val shouldRunAllCheckers by extra(isCI || project.hasProperty("runAllCheckers"))

/**
 * @param versionKey Either release number (like 2020.3) or key of intellijVersions (like eapOfLatestSupportedMajor)
 * @returns Corresponding release numbers.
 */
fun resolveIntelliJVersions(versionKey: String?): List<String> {
    if (versionKey == null) {
        return emptyList()
    }
    val regex = "/^[0-9].*$/".toRegex()
    if (regex.matches(versionKey)) {
        return listOf(versionKey)
    }

    val versionValue = intellijVersions[versionKey] ?: return emptyList()
    if (versionValue is List<*>) {
        return versionValue as List<String>
    }
    return listOf(versionValue) as List<String>
}

tasks.register<UpdateEapBuildNumber>("updateEapBuildNumber")

tasks.withType<DependencyUpdatesTask> {

    val isStableVersion: (String) -> Boolean = { version ->
        val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
        val regex = "/^[0-9,.v-]+(-r)?$/".toRegex()
        stableKeyword || regex.matches(version)
    }

    rejectVersionIf {
        !isStableVersion(this.candidate.version)
    }
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }

    apply<JavaLibraryPlugin>()

    java {
        sourceCompatibility = javaMajorVersion
        targetCompatibility = javaMajorVersion // redundant, added for clarity
    }

    // String interpolation support, see https://github.com/antkorwin/better-strings
    // This needs to be enabled in each subproject by default because there's going to be no warning
    // if this annotation processor isn't run in any subproject (the strings will be just interpreted verbatim, without interpolation applied).
    // We'd only capture that in CI's post-compile checks by analyzing constants in class files.
    dependencies {
        annotationProcessor(rootProject.libs.betterStrings)
        testAnnotationProcessor(rootProject.libs.betterStrings)
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(listOf(
                "-AcallToStringExplicitlyInInterpolations", // Enforce explicit `.toString()` call in code generated for string interpolations
                "-Werror", // Treat each compiler warning (esp. the ones coming from Checker Framework) as an error.
                "-Xlint:unchecked", // Warn of type-unsafe operations on generics.
        ))

        options.isFork = true
        options.forkOptions.jvmArgs?.addAll(jvmArgsForJavaCompilation
                ?: (if (isCI) listOf() else listOf("-Xmx6G", "-XX:+HeapDumpOnOutOfMemoryError")))

        // `sourceCompatibility` and `targetCompatibility` say nothing about the Java APIs available to the compiled code.
        // In fact, for X < Y it's perfectly possible to compile Java X code that uses Java Y APIs...
        // This will work fine, until we actually try to run those compiled classes under Java X-compatible JVM,
        // when we'll end up with NoSuchMethodError for APIs added between Java X and Java Y
        // (i.e. for X=8 and Y=11: InputStream#readAllBytes, Stream#takeWhile and String#isBlank).
        // `options.release = X` makes sure that regardless of Java version used to run the compiler,
        // only Java X-compatible APIs are available to the compiled code.
        // options.release = Integer.parseInt(javaMajorVersion.majorVersion)
        options.compilerArgs.addAll(listOf("--release", javaMajorVersion.majorVersion))
    }

    tasks.withType<Javadoc> {
        // See JDK-8200363 (https://bugs.openjdk.java.net/browse/JDK-8200363) for information about the `-Xwerror` option:
        // this is needed to make sure that javadoc always fails on warnings
        // (esp. important on CI since javadoc there for some reason seems to never raise any errors otherwise).

        // The '-quiet' as second argument is actually a hack around https://github.com/gradle/gradle/issues/2354:
        // since the one-parameter `addStringOption` doesn't seem to work, we need to add an extra `-quiet`, which is added anyway by Gradle.
        // options.addStringOption("Xwerror", "-quiet")
        // options.addStringOption("Xdoclint:all", "-quiet")
        options.quiet()
    }

    tasks.withType<Test> {
        testLogging {
            events = setOf(TestLogEvent.FAILED)
            exceptionFormat = TestExceptionFormat.FULL
            showCauses = true
            showExceptions = true
            showStackTraces = true
        }
    }


    apply<SpotlessPlugin>()
    configure<SpotlessExtension> {
        java {
            importOrder("java", "javax", "", "com.virtuslab")
            // See https://github.com/diffplug/spotless/blob/master/ECLIPSE_SCREENSHOTS.md on importing and exporting settings from Eclipse
            eclipse().configFile("$rootDir/config/spotless/formatting-rules.xml")
            removeUnusedImports()
            targetExclude("**/build/generated/**/*.*")
        }

        kotlin {
            ktfmt()
        }
        scala {
            scalafmt().configFile("$rootDir/scalafmt.conf")
        }
        groovy {
            target("/buildSrc/**/*.groovy")
            greclipse().configFile("$rootDir/config/spotless/formatting-rules.xml")
        }
    }

    if (!isCI) {
        tasks.withType<JavaCompile> {
            dependsOn("spotlessJavaApply")
        }
        tasks.withType<KotlinCompile> {
            dependsOn("spotlessKotlinApply")
        }
        tasks.withType<ScalaCompile> {
            dependsOn("spotlessScalaApply")
        }
        tasks.withType<GroovyCompile> {
            dependsOn("spotlessGroovyApply")
        }
    }


    apply<CheckerFrameworkPlugin>()
    configure<CheckerFrameworkExtension> {
        excludeTests = true
        checkers = listOf(
                "org.checkerframework.checker.nullness.NullnessChecker",
        )
        if (shouldRunAllCheckers) {
            // The experience shows that the below checkers are just rarely failing, as compared to GuiEffect/Nullness.
            // Hence, they're only applied in CI, or locally only if a dedicated Gradle project property is set.
            checkers.addAll(listOf(
                    "org.checkerframework.checker.index.IndexChecker",
                    "org.checkerframework.checker.interning.InterningChecker",
                    "org.checkerframework.checker.optional.OptionalChecker",
            ))
        }
        extraJavacArgs = listOf(
                "-AassumeAssertionsAreEnabled",
                "-AinvariantArrays",
                "-Alint=cast:redundant,cast:unsafe",
                "-ArequirePrefixInWarningSuppressions",
                "-AshowSuppressWarningsStrings",
                "-Astubs=$rootDir/config/checker/",
                // The `-AstubWarnIfNotFoundIgnoresClasses` flag is required since Checker 3.14.0,
                // the version since which `-AstubWarnIfNotFound` is assumed to be true for custom stub files.
                // Without this flag, we would end up with a lot of errors in subprojects where any of the stubbed libraries is NOT on the classpath:
                // e.g. compilation of a subproject where Vavr is NOT a dependency would fail
                // since for simplicity, we're providing the same set of stubs to Checker in each subproject
                // (`$rootDir/config/checker/`, which includes e.g. Vavr).
                "-AstubWarnIfNotFoundIgnoresClasses",
                "-AsuppressWarnings=allcheckers:type.anno.before.decl.anno,allcheckers:type.anno.before.modifier,allcheckers:type.checking.not.run,value:annotation",
        )
        dependencies {
            compileOnly(rootProject.libs.checker.qual)
            add("checkerFramework", rootProject.libs.checker)
        }
    }


    apply<CheckstylePlugin>()
    configure<CheckstyleExtension> {
        tasks.named<Checkstyle>("checkstyleTest") {
            enabled = false
        }

        configProperties = mapOf("rootCheckstyleConfigDir" to "$rootDir/config/checkstyle")
    }


    // A few libraries (like JGit and reflections) transitively pull in a version of slf4j-api
    // that might be different from the slf4j-api version that IntelliJ depends on.
    // SLF4J guarantees that the code compiled against a certain slf4j-api version will work with any other version of slf4j-api
    // (http://www.slf4j.org/manual.html#compatibility).
    // We rely on that guarantee: our plugin effectively uses whatever slf4j-api version is provided by IntelliJ.
    // SLF4J does NOT guarantee, however, that slf4j-api version X will work with any slf4j implementation version Y for X != Y.
    // To avoid a clash between JGit&co.'s slf4j-api and Intellij's slf4j implementation
    // (and also between JGit&co.'s slf4j-api and Intellij's slf4j-api), we need to exclude the former from ALL dependencies.
    configurations.runtimeClasspath {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    val addIntellijToCompileClasspath by extra {
        fun(params: Map<String, Boolean>) {
            val tasksBefore = mutableListOf<Task>()
            tasksBefore.addAll(project.tasks)

            apply<IntelliJPlugin>()

            val tasksAfter = mutableListOf<Task>()
            tasksAfter.addAll(project.tasks)
            tasksAfter.removeAll(tasksBefore)
            // For the frontend subprojects we only use gradle-intellij-plugin to provide dependencies,
            // but don't want the associated tasks to be available; they should only be available in the root project.
            tasksAfter.forEach { it.enabled = false }
            // The only task (as of gradle-intellij-plugin v1.7.0, at least) that needs to be enabled in all IntelliJ-aware modules
            // is `classpathIndexCleanup`, to avoid caching issues caused by `classpath.index` file
            // showing up in build/classes/ and build/resources/ directories.
            // See https://github.com/JetBrains/gradle-intellij-plugin/issues/1039 for details.
            tasks.withType<ClasspathIndexCleanupTask> {
                enabled = true
            }

            configure<CheckerFrameworkExtension> {
                // Technically, UI thread handling errors can happen outside of the (mostly frontend) modules that depend on IntelliJ,
                // but the risk is minuscule and not worth the extra computational burden in every single build.
                // This might change, however, if/when Checker Framework adds @Heavyweight annotation
                // (https://github.com/typetools/checker-framework/issues/3253).
                // t0d0
//                checkers.add("org.checkerframework.checker.guieffect.GuiEffectChecker")
            }

            configure<IntelliJPluginExtension> { // or configure IntellijPluginExtension?
                version.set(intellijVersions["buildTarget"] as String)
                // No need to instrument Java classes with nullability assertions, we've got this covered much better by Checker
                // (and we don't plan to expose any part of the plugin as an API for other plugins).
                instrumentCode.set(false)
                if (params["withGit4Idea"] == true) {
                    plugins.set(listOf("git4idea"))
                }
            }
        }
    }
//
//    val applyAliasingChecker by extra {
//        if (shouldRunAllCheckers) {
//            configure<CheckerFrameworkExtension> {
//                checkers.add("org.checkerframework.common.aliasing.AliasingChecker")
//            }
//        }
//    }
//
//    val applyI18nFormatterAndTaintingCheckers by extra {
//        // I18nFormatterChecker and TaintingChecker, like GuiEffectChecker and NullnessChecker, are enabled
//        // regardless of `CI` env var/`runAllCheckers` Gradle project property.
//        configure<CheckerFrameworkExtension> {
//            // t0d0
////            checkers.addAll(listOf(
////                    "org.checkerframework.checker.i18nformatter.I18nFormatterChecker",
////                    "org.checkerframework.checker.tainting.TaintingChecker"
////            ))
////            extraJavacArgs.add("-Abundlenames=GitMacheteBundle")
//        }
//
//        // Apparently, I18nFormatterChecker doesn't see resource bundles in its classpath unless they're defined in a separate module.
//        dependencies {
//            add("checkerFramework", project(":frontend:resourcebundles"))
//        }
//    }
//
//    val applySubtypingChecker by extra {
//        if (shouldRunAllCheckers) {
//            dependencies {
//                add("checkerFramework", project(":qual"))
//            }
//            configure<CheckerFrameworkExtension> {
//                checkers.add("org.checkerframework.common.subtyping.SubtypingChecker")
//                val qualClassDir = project(":qual").sourceSets["main"].output.classesDirs.asPath
//                extraJavacArgs.add("-ASubtypingChecker_qualDirs=${qualClassDir}")
//            }
//        }
//    }
//
//    val applyKotlinConfig by extra {
//        tasks.withType<KotlinCompile> {
//            // TODO (#785): revert this setting
////         kotlinOptions.allWarningsAsErrors = true
//
//            // Supress the warnings about different version of Kotlin used for compilation
//            // than bundled into the `buildTarget` version of IntelliJ.
//            // For compilation we use the Kotlin version from the earliest support IntelliJ,
//            // and as per https://kotlinlang.org/docs/components-stability.html,
//            // code compiled against an older version of kotlin-stdlib should work
//            // when a newer version of kotlin-stdlib is provided as a drop-in replacement.
//            kotlinOptions.freeCompilerArgs += listOf("-Xskip-metadata-version-check")
//
//            kotlinOptions.jvmTarget = javaMajorVersion.majorVersion
//        }
//    }
//
//    val commonsIO by extra {
//        dependencies {
//            implementation(rootProject.libs.commonsIO)
//        }
//    }
//
//    val ideProbe by extra {
//        repositories {
//            // Needed for com.intellij.remoterobot:remote-robot
//            maven { url = `java.net`.URI("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") }
//        }
//
//        dependencies {
//            // Note that we can't easily use Gradle's `testFixtures` configuration here
//            // as it doesn't seem to expose testFixtures resources in test classpath correctly.
//
//            // t0d0
//            // https://stackoverflow.com/questions/56297459/how-to-convert-sourcesets-from-a-project-to-kotlin-kts
//            // https://github.com/gradle/kotlin-dsl-samples/issues/577
//            // https://stackoverflow.com/questions/5644011/multi-project-test-dependencies-with-gradle
//            // https://github.com/hauner/gradle-plugins/tree/master/jartest
//
////            val dependencyNotation = testFixtures(project(":testCommon"))
////            uiTestImplementation(dependencyNotation)
////            uiTestImplementation(rootProject.libs.bundles.ideProbe)
//
//            // This is technically redundant (both since ide-probe pulls in scala-library anyway,
//            // and since ide-probe is meant to use in src/uiTest code, not src/test code),
//            // but apparently needed for IntelliJ to detect Scala SDK version in the project (it's probably https://youtrack.jetbrains.com/issue/SCL-14310).
////            testImplementation(rootProject.libs.scala.library)
//        }
//    }
//
//    val jcabiAspects by extra {
//        apply(plugin = "io.freefair.aspectj.post-compile-weaving")
////        apply<AspectJPlugin>()
//
//        tasks.withType<JavaCompile> {
//            // Turn off `adviceDidNotMatch` spam warnings
//            options.compilerArgs.add("-Xlint:ignore")
//        }
//
//        dependencies {
//            add("aspect", rootProject.libs.jcabi.aspects)
//        }
//    }
//
//    val jetbrainsAnnotations by extra {
//        dependencies {
//            compileOnly(rootProject.libs.jetbrains.annotations)
//            testCompileOnly(rootProject.libs.jetbrains.annotations)
//        }
//    }
//
//    val jgit by extra {
//        dependencies {
//            implementation(rootProject.libs.jgit)
//        }
//    }
//
//    val junit by extra {
//        dependencies {
//            testImplementation(rootProject.libs.junit)
//        }
//    }
//
//    val lombok by extra {
//        dependencies {
//            compileOnly(rootProject.libs.lombok)
//            annotationProcessor(rootProject.libs.lombok)
//            testCompileOnly(rootProject.libs.lombok)
//            testAnnotationProcessor(rootProject.libs.lombok)
//        }
//    }
//
//    val powerMock by extra {
//        dependencies {
//            testImplementation(rootProject.libs.bundles.powerMock)
//        }
//    }
//
//    val reflections by extra {
//        {
//            dependencies {
//                implementation(rootProject.libs.reflections)
//            }
//        }
//    }
//
//    val slf4jLambdaApi by extra {
//        {
//            dependencies {
//                // It's so useful for us because we are using invocations of methods that potentially consume some time
//                // also in debug messages, but this plugin allows us to use lambdas that generate log messages
//                // (mainly using string interpolation plugin) and these lambdas are evaluated only when needed
//                // (i.e. when the given log level is active)
//                implementation(rootProject.libs.slf4j.lambda)
//            }
//        }
//    }
//
//    val slf4jTestImpl by extra {
//        {
//            // We only need to provide an SLF4J implementation in the contexts which depend on the plugin but don't depend on IntelliJ.
//            // In our case, that's solely the tests of backend modules.
//            // In other contexts that require an SLF4J implementation (buildPlugin, runIde, UI tests),
//            // an SLF4J implementation is provided by IntelliJ.
//            // Note that we don't need to agree the SLF4J implementation version here with slf4j-api version pulled in by our dependencies (like JGit)
//            // since the latter is excluded (see the comment to `exclude group: 'org.slf4j'` for more nuances).
//            // The below dependency provides both slf4j-api and an implementation, both already in the same version.
//            // Global exclusion on slf4j-api does NOT apply to tests since it's only limited to `runtimeClasspath` configuration.
//            dependencies {
//                testRuntimeOnly(rootProject.libs.slf4j.simple)
//            }
//        }
//    }
//
//    val vavr by extra {
//        {
            dependencies {
                // Unlike any other current dependency, Vavr classes are very likely to end up in binary interface of the depending subproject,
                // hence it's better to just treat Vavr as an `api` and not `implementation` dependency by default.
                api(rootProject.libs.vavr)
            }
//        }
//    }
}


subprojects {
    // This is necessary to make sure that `buildPlugin` task puts jars of all relevant subprojects into the final zip.
    // No need to include near-empty (only with META-INF/MANIFEST.MF) jars
    // for subprojects that don't have any production code.
    if (sourceSets["main"].allSource.srcDirs.any { it?.exists() == true }) {
        rootProject.dependencies {
            implementation(project)
        }
    }

    // By default, the jar name will be formed only from the last segment of subproject path.
    // Since these last segments are NOT unique (there are many `api`s and `impl`s),
    // the effective jar name will be something like api.jar, api_1.jar, api_2.jar etc.,
    // which is suboptimal.
    // Let's use full name like frontend-ui-api.jar instead.
    base.archivesName.set(path.replaceFirst(":", "").replace(":", "-"))
}


// Root project config

group = "com.virtuslab"

apply<GrgitPlugin>()
apply(from = "version.gradle.kts")

val PROSPECTIVE_RELEASE_VERSION: String by extra

if (ciBranch == "master") {
    version = PROSPECTIVE_RELEASE_VERSION  // more precisely, "soon-to-happen-in-this-pipeline release version" in case of master builds

} else if (!file(".git").exists()) {
    // To make sure it's safe for Docker image builds where .git folder is unavailable
    version = "$PROSPECTIVE_RELEASE_VERSION-SNAPSHOT"

} else {
    val maybeSnapshot = if (ciBranch == "develop") "" else "-SNAPSHOT"

    val git = org.ajoberstar.grgit.Grgit.open(mapOf("currentDir" to projectDir))
    val lastTag = git.tag.list().sortedBy { it.dateTime }.last()
    val commitsSinceLastTag = git.log(mapOf("includes" to listOf("HEAD"), "excludes" to listOf(lastTag.fullName)))
    val maybeCommitCount = if (commitsSinceLastTag.isEmpty()) "" else "-" + commitsSinceLastTag.size
    val shortCommitHash = git.head().abbreviatedId
    val maybeDirty = if (git.status().isClean) "" else "-dirty"
    git.close()

    version = "$PROSPECTIVE_RELEASE_VERSION$maybeCommitCount$maybeSnapshot+git.$shortCommitHash$maybeDirty"
}



dependencies {
    // Checker is needed on root project runtime (not just compile-time) classpath for ArchUnit tests
    testImplementation(rootProject.libs.checker.qual)
    testImplementation(rootProject.libs.archunit)
}


apply<IntelliJPlugin>()
configure<IntelliJPluginExtension> {
    instrumentCode.set(false)
    pluginName.set("git-machete-intellij-plugin")
    version.set(intellijVersions["buildTarget"] as String)
    plugins.set(listOf("git4idea")) // Needed solely for ArchUnit
}

if (!isCI) {
    // The output of this task is for some reason very poorly cached,
    // and the task takes a significant amount of time,
    // while the index of searchable options is of little use for local development.
    tasks.withType<BuildSearchableOptionsTask> {
        enabled = false
    }

}

tasks.withType<PatchPluginXmlTask> {
    // `sinceBuild` is exclusive when we are using `*` in version but inclusive when without `*`
    sinceBuild.set(IntellijVersionHelper.toBuildNumber(intellijVersions["earliestSupportedMajor"] as String))

    // In `untilBuild` situation is inverted: it's inclusive when using `*` but exclusive when without `*`
    untilBuild.set(IntellijVersionHelper.toBuildNumber(intellijVersions["latestSupportedMajor"] as String) + ".*")

    // Note that the first line of the description should be self-contained since it is placed into embeddable card:
    // see e.g. https://plugins.jetbrains.com/search?search=git%20machete
    pluginDescription.set(file("$rootDir/DESCRIPTION.html").readText())

    changeNotes.set("<h3>v${rootProject.version}</h3>\n\n" + file("$rootDir/CHANGE-NOTES.html").readText())
}


tasks.withType<RunIdeTask> {
    maxHeapSize = "4G"
}

tasks.withType<RunPluginVerifierTask> {
    val maybeEap = if (intellijVersions["eapOfLatestSupportedMajor"] != null) listOf((intellijVersions["eapOfLatestSupportedMajor"] as String).replace("-EAP-(CANDIDATE-)?SNAPSHOT", ""))
    else emptyList()
    ideVersions.set(listOf(*(intellijVersions["latestMinorsOfOldSupportedMajors"] as List<String>).toTypedArray(), intellijVersions["latestStable"] as String, *maybeEap.toTypedArray()))

    val skippedFailureLevels = `java.util`.EnumSet.of(DEPRECATED_API_USAGES, EXPERIMENTAL_API_USAGES, NOT_DYNAMIC, SCHEDULED_FOR_REMOVAL_API_USAGES)
    failureLevel.set(`java.util`.EnumSet.complementOf(skippedFailureLevels))
}

tasks.withType<PublishPluginTask> {
    token.set(jetbrainsMarketplaceToken)
}


val uiTest = sourceSets.create("uiTest")

val uiTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}

val uiTestRuntimeOnly: Configuration by configurations.getting {
    extendsFrom(configurations.testRuntimeOnly.get())
}

val uiTestTargets = if (project.properties["against"] != null) resolveIntelliJVersions(project.properties["against"] as String) else listOf(intellijVersions["buildTarget"])

uiTestTargets.onEach { version ->
    tasks.register<Test>("uiTest_${version}") {
        description = "Runs UI tests."
        group = "verification"

        testClassesDirs = uiTest.output.classesDirs
        classpath = configurations["uiTestRuntimeClasspath"] + uiTest.output

        val bp = dependsOn(":buildPlugin")

        systemProperty("ui-test.intellij.version", version as String)
        systemProperty("ui-test.plugin.path", bp.outputs.files.first().path)

        // TODO (#945): caching of UI test results doesn't work in the CI anyway
        if (!isCI) {
            outputs.upToDateWhen { false }
        }

        if (project.properties["tests"] != null) {
            filter {
                includeTestsMatching("*.*${project.properties["tests"]}*")
            }
        }

        if (project.hasProperty("headless")) {
            environment("IDEPROBE_DISPLAY", "xvfb")
            environment("IDEPROBE_PATHS_SCREENSHOTS", System.getProperty("user.home") + "/.ideprobe-uitests/" + "/artifacts/uiTest" + version + "/screenshots")
            if (isCI) {
                environment("IDEPROBE_PATHS_BASE", System.getProperty("user.home") + "/.ideprobe-uitests/")
            }
        }

        testLogging {
            events.addAll(listOf(TestLogEvent.STANDARD_OUT, TestLogEvent.STANDARD_ERROR))
        }
    }
}

// Cannot use `task uiTest(type: Test)` syntax due to the name conflict with `val uiTest` above.
tasks.register("uiTest") {
    dependsOn(tasks.matching { task -> task.name.startsWith("uiTest_") })
}

val ideProbe: Unit by extra
