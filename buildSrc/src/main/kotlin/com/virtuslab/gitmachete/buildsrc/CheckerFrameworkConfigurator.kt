package com.virtuslab.gitmachete.buildsrc

import org.checkerframework.gradle.plugin.CheckerFrameworkExtension
import org.checkerframework.gradle.plugin.CheckerFrameworkPlugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.*

//  TODO (#1004): Remove hardcoded dependencies and deprecated code
object CheckerFrameworkConfigurator {
  fun configure(project: Project) {
    project.apply<CheckerFrameworkPlugin>()
    project.configure<CheckerFrameworkExtension> {
      excludeTests = true
      checkers =
          mutableListOf(
              "org.checkerframework.checker.nullness.NullnessChecker",
          )

      val shouldRunAllCheckers: Boolean by project.rootProject.extra

      if (shouldRunAllCheckers) {
        // The experience shows that the below checkers are just rarely failing, as compared to
        // GuiEffect/Nullness.
        // Hence, they're only applied in CI, or locally only if a dedicated Gradle project property
        // is set.
        checkers.addAll(
            listOf(
                "org.checkerframework.checker.index.IndexChecker",
                "org.checkerframework.checker.interning.InterningChecker",
                "org.checkerframework.checker.optional.OptionalChecker",
            ))
      }
      extraJavacArgs =
          mutableListOf(
              "-AassumeAssertionsAreEnabled",
              "-AinvariantArrays",
              "-Alint=cast:redundant,cast:unsafe",
              "-ArequirePrefixInWarningSuppressions",
              "-AshowSuppressWarningsStrings",
              "-Astubs=${project.rootDir}/config/checker/",
              // The `-AstubWarnIfNotFoundIgnoresClasses` flag is required since Checker 3.14.0,
              // the version since which `-AstubWarnIfNotFound` is assumed to be true for custom
              // stub files.
              // Without this flag, we would end up with a lot of errors in subprojects where any of
              // the stubbed libraries is NOT on the classpath:
              // e.g. compilation of a subproject where Vavr is NOT a dependency would fail
              // since for simplicity, we're providing the same set of stubs to Checker in each
              // subproject
              // (`$rootDir/config/checker/`, which includes e.g. Vavr).
              "-AstubWarnIfNotFoundIgnoresClasses",
              "-AsuppressWarnings=allcheckers:type.anno.before.decl.anno,allcheckers:type.anno.before.modifier,allcheckers:type.checking.not.run,value:annotation",
          )

      project.dependencies {
        add("compileOnly", "org.checkerframework:checker-qual:3.22.2")
        add("checkerFramework", "org.checkerframework:checker:3.22.2")
      }
    }
  }

  fun applyAliasingChecker(project: Project) {
    val shouldRunAllCheckers: Boolean by project.rootProject.extra

    if (shouldRunAllCheckers) {
      project.configure<CheckerFrameworkExtension> {
        checkers.add("org.checkerframework.common.aliasing.AliasingChecker")
      }
    }
  }

  fun applyI18nFormatterAndTaintingCheckers(project: Project) {
    // I18nFormatterChecker and TaintingChecker, like GuiEffectChecker and NullnessChecker, are
    // enabled
    // regardless of `CI` env var/`runAllCheckers` Gradle project property.
    project.configure<CheckerFrameworkExtension> {
      checkers.addAll(
          listOf(
              "org.checkerframework.checker.i18nformatter.I18nFormatterChecker",
              "org.checkerframework.checker.tainting.TaintingChecker"))
      extraJavacArgs.add("-Abundlenames=GitMacheteBundle")
    }

    // Apparently, I18nFormatterChecker doesn't see resource bundles in its classpath unless they're
    // defined in a separate module.
    project.dependencies { add("checkerFramework", project(":frontend:resourcebundles")) }
  }

  fun applySubtypingChecker(project: Project) {
    val shouldRunAllCheckers: Boolean by project.rootProject.extra

    if (shouldRunAllCheckers) {
      project.dependencies { add("checkerFramework", project(":qual")) }
      project.configure<CheckerFrameworkExtension> {
        checkers.add("org.checkerframework.common.subtyping.SubtypingChecker")

        val javaPlugin: JavaPluginConvention =
            project.project(":qual").getConvention().getPlugin(JavaPluginConvention::class.java)
        val mainSourceSet: SourceSet = javaPlugin.sourceSets.getByName("main")
        val qualClassDir = mainSourceSet.output.classesDirs.asPath

        extraJavacArgs.add("-ASubtypingChecker_qualDirs=${qualClassDir}")
      }
    }
  }
}
