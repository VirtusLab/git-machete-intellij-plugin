import com.virtuslab.gitmachete.buildsrc.*
import org.checkerframework.gradle.plugin.CheckerFrameworkExtension
import org.jetbrains.grammarkit.GrammarKitPlugin
import org.jetbrains.grammarkit.tasks.*

buildscript {
  repositories {
    gradlePluginPortal()
  }
  dependencies {
    classpath(libs.pluginPackages.jetbrains.grammarkit)
  }
}

apply<GrammarKitPlugin>()

dependencies {
  api(project(":qual"))
  implementation(project(":binding"))
  implementation(project(":frontend:base"))
  implementation(project(":frontend:resourcebundles"))
}

addIntellijToCompileClasspath(withGit4Idea = true)
lombok()
vavr()

applyI18nFormatterAndTaintingCheckers()

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
  tasks.withType<GenerateParserTask> {
    source.set("$grammarSourcesRoot/Machete.bnf")
    targetRoot.set(generatedParserJavaSourcesRoot)
    pathToParser.set("/$grammarJavaPackagePath/MacheteGeneratedParser.java")
    pathToPsiRoot.set("/$grammarJavaPackagePath/")
    purgeOldFiles.set(false)
  }

val generateMacheteLexer =
  tasks.withType<GenerateLexerTask> {
    dependsOn(generateMacheteParser)

    source.set("$grammarSourcesRoot/Machete.flex")
    targetDir.set("$generatedLexerJavaSourcesRoot/$grammarJavaPackagePath/")
    targetClass.set("MacheteGeneratedLexer")
    purgeOldFiles.set(false)
  }

tasks.withType<JavaCompile> { dependsOn(generateMacheteLexer) }

configure<CheckerFrameworkExtension> {
  val grammarPackageRegex = grammarJavaPackage.replace(".", "\\.") // replace all literal `.` with `\.`
  extraJavacArgs.add("-AskipDefs=^${grammarPackageRegex}\\.MacheteGenerated.*\$")
}
