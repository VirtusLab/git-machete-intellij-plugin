package com.virtuslab.gitmachete.buildsrc

import org.checkerframework.gradle.plugin.CheckerFrameworkExtension
import org.checkerframework.gradle.plugin.CheckerFrameworkPlugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.*

fun Project.configureCheckerFramework() {
  apply<CheckerFrameworkPlugin>()
  configure<CheckerFrameworkExtension> {
    excludeTests = true
    checkers =
      mutableListOf(
        "org.checkerframework.checker.nullness.NullnessChecker",
      )

    val shouldRunAllCheckers: Boolean by rootProject.extra

    if (shouldRunAllCheckers) {
      // The experience shows that the below checkers are just rarely failing, as compared to GuiEffect/Nullness.
      // Hence, they're only applied in CI, or locally only if a dedicated Gradle project property is set.
      checkers.addAll(
        listOf(
          "org.checkerframework.checker.index.IndexChecker",
          "org.checkerframework.checker.interning.InterningChecker",
          "org.checkerframework.checker.optional.OptionalChecker",
        ),
      )
    }
    val suppressedWarnings = listOf(
      "allcheckers:annotation",
      "allcheckers:type.anno.before.decl.anno",
      "allcheckers:type.anno.before.modifier",
      "allcheckers:type.checking.not.run",
      "guieffect:effects.redundant.uitype",
    )
    extraJavacArgs =
      mutableListOf(
        "-AassumeAssertionsAreEnabled",
        "-AinvariantArrays",
        "-Alint=cast:redundant,cast:unsafe",
        "-ArequirePrefixInWarningSuppressions",
        "-AshowSuppressWarningsStrings",
        "-Astubs=${rootProject.extra.get("configCheckerDirectory")}",
        // The `-AstubWarnIfNotFoundIgnoresClasses` flag is required since Checker 3.14.0,
        // the version since which `-AstubWarnIfNotFound` is assumed to be true for custom stub files.
        // Without this flag, we would end up with a lot of errors in subprojects where any of
        // the stubbed libraries is NOT on the classpath:
        // e.g. compilation of a subproject where Vavr is NOT a dependency would fail
        // since for simplicity, we're providing the same set of stubs to Checker in each subproject
        // (`$rootDir/config/checker/`, which includes e.g. Vavr).
        "-AstubWarnIfNotFoundIgnoresClasses",
        "-AsuppressWarnings=${suppressedWarnings.joinToString(",")}",
      )

    checker()
  }
}

fun Project.applyAliasingChecker() {
  val shouldRunAllCheckers: Boolean by rootProject.extra

  if (shouldRunAllCheckers) {
    configure<CheckerFrameworkExtension> {
      checkers.add("org.checkerframework.common.aliasing.AliasingChecker")
    }
  }
}

fun Project.applyGuiEffectChecker() {
  // GuiEffectChecker is enabled regardless of `CI` env var/`runAllCheckers` Gradle project property.
  // As for now, we only apply this checker in frontend modules.
  // Technically, UI thread handling errors can happen outside them as well,
  // but the risk is minuscule and not worth the extra computational burden in every single build.
  // This might change, however, if/when Checker Framework adds @Heavyweight annotation
  // (https://github.com/typetools/checker-framework/issues/3253).
  configure<CheckerFrameworkExtension> {
    checkers.add("org.checkerframework.checker.guieffect.GuiEffectChecker")
  }
}

fun Project.applyI18nFormatterAndTaintingCheckers() {
  // I18nFormatterChecker and TaintingChecker, like GuiEffectChecker and NullnessChecker, are enabled
  // regardless of `CI` env var/`runAllCheckers` Gradle project property.
  configure<CheckerFrameworkExtension> {
    checkers.addAll(
      listOf(
        "org.checkerframework.checker.i18nformatter.I18nFormatterChecker",
        "org.checkerframework.checker.tainting.TaintingChecker",
      ),
    )
    extraJavacArgs.add("-Abundlenames=GitMacheteBundle")
  }

  // Apparently, I18nFormatterChecker doesn't see resource bundles in its classpath unless they're
  // defined in a separate module.
  dependencies {
    "checkerFramework"(project(":frontend:resourcebundles"))
  }
}

fun Project.applySubtypingChecker() {
  val shouldRunAllCheckers: Boolean by rootProject.extra

  if (shouldRunAllCheckers) {
    dependencies {
      "checkerFramework"(project(":qual"))
    }
    configure<CheckerFrameworkExtension> {
      checkers.add("org.checkerframework.common.subtyping.SubtypingChecker")

      val javaPlugin = project(":qual").extensions.getByType<JavaPluginExtension>()
      val mainSourceSet = javaPlugin.sourceSets["main"]
      val qualClassDir = mainSourceSet.output.classesDirs.asPath

      extraJavacArgs.add("-AqualDirs=$qualClassDir")
    }
  }
}
