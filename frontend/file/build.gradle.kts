import com.virtuslab.gitmachete.buildsrc.*

plugins {
  id("org.jetbrains.intellij.platform.grammarkit")
}

dependencies {
  api(project(":qual"))
  implementation(project(":branchLayout:api"))
  implementation(project(":frontend:base"))
}

lombok()
slf4jLambdaApi()
vavr()

applyI18nFormatterAndTaintingCheckers()

val grammarSourcesRoot = "src/main/grammar"
// Outputs of these two tasks canNOT go into the same directory,
// as Gradle doesn't support caching of output directories when more than one task writes.
// Let's pick non-overlapping directories for the outputs instead.
val generatedParserJavaSourcesRoot = "build/generated/parser"
val generatedLexerJavaSourcesRoot = "build/generated/lexer"
val grammarJavaPackage = "com.virtuslab.gitmachete.frontend.file.grammar"

val additionalSourceDirs = listOf(generatedParserJavaSourcesRoot, generatedLexerJavaSourcesRoot)

sourceSets["main"].java { srcDir(additionalSourceDirs) }

tasks {
  generateParser {
    sourceFile.set(file("$grammarSourcesRoot/Machete.bnf"))
    // The output layout (parser class location, PSI root) is derived from the .bnf file's
    // `parserClass` and `psiPackage` attributes, so no further configuration is needed here.
    targetRootOutputDir.set(file(generatedParserJavaSourcesRoot))
  }

  generateLexer {
    dependsOn(generateParser)

    sourceFile.set(file("$grammarSourcesRoot/Machete.flex"))
    // The output file is placed in a subdirectory matching the `package` declared in the .flex file.
    targetRootOutputDir.set(file(generatedLexerJavaSourcesRoot))
  }

  compileJava {
    dependsOn(generateLexer)
  }
}

checkerFramework {
  val grammarPackageRegex = grammarJavaPackage.replace(".", "\\.") // replace all literal `.` with `\.`
  extraJavacArgs.add("-AskipDefs=^${grammarPackageRegex}\\.MacheteGenerated.*\$")
}
