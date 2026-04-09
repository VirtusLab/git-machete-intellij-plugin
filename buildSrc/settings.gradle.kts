import java.io.File

// buildSrc is a separate settings build: root pluginManagement does not apply here (gradle#29610).

rootProject.name = "buildSrc"

pluginManagement {
  repositories {
    mavenLocal()
    val f = File(settingsDir.parentFile, "gradle-local.properties")
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
