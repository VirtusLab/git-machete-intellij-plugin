import java.io.File

rootProject.name = "git-machete-intellij-plugin"

// Plugin resolution for this build (including buildSrc's `plugins { }`). See buildSrc/settings.gradle.kts too.
//
// Optional corporate proxy: repo-root `gradle-local.properties` (gitignored), key `mavenProxyUrl`.
// Without `~/.gradle/init.gradle` rewriting Central, you need that file on networks that require the proxy.
//
// If plugin resolution still hits repo.maven.apache.org directly and fails, set in ~/.gradle/gradle.properties:
//   includePublicMavenReposForPlugins=false
pluginManagement {
  repositories {
    mavenLocal()
    val f = File(settingsDir, "gradle-local.properties")
    if (f.isFile) {
      val url = java.util.Properties().apply { f.reader().use { load(it) } }
        .getProperty("mavenProxyUrl")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
      if (url != null) {
        maven(url = uri(url))
      }
    }
    if (providers.gradleProperty("includePublicMavenReposForPlugins").orNull != "false") {
      gradlePluginPortal()
      mavenCentral()
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
