import com.virtuslab.gitmachete.buildsrc.*

plugins {
  alias(libs.plugins.jetbrains.grammarkit)
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
val grammarJavaPackagePath = grammarJavaPackage.replace(".", "/")

val additionalSourceDirs = listOf(generatedParserJavaSourcesRoot, generatedLexerJavaSourcesRoot)

sourceSets["main"].java { srcDir(additionalSourceDirs) }

tasks {
  generateParser {
    sourceFile.set(file("$grammarSourcesRoot/Machete.bnf"))
    targetRootOutputDir.set(file(generatedParserJavaSourcesRoot))
    pathToParser.set("/$grammarJavaPackagePath/MacheteGeneratedParser.java")
    pathToPsiRoot.set("/$grammarJavaPackagePath/")
    purgeOldFiles.set(false)

    val platformLibs = intellijPlatform.platformPath.resolve("lib")
    classpath += fileTree(platformLibs) {
      include("*.jar")
    }
  }

  generateLexer {
    dependsOn(generateParser)

    sourceFile.set(file("$grammarSourcesRoot/Machete.flex"))
    targetOutputDir.set(file("$generatedLexerJavaSourcesRoot/$grammarJavaPackagePath/"))
    purgeOldFiles.set(false)
  }

  compileJava {
    dependsOn(generateLexer)
  }
}

checkerFramework {
  val grammarPackageRegex = grammarJavaPackage.replace(".", "\\.") // replace all literal `.` with `\.`
  extraJavacArgs.add("-AskipDefs=^${grammarPackageRegex}\\.MacheteGenerated.*\$")
}
