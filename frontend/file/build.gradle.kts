import com.virtuslab.gitmachete.buildsrc.BuildUtils
import com.virtuslab.gitmachete.buildsrc.CheckerFrameworkConfigurator
import org.checkerframework.gradle.plugin.CheckerFrameworkExtension
import org.jetbrains.grammarkit.tasks.*

plugins { alias(libs.plugins.jetbrains.grammarkit) }

dependencies {
  api(project(":qual"))
  implementation(project(":frontend:base"))
  implementation(project(":frontend:icons"))
  implementation(project(":frontend:resourcebundles"))
}

BuildUtils.lombok(project)
BuildUtils.vavr(project)
BuildUtils.addIntellijToCompileClasspath(project, withGit4Idea = true)

CheckerFrameworkConfigurator.applyI18nFormatterAndTaintingCheckers(project)

val grammarSourcesRoot = "src/main/grammar"
// Outputs of these two tasks canNOT go into the same directory,
// as Gradle doesn't support caching of output directories when more than one task writes.
// Let's pick non-overlapping directories for the outputs instead.
val generatedParserJavaSourcesRoot = "build/generated/parser"
val generatedLexerJavaSourcesRoot = "build/generated/lexer"
val grammarJavaPackage = "com.virtuslab.gitmachete.frontend.file.grammar"
val grammarJavaPackagePath = grammarJavaPackage.replace(".", "/")

val additionalSourceDirs = listOf(generatedParserJavaSourcesRoot, generatedLexerJavaSourcesRoot)

sourceSets["main"].java { srcDir(additionalSourceDirs) }

val generateMacheteParser =
    tasks.withType<GenerateParserTask>() {
      // See https://github.com/JetBrains/gradle-grammar-kit-plugin/issues/89
      outputs.cacheIf { true }

      source.set("$grammarSourcesRoot/Machete.bnf")
      targetRoot.set(generatedParserJavaSourcesRoot)
      pathToParser.set("/$grammarJavaPackagePath/MacheteGeneratedParser.java")
      pathToPsiRoot.set("/$grammarJavaPackagePath/")
      purgeOldFiles.set(false)
    }

val generateMacheteLexer =
    tasks.withType<GenerateLexerTask>() {
      outputs.cacheIf { true }

      dependsOn(generateMacheteParser)

      source.set("$grammarSourcesRoot/Machete.flex")
      targetDir.set("$generatedLexerJavaSourcesRoot/$grammarJavaPackagePath/")
      targetClass.set("MacheteGeneratedLexer")
      purgeOldFiles.set(false)
    }

tasks.withType<JavaCompile>() { dependsOn(generateMacheteLexer) }

configure<CheckerFrameworkExtension> {
  val grammarPackageRegex =
      grammarJavaPackage.replace(".", "\\.") // replace all literal `.` with `\.`
  val newExtraJavacArgs = extraJavacArgs.toMutableList()
  newExtraJavacArgs.add("-AskipDefs=^${grammarPackageRegex}\\.MacheteGenerated.*\$")
  extraJavacArgs = newExtraJavacArgs
}
