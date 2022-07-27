package com.virtuslab.gitmachete.buildsrc

import io.freefair.gradle.plugins.aspectj.AspectJPlugin
import org.checkerframework.gradle.plugin.CheckerFrameworkExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.*
import org.jetbrains.intellij.IntelliJPlugin
import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.intellij.tasks.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

object BuildUtils {
  fun applyKotlinConfig(project: Project) {
    project.tasks.withType<KotlinCompile> {
      //            TODO (#785): revert this setting
      //            kotlinOptions.allWarningsAsErrors = true

      // Supress the warnings about different version of Kotlin used for compilation
      // than bundled into the `buildTarget` version of IntelliJ.
      // For compilation we use the Kotlin version from the earliest support IntelliJ,
      // and as per https://kotlinlang.org/docs/components-stability.html,
      // code compiled against an older version of kotlin-stdlib should work
      // when a newer version of kotlin-stdlib is provided as a drop-in replacement.
      kotlinOptions.freeCompilerArgs += listOf("-Xskip-metadata-version-check")

      val javaMajorVersion: JavaVersion by project.rootProject.extra

      kotlinOptions.jvmTarget = javaMajorVersion.majorVersion
    }
  }

  fun addIntellijToCompileClasspath(project: Project, withGit4Idea: Boolean) {
    val tasksBefore = mutableListOf<Task>()
    tasksBefore.addAll(project.tasks)

    project.apply<IntelliJPlugin>()

    val tasksAfter = mutableListOf<Task>()
    tasksAfter.addAll(project.tasks)
    tasksAfter.removeAll(tasksBefore)
    // For the frontend subprojects we only use gradle-intellij-plugin to provide dependencies,
    // but don't want the associated tasks to be available; they should only be available in the
    // root project.
    tasksAfter.forEach { it.enabled = false }
    // The only task (as of gradle-intellij-plugin v1.7.0, at least) that needs to be enabled in all
    // IntelliJ-aware modules
    // is `classpathIndexCleanup`, to avoid caching issues caused by `classpath.index` file
    // showing up in build/classes/ and build/resources/ directories.
    // See https://github.com/JetBrains/gradle-intellij-plugin/issues/1039 for details.
    project.tasks.withType<ClasspathIndexCleanupTask> { enabled = true }

    project.configure<CheckerFrameworkExtension> {
      // Technically, UI thread handling errors can happen outside of the (mostly frontend) modules
      // that depend on IntelliJ,
      // but the risk is minuscule and not worth the extra computational burden in every single
      // build.
      // This might change, however, if/when Checker Framework adds @Heavyweight annotation
      // (https://github.com/typetools/checker-framework/issues/3253).
      checkers.add("org.checkerframework.checker.guieffect.GuiEffectChecker")
    }

    project.configure<IntelliJPluginExtension> { // or configure IntellijPluginExtension?
      version.set(IntellijVersionHelper.getInstance()["buildTarget"] as String)
      // No need to instrument Java classes with nullability assertions, we've got this covered much
      // better by Checker
      // (and we don't plan to expose any part of the plugin as an API for other plugins).
      instrumentCode.set(false)
      if (withGit4Idea) {
        plugins.set(listOf("git4idea"))
      }
    }
  }

  fun commonsIO(project: Project) {
    project.dependencies {
      val dependencyNotation = "commons-io:commons-io:2.11.0"
      add("implementation", dependencyNotation)
    }
  }

  fun ideProbe(project: Project) {
    project.repositories {
      // Needed for com.intellij.remoterobot:remote-robot
      maven {
        url = `java.net`.URI("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
      }
    }

    project.dependencies {
      add("uiTestImplementation", testFixtures(project(":testCommon")))
      add("uiTestImplementation", "org.virtuslab.ideprobe:junit-driver_2.13:0.36.1")
      add("uiTestImplementation", "org.virtuslab.ideprobe:robot-driver_2.13:0.36.1")

      // This is technically redundant (both since ide-probe pulls in scala-library anyway,
      // and since ide-probe is meant to use in src/uiTest code, not src/test code),
      // but apparently needed for IntelliJ to detect Scala SDK version in the project (it's
      // probably https://youtrack.jetbrains.com/issue/SCL-14310).
      add("testImplementation", "org.scala-lang:scala-library:2.13.8")
    }
  }

  fun jcabiAspects(project: Project) {
    project.apply<AspectJPlugin>()

    project.dependencies { add("aspect", "com.jcabi:jcabi-aspects:0.23.2") }
  }

  fun jetbrainsAnnotations(project: Project) {
    project.dependencies {
      val dependencyNotation = "org.jetbrains:annotations:23.0.0"
      add("compileOnly", dependencyNotation)
      add("testCompileOnly", dependencyNotation)
    }
  }

  fun jgit(project: Project) {
    project.dependencies {
      add("implementation", "org.eclipse.jgit:org.eclipse.jgit:6.2.0.202206071550-r")
    }
  }

  fun junit(project: Project) {
    project.dependencies { add("testImplementation", "junit:junit:4.13.2") }
  }

  fun lombok(project: Project) {
    project.dependencies {
      val dependencyNotation = "org.projectlombok:lombok:1.18.24"
      add("compileOnly", dependencyNotation)
      add("annotationProcessor", dependencyNotation)
      add("testCompileOnly", dependencyNotation)
      add("testAnnotationProcessor", dependencyNotation)
    }
  }

  fun powerMock(project: Project) {
    project.dependencies {
      add("testImplementation", "org.powermock:powermock-api-mockito2:2.0.9")
      add("testImplementation", "org.powermock:powermock-module-junit4:2.0.9")
    }
  }

  fun reflections(project: Project) {
    project.dependencies { add("implementation", "org.reflections:reflections:0.10.2") }
  }

  fun slf4jLambdaApi(project: Project) {
    project.dependencies {
      // It's so useful for us because we are using invocations of methods that potentially consume
      // some time
      // also in debug messages, but this plugin allows us to use lambdas that generate log messages
      // (mainly using string interpolation plugin) and these lambdas are evaluated only when needed
      // (i.e. when the given log level is active)
      add("implementation", "kr.pe.kwonnam.slf4j-lambda:slf4j-lambda-core:0.1")
    }
  }

  fun slf4jTestImpl(project: Project) {
    // We only need to provide an SLF4J implementation in the contexts which depend on the plugin
    // but don't depend on IntelliJ.
    // In our case, that's solely the tests of backend modules.
    // In other contexts that require an SLF4J implementation (buildPlugin, runIde, UI tests),
    // an SLF4J implementation is provided by IntelliJ.
    // Note that we don't need to agree the SLF4J implementation version here with slf4j-api version
    // pulled in by our dependencies (like JGit)
    // since the latter is excluded (see the comment to `exclude group: 'org.slf4j'` for more
    // nuances).
    // The below dependency provides both slf4j-api and an implementation, both already in the same
    // version.
    // Global exclusion on slf4j-api does NOT apply to tests since it's only limited to
    // `runtimeClasspath` configuration.
    project.dependencies { add("testRuntimeOnly", "org.slf4j:slf4j-simple:1.7.36") }
  }

  fun vavr(project: Project) {
    project.dependencies {
      // Unlike any other current dependency, Vavr classes are very likely to end up in binary
      // interface of the depending subproject,
      // hence it's better to just treat Vavr as an `api` and not `implementation` dependency by
      // default.
      add("api", "io.vavr:vavr:0.10.4")
    }
  }
}
