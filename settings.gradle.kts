import java.io.File

rootProject.name = "git-machete-intellij-plugin"

// Plugin resolution for this build (including buildSrc's `plugins { }`). See buildSrc/settings.gradle.kts too.
//
// Optional corporate proxy: repo-root `gradle-local.properties` (gitignored), key `mavenProxyUrl`.
// When set, plugin resolution uses that mirror only (no mavenCentral() here); otherwise mavenCentral().
//
// If plugin resolution still hits repo.maven.apache.org directly and fails, set in ~/.gradle/gradle.properties:
//   includePublicMavenReposForPlugins=false
pluginManagement {
  repositories {
    mavenLocal()
    val mavenProxyUrl = run {
      val f = File(settingsDir, "gradle-local.properties")
      if (!f.isFile) {
        return@run null
      }
      java.util.Properties().apply { f.reader().use { load(it) } }
        .getProperty("mavenProxyUrl")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    }
    if (mavenProxyUrl != null) {
      maven(url = uri(mavenProxyUrl))
    }
    if (providers.gradleProperty("includePublicMavenReposForPlugins").orNull != "false") {
      gradlePluginPortal()
      if (mavenProxyUrl == null) {
        mavenCentral()
      }
    }
  }
}

// Note: please keep the projects in a topological order
include(
  "qual",
  "testCommon",
  "gitCore:api",
  "gitCore:jGit",
  "branchLayout:api",
  "branchLayout:impl",
  "backend:api",
  "backend:impl",
  "frontend:base",
  "frontend:file",
  "frontend:graph:api",
  "frontend:graph:impl",
  "frontend:ui:api",
  "frontend:ui:impl",
  "frontend:actions",
)
