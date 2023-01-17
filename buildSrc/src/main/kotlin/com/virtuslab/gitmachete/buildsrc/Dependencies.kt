package com.virtuslab.gitmachete.buildsrc

import io.freefair.gradle.plugins.aspectj.AspectJPlugin
import org.checkerframework.gradle.plugin.CheckerFrameworkExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.*
import org.jetbrains.intellij.IntelliJPlugin
import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.intellij.tasks.ClasspathIndexCleanupTask
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

fun Project.applyKotlinConfig() {
  apply<KotlinPluginWrapper>()

  tasks.withType<KotlinCompile> {
    kotlinOptions {
      allWarningsAsErrors = true

      // Supress the warnings about different version of Kotlin used for compilation
      // than bundled into the `buildTarget` version of IntelliJ.
      // For compilation we use the Kotlin version from the earliest support IntelliJ,
      // and as per https://kotlinlang.org/docs/components-stability.html,
      // code compiled against an older version of kotlin-stdlib should work
      // when a newer version of kotlin-stdlib is provided as a drop-in replacement.
      freeCompilerArgs += listOf("-Xskip-metadata-version-check")

      val targetJavaVersion: JavaVersion by rootProject.extra
      jvmTarget = targetJavaVersion.toString()
    }
  }
}

fun Project.addIntellijToCompileClasspath(withGit4Idea: Boolean) {
  val tasksBefore = mutableListOf<Task>()
  tasksBefore.addAll(tasks)

  apply<IntelliJPlugin>()

  val tasksAfter = mutableListOf<Task>()
  tasksAfter.addAll(tasks)
  tasksAfter.removeAll(tasksBefore)
  // For the frontend subprojects we only use gradle-intellij-plugin to provide dependencies,
  // but don't want the associated tasks to be available; they should only be available in the root project.
  tasksAfter.forEach { it.enabled = false }
  // The only task (as of gradle-intellij-plugin v1.7.0, at least) that needs to be enabled
  // in all IntelliJ-aware modules is `classpathIndexCleanup`, to avoid caching issues caused by `classpath.index` file
  // showing up in build/classes/ and build/resources/ directories.
  // See https://github.com/JetBrains/gradle-intellij-plugin/issues/1039 for details.
  tasks.withType<ClasspathIndexCleanupTask> { enabled = true }

  configure<CheckerFrameworkExtension> {
    // Technically, UI thread handling errors can happen outside of the (mostly frontend) modules that depend on IntelliJ,
    // but the risk is minuscule and not worth the extra computational burden in every single build.
    // This might change, however, if/when Checker Framework adds @Heavyweight annotation
    // (https://github.com/typetools/checker-framework/issues/3253).
    checkers.add("org.checkerframework.checker.guieffect.GuiEffectChecker")
  }

  val intellijVersions: IntellijVersions by rootProject.extra
  configure<IntelliJPluginExtension> {
    version.set(intellijVersions.buildTarget)
    // No need to instrument Java classes with nullability assertions, we've got this covered much
    // better by Checker (and we don't plan to expose any part of the plugin as an API for other plugins).
    instrumentCode.set(false)
    if (withGit4Idea) {
      // Let's use the plugin *id* which remained unchanged in the 2022.2->2022.3 update
      // (which changed the plugin *folder name* from `git4idea` to `vcs-git`; `plugins` property apparently accepts both folder names and ids).
      plugins.set(listOf("Git4Idea"))
    }
  }
}

// See https://melix.github.io/blog/2021/03/version-catalogs-faq.html#_but_how_can_i_use_the_catalog_in_plugins_defined_in_buildsrc
private fun Project.versionCatalog(): VersionCatalog {
  return this.rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
}

fun Project.bundle(id: String): Provider<ExternalModuleDependencyBundle> {
  return this.versionCatalog().findBundle(id).get()
}

fun Project.lib(id: String): Provider<MinimalExternalModuleDependency> {
  return this.versionCatalog().findLibrary(id).get()
}

fun Project.apacheCommonsText() {
  dependencies {
    "implementation"(lib("apacheCommonsText"))
  }
}

fun Project.commonsIO() {
  dependencies {
    "implementation"(lib("commonsIO"))
  }
}

fun Project.ideProbe() {
  repositories {
    // Needed for com.intellij.remoterobot:remote-robot
    maven {
      url = URI("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    }
  }

  dependencies {
    "uiTestImplementation"(testFixtures(project(":testCommon")))
    "uiTestImplementation"(bundle("ideProbe"))
  }
}

fun Project.jcabiAspects() {
  apply<AspectJPlugin>()

  dependencies { "aspect"(lib("jcabi.aspects")) }
}

fun Project.jetbrainsAnnotations() {
  dependencies {
    val annotations = lib("jetbrains.annotations")
    "compileOnly"(annotations)
    "testCompileOnly"(annotations)
  }
}

fun Project.jgit() {
  dependencies {
    "implementation"(lib("jgit"))
  }
}

fun Project.junit() {
  dependencies {
    "testImplementation"(lib("junit"))
  }
}

fun Project.junit5() {
  dependencies {
    "testImplementation"(lib("junit5"))
  }
}

fun Project.lombok() {
  dependencies {
    val lombok = lib("lombok")
    "compileOnly"(lombok)
    "annotationProcessor"(lombok)
    "testCompileOnly"(lombok)
    "testAnnotationProcessor"(lombok)
  }
}

fun Project.powerMock() {
  dependencies {
    "testImplementation"(bundle("powerMock"))
  }
}

fun Project.powerMock5() {
  dependencies {
    "testImplementation"(bundle("powerMock5"))
  }
}

fun Project.slf4jLambdaApi() {
  dependencies {
    // It's so useful for us because we are using invocations of methods that potentially consume some time
    // also in debug messages, but this plugin allows us to use lambdas that generate log messages
    // (mainly using string interpolation plugin) and these lambdas are evaluated only when needed
    // (i.e. when the given log level is active)
    "implementation"(lib("slf4j-lambda"))
  }
}

fun Project.slf4jTestImpl() {
  // We only need to provide an SLF4J implementation in the contexts which depend on the plugin
  // but don't depend on IntelliJ.
  // In our case, that's solely the tests of certain backend modules.
  // In other contexts that require an SLF4J implementation (buildPlugin, runIde, UI tests),
  // an SLF4J implementation is provided by IntelliJ.
  // Note that we don't need to agree the SLF4J implementation version here with slf4j-api version
  // pulled in by our dependencies (like JGit)
  // since the latter is excluded (see the comment to `exclude group: 'org.slf4j'` for more nuances).
  // The below dependency provides both slf4j-api and an implementation, both already in the same version.
  // Global exclusion on slf4j-api does NOT apply to tests since it's only limited to
  // `runtimeClasspath` configuration.
  dependencies {
    "testRuntimeOnly"(lib("slf4j-simple"))
  }
}

fun Project.vavr() {
  dependencies {
    // Unlike any other current dependency, Vavr classes are very likely to end up in binary
    // interface of the depending subproject,
    // hence it's better to just treat Vavr as an `api` and not `implementation` dependency by default.
    "api"(lib("vavr"))
  }
}
