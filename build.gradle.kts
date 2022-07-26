import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.virtuslab.gitmachete.buildsrc.*
import org.ajoberstar.grgit.gradle.GrgitPlugin
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.intellij.IntelliJPlugin
import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.intellij.tasks.*
import org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel.*

plugins {
    checkstyle
    `java-library`
    scala
    groovy

    alias(libs.plugins.taskTree)
    alias(libs.plugins.versions)
    alias(libs.plugins.versionCatalogUpdate)

    alias(libs.plugins.grgit) apply false
    alias(libs.plugins.jetbrains.grammarkit) apply false
}

if (JavaVersion.current() != JavaVersion.VERSION_11) {
    throw GradleException("Project must be built with Java version 11")
}

val javaMajorVersion by extra(JavaVersion.VERSION_11)

val intellijSnapshotsUrl by extra("https://www.jetbrains.com/intellij-repository/snapshots/")

// See https://www.jetbrains.com/intellij-repository/releases/ -> Ctrl+F .idea
val intellijVersions = IntellijVersionHelper.getInstance()

val ciBranch by extra(System.getenv("CIRCLE_BRANCH"))
val isCI by extra(System.getenv("CI") == "true")
val jetbrainsMarketplaceToken by extra(System.getenv("JETBRAINS_MARKETPLACE_TOKEN"))

// This values can't be named in the same way as their corresponding properties to avoid a name clash.
val jvmArgsForJavaCompilation by extra((project.properties["compileJavaJvmArgs"] as String?)?.split(" "))
val shouldRunAllCheckers by extra(isCI || project.hasProperty("runAllCheckers"))

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
        options.compilerArgs.addAll(
            listOf(
                "-AcallToStringExplicitlyInInterpolations", // Enforce explicit `.toString()` call in code generated for string interpolations
                "-Werror", // Treat each compiler warning (esp. the ones coming from Checker Framework) as an error.
                "-Xlint:unchecked", // Warn of type-unsafe operations on generics.
            )
        )

        options.isFork = true
        options.forkOptions.jvmArgs?.addAll(
            jvmArgsForJavaCompilation
                ?: (if (isCI) listOf() else listOf("-Xmx6G", "-XX:+HeapDumpOnOutOfMemoryError"))
        )

        // `sourceCompatibility` and `targetCompatibility` say nothing about the Java APIs available to the compiled code.
        // In fact, for X < Y it's perfectly possible to compile Java X code that uses Java Y APIs...
        // This will work fine, until we actually try to run those compiled classes under Java X-compatible JVM,
        // when we'll end up with NoSuchMethodError for APIs added between Java X and Java Y
        // (i.e. for X=8 and Y=11: InputStream#readAllBytes, Stream#takeWhile and String#isBlank).
        // `options.release = X` makes sure that regardless of Java version used to run the compiler,
        // only Java X-compatible APIs are available to the compiled code.
        options.release.set(Integer.parseInt(javaMajorVersion.majorVersion))
        options.compilerArgs.addAll(listOf("--release", javaMajorVersion.majorVersion))
    }

    tasks.withType<Javadoc> {
        // See JDK-8200363 (https://bugs.openjdk.java.net/browse/JDK-8200363) for information about the `-Xwerror` option:
        // this is needed to make sure that javadoc always fails on warnings
        // (esp. important on CI since javadoc there for some reason seems to never raise any errors otherwise).

        // The '-quiet' as second argument is actually a hack around https://github.com/gradle/gradle/issues/2354:
        // since the one-parameter `addStringOption` doesn't seem to work, we need to add an extra `-quiet`, which is added anyway by Gradle.
        options.jFlags("Xwerror", "Xdoclint:all")
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

    SpotlessConfigurator.configure(project)

    CheckerFrameworkConfigurator.configure(project)

    CheckStyleConfigurator.configure(project)

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

val uiTestTargets = if (project.properties["against"] != null) IntellijVersionHelper.resolveIntelliJVersions(project.properties["against"] as String) else listOf(intellijVersions["buildTarget"])

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

tasks.register("uiTest") {
    dependsOn(tasks.matching { task -> task.name.startsWith("uiTest_") })
}

BuildUtils.ideProbe(project)
